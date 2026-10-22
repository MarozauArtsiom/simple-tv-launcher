package io.github.marozauartsiom.tvlauncher.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var store: DataStore<Preferences>
    private lateinit var settings: Settings

    @Before fun setUp() {
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }
        settings = Settings(store)
    }

    @After fun tearDown() = scope.cancel()

    private fun read() = runBlocking { settings.data.first() }

    @Test fun defaultsMatchTheSpec() {
        val d = read()
        assertEquals(ThemeId.Midnight, d.theme)
        assertEquals(LanguageChoice.Follow, d.language)
        assertEquals(BackgroundChoice.FromTheme, d.background)
        assertEquals(WeatherLocation.Auto, d.weatherLocation)
        assertEquals(emptyList<ItemKey>(), d.favourites)
        assertEquals(true, d.hdmi.promptEnabled)
        assertEquals(false, d.themeSchedule.enabled)
        assertNull(d.weatherCache)
    }

    @Test fun favouritesKeepTheOwnersOrderAndNeverDuplicate() = runBlocking {
        val a = ItemKey.app("a"); val b = ItemKey.app("b"); val c = ItemKey.shortcut("s1")
        settings.addFavourite(a); settings.addFavourite(b); settings.addFavourite(c); settings.addFavourite(a)
        assertEquals(listOf(a, b, c), read().favourites)
        settings.moveFavourite(c, 0)
        assertEquals(listOf(c, a, b), read().favourites)
        settings.removeFavourite(a)
        assertEquals(listOf(c, b), read().favourites)
    }

    @Test fun moveClampsAndIgnoresUnknownKeys() {
        val list = listOf(ItemKey.app("a"), ItemKey.app("b"), ItemKey.app("c"))
        assertEquals(listOf(list[1], list[2], list[0]), Favourites.move(list, list[0], 99))
        assertEquals(listOf(list[2], list[0], list[1]), Favourites.move(list, list[2], -5))
        assertEquals(list, Favourites.move(list, ItemKey.app("zzz"), 1))
    }

    @Test fun itemKeysDistinguishAppsFromShortcuts() {
        assertEquals(true, ItemKey.app("com.x").isApp)
        assertEquals("com.x", ItemKey.app("com.x").id)
        assertEquals(true, ItemKey.shortcut("42").isShortcut)
    }

    @Test fun hiddenAppsAndLaunchCounts() = runBlocking {
        settings.setHidden("com.a", true); settings.setHidden("com.b", true); settings.setHidden("com.a", false)
        assertEquals(setOf("com.b"), read().hidden)
        val k = ItemKey.app("com.a")
        repeat(3) { settings.recordLaunch(k) }
        settings.recordLaunch(ItemKey.app("com.b"))
        assertEquals(mapOf("pkg:com.a" to 3, "pkg:com.b" to 1), read().launchCounts)
    }

    @Test fun themeScheduleBackgroundAndLanguageRoundTrip() = runBlocking {
        settings.setTheme(ThemeId.Space)
        settings.setThemeSchedule(ThemeSchedule(true, ThemeId.Sky, ThemeId.Aurora))
        settings.setBackground(BackgroundChoice.Solid(0xFF1B2A44.toInt()))
        settings.setLanguage(LanguageChoice.Fixed("be"))
        val d = read()
        assertEquals(ThemeId.Space, d.theme)
        assertEquals(ThemeSchedule(true, ThemeId.Sky, ThemeId.Aurora), d.themeSchedule)
        assertEquals(BackgroundChoice.Solid(0xFF1B2A44.toInt()), d.background)
        assertEquals(LanguageChoice.Fixed("be"), d.language)
        settings.setBackground(BackgroundChoice.Image("bg.webp")); settings.setLanguage(LanguageChoice.Follow)
        assertEquals(BackgroundChoice.Image("bg.webp"), read().background)
        assertEquals(LanguageChoice.Follow, read().language)
    }

    @Test fun shortcutsWeatherAndHdmiRoundTrip() = runBlocking {
        val sc = listOf(Shortcut("1", "Новости", "https://example.org"))
        settings.setShortcuts(sc)
        val place = WeatherPlace("Брест", "Брестская область", "Беларусь", 52.09, 23.69)
        settings.setWeatherLocation(WeatherLocation.Manual(place))
        settings.setAutoPlace(place, 123L)
        settings.setWeatherCache(CachedJson("{\"a\":1}", 456L))
        settings.setHdmi(HdmiSettings(promptEnabled = false, overOtherApps = false, mutedInputs = setOf("HDMI3")))
        val d = read()
        assertEquals(sc, d.shortcuts)
        assertEquals(WeatherLocation.Manual(place), d.weatherLocation)
        assertEquals(place, d.autoPlace); assertEquals(123L, d.autoPlaceSavedAtMillis)
        assertEquals(CachedJson("{\"a\":1}", 456L), d.weatherCache)
        assertEquals(HdmiSettings(false, false, setOf("HDMI3")), d.hdmi)
        settings.setWeatherLocation(WeatherLocation.Auto)
        assertEquals(WeatherLocation.Auto, read().weatherLocation)
    }

    @Test fun damagedValuesFallBackToDefaultsInsteadOfThrowing() = runBlocking {
        store.edit {
            it[stringPreferencesKey("theme")] = "no-such-theme"
            it[stringPreferencesKey("language")] = "xx"
            it[stringPreferencesKey("shortcuts")] = "{not json"
            it[stringPreferencesKey("launch_counts")] = "[]"
            it[stringPreferencesKey("weather_location")] = "garbage"
            it[stringPreferencesKey("background")] = "color:zzz"
        }
        val d = read()
        assertEquals(ThemeId.Midnight, d.theme)
        assertEquals(LanguageChoice.Follow, d.language)
        assertEquals(emptyList<Shortcut>(), d.shortcuts)
        assertEquals(emptyMap<String, Int>(), d.launchCounts)
        assertEquals(WeatherLocation.Auto, d.weatherLocation)
        assertEquals(BackgroundChoice.FromTheme, d.background)
    }
}
