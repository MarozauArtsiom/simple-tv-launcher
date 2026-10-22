package io.github.marozauartsiom.tvlauncher.data.weather

import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherLocation

/** What the UI shows of the weather: derived from the stored settings, so it needs no state of its own. */
data class WeatherView(
    /** The city, or null if none is known yet. */
    val place: String?,
    /** Parsed forecast, or null if there is none (nothing fetched yet, or the cache is unreadable). */
    val data: WeatherData?,
    /** Older than six hours: show "—°" in the chip. */
    val stale: Boolean,
) {
    companion object {
        fun from(settings: SettingsData, now: Long, client: OpenMeteoClient): WeatherView =
            of(settings, now, settings.weatherCache?.let { client.parseForecast(it.json) })

        /** Like [from] with the cache already parsed (the UI parses once per fetch, not once per minute). */
        fun of(settings: SettingsData, now: Long, data: WeatherData?): WeatherView {
            val place = when (val location = settings.weatherLocation) {
                is WeatherLocation.Manual -> location.place.name
                WeatherLocation.Auto -> settings.autoPlace?.name
            }
            val cache = settings.weatherCache
            val stale = cache == null || now - cache.savedAtMillis > WeatherRepository.STALE_MS
            return WeatherView(place, data, stale)
        }
    }
}
