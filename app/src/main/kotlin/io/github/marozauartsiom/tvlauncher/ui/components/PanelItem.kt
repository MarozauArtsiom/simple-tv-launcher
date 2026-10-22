package io.github.marozauartsiom.tvlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

private val ItemShape = RoundedCornerShape(10.dp)
val PanelItemHeight = 44.dp

/**
 * The frame shared by rows and tiles in a panel: [height] dp high, [cornerRadius] corners, the item
 * colour, and in focus the theme's lighter fill, a 3 dp ring and ×1.03, never a full white fill.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PanelFrame(
    theme: ThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = PanelItemHeight,
    cornerRadius: Dp = 10.dp,
    transparent: Boolean = false,
    focus: FocusProgress = rememberFocusProgress(),
    content: @Composable () -> Unit,
) {
    val c = theme.colors
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(focus.track())
            .graphicsLayer {
                val scale = 1f + 0.03f * focus.value
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val p = focus.value
                if (p > 0f) {
                    val w = 3.dp.toPx()
                    drawRoundRect(
                        color = c.itemFocusRing.copy(alpha = p),
                        topLeft = Offset(-w / 2f, -w / 2f),
                        size = Size(size.width + w, size.height + w),
                        cornerRadius = CornerRadius(cornerRadius.toPx() + w / 2f),
                        style = Stroke(w),
                    )
                }
            },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (transparent) Color.Transparent else c.itemBg,
            focusedContainerColor = c.itemFocusFill,
            pressedContainerColor = c.itemFocusFill,
            contentColor = c.panelFg,
            focusedContentColor = c.panelFg,
            pressedContentColor = c.panelFg,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) { content() }
}

/** A row: [content] is laid out in a 12 dp padded row, 12 dp apart. */
@Composable
fun PanelRow(
    theme: ThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = PanelItemHeight,
    transparent: Boolean = false,
    focus: FocusProgress = rememberFocusProgress(),
    content: @Composable RowScope.() -> Unit,
) {
    PanelFrame(theme, onClick, modifier, height, 10.dp, transparent, focus) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** A quick tile: 58 dp high, 12 dp corners, a 20 dp icon over a one-line label. */
@Composable
fun PanelTile(
    label: String,
    icon: ImageVector,
    theme: ThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusProgress = rememberFocusProgress(),
) {
    PanelFrame(theme, onClick, modifier, height = 58.dp, cornerRadius = 12.dp, focus = focus) {
        Column(Modifier.fillMaxWidth().height(58.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            Icon(icon, contentDescription = null, tint = theme.colors.panelFg, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = theme.colors.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** A one-line row: optional 20 dp icon, the title, an optional muted value. */
@Composable
fun PanelItem(
    title: String,
    theme: ThemeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    value: String? = null,
    transparent: Boolean = false,
    /** Shows the check mark of the chosen item (and tells TalkBack it is selected). */
    selected: Boolean = false,
    focus: FocusProgress = rememberFocusProgress(),
) {
    val c = theme.colors
    PanelRow(theme, onClick, modifier.semantics { this.selected = selected }, PanelItemHeight, transparent, focus) {
        if (icon != null) Icon(icon, contentDescription = null, tint = c.panelFg, modifier = Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = c.panelFg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1)
        if (selected) Icon(Icons.Check, contentDescription = null, tint = c.signal, modifier = Modifier.size(20.dp))
    }
}

/** An on/off switch drawn like the mockup (44×24 pill). It shows state only; the row it sits in handles OK. */
@Composable
fun PanelSwitch(checked: Boolean, theme: ThemeSpec, modifier: Modifier = Modifier) {
    val c = theme.colors
    androidx.compose.foundation.layout.Box(
        modifier
            .size(44.dp, 24.dp)
            .background(if (checked) c.signal else c.panelMuted.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .padding(start = if (checked) 23.dp else 3.dp, top = 3.dp)
                .size(18.dp)
                .background(c.panelBg, androidx.compose.foundation.shape.CircleShape),
        )
    }
}
