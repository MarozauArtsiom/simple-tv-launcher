package io.github.marozauartsiom.tvlauncher.ui.home

import io.github.marozauartsiom.tvlauncher.data.prefs.TopButton
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import io.github.marozauartsiom.tvlauncher.data.apps.TvApp
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut
import io.github.marozauartsiom.tvlauncher.data.weather.Condition

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
    /** Installed apps the owner hid, by name: the collapsed "Hidden" section at the bottom of Home. */
    val hidden: List<HomeTile> = emptyList(),
) {
    val isEmpty: Boolean get() = favourites.isEmpty() && all.isEmpty()
}

/** Weather chip data, supplied by the weather feature once it lands. */
@Immutable
data class WeatherChip(
    val temperature: String,
    val place: String?,
    val description: String,
    val condition: Condition = Condition.PartlyCloudy,
    val isDay: Boolean = true,
)

/** One HDMI input chip. [hasSignal] draws the green dot. */
@Immutable
data class HdmiChip(val id: String, val label: String, val hasSignal: Boolean)

/** Everything the top bar shows besides the clock. */
@Immutable
data class TopBarState(
    val weather: WeatherChip? = null,
    val hdmi: List<HdmiChip> = emptyList(),
    val showIphone: Boolean = true,
    /** The owner's photo, shown in the round Settings button at the right end; null shows a person icon. */
    val avatar: ImageBitmap? = null,
    /** When the sleep timer turns the TV off (epoch millis), or null. */
    val sleepTimerAt: Long? = null,
    /** The "Remote" button (phone remote & AI access). */
    val showRemote: Boolean = false,
    /** The phone remote or AI access is on: the Remote button gets a dot. */
    val remoteOn: Boolean = false,
    /** Who is connected right now: the Remote button says so ("Remote · AI is controlling") instead of the badge. */
    val remotePhoneConnected: Boolean = false,
    val remoteAiConnected: Boolean = false,
    /** Buttons the owner hid in Settings → Top bar. */
    val hidden: Set<TopButton> = emptySet(),
)
