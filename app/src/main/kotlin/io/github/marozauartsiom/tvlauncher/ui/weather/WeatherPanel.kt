package io.github.marozauartsiom.tvlauncher.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherCodes
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherData
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherView
import io.github.marozauartsiom.tvlauncher.ui.components.PanelButton
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import java.time.format.DateTimeFormatter

private val HourFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The weather panel: the city, now (picture, temperature, wording, feels-like and wind), four 3-hour
 * steps, five days, and "Change city". It carries no text about the data source or how the place was
 * found (the owner's request; the credit is in Settings → About). Focus starts on the button.
 */
@Composable
fun WeatherPanel(
    theme: ThemeSpec,
    view: WeatherView,
    languageTag: String,
    onChangeCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val button = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { button.requestFocus() } }
    val c = theme.colors
    SidePanel(title = view.place ?: stringResource(R.string.item_weather), theme = theme, modifier = modifier) {
        val data = view.data
        if (data == null) {
            Text(stringResource(R.string.weather_no_data), style = MaterialTheme.typography.titleMedium, color = c.panelMuted)
        } else {
            Now(theme, data)
            Hours(theme, data)
            Days(theme, data, languageTag)
        }
        Spacer(Modifier.weight(1f))
        PanelButton(stringResource(R.string.weather_change_city), theme, onChangeCity, Modifier.focusRequester(button))
    }
}

@Composable
private fun Now(theme: ThemeSpec, data: WeatherData) {
    val c = theme.colors
    val current = data.current
    val condition = WeatherCodes.condition(current.code)
    Row(Modifier.padding(top = 0.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(WeatherIcons.of(condition, current.isDay), contentDescription = null, tint = c.panelFg, modifier = Modifier.size(44.dp))
        Text(WeatherText.temperature(current.temperature), style = MaterialTheme.typography.displaySmall, color = c.panelFg)
    }
    Text(stringResource(WeatherText.conditionRes(condition)), style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1)
    Text(
        stringResource(R.string.weather_feels, WeatherText.temperature(current.feelsLike), WeatherText.wind(current.windMs)),
        style = MaterialTheme.typography.bodyMedium,
        color = c.panelMuted,
        maxLines = 1,
    )
}

@Composable
private fun Hours(theme: ThemeSpec, data: WeatherData) {
    if (data.hours.isEmpty()) return
    val c = theme.colors
    Row(
        Modifier.padding(top = 16.dp).fillMaxWidth().background(c.itemBg, RoundedCornerShape(12.dp)).padding(vertical = 10.dp),
    ) {
        data.hours.forEach { hour ->
            val condition = WeatherCodes.condition(hour.code)
            val description = stringResource(WeatherText.conditionRes(condition))
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "${HourFormat.format(hour.time)}, $description, ${WeatherText.temperature(hour.temperature)}" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(HourFormat.format(hour.time), style = MaterialTheme.typography.labelMedium, color = c.panelMuted, maxLines = 1)
                Icon(WeatherIcons.of(condition, hour.isDay), null, tint = c.panelFg, modifier = Modifier.size(24.dp))
                Text(WeatherText.temperature(hour.temperature), style = MaterialTheme.typography.labelLarge, color = c.panelFg, maxLines = 1)
            }
        }
    }
}

@Composable
private fun Days(theme: ThemeSpec, data: WeatherData, languageTag: String) {
    val c = theme.colors
    Column(Modifier.padding(top = 12.dp)) {
        data.days.forEachIndexed { index, day ->
            val condition = WeatherCodes.condition(day.code)
            val name = if (index == 0) stringResource(R.string.weather_today) else WeatherText.weekday(day.date, languageTag)
            Row(
                Modifier.height(28.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, color = c.panelFg, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(WeatherIcons.of(condition, true), null, tint = c.panelFg, modifier = Modifier.size(22.dp))
                Text(WeatherText.temperature(day.max), style = MaterialTheme.typography.bodyMedium, color = c.panelFg, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(48.dp))
                Text(WeatherText.temperature(day.min), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(40.dp))
            }
        }
    }
}
