package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * Settings → About: name and version, the no-ads promise, and the licences we owe. The licence rows take
 * focus so the page can be scrolled and left like any other, but pressing OK on them does nothing:
 * there is nothing to open and no network to reach.
 */
@Composable
fun AboutPage(
    theme: ThemeSpec,
    versionName: String,
    versionCode: Long,
    memory: FocusMemory,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request("license:weather")
    }
    val licenses = listOf(
        Triple("license:weather", R.string.license_weather, R.string.license_weather_value),
        Triple("license:font", R.string.license_font, R.string.license_font_value),
        Triple("license:android", R.string.license_android, R.string.license_android_value),
    )
    SidePanel(title = stringResource(R.string.about_title), theme = theme, modifier = modifier) {
        PanelScroll {
            Row(Modifier.padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp, 40.dp).background(c.panelFg, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Hdmi, null, tint = c.panelBg, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, color = c.panelFg)
                    Text(stringResource(R.string.about_version, versionName, versionCode), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
                }
            }
            Text(
                stringResource(R.string.about_text),
                style = MaterialTheme.typography.bodyMedium,
                color = c.panelFg,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            Text(stringResource(R.string.about_licenses), style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(bottom = 4.dp))
            licenses.forEach { (id, title, value) ->
                PanelRow(theme, onClick = {}, modifier = memory.track(id), height = 56.dp) {
                    Icon(Icons.Info, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(value), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
