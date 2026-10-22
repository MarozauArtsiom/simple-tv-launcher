package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.runtime.Immutable
import io.github.marozauartsiom.tvlauncher.data.apps.TvApp
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut

/** One tile on Home: an installed app or a website shortcut. */
@Immutable
data class HomeTile(
    val key: ItemKey,
    val label: String,
    val app: TvApp? = null,
    val shortcut: Shortcut? = null,
)

/** What Home shows. [favourites] keep the owner's manual order; [all] is sorted by launch count. */
@Immutable
data class HomeContent(
    val favourites: List<HomeTile> = emptyList(),
    val all: List<HomeTile> = emptyList(),
) {
    val isEmpty: Boolean get() = favourites.isEmpty() && all.isEmpty()
}

/** Weather chip data, supplied by the weather feature once it lands. */
@Immutable
data class WeatherChip(val temperature: String, val place: String?, val description: String)

/** One HDMI input chip. [hasSignal] draws the green dot. */
@Immutable
data class HdmiChip(val id: String, val label: String, val hasSignal: Boolean)

/** Everything the top bar shows besides the clock. */
@Immutable
data class TopBarState(
    val weather: WeatherChip? = null,
    val hdmi: List<HdmiChip> = emptyList(),
    val showIphone: Boolean = true,
)
