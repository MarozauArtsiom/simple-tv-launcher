package io.github.marozauartsiom.tvlauncher

import android.content.Context
import android.content.res.Resources
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import io.github.marozauartsiom.tvlauncher.data.apps.AppLauncher
import io.github.marozauartsiom.tvlauncher.data.apps.AppRepository
import io.github.marozauartsiom.tvlauncher.data.apps.BannerLoader
import io.github.marozauartsiom.tvlauncher.data.apps.PackageManagerSource
import io.github.marozauartsiom.tvlauncher.data.background.BackgroundImages
import io.github.marozauartsiom.tvlauncher.data.install.ApkScanner
import io.github.marozauartsiom.tvlauncher.data.install.InstallResult
import io.github.marozauartsiom.tvlauncher.data.install.PackageManagerInspector
import io.github.marozauartsiom.tvlauncher.data.install.SessionApkInstaller
import io.github.marozauartsiom.tvlauncher.data.install.UsbInstaller
import io.github.marozauartsiom.tvlauncher.system.InstallResultReceiver
import kotlinx.coroutines.flow.MutableSharedFlow
import io.github.marozauartsiom.tvlauncher.data.prefs.Settings
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.data.weather.GeoIp
import io.github.marozauartsiom.tvlauncher.data.weather.OpenMeteoClient
import io.github.marozauartsiom.tvlauncher.data.weather.UrlConnectionFetcher
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherRepository
import io.github.marozauartsiom.tvlauncher.i18n.LocaleController
import io.github.marozauartsiom.tvlauncher.system.CrashGuard
import io.github.marozauartsiom.tvlauncher.system.hdmi.HdmiController
import io.github.marozauartsiom.tvlauncher.system.hdmi.HdmiInput
import io.github.marozauartsiom.tvlauncher.system.hdmi.InputLauncher
import io.github.marozauartsiom.tvlauncher.system.hdmi.OverlayHost
import io.github.marozauartsiom.tvlauncher.system.hdmi.TvInputManagerSource
import io.github.marozauartsiom.tvlauncher.ui.hdmi.HdmiPrompt
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import io.github.marozauartsiom.tvlauncher.system.ScreenOnWatcher
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundCache
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hand-built dependency container (no DI framework: small app, no annotation processing). Everything
 * is lazy, so a cold start only creates what the first frame needs.
 */
class AppGraph(private val context: Context) {
    val crashGuard by lazy { CrashGuard(context) }
    val settings by lazy { Settings(context) }
    val backgrounds by lazy { BackgroundCache(context) }
    val locale by lazy { LocaleController(context) }
    val screenOn by lazy { ScreenOnWatcher(context) }

    /** Long-lived work (package events, snapshots). Idle unless something happens. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val packageSource by lazy { PackageManagerSource(context) }
    val apps by lazy { AppRepository(packageSource, File(context.filesDir, "apps.json"), context.packageName.removeSuffix(".debug"), appScope, Dispatchers.IO) }
    val banners by lazy { BannerLoader(packageSource) }
    /** Pictures on USB drives and the stored background. Only touched while the background picker is open. */
    val backgroundImages by lazy {
        BackgroundImages(
            roots = ::usbRoots,
            storeDir = File(context.filesDir, "backgrounds"),
        )
    }
    /** The only network code: Open-Meteo (forecast, city search) and the IP location services. Idle until the UI asks. */
    val openMeteo by lazy { OpenMeteoClient(UrlConnectionFetcher()) }
    val weather by lazy {
        WeatherRepository(
            settings = settings,
            client = openMeteo,
            geoIp = GeoIp(UrlConnectionFetcher(), openMeteo),
            languageTag = { LocaleController.effectiveTag(locale.savedChoice(), Resources.getSystem().configuration.locales[0]) },
        )
    }
    /** The latest settings, for code that reacts to system events and cannot wait for a flow (the HDMI signal). */
    @Volatile
    var latestSettings: SettingsData = SettingsData()

    val inputLauncher by lazy {
        InputLauncher(
            canHandle = { intent -> context.packageManager.resolveActivity(intent, 0) != null },
            start = { context.startActivity(it) },
        )
    }

    private var overlay: OverlayHost? = null

    /** HDMI: the system's callback, the decision where to ask, and the optional window over other apps. */
    val hdmi by lazy {
        HdmiController(
            inputs = TvInputManagerSource(context),
            settings = { latestSettings.hdmi },
            clock = System::currentTimeMillis,
            canDrawOverlays = { android.provider.Settings.canDrawOverlays(context) },
            showOverOtherApps = ::showHdmiOverOtherApps,
        )
    }

    private fun showHdmiOverOtherApps(input: HdmiInput) {
        Handler(Looper.getMainLooper()).post {
            val snapshot = latestSettings
            val theme = Themes.byId(snapshot.theme)
            val host = OverlayHost(LocaleController.localized(context, snapshot.language))
            overlay?.hide()
            overlay = host
            host.show { dismiss ->
                LauncherTheme(theme) {
                    HdmiPrompt(
                        theme = theme,
                        input = input,
                        onSwitch = { inputLauncher.open(input.id); hdmi.dismiss(input.id); dismiss() },
                        onDismiss = { hdmi.dismiss(input.id); dismiss() },
                    )
                }
            }
        }
    }

    /** Mounted removable volumes (USB drives). */
    private fun usbRoots(): List<File> = context.getSystemService(StorageManager::class.java).storageVolumes
        .filter { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
        .mapNotNull { it.directory }

    /** Outcomes of installations, from the system installer (see `InstallResultReceiver`). */
    val installEvents = MutableSharedFlow<InstallResult>(extraBufferCapacity = 8)

    /** The USB installer's state machine. Idle until the page asks for a refresh. */
    val usbInstaller by lazy {
        UsbInstaller(
            scanner = ApkScanner(::usbRoots),
            inspector = PackageManagerInspector(context),
            installer = SessionApkInstaller(context, InstallResultReceiver::class.java),
            hasStorageAccess = { Environment.isExternalStorageManager() },
            canInstallPackages = { context.packageManager.canRequestPackageInstalls() },
            results = installEvents,
            scope = appScope,
        )
    }

    val launcher by lazy { AppLauncher(packageSource) { intent -> context.startActivity(intent) } }
}
