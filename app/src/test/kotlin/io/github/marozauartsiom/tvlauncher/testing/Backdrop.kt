package io.github.marozauartsiom.tvlauncher.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
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

/** The mockups' Home behind a panel, for panel screenshots. Build one per test class. */
class PanelBackdrop {
    private val cache = BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined).also { c ->
        runBlocking { Themes.all.forEach { c.load(it.background) } } // warm: no async work during capture
    }
    private val art = FakeArt()

    @Composable
    fun Over(theme: ThemeSpec, panel: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, cache)
            HomeScreen(
                content = HomeFixtures.content, theme = theme, topBar = HomeFixtures.topBar, clock = { HomeFixtures.clock },
                art = art, resets = emptyFlow(), actions = HomeActions(), overlayOpen = true,
            )
            panel()
        }
    }
}
