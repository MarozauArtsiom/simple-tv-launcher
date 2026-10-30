package io.github.marozauartsiom.tvlauncher.system.remote

import android.app.Activity

/**
 * The launcher's activity while it is resumed, so the remote's D-pad keys can go straight into it, and so the
 * "remote / AI" badge knows whether Home is on screen. [onChange] is told when it comes or goes (any thread).
 */
object LauncherInFront {
    @Volatile var activity: Activity? = null
        set(value) {
            val changed = (field != null) != (value != null)
            field = value
            if (changed) onChange()
        }

    @Volatile var onChange: () -> Unit = {}
}
