package io.github.marozauartsiom.tvlauncher.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marozauartsiom.tvlauncher.AppGraph
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.apps.LaunchResult
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.i18n.LocaleController
import io.github.marozauartsiom.tvlauncher.data.prefs.ShortcutsEditor
import io.github.marozauartsiom.tvlauncher.system.PackageActions
import io.github.marozauartsiom.tvlauncher.system.SystemSettings
import io.github.marozauartsiom.tvlauncher.system.WebLauncher
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.settings.SettingsPanel
import io.github.marozauartsiom.tvlauncher.ui.settings.settingsValues
import io.github.marozauartsiom.tvlauncher.ui.shortcuts.ShortcutsPage
import io.github.marozauartsiom.tvlauncher.ui.home.AppMenu
import io.github.marozauartsiom.tvlauncher.ui.home.AppMenuState
import io.github.marozauartsiom.tvlauncher.ui.home.FavouritesEditor
import io.github.marozauartsiom.tvlauncher.ui.home.HomeActions
import io.github.marozauartsiom.tvlauncher.ui.home.HomeContent
import io.github.marozauartsiom.tvlauncher.ui.home.HomeContentSource
import io.github.marozauartsiom.tvlauncher.ui.home.HomeScreen
import io.github.marozauartsiom.tvlauncher.ui.home.HomeTile
import io.github.marozauartsiom.tvlauncher.ui.home.MenuAction
import io.github.marozauartsiom.tvlauncher.ui.home.TopBarState
import io.github.marozauartsiom.tvlauncher.ui.home.rememberClock
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import java.util.Locale
import kotlinx.coroutines.launch

/** The whole UI. Home is always there; panels and menus open over it. */
@Composable
fun LauncherRoot(graph: AppGraph, viewModel: LauncherViewModel, safeMode: Boolean) {
    val settings by graph.settings.data.collectAsStateWithLifecycle(initialValue = SettingsData())
    // Safe mode always uses the default theme: the saved one could be what keeps crashing.
    val theme = if (safeMode) Themes.default else Themes.byId(settings.theme)
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()

    val source = remember(graph, viewModel) { HomeContentSource(graph.apps.apps, graph.settings.data, viewModel.resets) }
    val content by source.content.collectAsStateWithLifecycle(initialValue = HomeContent())
    val currentContent by rememberUpdatedState(content)
    val currentSettings by rememberUpdatedState(settings)

    val languageTag = LocaleController.effectiveTag(settings.language, Locale.getDefault())
    val clock by rememberClock(languageTag)

    val context = LocalContext.current
    val editor = remember(graph) { FavouritesEditor(graph.settings) }
    val packageActions = remember(context) { PackageActions { context.startActivity(it) } }
    val webLauncher = remember(context) { WebLauncher { context.startActivity(it) } }
    val shortcutsEditor = remember(graph) { ShortcutsEditor(graph.settings) }
    val systemSettings = remember(context) {
        SystemSettings(
            canHandle = { intent -> context.packageManager.resolveActivity(intent, 0) != null },
            start = { context.startActivity(it) },
        )
    }
    val settingsFocus = remember { FocusMemory() }
    var moveOrigin by remember { mutableStateOf<List<ItemKey>?>(null) }

    val actions = remember(graph, viewModel, context, webLauncher) {
        HomeActions(
            onOpen = { tile -> openTile(graph, webLauncher, context, tile) },
            onMenu = { tile, anchor -> viewModel.open(Overlay.AppMenu(tile.key, anchor)) },
            // The panels below are opened by their own tasks; until then these do nothing.
            onWeather = { },
            onHdmi = { },
            onIphone = { },
            onSettings = { viewModel.open(Overlay.Settings()) },
            onMoveStep = { key, delta ->
                graph.appScope.launch { editor.step(key, delta, currentContent.favourites.map { it.key }) }
            },
            onMoveDone = {
                moveOrigin = null
                viewModel.open(Overlay.None)
            },
            onMoveCancel = {
                moveOrigin?.let { order -> graph.appScope.launch { editor.restore(order) } }
                moveOrigin = null
                viewModel.open(Overlay.None)
            },
        )
    }

    // Back never leaves the launcher: it closes a panel, or sends Home to the first favourite.
    BackHandler { viewModel.onBack() }

    val menu = overlay as? Overlay.AppMenu
    val settingsOverlay = overlay as? Overlay.Settings
    val moveKey = (overlay as? Overlay.MoveMode)?.key

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
                overlayOpen = menu != null || settingsOverlay != null,
                moveKey = moveKey,
            )
            when (settingsOverlay?.page) {
                SettingsPage.Root -> SettingsPanel(
                    theme = theme,
                    tiles = remember(systemSettings) { systemSettings.available() },
                    values = settingsValues(settings),
                    memory = settingsFocus,
                    onTile = { tile ->
                        if (!systemSettings.open(tile)) unavailable(context)
                    },
                    onPage = { page -> if (page.hasUi) viewModel.open(Overlay.Settings(page)) },
                )
                SettingsPage.Shortcuts -> ShortcutsPage(
                    theme = theme,
                    shortcuts = settings.shortcuts,
                    onSave = shortcutsEditor::save,
                    onDelete = shortcutsEditor::delete,
                )
                else -> Unit // other pages: their own tasks
            }
            if (menu != null) {
                val tile = content.all.firstOrNull { it.key == menu.key }
                // The app vanished while the menu was opening: nothing to show.
                LaunchedEffect(tile == null) { if (tile == null) viewModel.open(Overlay.None) }
                if (tile != null) {
                    AppMenu(
                        state = AppMenuState(tile, isFavourite = menu.key in settings.favourites, anchor = menu.anchor),
                        theme = theme,
                        onAction = { action ->
                            handleMenuAction(action, tile, graph, editor, packageActions, context, viewModel, currentSettings.favourites) { moveOrigin = it }
                        },
                    )
                }
            }
        }
    }
}

