package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.system.SystemTile
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
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

/** The settings panel over Home, compared with `design/mockups/settings/settings-{midnight,sky,valley}.png`. */
class SettingsScreenshotTest : TvScreenshotTest() {
    private val cache by lazy {
        BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined).also { c ->
            runBlocking { Themes.all.forEach { c.load(it.background) } }
        }
    }
    private val art = FakeArt()
    private val memory = FocusMemory()

    @Test fun settingsInThreeThemes() {
        val themes = listOf("midnight", "sky", "valley").map(Themes::byKey)
        captureThemes("settings", themes) { theme -> Screen(theme) }
    }

    @Composable
    private fun Screen(theme: ThemeSpec) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, cache)
            HomeScreen(
                content = HomeFixtures.content, theme = theme, topBar = HomeFixtures.topBar, clock = { HomeFixtures.clock },
                art = art, resets = emptyFlow(), actions = HomeActions(), overlayOpen = true,
            )
            SettingsPanel(
                theme = theme,
                tiles = SystemTile.entries,
                values = SettingsFixtures.values.copy(theme = androidx.compose.ui.res.stringResource(theme.nameRes)),
                memory = memory,
                onTile = {}, onPage = {},
            )
        }
    }
}
