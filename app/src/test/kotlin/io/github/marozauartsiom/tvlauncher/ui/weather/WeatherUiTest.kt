package io.github.marozauartsiom.tvlauncher.ui.weather

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.hasSetTextAction
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherPlace
import io.github.marozauartsiom.tvlauncher.data.weather.CurrentWeather
import io.github.marozauartsiom.tvlauncher.data.weather.DayWeather
import io.github.marozauartsiom.tvlauncher.data.weather.HourWeather
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherData
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherView
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

object WeatherUiFixtures {
    /** The data of `design/mockups/weather-panel.html`. */
    val minsk = WeatherData(
        current = CurrentWeather(temperature = 7.2, feelsLike = 4.4, code = 2, isDay = false, windMs = 5.0),
        hours = listOf(
            HourWeather(LocalTime.of(21, 0), 6.0, 0, false), HourWeather(LocalTime.of(0, 0), 4.0, 3, false),
            HourWeather(LocalTime.of(3, 0), 3.0, 61, false), HourWeather(LocalTime.of(6, 0), 3.0, 3, false),
        ),
        days = listOf(
            DayWeather(LocalDate.of(2026, 10, 21), 2, 9.0, 3.0), DayWeather(LocalDate.of(2026, 10, 22), 61, 8.0, 4.0),
            DayWeather(LocalDate.of(2026, 10, 23), 3, 7.0, 2.0), DayWeather(LocalDate.of(2026, 10, 24), 0, 10.0, 1.0),
            DayWeather(LocalDate.of(2026, 10, 25), 2, 11.0, 3.0),
        ),
        sunrise = LocalTime.of(7, 5), sunset = LocalTime.of(18, 12),
    )
    val view = WeatherView("Минск", minsk, stale = false)
}

class WeatherTextTest {
    @Test fun temperaturesHaveASignAndARealMinus() {
        assertEquals("+7°", WeatherText.temperature(7.2))
        assertEquals("+8°", WeatherText.temperature(7.5))
        assertEquals("−3°", WeatherText.temperature(-3.4))
        assertEquals("0°", WeatherText.temperature(0.3))
        assertEquals("0°", WeatherText.temperature(-0.4)) // never "−0°"
    }

    @Test fun weekdaysAreShortAndCapitalisedInEachLanguage() {
        val thursday = LocalDate.of(2026, 10, 22)
        assertEquals("Чт", WeatherText.weekday(thursday, "ru"))
        assertEquals("Thu", WeatherText.weekday(thursday, "en"))
        assertEquals("Do", WeatherText.weekday(thursday, "de"))
        assertTrue(WeatherText.weekday(thursday, "pl").first().isUpperCase())
        assertTrue(WeatherText.weekday(thursday, "fr").first().isUpperCase())
        assertTrue(WeatherText.weekday(thursday, "be").first().isUpperCase())
    }

    @Test fun windIsWholeMetresPerSecond() = assertEquals("5", WeatherText.wind(4.6))
}

@OptIn(ExperimentalTestApi::class)
class WeatherPanelTest : TvScreenshotTest() {
    private var changeCity = 0

    private fun show(view: WeatherView = WeatherUiFixtures.view) {
        composeRule.setContent {
            LauncherTheme(Themes.default) { WeatherPanel(Themes.default, view, "en", { changeCity++ }) }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    @Test fun showsTheCityNowHoursAndDays() {
        show()
        composeRule.onNodeWithText("Минск").assertExists()
        composeRule.onNodeWithText("+7°").assertExists()
        composeRule.onNodeWithText("Partly cloudy").assertExists()
        composeRule.onNodeWithText("Feels like +4° · wind 5 m/s").assertExists()
        composeRule.onNodeWithText("21:00").assertExists()
        composeRule.onNodeWithText("06:00").assertExists()
        composeRule.onNodeWithText("Today").assertExists()
        composeRule.onNodeWithText("Fri").assertExists() // 22 October 2026 is a Thursday: the second day
    }

    @Test fun focusStartsOnChangeCityAndOkOpensTheSearch() {
        show()
        composeRule.onNodeWithText("Change city").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, changeCity)
    }

    @Test fun withoutDataItSaysSoAndStillOffersTheButton() {
        show(WeatherView("Минск", null, stale = true))
        composeRule.onNodeWithText("No data").assertExists()
        composeRule.onNodeWithText("Change city").assertIsFocused()
    }

    @Test fun theOwnersRequestHoldsNoTextAboutSourceOrLocationMode() {
        show()
        val all = composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode().let { root ->
            fun texts(node: androidx.compose.ui.semantics.SemanticsNode): List<String> =
                (if (androidx.compose.ui.semantics.SemanticsProperties.Text in node.config) node.config[androidx.compose.ui.semantics.SemanticsProperties.Text].map { it.text } else emptyList()) + node.children.flatMap(::texts)
            texts(root).joinToString(" ").lowercase()
        }
        listOf("open-meteo", "meteo", "ip ", "automatic", "detected", "source", "cc by").forEach { assertTrue(it, !all.contains(it)) }
    }
}

@OptIn(ExperimentalTestApi::class)
class CitySearchPageTest : TvScreenshotTest() {
    private val queries = mutableListOf<String>()
    private val picked = mutableListOf<WeatherPlace>()
    private var auto = 0
    private val brest = WeatherPlace("Брест", "Брестская Область", "Беларусь", 52.1, 23.7)
    private val brestFr = WeatherPlace("Брест", "Бретань", "Франция", 48.4, -4.5)

    private fun show(results: List<WeatherPlace> = listOf(brest, brestFr)) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                CitySearchPage(Themes.default, search = { q -> queries += q; results }, onAuto = { auto++ }, onPick = { picked += it })
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    private fun type(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
        composeRule.mainClock.advanceTimeBy(500) // past the debounce
        composeRule.waitForIdle()
    }

    @Test fun startsOnTheFieldWithAutoDetectBelow() {
        show()
        composeRule.onNodeWithText("Detect automatically").assertExists()
        press(Key.DirectionDown)
        composeRule.onNodeWithText("Detect automatically").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, auto)
    }

    @Test fun nothingIsSearchedBeforeTheOwnerTypesAtLeastTwoLetters() {
        show()
        composeRule.mainClock.advanceTimeBy(1000)
        assertEquals(emptyList<String>(), queries)
        type("Б")
        assertEquals(emptyList<String>(), queries)
    }

    @Test fun typingSearchesAfterAPauseAndShowsRegionAndCountry() {
        show()
        type("Брест")
        assertEquals(listOf("Брест"), queries) // one request for the whole word, not one per letter
        composeRule.onNodeWithText("Брестская Область, Беларусь").assertExists()
        composeRule.onNodeWithText("Бретань, Франция").assertExists()
    }

    @Test fun okOnAResultPicksThatPlace() {
        show()
        type("Брест")
        press(Key.DirectionDown, 2) // Detect automatically, then the first result
        press(Key.Enter)
        assertEquals(listOf(brest), picked)
    }

    @Test fun noMatchesSaysSo() {
        show(results = emptyList())
        type("Zzzzz")
        composeRule.onNodeWithText("Nothing found").assertExists()
    }

    @Test fun atMostThreeResultsAreListed() {
        show(results = (1..8).map { WeatherPlace("Town $it", null, "Land", it.toDouble(), 0.0) })
        type("Town")
        composeRule.onNodeWithText("Town 3").assertExists()
        composeRule.onNodeWithText("Town 4").assertDoesNotExist()
    }
}
