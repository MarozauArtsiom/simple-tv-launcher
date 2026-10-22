package io.github.marozauartsiom.tvlauncher.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/** A 40 dp pill button. In focus it fills with the theme's focus colour and grows ×1.05. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PanelButton(text: String, theme: ThemeSpec, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = theme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = c.itemBg,
            focusedContainerColor = c.itemFocusRing,
            pressedContainerColor = c.itemFocusRing,
            contentColor = c.panelFg,
            focusedContentColor = c.buttonFg,
            pressedContentColor = c.buttonFg,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Row(Modifier.height(40.dp).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}
