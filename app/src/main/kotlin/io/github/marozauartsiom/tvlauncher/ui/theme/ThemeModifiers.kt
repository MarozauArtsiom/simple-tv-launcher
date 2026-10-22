package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp

/**
 * Draws a theme [ShadowSpec] behind the element. Hard shadows (blur 0, the cartoon themes) are drawn
 * as an offset rounded rectangle, which is cheap. Soft shadows and glows use the platform elevation
 * shadow tinted with the spec's colour; Android renders it once per layer, not per frame.
 */
fun Modifier.themeShadow(shadow: ShadowSpec?, cornerRadius: Dp): Modifier = when {
    shadow == null -> this
    shadow.blur.value == 0f -> drawBehind {
        val spread = shadow.spread.toPx()
        drawRoundRect(
            color = shadow.color,
            topLeft = Offset(-spread, shadow.offsetY.toPx() - spread),
            size = Size(size.width + 2 * spread, size.height + 2 * spread),
            cornerRadius = CornerRadius(cornerRadius.toPx() + spread),
        )
    }
    else -> shadow(
        elevation = shadow.blur / 2,
        shape = RoundedCornerShape(cornerRadius),
        clip = false,
        ambientColor = shadow.color,
        spotColor = shadow.color,
    )
}
