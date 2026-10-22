package io.github.marozauartsiom.tvlauncher.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherPlace
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelTextField
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import kotlinx.coroutines.delay

private const val DEBOUNCE_MS = 400L
private const val MAX_RESULTS = 3

/**
 * Change city: a search field, "Detect automatically" and up to three matches (Gboard takes the bottom of
 * the screen, so the list stays short; refine the query for more). Typing searches after a short pause, and
 * only while this page is open. Back or Escape in the field stops typing; the next Back closes the page.
 */
@Composable
fun CitySearchPage(
    theme: ThemeSpec,
    search: suspend (String) -> List<WeatherPlace>,
    onAuto: () -> Unit,
    onPick: (WeatherPlace) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherPlace>?>(null) }
    val field = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { field.requestFocus() } }
    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = null
        } else {
            delay(DEBOUNCE_MS)
            results = search(query)
        }
    }
    val c = theme.colors
    SidePanel(title = stringResource(R.string.weather_city_title), theme = theme, modifier = modifier) {
        PanelTextField(
            value = query,
            onValueChange = { query = it },
            theme = theme,
            icon = Icons.Search,
            imeAction = ImeAction.Search,
            surfaceFocus = field,
        )
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PanelItem(stringResource(R.string.weather_auto), theme, onAuto, icon = Icons.Location)
            val found = results
            if (found != null && found.isEmpty()) {
                Text(stringResource(R.string.weather_no_results), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
            }
            found.orEmpty().take(MAX_RESULTS).forEach { place ->
                PanelRow(theme, onClick = { onPick(place) }, height = 56.dp) {
                    Icon(Icons.Search, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val detail = listOfNotNull(place.region, place.country).distinct().joinToString(", ")
                        if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
