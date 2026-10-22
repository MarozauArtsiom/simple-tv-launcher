package io.github.marozauartsiom.tvlauncher.data.weather

import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherPlace
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Open-Meteo (free, no key; data under CC BY 4.0, credited in Settings → About): the forecast and the
 * city search. Answers are parsed leniently; anything unusable gives null and the caller keeps what it has.
 */
class OpenMeteoClient(private val http: HttpFetcher) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** The raw forecast answer for [lat], [lon] (kept in the cache as is), only if it parses. */
    suspend fun forecastJson(lat: Double, lon: Double): String? {
        val result = http.get(forecastUrl(lat, lon)) ?: return null
        return result.body.takeIf { result.ok && parseForecast(it) != null }
    }

    suspend fun search(query: String, languageTag: String): List<WeatherPlace> {
        val name = query.trim()
        if (name.length < 2) return emptyList()
        val result = http.get(geocodingUrl(name, languageTag)) ?: return emptyList()
        if (!result.ok) return emptyList()
        return parsePlaces(result.body)
    }

    // ---- URLs ----

    fun forecastUrl(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast?latitude=${coordinate(lat)}&longitude=${coordinate(lon)}" +
            "&current=temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,is_day&forecast_hours=12" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset&forecast_days=5" +
            "&timezone=auto&wind_speed_unit=ms"

    fun geocodingUrl(name: String, languageTag: String): String =
        "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(name, "UTF-8")}&count=8" +
            "&language=${URLEncoder.encode(languageTag, "UTF-8")}&format=json"

    private fun coordinate(value: Double) = String.format(java.util.Locale.ROOT, "%.4f", value)

    // ---- parsing ----

    fun parseForecast(text: String): WeatherData? = runCatching {
        val dto = json.decodeFromString<ForecastDto>(text)
        val now = dto.current ?: return null
        val current = CurrentWeather(
            temperature = now.temperature ?: return null,
            feelsLike = now.apparent ?: now.temperature,
            code = now.code ?: return null,
            isDay = (now.isDay ?: 1) == 1,
            windMs = now.wind ?: 0.0,
        )
        val h = dto.hourly
        val hours = if (h == null) emptyList() else (0 until h.time.size step 3).take(4).mapNotNull { i ->
            val time = LocalDateTime.parse(h.time[i]).toLocalTime()
            HourWeather(time, h.temperature.getOrNull(i) ?: return@mapNotNull null, h.code.getOrNull(i) ?: return@mapNotNull null, (h.isDay.getOrNull(i) ?: 1) == 1)
        }
        val d = dto.daily
        val days = if (d == null) emptyList() else d.time.indices.mapNotNull { i ->
            DayWeather(
                LocalDate.parse(d.time[i]),
                d.code.getOrNull(i) ?: return@mapNotNull null,
                d.max.getOrNull(i) ?: return@mapNotNull null,
                d.min.getOrNull(i) ?: return@mapNotNull null,
            )
        }
        WeatherData(
            current = current,
            hours = hours,
            days = days,
            sunrise = d?.sunrise?.firstOrNull()?.let { LocalDateTime.parse(it).toLocalTime() },
            sunset = d?.sunset?.firstOrNull()?.let { LocalDateTime.parse(it).toLocalTime() },
        )
    }.getOrNull()

    fun parsePlaces(text: String): List<WeatherPlace> = runCatching {
        json.decodeFromString<GeocodingDto>(text).results.orEmpty().mapNotNull {
            WeatherPlace(
                name = it.name?.takeIf { n -> n.isNotBlank() } ?: return@mapNotNull null,
                region = it.admin1?.takeIf { r -> r.isNotBlank() },
                country = it.country?.takeIf { c -> c.isNotBlank() },
                latitude = it.latitude ?: return@mapNotNull null,
                longitude = it.longitude ?: return@mapNotNull null,
            )
        }
    }.getOrDefault(emptyList())

    @Serializable
    private class ForecastDto(val current: CurrentDto? = null, val hourly: HourlyDto? = null, val daily: DailyDto? = null)

    @Serializable
    private class CurrentDto(
        @kotlinx.serialization.SerialName("temperature_2m") val temperature: Double? = null,
        @kotlinx.serialization.SerialName("apparent_temperature") val apparent: Double? = null,
        @kotlinx.serialization.SerialName("weather_code") val code: Int? = null,
        @kotlinx.serialization.SerialName("is_day") val isDay: Int? = null,
        @kotlinx.serialization.SerialName("wind_speed_10m") val wind: Double? = null,
    )

    @Serializable
    private class HourlyDto(
        val time: List<String> = emptyList(),
        @kotlinx.serialization.SerialName("temperature_2m") val temperature: List<Double?> = emptyList(),
        @kotlinx.serialization.SerialName("weather_code") val code: List<Int?> = emptyList(),
        @kotlinx.serialization.SerialName("is_day") val isDay: List<Int?> = emptyList(),
    )

    @Serializable
    private class DailyDto(
        val time: List<String> = emptyList(),
        @kotlinx.serialization.SerialName("weather_code") val code: List<Int?> = emptyList(),
        @kotlinx.serialization.SerialName("temperature_2m_max") val max: List<Double?> = emptyList(),
        @kotlinx.serialization.SerialName("temperature_2m_min") val min: List<Double?> = emptyList(),
        val sunrise: List<String> = emptyList(),
        val sunset: List<String> = emptyList(),
    )

    @Serializable
    private class GeocodingDto(val results: List<PlaceDto>? = null)

    @Serializable
    private class PlaceDto(
        val name: String? = null,
        val admin1: String? = null,
        val country: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
    )
}
