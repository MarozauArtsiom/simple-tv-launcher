package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.HdmiSettings
import io.github.marozauartsiom.tvlauncher.system.hdmi.HdmiInput
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.PanelSwitch
import io.github.marozauartsiom.tvlauncher.ui.components.PermissionNote
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * Settings → HDMI signal: ask about a new signal (on/off), show the card over other apps (needs the overlay
 * permission, with the exact adb command while it is missing), and one switch per input.
 */
@Composable
fun HdmiSettingsPage(
    theme: ThemeSpec,
    settings: HdmiSettings,
    inputs: List<HdmiInput>,
    canDrawOverlays: Boolean,
    adbCommand: String,
    memory: FocusMemory,
    onChange: (HdmiSettings) -> Unit,
    onCheckPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request("ask")
    }
    val c = theme.colors
    SidePanel(title = stringResource(R.string.item_hdmi), theme = theme, modifier = modifier) {
        PanelScroll {
            SwitchRow(stringResource(R.string.hdmi_ask), settings.promptEnabled, theme, memory.track("ask")) {
                onChange(settings.copy(promptEnabled = !settings.promptEnabled))
            }
            SwitchRow(stringResource(R.string.hdmi_over_apps), settings.overOtherApps && canDrawOverlays, theme, memory.track("over")) {
                onChange(settings.copy(overOtherApps = !settings.overOtherApps))
            }
            if (!canDrawOverlays) {
                PermissionNote(
                    message = stringResource(R.string.hdmi_permission_hint),
                    command = adbCommand,
                    buttonText = stringResource(R.string.check_again),
                    theme = theme,
                    onCheck = onCheckPermission,
                    modifier = Modifier.padding(top = 4.dp),
                    buttonModifier = memory.track("check"),
                )
            }
            if (inputs.isNotEmpty()) {
                Text(stringResource(R.string.hdmi_inputs_section), style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(top = 12.dp))
                inputs.forEach { input ->
                    SwitchRow(input.label, input.id !in settings.mutedInputs, theme, memory.track("input:${input.id}")) {
                        val muted = if (input.id in settings.mutedInputs) settings.mutedInputs - input.id else settings.mutedInputs + input.id
                        onChange(settings.copy(mutedInputs = muted))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, theme: ThemeSpec, modifier: Modifier, onToggle: () -> Unit) {
    val c = theme.colors
    PanelRow(theme, onClick = onToggle, modifier = modifier.semantics { role = Role.Switch; selected = checked }) {
        Icon(Icons.Hdmi, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, modifier = Modifier.weight(1f))
        PanelSwitch(checked, theme)
    }
}
