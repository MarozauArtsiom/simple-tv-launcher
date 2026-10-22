package io.github.marozauartsiom.tvlauncher

import android.app.Application
import android.content.ComponentCallbacks2

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
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) graph.backgrounds.clear()
    }
}
