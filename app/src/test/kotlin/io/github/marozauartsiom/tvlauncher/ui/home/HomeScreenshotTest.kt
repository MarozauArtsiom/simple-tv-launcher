package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.i18n.DateText
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundCache
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** Home with the content of `design/mockups/home.html`, compared by eye with `design/mockups/themes/<id>.png`. */
class HomeScreenshotTest : TvScreenshotTest() {
    private val cache by lazy {
        BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined).also { c ->
            runBlocking { Themes.all.forEach { c.load(it.background) } } // warm: no async work during capture
        }
    }

    @Test fun homeInAllThemes() {
        val art = FakeArt()
        captureThemes("home") { theme -> Home(theme, art, HomeFixtures.content, HomeFixtures.topBar, HomeFixtures.clock) }
    }

    @Test fun homeWithoutFavouritesInThreeThemes() {
        val art = FakeArt()
        val content = HomeLists.build(HomeFixtures.apps, SettingsData(), locale = java.util.Locale.ENGLISH)
        captureThemes("home-empty", listOf(Themes.byKey("midnight"), Themes.byKey("sky"), Themes.byKey("valley"))) { theme ->
            Home(theme, art, content, HomeFixtures.topBar, HomeFixtures.clock)
        }
    }

    @Test fun homeInLongestLanguages() {
        val art = FakeArt()
        val places = mapOf("be" to "Мінск", "fr" to "Minsk")
        captureLocales("home-locale", listOf("be", "fr")) { tag ->
            val clock = ClockText("20:41", DateText.format(LocalDate.of(2026, 10, 21), tag))
            val topBar = HomeFixtures.topBar.copy(weather = HomeFixtures.topBar.weather?.copy(place = places[tag]))
            Home(Themes.default, art, HomeFixtures.content, topBar, clock)
        }
    }

    @Composable
    private fun Home(theme: ThemeSpec, art: FakeArt, content: HomeContent, topBar: TopBarState, clock: ClockText) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, cache)
            HomeScreen(
                content = content, theme = theme, topBar = topBar, clock = { clock }, art = art,
                resets = emptyFlow(), actions = NoActions,
            )
        }
    }

    private companion object {
        val NoActions = HomeActions({}, {}, {}, {}, {}, {})
    }
}
