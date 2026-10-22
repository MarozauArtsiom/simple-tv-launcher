package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundCache
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** Home with the app menu open, and in move mode, compared with `design/mockups/screens/app-menu-*.png` and `favourites-move-*.png`. */
@OptIn(ExperimentalTestApi::class)
class HomeOverlaysScreenshotTest : TvScreenshotTest() {
    private val cache by lazy {
        BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined).also { c ->
            runBlocking { Themes.all.forEach { c.load(it.background) } }
        }
    }
    private val art = FakeArt()
    private val threeThemes by lazy { listOf("midnight", "sky", "valley").map(Themes::byKey) }

    private var menuFor by mutableStateOf<AppMenuState?>(null)
    private var moveKey by mutableStateOf<ItemKey?>(null)

    @Test fun menuOnAFavourite() {
        val tile = HomeFixtures.content.favourites.first()
        captureThemes("app-menu", threeThemes, setup = {
            menuFor = AppMenuState(tile, isFavourite = true, anchor = Rect(96f, 278f, 416f, 458f + 44f))
        }) { theme -> Screen(theme) }
    }

    @Test fun menuOnAGridTileFlipsAboveAndOffersToAddToFavourites() {
        val tile = HomeFixtures.content.all.first { it.label == "Disney+" }
        captureThemes("app-menu-grid", listOf(Themes.byKey("midnight")), setup = {
            composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
            composeRule.waitForIdle()
            menuFor = AppMenuState(tile, isFavourite = false, anchor = Rect(96f, 636f, 416f, 816f))
        }) { theme -> Screen(theme) }
    }

    @Test fun moveMode() {
        captureThemes("favourites-move", threeThemes, setup = { moveKey = HomeFixtures.key("YouTube") }) { theme -> Screen(theme) }
    }

    @Composable
    private fun Screen(theme: ThemeSpec) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, cache)
            HomeScreen(
                content = HomeFixtures.content, theme = theme, topBar = HomeFixtures.topBar, clock = { HomeFixtures.clock },
                art = art, resets = emptyFlow(), actions = HomeActions(),
                overlayOpen = menuFor != null, moveKey = moveKey,
            )
            menuFor?.let { AppMenu(it, theme, onAction = {}) }
        }
    }
}
