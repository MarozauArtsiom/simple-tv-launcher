package io.github.marozauartsiom.tvlauncher.data.prefs

import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import java.time.LocalTime

/** Local sunrise and sunset, from the weather data when there is some. */
data class SunTimes(val sunrise: LocalTime, val sunset: LocalTime) {
    /** A usable day: sunrise before sunset (polar days and bad data are not). */
    val isSane: Boolean get() = sunrise.isBefore(sunset)

    companion object {
        /** Used without weather data: the theme changes at 07:00 and 20:00. */
        val Fixed = SunTimes(LocalTime.of(7, 0), LocalTime.of(20, 0))
    }
}

/**
 * The theme the schedule wants at [time]: the day theme from sunrise until sunset, the night theme
 * otherwise; null when the schedule is off (the saved theme applies). Without sane sun times the
 * fixed 07:00/20:00 are used.
 */
fun ThemeSchedule.themeAt(time: LocalTime, sun: SunTimes? = null): ThemeId? {
    if (!enabled) return null
    val times = sun?.takeIf { it.isSane } ?: SunTimes.Fixed
    val isDay = !time.isBefore(times.sunrise) && time.isBefore(times.sunset)
    return if (isDay) dayTheme else nightTheme
}
