package io.github.marozauartsiom.tvlauncher.data.weather

import androidx.compose.runtime.Immutable
import io.github.marozauartsiom.tvlauncher.data.prefs.SunTimes
import java.time.LocalDate
import java.time.LocalTime

@Immutable
data class CurrentWeather(val temperature: Double, val feelsLike: Double, val code: Int, val isDay: Boolean, val windMs: Double)

@Immutable
data class HourWeather(val time: LocalTime, val temperature: Double, val code: Int, val isDay: Boolean)

@Immutable
data class DayWeather(val date: LocalDate, val code: Int, val max: Double, val min: Double)

/** One forecast: now, four 3-hour steps, five days, and today's sunrise and sunset (local time). */
@Immutable
data class WeatherData(
    val current: CurrentWeather,
    val hours: List<HourWeather>,
    val days: List<DayWeather>,
    val sunrise: LocalTime?,
    val sunset: LocalTime?,
) {
    fun sunTimes(): SunTimes? = if (sunrise != null && sunset != null) SunTimes(sunrise, sunset).takeIf { it.isSane } else null
}

/** The kinds of weather the launcher words and draws. */
enum class Condition { Clear, MainlyClear, PartlyCloudy, Overcast, Fog, Drizzle, Rain, Snow, Showers, SnowShowers, Thunderstorm }

/** WMO weather codes (as Open-Meteo reports them) grouped into [Condition]s. */
object WeatherCodes {
    fun condition(code: Int): Condition = when (code) {
        0 -> Condition.Clear
        1 -> Condition.MainlyClear
        2 -> Condition.PartlyCloudy
        3 -> Condition.Overcast
        45, 48 -> Condition.Fog
        in 51..57 -> Condition.Drizzle
        in 61..67 -> Condition.Rain
        in 71..77 -> Condition.Snow
        in 80..82 -> Condition.Showers
        85, 86 -> Condition.SnowShowers
        in 95..99 -> Condition.Thunderstorm
        else -> Condition.Overcast // unknown code: the safest neutral picture
    }
}