private fun openTile(graph: AppGraph, web: WebLauncher, context: Context, tile: HomeTile) {
    val shortcut = tile.shortcut
    if (shortcut != null) {
        when (web.open(shortcut.url)) {
            WebLauncher.Result.Started -> graph.appScope.launch { graph.settings.recordLaunch(tile.key) }
            WebLauncher.Result.NoBrowser -> Toast.makeText(context, context.getString(R.string.shortcut_no_browser), Toast.LENGTH_LONG).show()
            WebLauncher.Result.Rejected -> Toast.makeText(context, context.getString(R.string.launch_failed, tile.label), Toast.LENGTH_SHORT).show()
        }
        return
    }
    val app = tile.app ?: return
    when (graph.launcher.launch(app.packageName)) {
        LaunchResult.Started -> graph.appScope.launch { graph.settings.recordLaunch(tile.key) }
        LaunchResult.NotFound -> {
            graph.appScope.launch { graph.apps.refresh() }
            Toast.makeText(context, context.getString(R.string.launch_failed, tile.label), Toast.LENGTH_SHORT).show()
        }
        LaunchResult.Failed -> Toast.makeText(context, context.getString(R.string.launch_failed, tile.label), Toast.LENGTH_SHORT).show()
    }
}

private fun handleMenuAction(
    action: MenuAction,
    tile: HomeTile,
    graph: AppGraph,
    editor: FavouritesEditor,
    packageActions: PackageActions,
    context: Context,
    viewModel: LauncherViewModel,
    favourites: List<ItemKey>,
    startMove: (List<ItemKey>) -> Unit,
) {
    val packageName = tile.app?.packageName
    when (action) {
        MenuAction.ToggleFavourite -> graph.appScope.launch { editor.setFavourite(tile.key, tile.key !in favourites) }
        MenuAction.Move -> {
            startMove(favourites)
            viewModel.open(Overlay.MoveMode(tile.key))
            return
        }
        MenuAction.Hide -> if (packageName != null) graph.appScope.launch { graph.settings.setHidden(packageName, true) }
        MenuAction.Info -> if (packageName != null && !packageActions.showInfo(packageName)) unavailable(context)
        MenuAction.Uninstall -> if (packageName != null && !packageActions.uninstall(packageName)) unavailable(context)
    }
    viewModel.open(Overlay.None)
}

private fun unavailable(context: Context) =
    Toast.makeText(context, context.getString(R.string.unavailable_on_tv), Toast.LENGTH_SHORT).show()
