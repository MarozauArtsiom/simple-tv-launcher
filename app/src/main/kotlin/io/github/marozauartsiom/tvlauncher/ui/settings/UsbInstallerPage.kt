package io.github.marozauartsiom.tvlauncher.ui.settings

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.install.ApkAction
import io.github.marozauartsiom.tvlauncher.data.install.ApkItem
import io.github.marozauartsiom.tvlauncher.data.install.UsbState
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelButton
import io.github.marozauartsiom.tvlauncher.ui.components.PanelRow
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.PermissionNote
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.home.ShortcutColors
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * Settings → Install from USB. The state machine lives in `UsbInstaller`; this only draws [state]:
 * the archives found (OK installs or updates), the two adb-only permission notes, the system installer
 * waiting for its dialog, and the result with "Open" / "Done".
 */
@Composable
fun UsbInstallerPage(
    theme: ThemeSpec,
    state: UsbState,
    storageCommand: String,
    unknownAppsCommand: String,
    memory: FocusMemory,
    onInstall: (ApkItem) -> Unit,
    onCheckAgain: () -> Unit,
    onOpen: (ApkItem) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    // Initial focus is whatever the state offers: the first archive, "Check again", or "Open".
    val focusTarget = when (state) {
        is UsbState.Ready -> "apk:${state.items.first().info.file.path}"
        UsbState.NoStorageAccess, UsbState.NeedsUnknownApps -> "check"
        is UsbState.Done -> "open"
        is UsbState.Failed -> "check"
        else -> null
    }
    val currentTarget by rememberUpdatedState(focusTarget)
    LaunchedEffect(focusTarget) {
        withFrameNanos { }
        // Back on the same list after an install attempt keeps the row the owner was on.
        if (state is UsbState.Ready && memory.restore()) return@LaunchedEffect
        currentTarget?.let { memory.request(it) }
    }
    SidePanel(title = stringResource(R.string.item_usb), theme = theme, modifier = modifier) {
        PanelScroll {
            when (state) {
                UsbState.Loading -> Unit
                UsbState.NoStorageAccess -> PermissionNote(
                    message = stringResource(R.string.usb_no_permission),
                    command = storageCommand,
                    buttonText = stringResource(R.string.check_again),
                    theme = theme,
                    onCheck = onCheckAgain,
                    buttonModifier = memory.track("check"),
                )
                UsbState.NeedsUnknownApps -> PermissionNote(
                    message = stringResource(R.string.usb_unknown_apps),
                    command = unknownAppsCommand,
                    buttonText = stringResource(R.string.check_again),
                    theme = theme,
                    onCheck = onCheckAgain,
                    buttonModifier = memory.track("check"),
                )
                UsbState.Empty -> {
                    Text(stringResource(R.string.usb_empty), style = MaterialTheme.typography.titleMedium, color = c.panelMuted)
                    PanelButton(stringResource(R.string.check_again), theme, onCheckAgain, Modifier.padding(top = 16.dp))
                }
                is UsbState.Ready -> {
                    Text(
                        pluralStringResource(R.plurals.usb_files, state.items.size, state.items.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = c.panelMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    state.items.forEach { item ->
                        PanelRow(theme, onClick = { onInstall(item) }, modifier = memory.track("apk:${item.info.file.path}"), height = 56.dp) {
                            ApkBanner(item)
                            ApkTexts(theme, item, secondary = "${item.info.versionName} · ${Formatter.formatShortFileSize(LocalContext.current, item.info.file.sizeBytes)}", Modifier.weight(1f))
                            Text(actionLabel(item.action), style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1)
                        }
                    }
                }
                is UsbState.Installing -> {
                    ApkStatusRow(theme, state.item, stringResource(R.string.usb_installing))
                    Text(
                        stringResource(R.string.usb_confirm_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.panelMuted,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                is UsbState.Done -> {
                    ApkStatusRow(theme, state.item, stringResource(R.string.usb_done), done = true)
                    Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PanelButton(stringResource(R.string.open), theme, { onOpen(state.item) }, memory.track("open"))
                        PanelButton(stringResource(R.string.done), theme, onDone)
                    }
                }
                is UsbState.Failed -> {
                    ApkStatusRow(theme, state.item, stringResource(if (state.cancelled) R.string.usb_cancelled else R.string.usb_error))
                    PanelButton(stringResource(R.string.check_again), theme, onCheckAgain, memory.track("check").padding(top = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun actionLabel(action: ApkAction): String = stringResource(
    when (action) {
        ApkAction.Install -> R.string.usb_install
        ApkAction.Update -> R.string.usb_update
        ApkAction.Installed -> R.string.usb_done
    },
)

/** A 72×40 tile in a colour taken from the package name, with the archive icon. */
@Composable
private fun ApkBanner(item: ApkItem) {
    Box(
        Modifier.size(72.dp, 40.dp).background(ShortcutColors.forUrl(item.info.packageName), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Apk, null, tint = Color(0xFFF4F5F8), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ApkTexts(theme: ThemeSpec, item: ApkItem, secondary: String, modifier: Modifier) {
    val c = theme.colors
    Column(modifier) {
        Text(item.info.label, style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(secondary, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The archive being installed (or just installed) with a one-line status. Not focusable: nothing to press here. */
@Composable
private fun ApkStatusRow(theme: ThemeSpec, item: ApkItem, status: String, done: Boolean = false) {
    val c = theme.colors
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(c.itemBg, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ApkBanner(item)
        Column(Modifier.weight(1f)) {
            Text(item.info.label, style = MaterialTheme.typography.titleMedium, color = c.panelFg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = c.panelMuted, maxLines = 1)
        }
        if (done) Icon(Icons.Check, null, tint = c.signal, modifier = Modifier.size(20.dp))
    }
}
