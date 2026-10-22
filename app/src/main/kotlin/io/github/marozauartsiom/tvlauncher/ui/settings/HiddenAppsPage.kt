package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.apps.ArtSource
import io.github.marozauartsiom.tvlauncher.data.apps.BannerArt
import io.github.marozauartsiom.tvlauncher.data.apps.TvApp
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * Settings → Hidden apps: each hidden app with a mini banner (72×40), its name and "Show"; OK brings it
 * back to Home and removes the row. With none, "No hidden apps". Apps that are hidden but no longer
 * installed are not listed (they cost nothing and vanish from the count).
 */
@Composable
fun HiddenAppsPage(
    theme: ThemeSpec,
    apps: List<TvApp>,
    art: ArtSource,
    memory: FocusMemory,
    onShow: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    val current by rememberUpdatedState(apps)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) current.firstOrNull()?.let { memory.request(rowId(it)) }
    }
    // After a row disappears, focus goes to the row now in its place (or the one above it).
    var lastIndex = remember { intArrayOf(0) }
    LaunchedEffect(apps.map { it.packageName }) {
        val focused = memory.last
        if (focused != null && apps.none { rowId(it) == focused } && apps.isNotEmpty()) {
            withFrameNanos { }
            memory.request(rowId(apps[lastIndex[0].coerceIn(0, apps.lastIndex)]))
        }
    }
    SidePanel(title = stringResource(R.string.item_hidden), theme = theme, modifier = modifier) {
        PanelScroll {
            if (apps.isEmpty()) {
                Text(stringResource(R.string.hidden_empty), style = MaterialTheme.typography.titleMedium, color = c.panelMuted)
            }
            apps.forEachIndexed { index, app ->
                PanelRow(
                    theme,
                    onClick = { lastIndex[0] = index; onShow(app) },
                    modifier = memory.track(rowId(app)),
                    height = 56.dp,
                ) {
                    MiniBanner(app, art)
                    Text(app.label, style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.show), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1)
                }
            }
            Text(
                stringResource(R.string.hidden_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = c.panelMuted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

private fun rowId(app: TvApp) = "hidden:${app.packageName}"

@Composable
private fun MiniBanner(app: TvApp, source: ArtSource) {
    val art: BannerArt? by produceState(source.peek(app), app.packageName) { value = source.load(app) }
    val shape = remember { RoundedCornerShape(8.dp) }
    Box(Modifier.size(72.dp, 40.dp).clip(shape)) {
        art?.let { Image(it.bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp, 40.dp)) }
    }
}
