package io.github.marozauartsiom.tvlauncher.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.marozauartsiom.tvlauncher.AppGraph
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.apps.Airplay
import io.github.marozauartsiom.tvlauncher.data.apps.LaunchResult
import io.github.marozauartsiom.tvlauncher.data.background.BackgroundDecoder
import io.github.marozauartsiom.tvlauncher.data.prefs.BackgroundChoice
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.data.prefs.LanguageChoice
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.i18n.LocaleController
import io.github.marozauartsiom.tvlauncher.data.prefs.ShortcutsEditor
import io.github.marozauartsiom.tvlauncher.data.prefs.WeatherLocation
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherCodes
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherRepository
import io.github.marozauartsiom.tvlauncher.data.weather.WeatherView
import io.github.marozauartsiom.tvlauncher.data.prefs.SunTimes
import io.github.marozauartsiom.tvlauncher.data.prefs.themeAt
import io.github.marozauartsiom.tvlauncher.system.PackageActions
import io.github.marozauartsiom.tvlauncher.system.SystemSettings
import io.github.marozauartsiom.tvlauncher.system.WebLauncher
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.settings.BackgroundPickerPage
import io.github.marozauartsiom.tvlauncher.ui.settings.LanguagePickerPage
import io.github.marozauartsiom.tvlauncher.ui.settings.PicturesState
import io.github.marozauartsiom.tvlauncher.ui.settings.SchedulePage
import io.github.marozauartsiom.tvlauncher.ui.settings.SettingsPanel
import io.github.marozauartsiom.tvlauncher.ui.settings.ThemePickerPage
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
import io.github.marozauartsiom.tvlauncher.ui.home.HdmiChip
import io.github.marozauartsiom.tvlauncher.ui.home.TopBarState
import io.github.marozauartsiom.tvlauncher.ui.hdmi.HdmiPrompt
import io.github.marozauartsiom.tvlauncher.ui.iphone.IphoneHintDialog
import io.github.marozauartsiom.tvlauncher.ui.settings.HdmiSettingsPage
import io.github.marozauartsiom.tvlauncher.ui.settings.AboutPage
import io.github.marozauartsiom.tvlauncher.ui.hdmi.InputsMenu
import io.github.marozauartsiom.tvlauncher.ui.settings.AvatarPickerPage
import io.github.marozauartsiom.tvlauncher.ui.settings.HiddenAppsPage
import io.github.marozauartsiom.tvlauncher.ui.settings.UsbInstallerPage
import io.github.marozauartsiom.tvlauncher.ui.home.WeatherChip
import io.github.marozauartsiom.tvlauncher.ui.weather.CitySearchPage
import io.github.marozauartsiom.tvlauncher.ui.weather.WeatherPanel
import io.github.marozauartsiom.tvlauncher.ui.weather.WeatherText
import io.github.marozauartsiom.tvlauncher.ui.home.rememberClock
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundChoices
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundPick
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import android.os.Environment
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.BitmapFactory
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The whole UI. Home is always there; panels and menus open over it. */
@Composable
fun LauncherRoot(graph: AppGraph, viewModel: LauncherViewModel, safeMode: Boolean) {
    val settings by graph.settings.data.collectAsStateWithLifecycle(initialValue = SettingsData())
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()

    val source = remember(graph, viewModel) { HomeContentSource(graph.apps.apps, graph.settings.data, viewModel.resets) }
    val content by source.content.collectAsStateWithLifecycle(initialValue = HomeContent())
    val currentContent by rememberUpdatedState(content)
    val currentSettings by rememberUpdatedState(settings)

    val languageTag = LocaleController.effectiveTag(settings.language, Locale.getDefault())
    val clock by rememberClock(languageTag)

    // Fetch the weather only while Home is on screen (STARTED), at most every 30 minutes. Nothing runs otherwise.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, graph) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                graph.weather.refresh()
                delay(WeatherRepository.FRESH_MS)
            }
        }
    }

    // The theme: a preview from the theme picker wins, then the time-of-day schedule, then the saved theme.
    // The schedule is re-checked whenever the clock state changes (each minute while Home is visible, and on resume).
    // Weather: parsed once per fetch; the view (city, stale or not) is cheap and follows the clock.
    val parsedWeather = remember(settings.weatherCache) { settings.weatherCache?.let { graph.openMeteo.parseForecast(it.json) } }
    val weatherView = remember(parsedWeather, settings.weatherLocation, settings.autoPlace, clock) {
        WeatherView.of(settings, System.currentTimeMillis(), parsedWeather)
    }
    // Today's sunrise and sunset, only if the forecast is from today; otherwise the schedule uses 07:00 and 20:00.
    val sun: SunTimes? = weatherView.data?.takeIf { it.days.firstOrNull()?.date == LocalDate.now() }?.sunTimes()

    var preview by remember { mutableStateOf<ThemeId?>(null) }
    val scheduled = settings.themeSchedule.themeAt((clock.at ?: LocalDateTime.now()).toLocalTime(), sun)
    // Safe mode always uses the default theme: the saved one could be what keeps crashing.
    val theme = if (safeMode) Themes.default else Themes.byId(preview ?: scheduled ?: settings.theme)

    // The background: a preview from the background picker, else the saved choice (theme art, a colour or a picture).
    var backgroundPreview by remember { mutableStateOf<BackgroundPick?>(null) }
    val backgroundSpec = if (safeMode) {
        theme.background
    } else {
        BackgroundChoices.preview(theme, backgroundPreview)
            ?: BackgroundChoices.resolve(theme, settings.background, graph.backgroundImages::fileFor)
    }

    val context = LocalContext.current
    val installedApps by graph.apps.apps.collectAsStateWithLifecycle()
    // The owner's photo for the round Settings button: a small stored square, decoded off the main thread.
    val avatar by produceState<ImageBitmap?>(null, settings.avatar) {
        value = settings.avatar?.let { name ->
            withContext(Dispatchers.IO) { graph.avatarImages.fileFor(name)?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() } }
        }
    }
    val hiddenApps = remember(installedApps, settings.hidden) { installedApps.filter { it.packageName in settings.hidden } }
    val hdmiStatus by graph.hdmi.status.collectAsStateWithLifecycle()
    val pendingHdmi by graph.hdmi.pending.collectAsStateWithLifecycle()
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
    val themeFocus = remember { FocusMemory() }
    val languageFocus = remember { FocusMemory() }
    val hdmiSettingsFocus = remember { FocusMemory() }
    val hiddenFocus = remember { FocusMemory() }
    val backgroundFocus = remember { FocusMemory() }
    val usbFocus = remember { FocusMemory() }
    val avatarFocus = remember { FocusMemory() }
    val aboutFocus = remember { FocusMemory() }
    val thumbnailDispatcher = remember { Dispatchers.Default.limitedParallelism(2) }
    val systemLocale = remember { android.content.res.Resources.getSystem().configuration.locales[0] }
    val scheduleFocus = remember { FocusMemory() }
    val pickDayFocus = remember { FocusMemory() }
    val pickNightFocus = remember { FocusMemory() }
    val pageScope = rememberCoroutineScope()
    var moveOrigin by remember { mutableStateOf<List<ItemKey>?>(null) }

    val actions = remember(graph, viewModel, context, webLauncher) {
        HomeActions(
            onOpen = { tile -> openTile(graph, webLauncher, context, tile) },
            onMenu = { tile, anchor -> viewModel.open(Overlay.AppMenu(tile.key, anchor)) },
            // The panels below are opened by their own tasks; until then these do nothing.
            onWeather = { viewModel.open(Overlay.Weather) },
            onInputs = { viewModel.open(Overlay.Inputs) },
            onIphone = { if (!Airplay.open(graph.launcher)) viewModel.open(Overlay.IphoneHint) },
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

    // A signal on HDMI: ask in the launcher once nothing else is open. Taking it clears it, so it is asked once.
    LaunchedEffect(pendingHdmi, overlay is Overlay.None) {
        val input = pendingHdmi
        if (input != null && overlay is Overlay.None) {
            graph.hdmi.consume(input)
            viewModel.open(Overlay.Hdmi(input.id))
        }
    }

    // Back never leaves the launcher: it closes a panel, or sends Home to the first favourite.
    BackHandler { viewModel.onBack() }

    val menu = overlay as? Overlay.AppMenu
    val settingsOverlay = overlay as? Overlay.Settings
    val moveKey = (overlay as? Overlay.MoveMode)?.key

    LauncherTheme(theme) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(backgroundSpec, graph.backgrounds)
            HomeScreen(
                content = content,
                theme = theme,
                topBar = TopBarState(weather = weatherChip(weatherView, context), hdmi = hdmiStatus.map { HdmiChip(it.input.id, it.input.label, it.connected) }, avatar = avatar),
                clock = { clock },
                art = graph.banners,
                resets = viewModel.resets,
                actions = actions,
                safeMode = safeMode,
                overlayOpen = menu != null || settingsOverlay != null || overlay is Overlay.Weather || overlay is Overlay.WeatherSearch || overlay is Overlay.Hdmi || overlay is Overlay.IphoneHint || overlay is Overlay.Inputs,
                moveKey = moveKey,
            )
            when (overlay) {
                is Overlay.Hdmi -> hdmiStatus.firstOrNull { it.input.id == (overlay as Overlay.Hdmi).inputId }?.input?.let { input ->
                    // The card is dismissed however it goes away (button, Back, timeout, Home key): no more questions until it disconnects.
                    DisposableEffect(input.id) { onDispose { graph.hdmi.dismiss(input.id) } }
                    Box(Modifier.fillMaxSize().background(Color(0x660B0D12))) {
                        HdmiPrompt(
                            theme = theme,
                            input = input,
                            onSwitch = {
                                if (!graph.inputLauncher.open(input.id)) unavailable(context)
                                viewModel.onBack()
                            },
                            onDismiss = { viewModel.onBack() },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 27.dp),
                        )
                    }
                } ?: LaunchedEffect(Unit) { viewModel.onBack() } // the input vanished
                Overlay.Inputs -> InputsMenu(
                    inputs = hdmiStatus.map { HdmiChip(it.input.id, it.input.label, it.connected) },
                    theme = theme,
                    onOpen = { id ->
                        if (!graph.inputLauncher.open(id)) unavailable(context)
                        viewModel.open(Overlay.None)
                    },
                )
                Overlay.IphoneHint -> IphoneHintDialog(
                    theme = theme,
                    onInstallFromUsb = {
                        // The USB installer page lands in T22; until then the card just closes.
                        viewModel.open(if (SettingsPage.Usb.hasUi) Overlay.Settings(SettingsPage.Usb) else Overlay.None)
                    },
                    onClose = { viewModel.onBack() },
                )
                Overlay.Weather -> WeatherPanel(theme, weatherView, languageTag, onChangeCity = { viewModel.open(Overlay.WeatherSearch) })
                Overlay.WeatherSearch -> CitySearch(theme, graph, languageTag, viewModel)
                else -> Unit
            }
            when (settingsOverlay?.page) {
                SettingsPage.Root -> SettingsPanel(
                    theme = theme,
                    tiles = remember(systemSettings) { systemSettings.available() },
                    values = settingsValues(settings, scheduled ?: settings.theme, hiddenCount = hiddenApps.size),
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
                SettingsPage.Theme -> ThemePickerPage(
                    theme = theme,
                    title = stringResource(R.string.item_theme),
                    chosen = settings.theme,
                    onPreview = { preview = it },
                    onChoose = { id -> pageScope.launch { graph.settings.setTheme(id) } },
                    memory = themeFocus,
                    scheduleValue = stringResource(if (settings.themeSchedule.enabled) R.string.on else R.string.off),
                    onOpenSchedule = { viewModel.open(Overlay.Settings(SettingsPage.Schedule)) },
                )
                SettingsPage.Schedule -> SchedulePage(
                    theme = theme,
                    schedule = settings.themeSchedule,
                    sun = sun,
                    memory = scheduleFocus,
                    onToggle = { on -> pageScope.launch { graph.settings.setThemeSchedule(settings.themeSchedule.copy(enabled = on)) } },
                    onPickDay = { viewModel.open(Overlay.Settings(SettingsPage.SchedulePickDay)) },
                    onPickNight = { viewModel.open(Overlay.Settings(SettingsPage.SchedulePickNight)) },
                )
                SettingsPage.SchedulePickDay, SettingsPage.SchedulePickNight -> {
                    val day = settingsOverlay.page == SettingsPage.SchedulePickDay
                    ThemePickerPage(
                        theme = theme,
                        title = stringResource(if (day) R.string.schedule_day else R.string.schedule_night),
                        chosen = if (day) settings.themeSchedule.dayTheme else settings.themeSchedule.nightTheme,
                        onPreview = { preview = it },
                        onChoose = { id ->
                            pageScope.launch {
                                val current = settings.themeSchedule
                                graph.settings.setThemeSchedule(if (day) current.copy(dayTheme = id) else current.copy(nightTheme = id))
                            }
                            viewModel.open(Overlay.Settings(SettingsPage.Schedule))
                        },
                        memory = if (day) pickDayFocus else pickNightFocus,
                    )
                }
                SettingsPage.Background -> {
                    var accessChecks by remember { mutableIntStateOf(0) }
                    val pictures by produceState<PicturesState>(PicturesState.Loading, accessChecks) {
                        value = if (Environment.isExternalStorageManager()) {
                            PicturesState.Loaded(graph.backgroundImages.scan())
                        } else {
                            PicturesState.NoAccess
                        }
                    }
                    BackgroundPickerPage(
                        theme = theme,
                        saved = settings.background,
                        pictures = pictures,
                        adbCommand = "adb shell appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow",
                        memory = backgroundFocus,
                        thumbnail = { image -> withContext(thumbnailDispatcher) { BackgroundDecoder.thumbnail(File(image.path))?.asImageBitmap() } },
                        onPreview = { backgroundPreview = it },
                        onChoose = { pick ->
                            graph.appScope.launch {
                                when (pick) {
                                    BackgroundPick.FromTheme -> { graph.settings.setBackground(BackgroundChoice.FromTheme); graph.backgroundImages.clear() }
                                    is BackgroundPick.Solid -> { graph.settings.setBackground(BackgroundChoice.Solid(pick.argb)); graph.backgroundImages.clear() }
                                    is BackgroundPick.Picture -> {
                                        val name = graph.backgroundImages.import(File(pick.path))
                                        if (name != null) graph.settings.setBackground(BackgroundChoice.Image(name))
                                        else withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.bg_import_failed), Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            }
                        },
                        onCheckAccess = { accessChecks++ },
                    )
                }
                SettingsPage.Avatar -> {
                    var accessChecks by remember { mutableIntStateOf(0) }
                    val pictures by produceState<PicturesState>(PicturesState.Loading, accessChecks) {
                        value = if (Environment.isExternalStorageManager()) PicturesState.Loaded(graph.avatarImages.scan()) else PicturesState.NoAccess
                    }
                    AvatarPickerPage(
                        theme = theme,
                        hasPhoto = settings.avatar != null,
                        pictures = pictures,
                        adbCommand = "adb shell appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow",
                        memory = avatarFocus,
                        thumbnail = { image -> withContext(thumbnailDispatcher) { BackgroundDecoder.thumbnail(File(image.path))?.asImageBitmap() } },
                        onChoose = { image ->
                            graph.appScope.launch {
                                if (image == null) {
                                    graph.settings.setAvatar(null); graph.avatarImages.clear()
                                } else {
                                    val name = graph.avatarImages.import(File(image.path), AvatarSize, AvatarSize)
                                    if (name != null) graph.settings.setAvatar(name)
                                    else withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.bg_import_failed), Toast.LENGTH_SHORT).show() }
                                }
                            }
                            viewModel.onBack()
                        },
                        onCheckAccess = { accessChecks++ },
                    )
                }
                SettingsPage.Hidden -> HiddenAppsPage(
                    theme = theme,
                    apps = hiddenApps,
                    art = graph.banners,
                    memory = hiddenFocus,
                    onShow = { app -> graph.appScope.launch { graph.settings.setHidden(app.packageName, false) } },
                )
                SettingsPage.Hdmi -> {
                    var permissionChecks by remember { mutableIntStateOf(0) }
                    val canDraw = remember(permissionChecks) { android.provider.Settings.canDrawOverlays(context) }
                    HdmiSettingsPage(
                        theme = theme,
                        settings = settings.hdmi,
                        inputs = hdmiStatus.map { it.input },
                        canDrawOverlays = canDraw,
                        adbCommand = "adb shell appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow",
                        memory = hdmiSettingsFocus,
                        onChange = { new -> graph.appScope.launch { graph.settings.setHdmi(new) } },
                        onCheckPermission = { permissionChecks++ },
                    )
                }
                SettingsPage.Usb -> {
                    // The page looks at the drive when it opens and on "Check again" / "Done"; never in the background.
                    LaunchedEffect(Unit) { graph.usbInstaller.refresh() }
                    val usbState by graph.usbInstaller.state.collectAsStateWithLifecycle()
                    UsbInstallerPage(
                        theme = theme,
                        state = usbState,
                        storageCommand = "adb shell appops set --uid ${context.packageName} MANAGE_EXTERNAL_STORAGE allow",
                        unknownAppsCommand = "adb shell appops set --uid ${context.packageName} REQUEST_INSTALL_PACKAGES allow",
                        memory = usbFocus,
                        onInstall = graph.usbInstaller::install,
                        onCheckAgain = graph.usbInstaller::refresh,
                        onOpen = { item ->
                            when (graph.launcher.launch(item.info.packageName)) {
                                LaunchResult.Started -> viewModel.open(Overlay.None)
                                else -> Toast.makeText(context, context.getString(R.string.launch_failed, item.info.label), Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDone = graph.usbInstaller::refresh,
                    )
                }
                SettingsPage.About -> {
                    val info = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull() }
                    AboutPage(theme, info?.versionName.orEmpty(), info?.longVersionCode ?: 0L, aboutFocus)
                }
                SettingsPage.Weather -> CitySearch(theme, graph, languageTag, viewModel)
                SettingsPage.Language -> LanguagePickerPage(
                    theme = theme,
                    selected = settings.language,
                    tvTag = LocaleController.effectiveTag(LanguageChoice.Follow, systemLocale),
                    memory = languageFocus,
                    onChoose = { choice ->
                        graph.appScope.launch { graph.settings.setLanguage(choice) }
                        graph.locale.apply(choice, context.findActivity()) // applies at once; recreates the activity below API 33
                    },
                )
                else -> Unit // other pages: their own tasks
            }
            if (menu != null) {
                val tile = content.all.firstOrNull { it.key == menu.key } ?: content.hidden.firstOrNull { it.key == menu.key }
                // The app vanished while the menu was opening: nothing to show.
                LaunchedEffect(tile == null) { if (tile == null) viewModel.open(Overlay.None) }
                if (tile != null) {
                    AppMenu(
                        state = AppMenuState(tile, isFavourite = menu.key in settings.favourites, anchor = menu.anchor, isHidden = content.hidden.any { it.key == menu.key }),
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
        MenuAction.Unhide -> if (packageName != null) graph.appScope.launch { graph.settings.setHidden(packageName, false) }
        MenuAction.Info -> if (packageName != null && !packageActions.showInfo(packageName)) unavailable(context)
        MenuAction.Uninstall -> if (packageName != null && !packageActions.uninstall(packageName)) unavailable(context)
    }
    viewModel.open(Overlay.None)
}

/** Side of the stored profile photo in px: the button is 36 dp, so 2× xhdpi with room to spare. */
private const val AvatarSize = 160

private fun unavailable(context: Context) =
    Toast.makeText(context, context.getString(R.string.unavailable_on_tv), Toast.LENGTH_SHORT).show()

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** The city search with its effects: a pick or "detect automatically" is saved, the forecast is fetched at once, and the page closes. */
@Composable
private fun CitySearch(theme: ThemeSpec, graph: AppGraph, languageTag: String, viewModel: LauncherViewModel) {
    fun choose(location: WeatherLocation) {
        graph.appScope.launch {
            graph.settings.setWeatherLocation(location)
            graph.weather.refresh(force = true)
        }
        viewModel.onBack()
    }
    CitySearchPage(
        theme = theme,
        search = { query -> graph.openMeteo.search(query, languageTag) },
        onAuto = { choose(WeatherLocation.Auto) },
        onPick = { place -> choose(WeatherLocation.Manual(place)) },
    )
}

/** The top-bar chip: temperature (or "—°" when the data is more than six hours old), the city, and a wording for TalkBack. */
private fun weatherChip(view: WeatherView, context: Context): WeatherChip? {
    val data = view.data ?: return null
    val condition = WeatherCodes.condition(data.current.code)
    return WeatherChip(
        temperature = if (view.stale) WeatherText.UNKNOWN_TEMPERATURE else WeatherText.temperature(data.current.temperature),
        place = view.place,
        description = context.getString(WeatherText.conditionRes(condition)),
        condition = condition,
        isDay = data.current.isDay,
    )
}
