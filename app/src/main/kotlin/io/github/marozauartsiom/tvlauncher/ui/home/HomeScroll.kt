package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlin.math.abs

/**
 * How Home's lists follow focus. It replaces Compose's TV default (a pivot that keeps the focused item
 * at a fixed point), which re-aims on every frame of our focus animation: the tile's bounds include its
 * scale and tilt, so the target moved while the tile grew and the whole screen jerked (T28).
 *
 * Rule: a tile that is on screen between the margins does not scroll the list at all, even while it
 * grows (up to [growth] of its size on each side). One that is not is brought just inside the margin
 * on the side it came from.
 */
@OptIn(ExperimentalFoundationApi::class)
class CalmScrollSpec(
    private val leadingMarginPx: Float,
    private val trailingMarginPx: Float,
    private val growth: Float = MaxFocusGrowth,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val start = leadingMarginPx
        val end = containerSize - trailingMarginPx
        val slack = size * growth
        val leading = offset
        val trailing = offset + size
        return when {
            // Taller than the room between the margins: line its start up with the margin.
            size - 2 * slack > end - start -> leading - start
            leading >= start - slack && trailing <= end + slack -> 0f
            leading < start -> leading - start
            else -> trailing - end
        }.let { if (abs(it) < 0.5f) 0f else it }
    }

    companion object {
        /** A focused card grows ×1.1 from its centre (5 % per side) and may tilt a little: allow 6 %. */
        const val MaxFocusGrowth = 0.06f
    }
}

@Composable
internal fun rememberCalmScrollSpec(leadingMargin: Dp, trailingMargin: Dp): CalmScrollSpec {
    val density = LocalDensity.current
    return remember(density, leadingMargin, trailingMargin) {
        with(density) { CalmScrollSpec(leadingMargin.toPx(), trailingMargin.toPx()) }
    }
}
