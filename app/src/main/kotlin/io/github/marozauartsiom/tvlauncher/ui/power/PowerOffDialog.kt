package io.github.marozauartsiom.tvlauncher.ui.power

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
import io.github.marozauartsiom.tvlauncher.ui.components.PermissionNote
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/**
 * "Turn the TV off completely?" with Yes (focused) and No. Without the power helper enabled, the adb
 * command that enables it is shown instead, with "Check again".
 */
@Composable
fun PowerOffDialog(
    theme: ThemeSpec,
    helperEnabled: Boolean,
    adbCommand: String,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onCheckAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    val first = remember { FocusRequester() }
    LaunchedEffect(helperEnabled) { withFrameNanos { }; runCatching { first.requestFocus() } }
    val radius = if (theme.panelRadius.value > 0f) theme.panelRadius else 20.dp
    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(520.dp).background(c.panelBg, RoundedCornerShape(radius)).padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.power_off_question), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = theme.titleWeight), color = c.panelFg)
            if (helperEnabled) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelButton(stringResource(R.string.yes), theme, onYes, Modifier.focusRequester(first))
                    PanelButton(stringResource(R.string.no), theme, onNo)
                }
            } else {
                PermissionNote(
                    message = stringResource(R.string.power_needs_service),
                    command = adbCommand,
                    buttonText = stringResource(R.string.check_again),
                    theme = theme,
                    onCheck = onCheckAgain,
                    buttonModifier = Modifier.focusRequester(first),
                )
            }
        }
    }
}
