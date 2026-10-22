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

private data class Layer(val spec: BackgroundSpec, val bitmap: ImageBitmap?)

private const val CROSSFADE_MS = 300

/** Draws [spec]'s background (the theme's own). See the overload below. */
@Composable
fun ThemedBackground(spec: ThemeSpec, cache: BackgroundCache, modifier: Modifier = Modifier) =
    ThemedBackground(spec.background, cache, modifier)

/**
 * Draws a background. When it changes it crossfades from the previous one in 300 ms; the first draw has
 * no animation. Bitmaps come from [cache], decoded off the main thread.
 */
@Composable
fun ThemedBackground(background: BackgroundSpec, cache: BackgroundCache, modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf(Layer(background, cache.peek(background))) }
    var previous by remember { mutableStateOf<Layer?>(null) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(background) {
        val bitmap = cache.peek(background) ?: cache.load(background)
        if (current.spec == background) {
            current = Layer(background, bitmap) // first load of the initial background: no fade
        } else {
            previous = current
            current = Layer(background, bitmap)
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
    when (val bg = layer.spec) {
        is ImageBackground -> drawPicture(layer.bitmap, bg.scrim, alpha)
        is FileBackground -> drawPicture(layer.bitmap, bg.scrim, alpha)
        is SolidBackground -> drawRect(bg.color, alpha = alpha)
        is RadialBackground -> drawRadial(bg, alpha)
    }
}

private fun DrawScope.drawPicture(bitmap: ImageBitmap?, scrim: List<Pair<Float, Color>>, alpha: Float) {
    bitmap?.let {
        drawImage(
            it, dstOffset = IntOffset.Zero, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            alpha = alpha, filterQuality = FilterQuality.Low,
        )
    }
    if (scrim.isNotEmpty()) drawRect(Brush.verticalGradient(*scrim.toTypedArray()), alpha = alpha)
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
