package io.github.marozauartsiom.tvlauncher.ui.shortcuts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.data.prefs.SaveResult
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.home.FakeArt
import io.github.marozauartsiom.tvlauncher.ui.home.HomeActions
import io.github.marozauartsiom.tvlauncher.ui.home.HomeFixtures
import io.github.marozauartsiom.tvlauncher.ui.home.HomeScreen
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundCache
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** The shortcuts list and editor over Home, compared with `design/mockups/screens/shortcuts-{list,edit}-*.png`. */
@OptIn(ExperimentalTestApi::class)
class ShortcutsScreenshotTest : TvScreenshotTest() {
    private val cache by lazy {
        BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined).also { c ->
            runBlocking { Themes.all.forEach { c.load(it.background) } }
        }
    }
    private val art = FakeArt()
    private val threeThemes by lazy { listOf("midnight", "sky", "valley").map(Themes::byKey) }
    private val shortcuts = listOf(
        Shortcut("n", "Новости", "https://news.example.com"),
        Shortcut("k", "Кино онлайн", "https://kino.example.org"),
        Shortcut("p", "Погода на неделю", "https://weather.example.net"),
    )

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    @Test fun list() = captureThemes("shortcuts-list", threeThemes) { theme -> Screen(theme) }

    @Test fun editor() = captureThemes("shortcuts-edit", threeThemes, setup = {
        press(Key.DirectionDown); press(Key.Enter) // open "Новости"
        press(Key.DirectionDown)                   // the address field, as in the mockup
    }) { theme -> Screen(theme) }

    @Composable
    private fun Screen(theme: ThemeSpec) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, cache)
            HomeScreen(
                content = HomeFixtures.content, theme = theme, topBar = HomeFixtures.topBar, clock = { HomeFixtures.clock },
                art = art, resets = emptyFlow(), actions = HomeActions(), overlayOpen = true,
            )
            ShortcutsPage(
                theme = theme,
                shortcuts = shortcuts,
                onSave = { _, _, _ -> SaveResult.Invalid },
                onDelete = {},
            )
        }
    }
}
