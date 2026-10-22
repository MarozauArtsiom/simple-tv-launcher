package io.github.marozauartsiom.tvlauncher.ui.iphone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.ui.components.PanelButton
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * The card shown when the iPhone button is pressed and AirPlay Server is not installed: what to put on a
 * USB drive and how to mirror from the iPhone. "Install from USB" has focus; Back or "Close" dismisses.
 * A 520 dp dialog centred over a 60 % dim.
 */
@Composable
fun IphoneHintDialog(
    theme: ThemeSpec,
    onInstallFromUsb: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    val install = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { install.requestFocus() } }
    val radius = if (theme.panelRadius.value > 0f) theme.panelRadius else 20.dp
    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(520.dp)
                .background(c.panelBg, RoundedCornerShape(radius))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.iphone_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = theme.titleWeight), color = c.panelFg)
            Text(stringResource(R.string.iphone_text), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Step("1.", stringResource(R.string.iphone_step1), theme)
                Step("2.", stringResource(R.string.iphone_step2), theme)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PanelButton(stringResource(R.string.item_usb), theme, onInstallFromUsb, Modifier.focusRequester(install))
                PanelButton(stringResource(R.string.close), theme, onClose)
            }
        }
    }
}

@Composable
private fun Step(number: String, text: String, theme: ThemeSpec) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(number, style = MaterialTheme.typography.bodyMedium, color = theme.colors.panelFg)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = theme.colors.panelFg)
    }
}
