package io.github.marozauartsiom.tvlauncher.system.remote

import android.app.Activity

/** The launcher's activity while it is resumed, so the remote's D-pad keys can go straight into it. */
object LauncherInFront {
    @Volatile var activity: Activity? = null
}
