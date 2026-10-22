package io.github.marozauartsiom.tvlauncher.i18n

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.Launcher
import java.time.LocalDate
import org.junit.Test

/** The longest strings of the settings panel in all six languages, to catch clipping early. */
class LanguageSampleScreenshotTest : TvScreenshotTest() {
    @Test
    fun settingsStringsInAllLanguages() = captureLocales("language-sample") { tag -> Sample(tag) }
}

@Composable
private fun Sample(tag: String) {
    val c = Launcher.colors
    Row(Modifier.fillMaxSize().background(c.panelBg)) {
        Column(Modifier.width(560.dp).fillMaxHeight().padding(start = 48.dp, top = 27.dp, end = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(DateText.format(LocalDate.of(2026, 10, 21), tag), style = MaterialTheme.typography.titleMedium, color = c.panelMuted)
            Text(stringResource(R.string.all_apps), style = MaterialTheme.typography.titleLarge, color = c.panelFg)
            Text(stringResource(R.string.favourites_empty_hint), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
            Text(stringResource(R.string.hdmi_prompt_title, "HDMI 2"), style = MaterialTheme.typography.titleMedium, color = c.panelFg)
            Text(stringResource(R.string.weather_feels, "+4°", "5"), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
            Text(pluralStringResource(R.plurals.usb_files, 3, 3), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
            Text(stringResource(R.string.schedule_note_fixed), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
        }
        Column(Modifier.width(400.dp).fillMaxHeight().padding(top = 27.dp, end = 48.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, color = c.panelFg, modifier = Modifier.padding(bottom = 8.dp))
            listOf(
                R.string.theme_schedule_item, R.string.item_hidden, R.string.item_usb, R.string.item_hdmi,
                R.string.item_shortcuts, R.string.language_follow_tv, R.string.tile_sleep, R.string.theme_valley, R.string.theme_ocean,
            ).forEach { id ->
                Box(Modifier.fillMaxWidth().background(c.itemBg, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                    Text(stringResource(id), style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
