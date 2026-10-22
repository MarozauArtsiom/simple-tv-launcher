package io.github.marozauartsiom.tvlauncher.data.prefs

import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeSchedulingTest {
    private val schedule = ThemeSchedule(enabled = true, dayTheme = ThemeId.Paper, nightTheme = ThemeId.Space)
    private fun at(h: Int, m: Int = 0) = LocalTime.of(h, m)

    @Test fun offMeansTheSavedThemeApplies() {
        assertNull(schedule.copy(enabled = false).themeAt(at(12)))
    }

    @Test fun withoutWeatherItSwitchesAtSevenAndTwenty() {
        assertEquals(ThemeId.Space, schedule.themeAt(at(6, 59)))
        assertEquals(ThemeId.Paper, schedule.themeAt(at(7, 0)))
        assertEquals(ThemeId.Paper, schedule.themeAt(at(19, 59)))
        assertEquals(ThemeId.Space, schedule.themeAt(at(20, 0)))
        assertEquals(ThemeId.Space, schedule.themeAt(at(0, 0)))
    }

    @Test fun sunTimesFromTheWeatherMoveTheSwitch() {
        val winter = SunTimes(sunrise = at(8, 40), sunset = at(16, 15))
        assertEquals(ThemeId.Space, schedule.themeAt(at(8, 39), winter))
        assertEquals(ThemeId.Paper, schedule.themeAt(at(8, 40), winter))
        assertEquals(ThemeId.Paper, schedule.themeAt(at(16, 14), winter))
        assertEquals(ThemeId.Space, schedule.themeAt(at(16, 15), winter))
    }

    @Test fun nonsenseSunTimesFallBackToTheFixedHours() {
        val broken = SunTimes(sunrise = at(20, 0), sunset = at(7, 0))
        assertEquals(ThemeId.Paper, schedule.themeAt(at(12), broken))
        assertEquals(ThemeId.Space, schedule.themeAt(at(22), broken))
    }
}
