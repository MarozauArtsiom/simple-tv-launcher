package io.github.marozauartsiom.tvlauncher.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marozauartsiom.tvlauncher.AppGraph
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.apps.LaunchResult
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.i18n.LocaleController
import io.github.marozauartsiom.tvlauncher.ui.home.HomeActions
import io.github.marozauartsiom.tvlauncher.ui.home.HomeContent
import io.github.marozauartsiom.tvlauncher.ui.home.HomeContentSource
import io.github.marozauartsiom.tvlauncher.ui.home.HomeScreen
import io.github.marozauartsiom.tvlauncher.ui.home.TopBarState
import io.github.marozauartsiom.tvlauncher.ui.home.rememberClock
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import java.util.Locale
import kotlinx.coroutines.launch

/** The whole UI. Home is always there; panels (later tasks) open over it. */
@Composable
fun LauncherRoot(graph: AppGraph, viewModel: LauncherViewModel, safeMode: Boolean) {
    val settings by graph.settings.data.collectAsStateWithLifecycle(initialValue = SettingsData())
    // Safe mode always uses the default theme: the saved one could be what keeps crashing.
    val theme = if (safeMode) Themes.default else Themes.byId(settings.theme)

    val source = remember(graph, viewModel) { HomeContentSource(graph.apps.apps, graph.settings.data, viewModel.resets) }
    val content by source.content.collectAsStateWithLifecycle(initialValue = HomeContent())

    val languageTag = LocaleController.effectiveTag(settings.language, Locale.getDefault())
    val clock by rememberClock(languageTag)

    val context = LocalContext.current
    val actions = remember(graph, viewModel, context) {
        HomeActions(
            onOpen = { tile ->
                val app = tile.app
                if (app != null) {
                    when (graph.launcher.launch(app.packageName)) {
                        LaunchResult.Started -> graph.appScope.launch { graph.settings.recordLaunch(tile.key) }
                        LaunchResult.NotFound -> {
                            graph.appScope.launch { graph.apps.refresh() }
                            Toast.makeText(context, context.getString(R.string.launch_failed, tile.label), Toast.LENGTH_SHORT).show()
                        }
                        LaunchResult.Failed ->
                            Toast.makeText(context, context.getString(R.string.launch_failed, tile.label), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onMenu = { tile -> viewModel.open(Overlay.AppMenu(tile.key)) },
            onWeather = { viewModel.open(Overlay.Weather) },
            onHdmi = { },
            onIphone = { viewModel.open(Overlay.IphoneHint) },
            onSettings = { viewModel.open(Overlay.Settings()) },
        )
    }

    // Back never leaves the launcher: it closes a panel, or sends Home to the first favourite.
    BackHandler { viewModel.onBack() }

    LauncherTheme(theme) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, graph.backgrounds)
            HomeScreen(
                content = content,
                theme = theme,
                topBar = TopBarState(),
                clock = { clock },
                art = graph.banners,
                resets = viewModel.resets,
                actions = actions,
                safeMode = safeMode,
            )
        }
    }
}
