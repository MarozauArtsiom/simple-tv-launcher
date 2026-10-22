package io.github.marozauartsiom.tvlauncher.data.weather

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.marozauartsiom.tvlauncher.data.prefs.CachedJson
import io.github.marozauartsiom.tvlauncher.data.prefs.Settings
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherLocation
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherPlace
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GeoIpTest {
    private fun geoIp(http: FakeHttp) = GeoIp(http, OpenMeteoClient(http))

    @Test fun ipinfoGivesTheLocalisedNearestCity() = runBlocking {
        val http = FakeHttp().ok("ipinfo.io", fixture("ipinfo-minsk.json")).ok("geocoding", fixture("geocoding-minsk.json"))
        val place = geoIp(http).lookup("be")!!
        assertEquals("Мінск", place.name)
        assertEquals("Мінск", place.region)
        assertEquals(53.9, place.latitude, 0.001) // the Belarusian one, not the Nebraska one with the same name
        assertTrue(http.requests.any { it.contains("language=be") && it.contains("name=Minsk") })
    }

    @Test fun ipapiIsTheFallbackWhenIpinfoFails() = runBlocking {
        val http = FakeHttp().on("ipinfo.io", HttpResult(429, "")).ok("ipapi.co", fixture("ipapi-minsk.json")).ok("geocoding", fixture("geocoding-minsk.json"))
        assertEquals("Мінск", geoIp(http).lookup("be")!!.name)
    }

    @Test fun aRateLimitedFallbackIsNotAnAnswer() = runBlocking {
        val http = FakeHttp().on("ipinfo.io", null).ok("ipapi.co", fixture("ipapi-ratelimited.json"))
        assertNull(geoIp(http).lookup("en"))
    }

    @Test fun bothServicesDownMeansNoPlaceAndNoGuess() = runBlocking {
        assertNull(geoIp(FakeHttp()).lookup("en"))
    }

    @Test fun withoutAGeocodingMatchTheIpNameAndCoordinatesAreUsed() = runBlocking {
        val http = FakeHttp().ok("ipinfo.io", fixture("ipinfo-minsk.json")) // geocoding offline
        val place = geoIp(http).lookup("en")!!
        assertEquals("Minsk", place.name)
        assertEquals("Minsk City", place.region)
        assertEquals(27.5667, place.longitude, 0.0001)
    }

    @Test fun aGeocodingResultFarFromTheAddressIsIgnored() = runBlocking {
        val far = """{"results":[{"name":"Minsk","latitude":40.0,"longitude":-100.0,"country":"US"}]}"""
        val http = FakeHttp().ok("ipinfo.io", fixture("ipinfo-minsk.json")).ok("geocoding", far)
        assertEquals(53.9, geoIp(http).lookup("en")!!.latitude, 0.001)
    }

    @Test fun distanceIsRoughlyRight() {
        assertEquals(0.0, GeoIp.distanceKm(53.9, 27.5, 53.9, 27.5), 0.001)
        assertEquals(954.0, GeoIp.distanceKm(53.9, 27.56, 52.52, 13.4), 15.0) // Minsk–Berlin
    }
}

class WeatherRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var store: DataStore<Preferences>
    private lateinit var settings: Settings
    private var now = 1_000_000_000L
    private val minsk = WeatherPlace("Мінск", "Мінск", "Беларусь", 53.9, 27.5667)

    @Before fun setUp() {
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }
        settings = Settings(store)
    }

    @After fun tearDown() = scope.cancel()

    private fun repo(http: FakeHttp) = WeatherRepository(settings, OpenMeteoClient(http), GeoIp(http, OpenMeteoClient(http)), { "be" }, { now }, retryDelayMs = 0)
    private fun data() = runBlocking { settings.data.first() }
    private fun goodHttp() = FakeHttp().ok("/v1/forecast", fixture("forecast-minsk.json")).ok("ipinfo.io", fixture("ipinfo-minsk.json")).ok("geocoding", fixture("geocoding-minsk.json"))

    @Test fun autoModeFindsThePlaceFetchesAndCaches() = runBlocking {
        val http = goodHttp()
        assertTrue(repo(http).refresh())
        assertEquals("Мінск", data().autoPlace?.name)
        assertEquals(now, data().weatherCache?.savedAtMillis)
        assertNotNull(OpenMeteoClient(http).parseForecast(data().weatherCache!!.json))
    }

    @Test fun manualModeNeverAsksTheIpServices() = runBlocking {
        settings.setWeatherLocation(WeatherLocation.Manual(minsk))
        val http = goodHttp()
        assertTrue(repo(http).refresh())
        assertTrue(http.requests.none { it.contains("ipinfo") || it.contains("ipapi") })
        assertTrue(http.requests.single { it.contains("/v1/forecast") }.contains("latitude=53.9000&longitude=27.5667"))
    }

    @Test fun freshDataIsNotFetchedAgainWithinThirtyMinutes() = runBlocking {
        val http = goodHttp()
        val repo = repo(http)
        assertTrue(repo.refresh())
        val requests = http.requests.size
        now += 29 * 60_000
        assertFalse(repo.refresh())
        assertEquals(requests, http.requests.size)
        now += 2 * 60_000 // 31 minutes
        assertTrue(repo.refresh())
    }

    @Test fun aNewCityForcesAFetchAtOnce() = runBlocking {
        val http = goodHttp()
        val repo = repo(http)
        repo.refresh()
        settings.setWeatherLocation(WeatherLocation.Manual(minsk))
        assertTrue(repo.refresh(force = true))
    }

    @Test fun theIpPlaceIsKeptForADay() = runBlocking {
        val http = goodHttp()
        val repo = repo(http)
        repo.refresh()
        now += 31 * 60_000
        repo.refresh()
        assertEquals(1, http.requests.count { it.contains("ipinfo") }) // not asked again after 31 min
        now += 24 * 3_600_000L
        repo.refresh()
        assertEquals(2, http.requests.count { it.contains("ipinfo") })
    }

    @Test fun aFailedFetchKeepsTheOldDataAndRetriesOnce() = runBlocking {
        val old = CachedJson(fixture("forecast-minsk.json"), now - 40 * 60_000)
        settings.setWeatherCache(old)
        settings.setWeatherLocation(WeatherLocation.Manual(minsk))
        val http = FakeHttp().on("/v1/forecast", HttpResult(500, ""), HttpResult(500, ""))
        assertFalse(repo(http).refresh())
        assertEquals(2, http.requests.size) // the request and one retry
        assertEquals(old, data().weatherCache)
    }

    @Test fun theRetryCanSucceed() = runBlocking {
        settings.setWeatherLocation(WeatherLocation.Manual(minsk))
        val http = FakeHttp().on("/v1/forecast", null, HttpResult(200, fixture("forecast-minsk.json")))
        assertTrue(repo(http).refresh())
        assertNotNull(data().weatherCache)
    }

    @Test fun withoutAPlaceNothingIsFetchedAndNoGuessIsMade() = runBlocking {
        val http = FakeHttp() // offline: no IP answer
        assertFalse(repo(http).refresh())
        assertNull(data().weatherCache)
        assertTrue(http.requests.none { it.contains("/v1/forecast") })
    }

    @Test fun aFailedIpLookupKeepsTheLastKnownPlace() = runBlocking {
        settings.setAutoPlace(minsk, now - 2 * 24 * 3_600_000L) // older than a day
        val http = FakeHttp().ok("/v1/forecast", fixture("forecast-minsk.json")) // the IP services are offline
        assertTrue(repo(http).refresh())
        assertEquals("Мінск", data().autoPlace?.name)
    }
}

class WeatherViewTest {
    private val client = OpenMeteoClient(FakeHttp())
    private val json = fixture("forecast-minsk.json")

    private fun view(cache: CachedJson?, location: WeatherLocation = WeatherLocation.Manual(WeatherPlace("Мінск", null, null, 1.0, 2.0)), now: Long = 100_000_000L) =
        WeatherView.from(io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData(weatherCache = cache, weatherLocation = location), now, client)

    @Test fun freshDataIsShownWithTheCityName() {
        val v = view(CachedJson(json, 100_000_000L - 60_000))
        assertEquals("Мінск", v.place)
        assertNotNull(v.data)
        assertFalse(v.stale)
    }

    @Test fun dataOlderThanSixHoursIsStale() {
        val six = 6 * 3_600_000L
        assertFalse(view(CachedJson(json, 100_000_000L - six)).stale)
        assertTrue(view(CachedJson(json, 100_000_000L - six - 1)).stale)
    }

    @Test fun noCacheMeansNoDataAndStale() {
        val v = view(null)
        assertNull(v.data)
        assertTrue(v.stale)
    }

    @Test fun anUnreadableCacheGivesNoData() = assertNull(view(CachedJson("junk", 100_000_000L)).data)

    @Test fun autoModeUsesTheDetectedPlaceNameOrNone() {
        assertNull(view(null, WeatherLocation.Auto).place)
        val s = io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData(autoPlace = WeatherPlace("Брест", null, null, 1.0, 2.0))
        assertEquals("Брест", WeatherView.from(s, 0, client).place)
    }
}
