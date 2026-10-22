package io.github.marozauartsiom.tvlauncher.ui.power

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PermissionNote
import io.github.marozauartsiom.tvlauncher.ui.home.SafeHorizontal
import io.github.marozauartsiom.tvlauncher.ui.home.TopBarHeight
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/** Minutes offered by the sleep timer. */
val SleepTimerChoices = listOf(15, 30, 45, 60, 90, 120)

/**
 * The top bar's sleep timer popup: "Off" and 15…120 minutes; the running choice has the check mark and
 * focus. Without the power helper the adb command is shown instead (the timer could not turn the TV off).
 */
@Composable
fun SleepTimerMenu(
    theme: ThemeSpec,
    runningMinutes: Int?,
    helperEnabled: Boolean,
    adbCommand: String,
    onChoose: (Int?) -> Unit,
    onCheckAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = theme.colors
    val first = remember { FocusRequester() }
    LaunchedEffect(helperEnabled) { withFrameNanos { }; runCatching { first.requestFocus() } }
    // The running timer's nearest choice is focused; with none, "Off".
    val focusedChoice = runningMinutes?.let { m -> SleepTimerChoices.minByOrNull { kotlin.math.abs(it - m) } }
    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = SafeHorizontal, top = TopBarHeight + 8.dp)
                .width(if (helperEnabled) 290.dp else 420.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .background(c.panelBg, RoundedCornerShape(16.dp))
                .padding(8.dp)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.timer_title), style = MaterialTheme.typography.labelLarge, color = c.panelMuted, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            if (!helperEnabled) {
                PermissionNote(stringResource(R.string.power_needs_service), adbCommand, stringResource(R.string.check_again), theme, onCheckAgain, buttonModifier = Modifier.focusRequester(first))
                return@Column
            }
            PanelItem(
                title = stringResource(R.string.timer_off),
                theme = theme,
                transparent = true,
                selected = runningMinutes == null,
                onClick = { onChoose(null) },
                modifier = if (focusedChoice == null) Modifier.focusRequester(first) else Modifier,
            )
            SleepTimerChoices.forEach { minutes ->
                PanelItem(
                    title = pluralStringResource(R.plurals.timer_minutes, minutes, minutes),
                    theme = theme,
                    transparent = true,
                    selected = minutes == focusedChoice,
                    onClick = { onChoose(minutes) },
                    modifier = if (minutes == focusedChoice) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}
