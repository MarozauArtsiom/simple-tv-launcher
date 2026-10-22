package io.github.marozauartsiom.tvlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * The right-hand panel used by Settings and its pages: 400 dp wide (more for the pickers), full height,
 * over a dim of the screen behind. It slides in from the right in 200 ms. Padding is 27 dp top and
 * bottom, 32 dp left and 48 dp right (the safe area).
 */
@Composable
fun SidePanel(
    title: String,
    theme: ThemeSpec,
    modifier: Modifier = Modifier,
    width: Dp = 400.dp,
    dim: Float = 0.45f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val slide = remember { Animatable(1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
    val shape = remember(theme.panelRadius) { RoundedCornerShape(topStart = theme.panelRadius, bottomStart = theme.panelRadius) }

    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = dim))) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(width)
                .graphicsLayer { translationX = slide.value * width.toPx() }
                .background(theme.colors.panelBg, shape)
                .padding(PaddingValues(start = 32.dp, end = 48.dp, top = 27.dp, bottom = 27.dp))
                .focusGroup(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = theme.titleWeight),
                color = theme.colors.panelFg,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            content()
        }
    }
}

/**
 * The scrolling body of a panel. The 6 dp inside the scroll area leaves room for the focus ring and
 * the ×1.03 growth of the first and last rows, which a scroll container would otherwise clip. When
 * more content is below the edge, the last 56 dp fade into the panel colour.
 */
@Composable
fun PanelScroll(
    modifier: Modifier = Modifier,
    spacing: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Box(modifier) {
        Column(Modifier.verticalScroll(scroll)) {
            Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
        }
        val background = io.github.marozauartsiom.tvlauncher.ui.theme.Launcher.colors.panelBg
        if (scroll.canScrollForward) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(background.copy(alpha = 0f), background))),
            )
        }
    }
}
