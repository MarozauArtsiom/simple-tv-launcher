package io.github.marozauartsiom.tvlauncher

import android.app.Application

/** Keep this class nearly empty: it runs on every cold start before the first frame. */
class LauncherApp : Application() {
    val graph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        graph.crashGuard.install()
        graph.screenOn.register()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Keep the caches when merely going to another app (UI_HIDDEN), so Home reappears instantly;
        // free them when the system really needs memory (the process is a background candidate).
        if (level >= TRIM_BACKGROUND) { graph.backgrounds.clear(); graph.banners.clear() }
    }

    private companion object {
        const val TRIM_BACKGROUND = 40 // ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
    }
}
