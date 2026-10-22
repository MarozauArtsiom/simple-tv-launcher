package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

private data class Layer(val spec: ThemeSpec, val bitmap: ImageBitmap?)

private const val CROSSFADE_MS = 300

/**
 * Draws the theme's background. When the theme changes it crossfades from the previous background
 * in 300 ms; the first draw has no animation. Bitmaps come from [cache], decoded off the main thread.
 */
@Composable
fun ThemedBackground(spec: ThemeSpec, cache: BackgroundCache, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf(Layer(spec, cache.peek(spec.background))) }
    var previous by remember { mutableStateOf<Layer?>(null) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(spec.id) {
        val bitmap = cache.peek(spec.background) ?: cache.load(spec.background)
        if (current.spec.id == spec.id) {
            current = Layer(spec, bitmap) // first load of the initial theme: no fade
        } else {
            previous = current
            current = Layer(spec, bitmap)
            fade.snapTo(0f)
            fade.animateTo(1f, tween(CROSSFADE_MS))
            previous = null
        }
    }

    Canvas(modifier.fillMaxSize().clipToBounds()) {
        previous?.let { drawLayer(it, 1f) }
        drawLayer(current, if (previous != null) fade.value else 1f)
    }
}

private fun DrawScope.drawLayer(layer: Layer, alpha: Float) {
    when (val bg = layer.spec.background) {
        is ImageBackground -> {
            layer.bitmap?.let {
                drawImage(
                    it, dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    alpha = alpha, filterQuality = FilterQuality.Low,
                )
            }
            if (bg.scrim.isNotEmpty()) {
                drawRect(Brush.verticalGradient(*bg.scrim.toTypedArray()), alpha = alpha)
            }
        }
        is RadialBackground -> drawRadial(bg, alpha)
    }
}

/** CSS `radial-gradient(RX RY at CX CY, inner 0%, outer STOP%)` on a 960x540 dp canvas. */
private fun DrawScope.drawRadial(bg: RadialBackground, alpha: Float) {
    val rx = bg.radiusX.toPx() * (size.width / (960f * density))
    val ry = bg.radiusY.toPx() * (size.height / (540f * density))
    val center = Offset(bg.centerX * size.width, bg.centerY * size.height)
    withTransform({ scale(1f, ry / rx, pivot = center) }) {
        drawRect(
            brush = Brush.radialGradient(0f to bg.inner, bg.stop to bg.outer, 1f to bg.outer, center = center, radius = rx),
            topLeft = Offset(-size.width * 3, -size.height * 12),
            size = Size(size.width * 7, size.height * 26),
            alpha = alpha,
        )
    }
}
