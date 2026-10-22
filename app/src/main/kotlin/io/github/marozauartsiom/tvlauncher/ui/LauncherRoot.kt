package io.github.marozauartsiom.tvlauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.AppGraph
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import androidx.compose.ui.res.stringResource

/** The whole UI. Home, panels and the safe-mode screen are drawn from here as the tasks land. */
@Composable
fun LauncherRoot(graph: AppGraph, viewModel: LauncherViewModel, safeMode: Boolean) {
    val settings by graph.settings.data.collectAsStateWithLifecycle(initialValue = SettingsData())
    val theme = Themes.byId(settings.theme)

    // Back never leaves the launcher: it closes a panel, or sends Home to the first favourite.
    BackHandler { viewModel.onBack() }

    LauncherTheme(theme) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, graph.backgrounds)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = theme.colors.onSurface)
            }
        }
    }
}
