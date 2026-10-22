package io.github.marozauartsiom.tvlauncher

import android.content.Context
import android.content.res.Resources
import android.os.Environment
import android.os.storage.StorageManager
import io.github.marozauartsiom.tvlauncher.data.apps.AppLauncher
import io.github.marozauartsiom.tvlauncher.data.apps.AppRepository
import io.github.marozauartsiom.tvlauncher.data.apps.BannerLoader
import io.github.marozauartsiom.tvlauncher.data.apps.PackageManagerSource
import io.github.marozauartsiom.tvlauncher.data.background.BackgroundImages
import io.github.marozauartsiom.tvlauncher.data.prefs.Settings
import io.github.marozauartsiom.tvlauncher.data.weather.GeoIp
import io.github.marozauartsiom.tvlauncher.data.weather.OpenMeteoClient
import io.github.marozauartsiom.tvlauncher.data.weather.UrlConnectionFetcher
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherRepository
import io.github.marozauartsiom.tvlauncher.i18n.LocaleController
import io.github.marozauartsiom.tvlauncher.system.CrashGuard
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
            roots = {
                context.getSystemService(StorageManager::class.java).storageVolumes
                    .filter { it.isRemovable && it.state == Environment.MEDIA_MOUNTED }
                    .mapNotNull { it.directory }
            },
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
    val launcher by lazy { AppLauncher(packageSource) { intent -> context.startActivity(intent) } }
}
