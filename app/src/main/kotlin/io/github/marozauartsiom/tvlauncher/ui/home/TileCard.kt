package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.data.apps.ArtSource
import io.github.marozauartsiom.tvlauncher.data.apps.BannerArt
import io.github.marozauartsiom.tvlauncher.ui.components.FocusProgress
import io.github.marozauartsiom.tvlauncher.ui.components.rememberFocusProgress
import io.github.marozauartsiom.tvlauncher.ui.theme.ShadowSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

val CardWidth = 160.dp
val CardHeight = 90.dp

private val CardText = Color(0xFFF4F5F8)

/**
 * One 160x90 dp tile. Apps show their own banner; apps without one show a generated tile with the
 * name drawn here; shortcuts show a coloured tile with the first letter. The theme only frames it
 * (sticker outline and hard shadow at rest, ring/tilt/glow in focus).
 *
 * [theme] is passed in, not read from a CompositionLocal, because this runs inside lazy lists.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TileCard(
    tile: HomeTile,
    theme: ThemeSpec,
    art: ArtSource,
    onClick: () -> Unit,
    onLongClick: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusProgress = rememberFocusProgress(),
    moving: Boolean = false,
    /** Drawn as focused although focus is elsewhere: the tile a menu is open for. */
    pinned: Boolean = false,
) {
    val shape = remember(theme.cardRadius) { RoundedCornerShape(theme.cardRadius) }
    val probe = remember { BoundsProbe() } // asked only on a long press: no layout callback runs for it
    Surface(
        onClick = onClick,
        onLongClick = { onLongClick(probe.bounds()) },
        modifier = modifier
            .size(CardWidth, CardHeight)
            .then(probe.modifier)
            .then(focus.track())
            .cardFrame(theme, shape, focus, moving, pinned)
            .semantics { contentDescription = tile.label },
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = theme.colors.chipBg,
            focusedContainerColor = theme.colors.chipBg,
            pressedContainerColor = theme.colors.chipBg,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Box(Modifier.fillMaxSize().clip(shape)) {
            when {
                tile.app != null -> AppArt(tile, art)
                tile.shortcut != null -> ShortcutArt(tile)
            }
        }
    }
}

@Composable
private fun BoxScope.AppArt(tile: HomeTile, source: ArtSource) {
    val app = tile.app ?: return
    val art: BannerArt? by produceState(source.peek(app), app.packageName) { value = source.load(app) }
    val loaded = art
    if (loaded != null) {
        Image(
            bitmap = loaded.bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
    if (loaded == null || loaded.isGenerated) TileName(tile.label, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
}

/** A website shortcut: a tile coloured from the address, with the first letter and the title. */
@Composable
private fun ShortcutArt(tile: HomeTile) {
    val shortcut = tile.shortcut ?: return
    Box(Modifier.fillMaxSize().background(ShortcutColors.forUrl(shortcut.url))) {
        Text(
            text = shortcut.title.trim().take(1).uppercase(),
            style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CardText),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
        )
        TileName(tile.label, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
    }
}

@Composable
private fun TileName(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = CardText,
            fontWeight = FontWeight.Medium,
            shadow = androidx.compose.ui.graphics.Shadow(Color(0x59000000), Offset(0f, 1f), 2f),
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 8.dp),
    )
}

/** Dark, readable tile colours for shortcuts, picked by a stable hash of the address. */
object ShortcutColors {
    private val palette = listOf(
        0xFF2C4A6B, 0xFF24523A, 0xFF5A3A1A, 0xFF43305E, 0xFF6B1F24, 0xFF1F4A5C, 0xFF5C4A1F, 0xFF3B3F4A,
    ).map { Color(it) }

    fun forUrl(url: String): Color = palette[(url.hashCode() and Int.MAX_VALUE) % palette.size]
}

/**
 * The theme's frame around a card, driven by [focus]:
 * at rest the white sticker outline and the card shadow, in focus the ring, glow, scale and tilt.
 * Hard (cartoon) shadows are drawn as offset shapes; soft ones use the layer's elevation shadow.
 */
fun Modifier.cardFrame(theme: ThemeSpec, shape: RoundedCornerShape, focus: FocusProgress, moving: Boolean = false, pinned: Boolean = false): Modifier = this
    .graphicsLayer {
        val p = if (pinned) 1f else focus.value
        val scale = 1f + (theme.focus.scale - 1f) * p
        scaleX = scale
        scaleY = scale
        rotationZ = theme.focus.rotation * p
        val resting = theme.cardShadow?.takeIf { it.blur.value > 0f }
        val glow = theme.focus.glow?.takeIf { it.blur.value > 0f }
        val spec = if (p >= 0.5f) glow else resting
        val blur = (1f - p) * (resting?.blur?.toPx() ?: 0f) + p * (glow?.blur?.toPx() ?: 0f)
        shadowElevation = blur / 2f
        ambientShadowColor = spec?.color ?: Color.Transparent
        spotShadowColor = spec?.color ?: Color.Transparent
        this.shape = shape
        clip = false
    }
    .drawBehind {
        val p = if (pinned) 1f else focus.value
        val radius = theme.cardRadius.toPx()
        if (p < 1f) {
            val rest = 1f - p
            theme.cardShadow?.takeIf { it.blur.value == 0f }?.let { drawHardShadow(it, radius, rest) }
            if (theme.cardOutline.value > 0f) drawRing(Color.White.copy(alpha = rest), theme.cardOutline.toPx(), radius)
        }
        if (p > 0f) {
            theme.focus.glow?.takeIf { it.blur.value == 0f }?.let { drawHardShadow(it, radius, p) }
            drawRing(theme.colors.focus.copy(alpha = p), theme.focus.ring.toPx(), radius)
        }
        if (moving) drawMoveRing(theme.colors.focus, radius)
    }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHardShadow(shadow: ShadowSpec, radius: Float, alpha: Float) {
    val spread = shadow.spread.toPx()
    drawRoundRect(
        color = shadow.color.copy(alpha = shadow.color.alpha * alpha),
        topLeft = Offset(-spread, shadow.offsetY.toPx() - spread),
        size = Size(size.width + 2 * spread, size.height + 2 * spread),
        cornerRadius = CornerRadius(radius + spread),
    )
}

/** The dashed ring around the tile being moved: 3 dp dashes, 5 dp outside the edge. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoveRing(color: Color, radius: Float) {
    val gap = 5.dp.toPx()
    val width = 3.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-gap, -gap),
        size = Size(size.width + 2 * gap, size.height + 2 * gap),
        cornerRadius = CornerRadius(radius + gap),
        style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(width * 2f, width * 1.5f))),
    )
}

/** A ring of [width] just outside the edge (like a CSS `0 0 0 Npx` box-shadow), or just inside it. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRing(color: Color, width: Float, radius: Float, inside: Boolean = false) {
    val half = width / 2f
    val topLeft = if (inside) Offset(half, half) else Offset(-half, -half)
    val grow = if (inside) -width else width
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = Size(size.width + grow, size.height + grow),
        cornerRadius = CornerRadius((radius + grow / 2f).coerceAtLeast(0f)),
        style = Stroke(width),
    )
}
