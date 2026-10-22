package io.github.marozauartsiom.tvlauncher.ui.weather

import androidx.annotation.StringRes
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.weather.Condition
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Wording and number formatting for the weather: °C, wind in m/s, a real minus sign. */
object WeatherText {
    /** "+7°", "−3°", "0°" (rounded; −0.4 is "0°", never "−0°"). */
    fun temperature(value: Double): String {
        val rounded = value.roundToInt()
        return when {
            rounded > 0 -> "+$rounded°"
            rounded < 0 -> "−${-rounded}°"
            else -> "0°"
        }
    }

    const val UNKNOWN_TEMPERATURE = "—°"

    fun wind(ms: Double): String = ms.roundToInt().toString()

    @StringRes
    fun conditionRes(condition: Condition): Int = when (condition) {
        Condition.Clear -> R.string.wx_clear
        Condition.MainlyClear -> R.string.wx_mainly_clear
        Condition.PartlyCloudy -> R.string.wx_partly_cloudy
        Condition.Overcast -> R.string.wx_overcast
        Condition.Fog -> R.string.wx_fog
        Condition.Drizzle -> R.string.wx_drizzle
        Condition.Rain -> R.string.wx_rain
        Condition.Snow -> R.string.wx_snow
        Condition.Showers -> R.string.wx_showers
        Condition.SnowShowers -> R.string.wx_snow_showers
        Condition.Thunderstorm -> R.string.wx_thunderstorm
    }

    /** "Чт", "Thu", "Do" … in the UI language, first letter capital. */
    fun weekday(date: LocalDate, languageTag: String): String {
        val locale = Locale.forLanguageTag(languageTag)
        return date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').replaceFirstChar { it.titlecase(locale) }
    }
}
