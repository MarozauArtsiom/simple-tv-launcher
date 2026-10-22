package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.BackgroundChoice
import io.github.marozauartsiom.tvlauncher.data.prefs.LanguageChoice
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherLocation
import io.github.marozauartsiom.tvlauncher.system.SystemTile
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import io.github.marozauartsiom.tvlauncher.ui.SettingsPage
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.PanelTile
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/** The muted values shown at the right of the launcher rows. Null hides the value. */
@Immutable
data class SettingsValues(
    val theme: String,
    val background: String,
    val language: String,
    val weather: String?,
    val hdmi: String,
    val hiddenCount: Int,
    val shortcutsCount: Int,
)

private fun SystemTile.icon(): ImageVector = when (this) {
    SystemTile.Wifi -> Icons.Wifi
    SystemTile.Bluetooth -> Icons.Bluetooth
    SystemTile.Sound -> Icons.Sound
    SystemTile.Picture -> Icons.Picture
    SystemTile.Sleep -> Icons.Sleep
    SystemTile.Android -> Icons.Gear
}

private fun SystemTile.label(): Int = when (this) {
    SystemTile.Wifi -> R.string.tile_wifi
    SystemTile.Bluetooth -> R.string.tile_bluetooth
    SystemTile.Sound -> R.string.tile_sound
    SystemTile.Picture -> R.string.tile_picture
    SystemTile.Sleep -> R.string.tile_sleep
    SystemTile.Android -> R.string.tile_android
}

private const val Columns = 3

private class Entry(val page: SettingsPage, val icon: ImageVector, @StringRes val title: Int, val value: String?)

/**
 * The settings panel: the TV's quick tiles (3 per row, only the ones this TV can open), then the
 * launcher's own pages. Focus starts on the first tile, or comes back to the row that was open when
 * a page is closed ([memory] outlives this composable for that).
 */
@Composable
fun SettingsPanel(
    theme: ThemeSpec,
    tiles: List<SystemTile>,
    values: SettingsValues,
    memory: FocusMemory,
    onTile: (SystemTile) -> Unit,
    onPage: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request(tiles.firstOrNull()?.let { "tile:$it" } ?: "page:${SettingsPage.Theme}")
    }
    val c = theme.colors
    SidePanel(title = stringResource(R.string.settings_title), theme = theme, modifier = modifier) {
        PanelScroll {
            if (tiles.isNotEmpty()) {
                SectionLabel(stringResource(R.string.section_tv), theme, first = true)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tiles.chunked(Columns).forEach { rowTiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTiles.forEach { tile ->
                                PanelTile(stringResource(tile.label()), tile.icon(), theme, { onTile(tile) }, memory.track("tile:$tile").weight(1f))
                            }
                            repeat(Columns - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            SectionLabel(stringResource(R.string.section_launcher), theme, first = tiles.isEmpty())
            val items = listOf(
                Entry(SettingsPage.Theme, Icons.Palette, R.string.item_theme, values.theme),
                Entry(SettingsPage.Background, Icons.Image, R.string.item_background, values.background),
                Entry(SettingsPage.Language, Icons.Language, R.string.item_language, values.language),
                Entry(SettingsPage.Weather, Icons.Cloud, R.string.item_weather, values.weather),
                Entry(SettingsPage.Hdmi, Icons.Hdmi, R.string.item_hdmi, values.hdmi),
                Entry(SettingsPage.Hidden, Icons.Hidden, R.string.item_hidden, values.hiddenCount.takeIf { it > 0 }?.toString()),
                Entry(SettingsPage.Shortcuts, Icons.Link, R.string.item_shortcuts, values.shortcutsCount.takeIf { it > 0 }?.toString()),
                Entry(SettingsPage.Usb, Icons.Usb, R.string.item_usb, null),
                Entry(SettingsPage.About, Icons.Info, R.string.item_about, null),
            )
            items.forEach { entry ->
                PanelRow(theme, onClick = { onPage(entry.page) }, modifier = memory.track("page:${entry.page}")) {
                    Icon(entry.icon, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(entry.title),
                        style = MaterialTheme.typography.titleMedium,
                        color = c.panelFg,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (entry.value != null) Text(entry.value, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1)
                    Icon(Icons.Chevron, null, tint = c.panelMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, theme: ThemeSpec, first: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = theme.colors.panelMuted,
        // 16 dp above (10 for the first: the scroll area already adds 6) and 8 below, minus the 4 dp the column adds
        modifier = Modifier.padding(top = if (first) 10.dp else 12.dp, bottom = 4.dp),
    )
}

/** The right-hand values of the launcher rows, from the saved settings (needs resources, so a composable). */
@Composable
fun settingsValues(settings: SettingsData, themeId: io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId = settings.theme): SettingsValues {
    val theme = Themes.byId(themeId)
    val language = when (val choice = settings.language) {
        LanguageChoice.Follow -> stringResource(R.string.language_follow_tv)
        is LanguageChoice.Fixed -> languageName(choice.tag)
    }
    val background = when (settings.background) {
        BackgroundChoice.FromTheme -> stringResource(R.string.value_from_theme)
        is BackgroundChoice.Solid -> stringResource(R.string.bg_colors)
        is BackgroundChoice.Image -> stringResource(R.string.item_background)
    }
    val place = when (val w = settings.weatherLocation) {
        WeatherLocation.Auto -> settings.autoPlace?.name
        is WeatherLocation.Manual -> w.place.name
    }
    return SettingsValues(
        theme = stringResource(theme.nameRes),
        background = background,
        language = language,
        weather = place,
        hdmi = stringResource(if (settings.hdmi.promptEnabled) R.string.on else R.string.off),
        hiddenCount = settings.hidden.size,
        shortcutsCount = settings.shortcuts.size,
    )
}

@Composable
private fun languageName(tag: String): String = when (tag) {
    "be" -> stringResource(R.string.lang_be)
    "ru" -> stringResource(R.string.lang_ru)
    "pl" -> stringResource(R.string.lang_pl)
    "de" -> stringResource(R.string.lang_de)
    "fr" -> stringResource(R.string.lang_fr)
    else -> stringResource(R.string.lang_en)
}
