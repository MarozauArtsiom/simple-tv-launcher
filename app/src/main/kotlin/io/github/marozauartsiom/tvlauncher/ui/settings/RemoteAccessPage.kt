package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.system.remote.ClientKind
import io.github.marozauartsiom.tvlauncher.system.remote.RemoteState
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.PanelSwitch
import io.github.marozauartsiom.tvlauncher.ui.components.PermissionNote
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * Settings → Phone remote & AI access (`design/mockups/settings-remote.html`): two switches, the pairing card
 * (QR code for the phone, the 6-digit code, the address), who is connected, and the rules in one line. Nothing
 * here is saved: turning the TV off turns both switches off and forgets every key.
 */
@Composable
fun RemoteAccessPage(
    theme: ThemeSpec,
    state: RemoteState,
    helperCommand: String,
    memory: FocusMemory,
    onPhone: (Boolean) -> Unit,
    onAi: (Boolean) -> Unit,
    onDisconnect: (String) -> Unit,
    onDisconnectAll: () -> Unit,
    onCheckHelper: () -> Unit,
    modifier: Modifier = Modifier,
    onExternal: (Boolean) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        withFrameNanos { }
        // Whichever exists: the first switch, or "Check again" while the helper is off.
        if (!memory.restore() && !memory.request("phone")) memory.request("check")
    }
    val c = theme.colors
    SidePanel(title = stringResource(R.string.item_remote), theme = theme, modifier = modifier) {
        PanelScroll {
            if (!state.helperReady) {
                PermissionNote(
                    message = stringResource(R.string.remote_needs_helper),
                    command = helperCommand,
                    buttonText = stringResource(R.string.check_again),
                    theme = theme,
                    onCheck = onCheckHelper,
                    buttonModifier = memory.track("check"),
                )
            } else {
                IconSwitchRow(stringResource(R.string.remote_phone), Icons.Phone, state.phoneOn, theme, memory.track("phone")) { onPhone(!state.phoneOn) }
                IconSwitchRow(stringResource(R.string.remote_ai), Icons.Sparkle, state.aiOn, theme, memory.track("ai")) { onAi(!state.aiOn) }
                if (state.aiOn) {
                    IconSwitchRow(stringResource(R.string.remote_external), Icons.Cloud, state.externalOn, theme, memory.track("external")) { onExternal(!state.externalOn) }
                }
                if (state.noNetwork) Muted(stringResource(R.string.remote_no_network), theme)
                val address = state.address
                if (state.anyOn && address != null) {
                    val url = state.externalUrl
                    // With the internet address up and no phone remote, that card replaces the home-network one.
                    if (state.phoneOn || url == null) {
                        PairCard(theme, address, state.code, qr = if (state.phoneOn) "http://$address/?c=${state.code}" else "http://$address/pair")
                    }
                    if (state.clients.isNotEmpty()) {
                        Text(stringResource(R.string.remote_connected), style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(top = 8.dp))
                        state.clients.forEach { client ->
                            PanelItem(
                                title = if (client.kind == ClientKind.Phone) stringResource(R.string.remote_phone_client) else client.name,
                                theme = theme,
                                icon = if (client.kind == ClientKind.Phone) Icons.Phone else Icons.Sparkle,
                                value = stringResource(R.string.remote_disconnect),
                                onClick = { onDisconnect(client.id) },
                                modifier = memory.track("client:${client.id}"),
                            )
                        }
                        if (state.clients.size > 1) {
                            PanelItem(stringResource(R.string.remote_disconnect_all), theme, onDisconnectAll, memory.track("all"))
                        }
                    }
                    if (state.aiOn && !state.externalOn) Muted(stringResource(R.string.remote_ai_hint, "$address/pair"), theme)
                    if (state.externalOn) {
                        if (url != null) ExternalCard(theme, url, state.code) else Muted(stringResource(R.string.remote_external_connecting), theme)
                        Muted(stringResource(R.string.remote_external_note), theme)
                    } else if (state.externalFailed) {
                        Muted(stringResource(R.string.remote_external_failed), theme)
                    }
                    when (state.fullKeys) {
                        true -> Muted(stringResource(R.string.remote_keys_full), theme)
                        false -> Muted(stringResource(R.string.remote_keys_basic), theme)
                        null -> Muted(stringResource(R.string.remote_keys_checking), theme)
                    }
                }
                Muted(stringResource(R.string.remote_auto_off), theme)
            }
        }
    }
}

@Composable
internal fun IconSwitchRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, theme: ThemeSpec, modifier: Modifier, onToggle: () -> Unit) {
    val c = theme.colors
    PanelRow(theme, onClick = onToggle, modifier = modifier.semantics { role = Role.Switch; selected = checked }) {
        Icon(icon, null, tint = c.panelFg, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, modifier = Modifier.weight(1f))
        PanelSwitch(checked, theme)
    }
}

@Composable
private fun Muted(text: String, theme: ThemeSpec) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = theme.colors.panelMuted, modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp))
}

/** The QR code (for the phone's camera), the code in big digits, and the address. Not focusable: nothing to press. */
@Composable
private fun PairCard(theme: ThemeSpec, address: String, code: String, qr: String) {
    val c = theme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).background(c.itemBg, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QrCode(qr, Modifier.size(112.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.remote_scan), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
            Text(
                code.chunked(3).joinToString(" "),
                fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = c.panelFg,
                modifier = Modifier.semantics { contentDescription = code.toList().joinToString(" ") },
            )
            Text(address, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
        }
    }
}

/** The internet address of the AI pairing page: a QR code (code included) and the address as text. */
@Composable
private fun ExternalCard(theme: ThemeSpec, url: String, code: String) {
    val c = theme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).background(c.itemBg, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QrCode("$url/pair?c=$code", Modifier.size(112.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.remote_external_scan), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted)
            Text(
                code.chunked(3).joinToString(" "),
                fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = c.panelFg,
                modifier = Modifier.semantics { contentDescription = code.toList().joinToString(" ") },
            )
            Text(url.removePrefix("https://").substringBefore('/'), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1)
        }
    }
}

/** A QR code drawn as squares (zxing only computes the matrix). Black on white with a quiet zone, any theme. */
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier) {
    val matrix: BitMatrix = remember(content) {
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    }
    Canvas(modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp)) {
        val cell = size.minDimension / matrix.width
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
