package io.github.marozauartsiom.tvlauncher.data.weather

import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoClientTest {
    private val http = FakeHttp()
    private val client = OpenMeteoClient(http)

    @Test fun theRealForecastAnswerParses() {
        val data = client.parseForecast(fixture("forecast-minsk.json"))!!
        assertEquals(11.2, data.current.temperature, 0.001)
        assertEquals(8.1, data.current.feelsLike, 0.001)
        assertEquals(3, data.current.code)
        assertEquals(false, data.current.isDay)
        assertEquals(4.6, data.current.windMs, 0.001)
    }

    @Test fun hoursAreEveryThirdHourFour() {
        val data = client.parseForecast(fixture("forecast-minsk.json"))!!
        assertEquals(listOf(LocalTime.of(21, 0), LocalTime.of(0, 0), LocalTime.of(3, 0), LocalTime.of(6, 0)), data.hours.map { it.time })
        assertEquals(listOf(11.2, 11.2, 10.9, 10.4), data.hours.map { it.temperature })
    }

    @Test fun fiveDaysWithSunTimes() {
        val data = client.parseForecast(fixture("forecast-minsk.json"))!!
        assertEquals(5, data.days.size)
        assertEquals(LocalDate.of(2026, 10, 22), data.days.first().date)
        assertEquals(12.8, data.days.first().max, 0.001)
        assertEquals(10.0, data.days.first().min, 0.001)
        assertEquals(51, data.days[1].code)
        assertEquals(LocalTime.of(7, 50), data.sunrise)
        assertEquals(LocalTime.of(17, 57), data.sunset)
        assertEquals(LocalTime.of(17, 57), data.sunTimes()!!.sunset)
    }

    @Test fun garbageOrIncompleteAnswersGiveNull() {
        assertNull(client.parseForecast("not json"))
        assertNull(client.parseForecast("{}"))
        assertNull(client.parseForecast("""{"current":{"temperature_2m":5.0}}""")) // no weather code
        assertNull(client.parseForecast("""{"current":{"temperature_2m":5.0,"weather_code":1},"hourly":{"time":["bad"],"temperature_2m":[1.0],"weather_code":[1]}}"""))
    }

    @Test fun aForecastWithoutHoursOrDaysStillWorks() {
        val data = client.parseForecast("""{"current":{"temperature_2m":5.0,"weather_code":1,"is_day":1}}""")!!
        assertTrue(data.hours.isEmpty() && data.days.isEmpty())
        assertNull(data.sunTimes())
    }

    @Test fun forecastRequestAsksForExactlyWhatTheSkillSays() {
        val url = client.forecastUrl(53.9, 27.5667)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?latitude=53.9000&longitude=27.5667"))
        listOf("temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m", "forecast_hours=12", "forecast_days=5", "timezone=auto", "wind_speed_unit=ms", "sunrise,sunset")
            .forEach { assertTrue(it, url.contains(it)) }
    }

    @Test fun coordinatesNeverUseACommaDecimalSeparator() {
        val saved = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.GERMANY)
        try {
            assertTrue(client.forecastUrl(53.9, 27.5).contains("latitude=53.9000&longitude=27.5000"))
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }

    @Test fun forecastJsonIsReturnedAsIsWhenItIsGood() = runBlocking {
        val body = fixture("forecast-minsk.json")
        http.ok("/v1/forecast", body)
        assertEquals(body, client.forecastJson(53.9, 27.56))
    }

    @Test fun forecastJsonIsNullOnErrorsOfflineAndJunk() = runBlocking {
        assertNull(client.forecastJson(1.0, 2.0)) // offline
        http.on("/v1/forecast", HttpResult(500, "oops"))
        assertNull(client.forecastJson(1.0, 2.0))
        val junk = FakeHttp().ok("/v1/forecast", "<html>captive portal</html>")
        assertNull(OpenMeteoClient(junk).forecastJson(1.0, 2.0))
    }

    @Test fun searchParsesNamesRegionsAndCountries() = runBlocking {
        http.ok("geocoding-api.open-meteo.com", fixture("geocoding-brest.json"))
        val places = client.search("Брест", "ru")
        assertEquals(3, places.size)
        assertEquals("Брест", places[0].name)
        assertEquals("Брестская Область", places[0].region)
        assertEquals("Беларусь", places[0].country)
        assertEquals(52.10894, places[0].latitude, 0.00001)
    }

    @Test fun searchEncodesTheQueryAndAsksInTheUiLanguage() = runBlocking {
        http.ok("geocoding-api.open-meteo.com", """{"results":[]}""")
        client.search("Санкт Петербург", "be")
        val url = http.requests.single()
        assertTrue(url, url.contains("name=%D0%A1%D0%B0%D0%BD%D0%BA%D1%82+%D0%9F%D0%B5%D1%82%D0%B5%D1%80%D0%B1%D1%83%D1%80%D0%B3"))
        assertTrue(url.contains("language=be"))
        assertTrue(url.contains("count=8"))
    }

    @Test fun noResultsAndShortQueriesAndErrorsGiveAnEmptyList() = runBlocking {
        http.ok("geocoding-api.open-meteo.com", """{"generationtime_ms":0.5}""")
        assertEquals(emptyList<Any>(), client.search("Nowhereville", "en"))
        assertEquals(emptyList<Any>(), client.search("a", "en")) // too short: not even a request
        assertEquals(1, http.requests.size)
        assertEquals(emptyList<Any>(), OpenMeteoClient(FakeHttp()).search("Minsk", "en")) // offline
        assertEquals(emptyList<Any>(), OpenMeteoClient(FakeHttp().on("geocoding", HttpResult(429, "slow down"))).search("Minsk", "en"))
    }

    @Test fun resultsWithoutCoordinatesOrNamesAreDropped() {
        val places = client.parsePlaces("""{"results":[{"name":"A"},{"latitude":1.0,"longitude":2.0},{"name":"B","latitude":1.0,"longitude":2.0}]}""")
        assertEquals(listOf("B"), places.map { it.name })
        assertNotNull(places.single())
    }

    @Test fun everyWmoCodeGroupMapsToACondition() {
        val expected = mapOf(
            0 to Condition.Clear, 1 to Condition.MainlyClear, 2 to Condition.PartlyCloudy, 3 to Condition.Overcast,
            45 to Condition.Fog, 48 to Condition.Fog, 51 to Condition.Drizzle, 57 to Condition.Drizzle,
            61 to Condition.Rain, 67 to Condition.Rain, 71 to Condition.Snow, 77 to Condition.Snow,
            80 to Condition.Showers, 82 to Condition.Showers, 85 to Condition.SnowShowers, 86 to Condition.SnowShowers,
            95 to Condition.Thunderstorm, 96 to Condition.Thunderstorm, 99 to Condition.Thunderstorm, 123 to Condition.Overcast,
        )
        expected.forEach { (code, condition) -> assertEquals("code $code", condition, WeatherCodes.condition(code)) }
    }
}
