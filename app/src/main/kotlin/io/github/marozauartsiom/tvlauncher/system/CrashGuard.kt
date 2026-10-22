package io.github.marozauartsiom.tvlauncher.system

import android.content.Context

/**
 * The launcher may be the only Home on the TV, so a crash loop would leave a black screen.
 * This remembers recent crashes; after two within ten minutes the next start runs in safe mode
 * (minimal UI: app grid and Settings, no weather or HDMI watching).
 */
class CrashGuard(context: Context, private val clock: () -> Long = System::currentTimeMillis) {
    private val prefs = context.applicationContext.getSharedPreferences("crash_guard", Context.MODE_PRIVATE)

    /** Records crashes of any thread, then lets the previous handler do its normal work. */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { recordCrash() }
            previous?.uncaughtException(thread, error)
        }
    }

    fun recordCrash() {
        val now = clock()
        val recent = if (now - prefs.getLong(LAST, 0) <= WINDOW_MS) prefs.getInt(COUNT, 0) else 0
        prefs.edit().putLong(LAST, now).putInt(COUNT, recent + 1).commit() // commit: the process is dying
    }

    fun safeMode(): Boolean =
        prefs.getInt(COUNT, 0) >= THRESHOLD && clock() - prefs.getLong(LAST, 0) <= WINDOW_MS

    /** Call after the launcher has run fine for a while. */
    fun markStable() = prefs.edit().putInt(COUNT, 0).apply()

    companion object {
        private const val COUNT = "count"
        private const val LAST = "last"
        const val WINDOW_MS = 10 * 60 * 1000L
        const val THRESHOLD = 2
        const val STABLE_AFTER_MS = 30_000L
    }
}
