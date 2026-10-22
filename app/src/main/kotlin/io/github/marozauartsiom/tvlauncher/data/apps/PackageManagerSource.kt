package io.github.marozauartsiom.tvlauncher.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.UserHandle

class PackageManagerSource(private val context: Context) : PackageSource {
    private val pm: PackageManager get() = context.packageManager

    override fun queryLaunchable(): List<TvApp> {
        fun query(category: String, leanback: Boolean) =
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).map { info ->
                val ai = info.activityInfo
                TvApp(ai.packageName, ai.name, info.loadLabel(pm).toString(), leanback)
            }
        // Two PackageManager calls (binder IPC): keep this off the main thread.
        return query(Intent.CATEGORY_LEANBACK_LAUNCHER, true) + query(Intent.CATEGORY_LAUNCHER, false)
    }

    override fun loadArt(app: TvApp): LoadedArt? = runCatching {
        val component = android.content.ComponentName(app.packageName, app.activityClass)
        val info = pm.getActivityInfo(component, 0)
        info.loadBanner(pm)?.let { return LoadedArt(it, isBanner = true) }
        pm.getApplicationBanner(app.packageName)?.let { return LoadedArt(it, isBanner = true) }
        LoadedArt(info.loadIcon(pm), isBanner = false)
    }.getOrNull()

    override fun launchIntent(packageName: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(packageName) ?: pm.getLaunchIntentForPackage(packageName)

    override fun registerChanges(onChange: () -> Unit): () -> Unit {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return {}
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String, user: UserHandle) = onChange()
            override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange()
            override fun onPackageChanged(packageName: String, user: UserHandle) = onChange()
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChange()
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        return { launcherApps.unregisterCallback(callback) }
    }
}
