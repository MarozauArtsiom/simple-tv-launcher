package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.FocusProgress
import io.github.marozauartsiom.tvlauncher.ui.components.focusRing
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.components.rememberFocusProgress
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeFamily
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes

private val ThumbWidth = 148.dp
private val ThumbHeight = 83.dp
private val families = listOf(
    ThemeFamily.Dark to R.string.family_dark,
    ThemeFamily.Light to R.string.family_light,
    ThemeFamily.Cartoon to R.string.family_cartoon,
    ThemeFamily.DarkCartoon to R.string.family_dark_cartoon,
    ThemeFamily.Halloween to R.string.family_halloween,
)

/**
 * The theme picker: one row of thumbnails per family. Focusing a thumbnail previews that theme on the whole launcher ([onPreview]),
 * OK chooses it ([onChoose]), and leaving the page (Back) clears the preview, so the saved theme returns.
 *
 * [chosen] is the theme with the check mark. When [onOpenSchedule] is set, the "change by time of day"
 * row is shown below the thumbnails, with [scheduleValue] at its right.
 *
 * [theme] is the theme currently drawn (the previewed one), so the panel itself follows the preview.
 */
@Composable
fun ThemePickerPage(
    theme: ThemeSpec,
    title: String,
    chosen: ThemeId,
    onPreview: (ThemeId?) -> Unit,
    onChoose: (ThemeId) -> Unit,
    memory: FocusMemory,
    modifier: Modifier = Modifier,
    scheduleValue: String? = null,
    onOpenSchedule: (() -> Unit)? = null,
) {
    DisposableEffect(Unit) { onDispose { onPreview(null) } }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request("theme:${chosen.key}")
    }
    SidePanel(title = title, theme = theme, modifier = modifier, width = 560.dp, dim = 0.15f) {
        PanelScroll {
            families.forEach { (family, titleRes) ->
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = theme.colors.panelMuted,
                    modifier = Modifier.padding(top = if (family == ThemeFamily.Dark) 0.dp else 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Themes.all.filter { it.family == family }.forEach { candidate ->
                        ThemeThumbnail(
                            spec = candidate,
                            panel = theme,
                            isChosen = candidate.id == chosen,
                            onFocused = { onPreview(candidate.id) },
                            onClick = { onChoose(candidate.id) },
                            modifier = memory.track("theme:${candidate.id.key}"),
                        )
                    }
                }
            }
            if (onOpenSchedule != null) {
                PanelItem(
                    title = stringResource(R.string.theme_schedule_item),
                    theme = theme,
                    icon = Icons.Schedule,
                    value = scheduleValue,
                    onClick = onOpenSchedule,
                    modifier = memory.track("schedule").padding(top = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ThemeThumbnail(
    spec: ThemeSpec,
    panel: ThemeSpec,
    isChosen: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusProgress = rememberFocusProgress(),
) {
    val name = stringResource(spec.nameRes)
    val shape = remember { RoundedCornerShape(10.dp) }
    val ring = panel.colors.itemFocusRing
    LaunchedEffect(focus.focused) { if (focus.focused) onFocused() }
    Box(modifier.zIndex(if (focus.focused) 1f else 0f)) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(ThumbWidth, ThumbHeight)
                .then(focus.track())
                .focusRing(focus, ring, 10.dp, growth = 0.06f)
                .semantics { contentDescription = name; selected = isChosen },
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        ) {
            Box(Modifier.size(ThumbWidth, ThumbHeight).clip(shape)) {
                Image(painterResource(spec.thumbnailRes), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(ThumbWidth, ThumbHeight))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color(0xB80A0C10), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                )
            }
        }
        if (isChosen) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(6.dp, (-6).dp)
                    .size(24.dp)
                    .background(panel.colors.signal, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Check, contentDescription = null, tint = Color(0xFF0B0D12), modifier = Modifier.size(16.dp))
            }
        }
    }
}
