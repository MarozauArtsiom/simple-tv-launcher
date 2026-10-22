package io.github.marozauartsiom.tvlauncher

import android.content.Context
import io.github.marozauartsiom.tvlauncher.data.prefs.Settings
import io.github.marozauartsiom.tvlauncher.i18n.LocaleController
import io.github.marozauartsiom.tvlauncher.system.CrashGuard
import io.github.marozauartsiom.tvlauncher.system.ScreenOnWatcher
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundCache

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
}
