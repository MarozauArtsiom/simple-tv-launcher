package io.github.marozauartsiom.tvlauncher

import android.app.Application
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.launch

/** Keep this class nearly empty: it runs on every cold start before the first frame. */
class LauncherApp : Application() {
    val graph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        graph.crashGuard.install()
        graph.screenOn.register()
        // Keep a copy of the settings for code that answers system events (a signal on HDMI while another app is open).
        graph.appScope.launch { graph.settings.data.collect { graph.latestSettings = it } }
        // The system calls us when an HDMI input changes; nothing polls. Registered a moment after the first frame.
        Handler(Looper.getMainLooper()).postDelayed({ graph.hdmi.start() }, HDMI_START_DELAY_MS)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Keep the caches when merely going to another app (UI_HIDDEN), so Home reappears instantly;
        // free them when the system really needs memory (the process is a background candidate).
        if (level >= TRIM_BACKGROUND) { graph.backgrounds.clear(); graph.banners.clear() }
    }

    private companion object {
        const val TRIM_BACKGROUND = 40 // ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        const val HDMI_START_DELAY_MS = 2_000L
    }
}
