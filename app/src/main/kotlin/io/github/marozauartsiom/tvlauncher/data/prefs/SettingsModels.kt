package io.github.marozauartsiom.tvlauncher.data.prefs

import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import kotlinx.serialization.Serializable

/**
 * What can sit in Favourites and the app grid: an installed app, or a website shortcut.
 * Stored as `pkg:<package>` / `url:<id>`.
 */
@JvmInline
value class ItemKey(val value: String) {
    val isApp: Boolean get() = value.startsWith("pkg:")
    val isShortcut: Boolean get() = value.startsWith("url:")
    val id: String get() = value.substringAfter(':')

    companion object {
        fun app(packageName: String) = ItemKey("pkg:$packageName")
        fun shortcut(id: String) = ItemKey("url:$id")
    }
}

@Serializable
data class Shortcut(val id: String, val title: String, val url: String)

sealed interface BackgroundChoice {
    /** The theme's own background (the default). */
    data object FromTheme : BackgroundChoice

    /** A solid colour, as ARGB. */
    data class Solid(val argb: Int) : BackgroundChoice

    /** An image copied into app storage (file name inside the backgrounds folder). */
    data class Image(val fileName: String) : BackgroundChoice
}

/** The launcher language. [Follow] uses the TV's language and falls back to English. */
sealed interface LanguageChoice {
    data object Follow : LanguageChoice
    data class Fixed(val tag: String) : LanguageChoice

    companion object {
        /** In the order shown to the user: Belarusian first. */
        val supportedTags = listOf("be", "ru", "en", "pl", "de", "fr")
    }
}

@Serializable
data class WeatherPlace(
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
)

sealed interface WeatherLocation {
    /** Detected from the internet connection (IP geolocation). */
    data object Auto : WeatherLocation
    data class Manual(val place: WeatherPlace) : WeatherLocation
}

data class ThemeSchedule(
    val enabled: Boolean = false,
    val dayTheme: ThemeId = ThemeId.Paper,
    val nightTheme: ThemeId = ThemeId.Space,
)

data class HdmiSettings(
    val promptEnabled: Boolean = true,
    val overOtherApps: Boolean = true,
    /** Inputs the owner switched the prompt off for. */
    val mutedInputs: Set<String> = emptySet(),
)

/** The buttons of the top bar that the owner can hide. Settings (the avatar) always stays: it is the way back. */
enum class TopButton(val key: String) {
    Weather("weather"), Inputs("inputs"), Timer("timer"), Remote("remote"), Iphone("iphone");

    companion object {
        fun fromKey(key: String): TopButton? = entries.firstOrNull { it.key == key }
    }
}

/** A cached network result: the raw JSON (parsed by the weather feature) and when it was stored. */
data class CachedJson(val json: String, val savedAtMillis: Long)

data class SettingsData(
    val favourites: List<ItemKey> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val shortcuts: List<Shortcut> = emptyList(),
    val launchCounts: Map<String, Int> = emptyMap(),
    val theme: ThemeId = ThemeId.Midnight,
    val themeSchedule: ThemeSchedule = ThemeSchedule(),
    val background: BackgroundChoice = BackgroundChoice.FromTheme,
    val language: LanguageChoice = LanguageChoice.Follow,
    val weatherLocation: WeatherLocation = WeatherLocation.Auto,
    val autoPlace: WeatherPlace? = null,
    val autoPlaceSavedAtMillis: Long = 0,
    val weatherCache: CachedJson? = null,
    val hdmi: HdmiSettings = HdmiSettings(),
    /** The owner's photo in the top bar (file name inside the avatar folder), or null for the plain icon. */
    val avatar: String? = null,
    /** When the sleep timer turns the TV off (epoch millis), or null when no timer is set. */
    val sleepTimerAt: Long? = null,
    /** Top-bar buttons the owner switched off. A hidden weather chip also stops the weather fetch. */
    val hiddenTopButtons: Set<TopButton> = emptySet(),
)
