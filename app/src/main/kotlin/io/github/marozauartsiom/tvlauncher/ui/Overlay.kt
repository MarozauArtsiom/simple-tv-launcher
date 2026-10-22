package io.github.marozauartsiom.tvlauncher.ui

import androidx.compose.ui.geometry.Rect
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey

enum class SettingsPage {
    Root, Theme, Schedule, Background, Language, Weather, Hdmi, Hidden, Usb, About, Shortcuts;

    /** Pages that are built so far. Opening one that is not does nothing, so there are no invisible dead ends. */
    val hasUi: Boolean get() = this == Root || this == Shortcuts
}

/** What is shown over Home. There is no navigation library: Home never goes away, panels open over it. */
sealed interface Overlay {
    data object None : Overlay
    data class Settings(val page: SettingsPage = SettingsPage.Root) : Overlay
    data object Weather : Overlay
    data object WeatherSearch : Overlay
    data class Hdmi(val inputId: String) : Overlay
    /** [anchor] is the tile's bounds on screen, so the menu opens next to it. */
    data class AppMenu(val key: ItemKey, val anchor: Rect? = null) : Overlay
    data class MoveMode(val key: ItemKey) : Overlay
    data object IphoneHint : Overlay

    /** The overlay that Back leads to. [None] means the next Back is handled by Home itself. */
    fun back(): Overlay = when (this) {
        None -> None
        is Settings -> if (page == SettingsPage.Root) None else Settings(SettingsPage.Root)
        WeatherSearch -> Weather
        else -> None
    }
}
