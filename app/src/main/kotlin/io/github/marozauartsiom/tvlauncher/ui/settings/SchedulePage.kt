package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.SunTimes
import io.github.marozauartsiom.tvlauncher.data.prefs.ThemeSchedule
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.PanelSwitch
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import java.time.format.DateTimeFormatter

private val HourMinute = DateTimeFormatter.ofPattern("HH:mm")

/**
 * "Theme by time of day": a switch, the day theme and the evening/night theme (each opens the theme
 * picker), and a note with today's sunset and sunrise, or the fixed 07:00/20:00 without weather data.
 */
@Composable
fun SchedulePage(
    theme: ThemeSpec,
    schedule: ThemeSchedule,
    sun: SunTimes?,
    memory: FocusMemory,
    onToggle: (Boolean) -> Unit,
    onPickDay: () -> Unit,
    onPickNight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request("switch")
    }
    val c = theme.colors
    SidePanel(title = stringResource(R.string.schedule_title), theme = theme, modifier = modifier) {
        PanelScroll {
            PanelRow(theme, onClick = { onToggle(!schedule.enabled) }, modifier = memory.track("switch")) {
                Icon(Icons.Schedule, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.schedule_switch), style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, modifier = Modifier.weight(1f))
                PanelSwitch(schedule.enabled, theme)
            }
            ThemeRow(stringResource(R.string.schedule_day), schedule.dayTheme, stringResource(R.string.schedule_until_sunset), theme, memory, "day", onPickDay)
            ThemeRow(stringResource(R.string.schedule_night), schedule.nightTheme, stringResource(R.string.schedule_after_sunset), theme, memory, "night", onPickNight)
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .background(c.itemBg, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Sunrise, null, tint = c.panelMuted, modifier = Modifier.padding(top = 2.dp).size(20.dp))
                val note = if (sun != null && sun.isSane) {
                    stringResource(R.string.schedule_note, HourMinute.format(sun.sunset), HourMinute.format(sun.sunrise))
                } else {
                    stringResource(R.string.schedule_note_fixed)
                }
                Text(note, style = MaterialTheme.typography.bodyMedium, color = c.panelFg)
            }
        }
    }
}

@Composable
private fun ThemeRow(
    label: String,
    id: io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId,
    value: String,
    theme: ThemeSpec,
    memory: FocusMemory,
    memoryId: String,
    onClick: () -> Unit,
) {
    val c = theme.colors
    Text(label, style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(top = 4.dp))
    PanelRow(theme, onClick = onClick, modifier = memory.track(memoryId)) {
        Icon(Icons.Palette, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
        Text(stringResource(Themes.byId(id).nameRes), style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1)
        Icon(Icons.Chevron, null, tint = c.panelMuted, modifier = Modifier.size(18.dp))
    }
}
