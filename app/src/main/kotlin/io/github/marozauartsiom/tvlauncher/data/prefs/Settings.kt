package io.github.marozauartsiom.tvlauncher.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * All persistent settings in one Preferences DataStore. Reads never throw: a value that can't be
 * parsed falls back to its default, so a damaged file can't stop the launcher from starting.
 */
class Settings(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    private object Keys {
        val favourites = stringPreferencesKey("favourites")
        val hidden = stringSetPreferencesKey("hidden")
        val shortcuts = stringPreferencesKey("shortcuts")
        val launchCounts = stringPreferencesKey("launch_counts")
        val theme = stringPreferencesKey("theme")
        val scheduleEnabled = booleanPreferencesKey("theme_schedule_enabled")
        val scheduleDay = stringPreferencesKey("theme_schedule_day")
        val scheduleNight = stringPreferencesKey("theme_schedule_night")
        val background = stringPreferencesKey("background")
        val language = stringPreferencesKey("language")
        val weatherLocation = stringPreferencesKey("weather_location")
        val autoPlace = stringPreferencesKey("weather_auto_place")
        val autoPlaceAt = longPreferencesKey("weather_auto_place_at")
        val weatherCache = stringPreferencesKey("weather_cache")
        val weatherCacheAt = longPreferencesKey("weather_cache_at")
        val hdmiPrompt = booleanPreferencesKey("hdmi_prompt")
        val hdmiOverlay = booleanPreferencesKey("hdmi_overlay")
        val hdmiMuted = stringSetPreferencesKey("hdmi_muted")
        val avatar = stringPreferencesKey("avatar")
        val sleepTimerAt = longPreferencesKey("sleep_timer_at")
        val hiddenTopButtons = stringSetPreferencesKey("top_bar_hidden")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val data: Flow<SettingsData> = store.data.map { it.toData() }.distinctUntilChanged()

    private fun Preferences.toData(): SettingsData {
        val d = SettingsData()
        return SettingsData(
            favourites = this[Keys.favourites]?.split('\n')?.filter { it.isNotBlank() }?.map(::ItemKey) ?: d.favourites,
            hidden = this[Keys.hidden] ?: d.hidden,
            shortcuts = decode(ListSerializer(Shortcut.serializer()), this[Keys.shortcuts]) ?: d.shortcuts,
            launchCounts = decode(MapSerializer(String.serializer(), Int.serializer()), this[Keys.launchCounts]) ?: d.launchCounts,
            theme = ThemeId.entries.firstOrNull { it.key == this[Keys.theme] } ?: d.theme,
            themeSchedule = ThemeSchedule(
                enabled = this[Keys.scheduleEnabled] ?: false,
                dayTheme = ThemeId.entries.firstOrNull { it.key == this[Keys.scheduleDay] } ?: d.themeSchedule.dayTheme,
                nightTheme = ThemeId.entries.firstOrNull { it.key == this[Keys.scheduleNight] } ?: d.themeSchedule.nightTheme,
            ),
            background = parseBackground(this[Keys.background]),
            avatar = this[Keys.avatar],
            sleepTimerAt = this[Keys.sleepTimerAt],
            hiddenTopButtons = this[Keys.hiddenTopButtons]?.mapNotNull(TopButton::fromKey)?.toSet() ?: d.hiddenTopButtons,
            language = parseLanguage(this[Keys.language]),
            weatherLocation = decode(WeatherPlace.serializer(), this[Keys.weatherLocation])?.let { WeatherLocation.Manual(it) } ?: WeatherLocation.Auto,
            autoPlace = decode(WeatherPlace.serializer(), this[Keys.autoPlace]),
            autoPlaceSavedAtMillis = this[Keys.autoPlaceAt] ?: 0,
            weatherCache = this[Keys.weatherCache]?.let { CachedJson(it, this[Keys.weatherCacheAt] ?: 0) },
            hdmi = HdmiSettings(
                promptEnabled = this[Keys.hdmiPrompt] ?: d.hdmi.promptEnabled,
                overOtherApps = this[Keys.hdmiOverlay] ?: d.hdmi.overOtherApps,
                mutedInputs = this[Keys.hdmiMuted] ?: d.hdmi.mutedInputs,
            ),
        )
    }

    private fun <T> decode(serializer: kotlinx.serialization.KSerializer<T>, text: String?): T? =
        text?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }

    private fun parseBackground(text: String?): BackgroundChoice = when {
        text == null || text == "theme" -> BackgroundChoice.FromTheme
        text.startsWith("color:") -> text.removePrefix("color:").toLongOrNull(16)?.let { BackgroundChoice.Solid(it.toInt()) } ?: BackgroundChoice.FromTheme
        text.startsWith("image:") -> BackgroundChoice.Image(text.removePrefix("image:"))
        else -> BackgroundChoice.FromTheme
    }

    private fun parseLanguage(text: String?): LanguageChoice =
        if (text in LanguageChoice.supportedTags) LanguageChoice.Fixed(text!!) else LanguageChoice.Follow

    // ---- Favourites (manual order only; nothing reorders them automatically) ----

    suspend fun setFavourites(items: List<ItemKey>) = store.edit { it[Keys.favourites] = items.distinct().joinToString("\n") { k -> k.value } }

    suspend fun updateFavourites(change: (List<ItemKey>) -> List<ItemKey>) = store.edit { prefs ->
        val current = prefs[Keys.favourites]?.split('\n')?.filter { it.isNotBlank() }?.map(::ItemKey).orEmpty()
        prefs[Keys.favourites] = change(current).distinct().joinToString("\n") { it.value }
    }

    suspend fun addFavourite(key: ItemKey) = updateFavourites { it + key }
    suspend fun removeFavourite(key: ItemKey) = updateFavourites { list -> list.filterNot { it == key } }
    suspend fun moveFavourite(key: ItemKey, toIndex: Int) = updateFavourites { Favourites.move(it, key, toIndex) }

    // ---- Hidden apps, shortcuts, launch counters ----

    suspend fun setHidden(packageName: String, hidden: Boolean) = store.edit {
        val now = it[Keys.hidden].orEmpty()
        it[Keys.hidden] = if (hidden) now + packageName else now - packageName
    }

    suspend fun setShortcuts(shortcuts: List<Shortcut>) =
        store.edit { it[Keys.shortcuts] = json.encodeToString(ListSerializer(Shortcut.serializer()), shortcuts) }

    suspend fun recordLaunch(key: ItemKey) = store.edit { prefs ->
        val serializer = MapSerializer(String.serializer(), Int.serializer())
        val counts = decode(serializer, prefs[Keys.launchCounts]).orEmpty().toMutableMap()
        counts[key.value] = (counts[key.value] ?: 0) + 1
        prefs[Keys.launchCounts] = json.encodeToString(serializer, counts)
    }

    // ---- Look and language ----

    suspend fun setTheme(id: ThemeId) = store.edit { it[Keys.theme] = id.key }

    suspend fun setThemeSchedule(schedule: ThemeSchedule) = store.edit {
        it[Keys.scheduleEnabled] = schedule.enabled
        it[Keys.scheduleDay] = schedule.dayTheme.key
        it[Keys.scheduleNight] = schedule.nightTheme.key
    }

    suspend fun setBackground(choice: BackgroundChoice) = store.edit {
        it[Keys.background] = when (choice) {
            BackgroundChoice.FromTheme -> "theme"
            is BackgroundChoice.Solid -> "color:" + (choice.argb.toLong() and 0xFFFFFFFFL).toString(16)
            is BackgroundChoice.Image -> "image:" + choice.fileName
        }
    }

    suspend fun setAvatar(fileName: String?) = store.edit {
        if (fileName == null) it.remove(Keys.avatar) else it[Keys.avatar] = fileName
    }

    suspend fun setSleepTimer(atMillis: Long?) = store.edit {
        if (atMillis == null) it.remove(Keys.sleepTimerAt) else it[Keys.sleepTimerAt] = atMillis
    }

    suspend fun setTopButtonHidden(button: TopButton, hidden: Boolean) = store.edit {
        val now = it[Keys.hiddenTopButtons].orEmpty()
        it[Keys.hiddenTopButtons] = if (hidden) now + button.key else now - button.key
    }

    suspend fun setLanguage(choice: LanguageChoice) = store.edit {
        when (choice) {
            LanguageChoice.Follow -> it.remove(Keys.language)
            is LanguageChoice.Fixed -> it[Keys.language] = choice.tag
        }
    }

    // ---- Weather ----

    suspend fun setWeatherLocation(location: WeatherLocation) = store.edit {
        when (location) {
            WeatherLocation.Auto -> it.remove(Keys.weatherLocation)
            is WeatherLocation.Manual -> it[Keys.weatherLocation] = json.encodeToString(WeatherPlace.serializer(), location.place)
        }
    }

    suspend fun setAutoPlace(place: WeatherPlace, savedAtMillis: Long) = store.edit {
        it[Keys.autoPlace] = json.encodeToString(WeatherPlace.serializer(), place)
        it[Keys.autoPlaceAt] = savedAtMillis
    }

    suspend fun setWeatherCache(cache: CachedJson) = store.edit {
        it[Keys.weatherCache] = cache.json
        it[Keys.weatherCacheAt] = cache.savedAtMillis
    }

    // ---- HDMI ----

    suspend fun setHdmi(settings: HdmiSettings) = store.edit {
        it[Keys.hdmiPrompt] = settings.promptEnabled
        it[Keys.hdmiOverlay] = settings.overOtherApps
        it[Keys.hdmiMuted] = settings.mutedInputs
    }
}

/** Pure list operations for Favourites, kept out of the store so they are trivial to test. */
object Favourites {
    /** Moves [key] so it ends up at [toIndex] (clamped). Unknown keys leave the list unchanged. */
    fun move(list: List<ItemKey>, key: ItemKey, toIndex: Int): List<ItemKey> {
        if (key !in list) return list
        val without = list - key
        return without.toMutableList().apply { add(toIndex.coerceIn(0, without.size), key) }
    }
}
