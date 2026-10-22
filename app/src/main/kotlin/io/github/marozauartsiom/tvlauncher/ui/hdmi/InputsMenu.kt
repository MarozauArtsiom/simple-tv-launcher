package io.github.marozauartsiom.tvlauncher.ui.hdmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.home.HdmiChip
import io.github.marozauartsiom.tvlauncher.ui.home.SafeHorizontal
import io.github.marozauartsiom.tvlauncher.ui.home.TopBarHeight
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * The popup of the top bar's "Inputs" button: every HDMI input with whether something is connected.
 * It hangs under the right end of the top bar; focus starts on the first connected input (else the
 * first one); OK switches to it; Back closes it (handled by the shell).
 */
@Composable
fun InputsMenu(inputs: List<HdmiChip>, theme: ThemeSpec, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val first = remember { FocusRequester() }
    val start = inputs.indexOfFirst { it.hasSignal }.coerceAtLeast(0)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = SafeHorizontal, top = TopBarHeight + 8.dp)
                .width(290.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .background(theme.colors.panelBg, RoundedCornerShape(16.dp))
                .padding(8.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            inputs.forEachIndexed { index, input ->
                PanelItem(
                    title = input.label,
                    value = stringResource(if (input.hasSignal) R.string.input_connected else R.string.input_empty),
                    icon = Icons.Hdmi,
                    theme = theme,
                    transparent = true,
                    selected = input.hasSignal,
                    onClick = { onOpen(input.id) },
                    modifier = if (index == start) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}
