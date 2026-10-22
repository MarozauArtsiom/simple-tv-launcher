package io.github.marozauartsiom.tvlauncher.ui.weather

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherPlace
import io.github.marozauartsiom.tvlauncher.data.weather.Condition
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.home.TopBarState
import io.github.marozauartsiom.tvlauncher.ui.home.WeatherChip
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Test

/** The weather panel and the city search over Home, compared with `design/mockups/weather-panel.png` and `screens/weather-search-*.png`. */
@OptIn(ExperimentalTestApi::class)
class WeatherScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }
    private val threeThemes by lazy { listOf("midnight", "sky", "valley").map(Themes::byKey) }

    @Test fun panel() {
        captureThemes("weather-panel", threeThemes) { theme ->
            backdrop.Over(theme) { WeatherPanel(theme, WeatherUiFixtures.view, "ru", {}) }
        }
    }

    @Test fun panelInTheLongestLanguage() {
        captureLocales("weather-panel-locale", listOf("be", "fr")) { tag ->
            backdrop.Over(Themes.default) { WeatherPanel(Themes.default, WeatherUiFixtures.view, tag, {}) }
        }
    }

    @Test fun search() {
        val results = listOf(
            WeatherPlace("Брест", "Брестская область", "Беларусь", 52.1, 23.7),
            WeatherPlace("Брест", "Бретань", "Франция", 48.4, -4.5),
            WeatherPlace("Брестовица", "Брестская область", "Беларусь", 52.0, 23.8),
        )
        captureThemes("weather-search", threeThemes, setup = {
            composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) } // off the field, onto "Detect automatically"
            composeRule.waitForIdle()
        }) { theme ->
            backdrop.Over(theme) {
                CitySearchPage(theme, search = { results }, onAuto = {}, onPick = {}, initialQuery = "Брест")
            }
        }
    }

    @Test fun chipVariants() {
        val states = mapOf(
            "stale" to WeatherChip("—°", "Минск", "", Condition.Overcast, true),
            "night" to WeatherChip("−3°", "Минск", "", Condition.Clear, false),
            "rain" to WeatherChip("+7°", "Минск", "", Condition.Rain, true),
        )
        captureThemes("weather-chip", listOf(Themes.byKey("sky"))) { theme ->
            androidx.compose.foundation.layout.Column {
                states.values.forEach { chip ->
                    io.github.marozauartsiom.tvlauncher.ui.home.TopBar(
                        state = TopBarState(weather = chip, showIphone = false),
                        clock = { io.github.marozauartsiom.tvlauncher.ui.home.HomeFixtures.clock }, theme = theme,
                        onWeather = {}, onInputs = {}, onIphone = {}, onSettings = {},
                        focus = io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory(),
                    )
                }
            }
        }
    }
}
