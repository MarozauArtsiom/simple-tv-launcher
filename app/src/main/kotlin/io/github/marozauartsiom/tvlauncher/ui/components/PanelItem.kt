package io.github.marozauartsiom.tvlauncher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
 * The frame of a row in a panel or menu: [height] dp high, the item colour, and in focus the theme's
 * lighter fill, a 3 dp ring and ×1.03, never a full white fill. [content] is laid out in a padded row.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
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
    val c = theme.colors
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
                        cornerRadius = CornerRadius(10.dp.toPx() + w / 2f),
                        style = Stroke(w),
                    )
                }
            },
        shape = ClickableSurfaceDefaults.shape(ItemShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (transparent) Color.Transparent else c.itemBg,
            focusedContainerColor = c.itemFocusFill,
            pressedContainerColor = c.itemFocusFill,
            contentColor = c.panelFg,
            focusedContentColor = c.panelFg,
            pressedContentColor = c.panelFg,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
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
    focus: FocusProgress = rememberFocusProgress(),
) {
    val c = theme.colors
    PanelRow(theme, onClick, modifier, PanelItemHeight, transparent, focus) {
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
    }
}
