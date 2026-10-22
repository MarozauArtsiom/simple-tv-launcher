package io.github.marozauartsiom.tvlauncher.ui.hdmi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.system.hdmi.HdmiInput
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelButton
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import kotlinx.coroutines.delay

const val PromptSeconds = 15

/**
 * "Signal on HDMI 2 — switch?": a 460 dp card for the bottom-right corner of the safe area. "Switch" has
 * focus; "Not now" or Back dismisses; it hides itself after 15 s (shown as a countdown). The same card is
 * shown inside the launcher and, with the overlay permission, over other apps.
 *
 * [onSwitch] and [onDismiss] are called once; the caller removes the card.
 */
@Composable
fun HdmiPrompt(
    theme: ThemeSpec,
    input: HdmiInput,
    onSwitch: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    seconds: Int = PromptSeconds,
) {
    val c = theme.colors
    val switch = remember { FocusRequester() }
    var left by remember { mutableIntStateOf(seconds) }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { switch.requestFocus() } }
    LaunchedEffect(Unit) {
        while (left > 0) { delay(1000); left-- }
        onDismiss()
    }
    val shape = remember(theme.panelRadius) { RoundedCornerShape(16.dp) }
    Column(
        modifier = modifier
            .width(460.dp)
            .background(c.panelBg, shape)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .onPreviewKeyEvent { event ->
                // Back means "not now".
                if (event.key == Key.Back || event.key == Key.Escape) {
                    if (event.type == KeyEventType.KeyUp) onDismiss()
                    true
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(c.signal.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Hdmi, contentDescription = null, tint = c.signal, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    stringResource(R.string.hdmi_prompt_title, input.label),
                    style = MaterialTheme.typography.titleLarge,
                    color = c.panelFg,
                    maxLines = 1,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                input.deviceName?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            PanelButton(stringResource(R.string.hdmi_switch), theme, onSwitch, Modifier.focusRequester(switch))
            PanelButton(stringResource(R.string.hdmi_not_now), theme, onDismiss)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.hdmi_countdown, left), style = MaterialTheme.typography.labelMedium, color = c.panelMuted, maxLines = 1)
        }
    }
}
