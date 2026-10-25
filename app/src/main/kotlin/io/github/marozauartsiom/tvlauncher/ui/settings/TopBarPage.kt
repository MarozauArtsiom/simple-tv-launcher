package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.TopButton
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelScroll
import io.github.marozauartsiom.tvlauncher.ui.components.SidePanel
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/** Settings → Top bar: one switch per button of the top bar. The settings button (the avatar) can't be hidden. */
@Composable
fun TopBarPage(
    theme: ThemeSpec,
    hidden: Set<TopButton>,
    memory: FocusMemory,
    onToggle: (TopButton, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!memory.restore()) memory.request(TopButton.entries.first().key)
    }
    SidePanel(title = stringResource(R.string.item_top_bar), theme = theme, modifier = modifier) {
        PanelScroll {
            TopButton.entries.forEach { button ->
                val shown = button !in hidden
                IconSwitchRow(stringResource(button.label()), button.icon(), shown, theme, memory.track(button.key)) { onToggle(button, shown) }
            }
            Text(
                stringResource(R.string.top_bar_note),
                style = MaterialTheme.typography.bodyMedium,
                color = theme.colors.panelMuted,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

private fun TopButton.label(): Int = when (this) {
    TopButton.Weather -> R.string.item_weather
    TopButton.Inputs -> R.string.inputs_chip
    TopButton.Timer -> R.string.top_bar_timer
    TopButton.Remote -> R.string.remote_chip
    TopButton.Iphone -> R.string.top_bar_iphone
}

private fun TopButton.icon(): ImageVector = when (this) {
    TopButton.Weather -> Icons.Cloud
    TopButton.Inputs -> Icons.Hdmi
    TopButton.Timer -> Icons.Sleep
    TopButton.Remote -> Icons.Remote
    TopButton.Iphone -> Icons.Phone
}
