package io.github.marozauartsiom.tvlauncher.data.apps

/**
 * The iPhone button opens the AirPlay Server app (open source, receives the iPhone's screen mirroring), so
 * the receiver runs only when wanted. The launcher itself contains no AirPlay code.
 */
object Airplay {
    const val PACKAGE = "io.github.jqssun.airplay"

    /** True if the app was started; false means it is not installed (or refused), and the how-to card should be shown. */
    fun open(launcher: AppLauncher): Boolean = launcher.launch(PACKAGE) == LaunchResult.Started
}
