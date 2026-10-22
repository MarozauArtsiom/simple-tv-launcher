package io.github.marozauartsiom.tvlauncher.data.weather

import io.github.marozauartsiom.tvlauncher.data.prefs.CachedJson
import io.github.marozauartsiom.tvlauncher.data.prefs.Settings
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherLocation
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherPlace
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fetches the forecast on demand. It does nothing by itself: the UI calls [refresh] while Home is on
 * screen (at most every 30 minutes) and after the owner changes the city. The answer is stored in the
 * settings store, which is also what the UI reads, so the last result shows instantly at the next start.
 * A failed fetch keeps the old data (one retry after a few seconds).
 */
class WeatherRepository(
    private val settings: Settings,
    private val client: OpenMeteoClient,
    private val geoIp: GeoIp,
    private val languageTag: () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val retryDelayMs: Long = 3_000,
) {
    private val lock = Mutex()

    /** True if new data was stored. [force] ignores the 30-minute rule (a new city was chosen). */
    suspend fun refresh(force: Boolean = false): Boolean = lock.withLock {
        val data = settings.data.first()
        val now = clock()
        val cache = data.weatherCache
        if (!force && cache != null && now - cache.savedAtMillis < FRESH_MS) return false

        val place = placeFor(data.weatherLocation, data.autoPlace, data.autoPlaceSavedAtMillis, now) ?: return false
        val json = client.forecastJson(place.latitude, place.longitude)
            ?: run { delay(retryDelayMs); client.forecastJson(place.latitude, place.longitude) }
            ?: return false
        settings.setWeatherCache(CachedJson(json, clock()))
        true
    }

    private suspend fun placeFor(location: WeatherLocation, auto: WeatherPlace?, autoSavedAt: Long, now: Long): WeatherPlace? = when (location) {
        is WeatherLocation.Manual -> location.place
        WeatherLocation.Auto -> {
            if (auto != null && now - autoSavedAt < AUTO_PLACE_MS) {
                auto
            } else {
                val found = geoIp.lookup(languageTag())
                if (found != null) settings.setAutoPlace(found, now)
                found ?: auto // a failed lookup keeps the last known place
            }
        }
    }

    companion object {
        /** New data is fetched at most this often. */
        const val FRESH_MS = 30 * 60 * 1000L
        /** After this long the cached data is shown as "—°" instead of a temperature. */
        const val STALE_MS = 6 * 60 * 60 * 1000L
        /** The place found from the IP address is kept for a day. */
        const val AUTO_PLACE_MS = 24 * 60 * 60 * 1000L
    }
}
