package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.ui.components.FocusProgress
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.rememberFocusProgress
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.themeShadow

val SafeHorizontal = 48.dp
val SafeVertical = 27.dp
val TopBarHeight = 71.dp

/** Clock and date on the left, weather chip next to them, HDMI/iPhone/settings chips on the right. */
@Composable
fun TopBar(
    state: TopBarState,
    clock: () -> ClockText,
    theme: ThemeSpec,
    onWeather: () -> Unit,
    onHdmi: (String) -> Unit,
    onIphone: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TopBarHeight)
            .padding(start = SafeHorizontal, end = SafeHorizontal, top = SafeVertical),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ClockAndDate(clock, theme)
            state.weather?.let { w ->
                val description = stringResource(R.string.cd_weather, w.description)
                TopChip(theme, onWeather, Modifier.semantics { contentDescription = description }) {
                    WeatherIcon(theme)
                    val meta = theme.colors.chipMeta
                    Text(
                        text = remember(w, meta) {
                            buildAnnotatedString {
                                append(w.temperature)
                                w.place?.let { withStyle(SpanStyle(color = meta, fontWeight = FontWeight.Normal)) { append(" · $it") } }
                            }
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = theme.colors.chipFg,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            state.hdmi.forEach { input ->
                val signalText = if (input.hasSignal) stringResource(R.string.cd_hdmi_signal, input.label) else input.label
                TopChip(theme, { onHdmi(input.id) }, Modifier.semantics { contentDescription = signalText }) {
                    ChipIcon(Icons.Hdmi, theme)
                    Text(input.label, style = MaterialTheme.typography.labelLarge, color = theme.colors.chipFg)
                    if (input.hasSignal) Box(Modifier.size(8.dp).background(theme.colors.signal, CircleShape))
                }
            }
            if (state.showIphone) {
                val description = stringResource(R.string.cd_iphone)
                TopChip(theme, onIphone, Modifier.semantics { contentDescription = description }) {
                    ChipIcon(Icons.Phone, theme)
                    Text(stringResource(R.string.iphone_chip), style = MaterialTheme.typography.labelLarge, color = theme.colors.chipFg)
                }
            }
            val settings = stringResource(R.string.cd_settings)
            TopChip(theme, onSettings, Modifier.semantics { contentDescription = settings }, iconOnly = true) {
                ChipIcon(Icons.Gear, theme)
            }
        }
    }
}

@Composable
private fun ClockAndDate(clock: () -> ClockText, theme: ThemeSpec) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = clock().time,
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = theme.clockWeight),
            color = theme.colors.onSurface,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = clock().date,
            style = MaterialTheme.typography.titleMedium,
            color = theme.colors.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun ChipIcon(icon: ImageVector, theme: ThemeSpec) {
    Icon(icon, contentDescription = null, tint = theme.colors.chipFg, modifier = Modifier.size(18.dp))
}

@Composable
private fun WeatherIcon(theme: ThemeSpec) {
    val sun = theme.colors.weatherSun.takeIf { it != Color.Unspecified } ?: theme.colors.chipFg
    val cloud = theme.colors.weatherCloud.takeIf { it != Color.Unspecified } ?: theme.colors.chipFg
    Box(Modifier.size(18.dp)) {
        Icon(Icons.WeatherSun, null, tint = sun.copy(alpha = sun.alpha * 0.7f), modifier = Modifier.size(18.dp))
        Icon(Icons.WeatherCloud, null, tint = cloud, modifier = Modifier.size(18.dp))
    }
}

/** A top-bar chip: 36 dp high pill, themed colours and shadow; in focus the theme ring and ×1.05. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopChip(
    theme: ThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
    focus: FocusProgress = rememberFocusProgress(),
    content: @Composable RowScope.() -> Unit,
) {
    val shape = remember { RoundedCornerShape(18.dp) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(focus.track())
            .graphicsLayer {
                val scale = 1f + 0.05f * focus.value
                scaleX = scale
                scaleY = scale
            }
            .themeShadow(theme.chipShadow, 18.dp)
            .drawBehind {
                val p = focus.value
                if (theme.chipRingWidth.value > 0f && p < 1f) {
                    drawRing(theme.colors.chipRing, theme.chipRingWidth.toPx(), 18.dp.toPx(), inside = true)
                }
                if (p > 0f) drawRing(theme.colors.focus.copy(alpha = p), 3.dp.toPx(), 18.dp.toPx())
            },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = theme.colors.chipBg,
            focusedContainerColor = theme.colors.chipBg,
            pressedContainerColor = theme.colors.chipBg,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            modifier = Modifier.height(36.dp).then(if (iconOnly) Modifier.width(36.dp) else Modifier.padding(horizontal = 14.dp)),
            horizontalArrangement = if (iconOnly) Arrangement.Center else Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
