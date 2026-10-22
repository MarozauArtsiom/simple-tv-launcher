package io.github.marozauartsiom.tvlauncher.data.weather

import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherPlace
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Where the TV is, from its internet address (a TV has no GPS, so there is no permission dialog):
 * ipinfo.io, then ipapi.co if that fails. The city name is then looked up in Open-Meteo in the UI
 * language and the result nearest to the address is used, which gives a localised name.
 * A VPN on the TV shows the VPN's city; then the owner picks the city by hand.
 */
class GeoIp(private val http: HttpFetcher, private val client: OpenMeteoClient) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun lookup(languageTag: String): WeatherPlace? {
        val hit = ipinfo() ?: ipapi() ?: return null
        val city = hit.city
        if (city != null) {
            val nearest = client.search(city, languageTag)
                .map { it to distanceKm(hit.latitude, hit.longitude, it.latitude, it.longitude) }
                .filter { it.second <= MAX_REFINE_KM }
                .minByOrNull { it.second }
                ?.first
            if (nearest != null) return nearest
        }
        val name = city ?: hit.region ?: hit.country ?: return null
        return WeatherPlace(name, hit.region.takeIf { city != null }, hit.country, hit.latitude, hit.longitude)
    }

    private class Hit(val city: String?, val region: String?, val country: String?, val latitude: Double, val longitude: Double)

    private suspend fun ipinfo(): Hit? {
        val result = http.get("https://ipinfo.io/json")?.takeIf { it.ok } ?: return null
        val dto = runCatching { json.decodeFromString<IpInfoDto>(result.body) }.getOrNull() ?: return null
        val (lat, lon) = dto.loc?.split(',')?.takeIf { it.size == 2 }?.let { it[0].trim().toDoubleOrNull() to it[1].trim().toDoubleOrNull() } ?: return null
        return Hit(dto.city?.ifBlank { null }, dto.region?.ifBlank { null }, dto.country?.ifBlank { null }, lat ?: return null, lon ?: return null)
    }

    private suspend fun ipapi(): Hit? {
        val result = http.get("https://ipapi.co/json")?.takeIf { it.ok } ?: return null
        val dto = runCatching { json.decodeFromString<IpApiDto>(result.body) }.getOrNull() ?: return null
        if (dto.error == true) return null
        return Hit(dto.city?.ifBlank { null }, dto.region?.ifBlank { null }, dto.country?.ifBlank { null }, dto.latitude ?: return null, dto.longitude ?: return null)
    }

    @Serializable
    private class IpInfoDto(val city: String? = null, val region: String? = null, val country: String? = null, val loc: String? = null)

    @Serializable
    private class IpApiDto(
        val city: String? = null,
        val region: String? = null,
        @SerialName("country_name") val country: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val error: Boolean? = null,
    )

    companion object {
        private const val MAX_REFINE_KM = 100.0

        fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return 2 * r * asin(sqrt(a))
        }
    }
}
