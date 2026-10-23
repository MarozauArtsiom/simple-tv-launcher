package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.data.prefs.SunTimes
import io.github.marozauartsiom.tvlauncher.data.prefs.ThemeSchedule
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ThemePickerPageTest : TvScreenshotTest() {
    private val previews = mutableListOf<ThemeId?>()
    private val chosen = mutableListOf<ThemeId>()
    private var showPage by mutableStateOf(true)
    private var scheduleOpened = 0

    private fun show(saved: ThemeId = ThemeId.Midnight) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                if (showPage) ThemePickerPage(
                    theme = Themes.default, title = "Theme", chosen = saved,
                    onPreview = { previews += it }, onChoose = { chosen += it }, memory = FocusMemory(),
                    scheduleValue = "Off", onOpenSchedule = { scheduleOpened++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    @Test fun focusStartsOnTheSavedThemeAndItHasTheCheck() {
        show(saved = ThemeId.Sky)
        composeRule.onNodeWithContentDescription("Sky").assertIsFocused()
        composeRule.onNodeWithContentDescription("Sky").assertIsSelected()
        composeRule.onNodeWithContentDescription("Midnight").assertExists()
    }

    @Test fun fourteenThemesInFiveFamilies() {
        show()
        listOf("Dark", "Light", "Cartoon", "Dark cartoon", "Halloween").forEach { composeRule.onNodeWithText(it).assertExists() }
        Themes.all.forEach { composeRule.onNodeWithContentDescription(composeRule.activity.getString(it.nameRes)).assertExists() }
    }

    @Test fun focusingAThumbnailPreviewsItWithoutSaving() {
        show()
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        assertEquals(ThemeId.Midnight, previews.first())
        assertEquals(ThemeId.Sky, previews.last()) // right then down from Midnight: Graphite, then Sky
        assertEquals(emptyList<ThemeId>(), chosen)
    }

    @Test fun okChoosesTheFocusedTheme() {
        show()
        press(Key.DirectionRight, 2) // Aurora
        press(Key.Enter)
        assertEquals(listOf(ThemeId.Aurora), chosen)
    }

    @Test fun leavingThePageEndsThePreview() {
        show()
        press(Key.DirectionRight)
        showPage = false // Back
        composeRule.waitForIdle()
        assertEquals(null, previews.last())
    }

    @Test fun theScheduleRowIsBelowTheThumbnails() {
        show()
        press(Key.DirectionDown, 5) // one row per family: dark, light, cartoon, dark cartoon, Halloween
        composeRule.onNodeWithText("Change by time of day").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, scheduleOpened)
    }
}

@OptIn(ExperimentalTestApi::class)
class SchedulePageTest : TvScreenshotTest() {
    private val toggles = mutableListOf<Boolean>()
    private var picked = ""
    private var schedule by mutableStateOf(ThemeSchedule(enabled = false, dayTheme = ThemeId.Paper, nightTheme = ThemeId.Space))

    private fun show(sun: SunTimes? = null) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                SchedulePage(
                    Themes.default, schedule, sun, FocusMemory(),
                    onToggle = { toggles += it; schedule = schedule.copy(enabled = it) },
                    onPickDay = { picked = "day" }, onPickNight = { picked = "night" },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    @Test fun okOnTheFirstRowTogglesTheSchedule() {
        show()
        composeRule.onNodeWithText("Change automatically").assertIsFocused()
        press(Key.Enter)
        press(Key.Enter)
        assertEquals(listOf(true, false), toggles)
    }

    @Test fun dayAndNightRowsShowTheirThemesAndOpenThePickers() {
        show()
        composeRule.onNodeWithText("Paper").assertExists()
        composeRule.onNodeWithText("Space").assertExists()
        press(Key.DirectionDown); press(Key.Enter)
        assertEquals("day", picked)
        press(Key.DirectionDown); press(Key.Enter)
        assertEquals("night", picked)
    }

    @Test fun noteExplainsTheFixedHoursWithoutWeatherData() {
        show(sun = null)
        composeRule.onNodeWithText("Without weather data the theme changes at 07:00 and 20:00.").assertExists()
    }

    @Test fun noteShowsTodaysSunTimesWhenKnown() {
        show(sun = SunTimes(LocalTime.of(7, 5), LocalTime.of(18, 12)))
        composeRule.onNodeWithText("Sunset 18:12 · sunrise 07:05. The times come from the weather data.").assertExists()
    }
}
