package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.data.prefs.SunTimes
import io.github.marozauartsiom.tvlauncher.data.prefs.ThemeSchedule
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.home.FakeArt
import io.github.marozauartsiom.tvlauncher.ui.home.HomeActions
import io.github.marozauartsiom.tvlauncher.ui.home.HomeFixtures
import io.github.marozauartsiom.tvlauncher.ui.home.HomeScreen
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundCache
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeId
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemedBackground
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test

/** The theme picker (with live preview) and the schedule page, compared with `settings/theme-picker.png` and `screens/settings-schedule-*.png`. */
@OptIn(ExperimentalTestApi::class)
class ThemePagesScreenshotTest : TvScreenshotTest() {
    private val cache by lazy {
        BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined).also { c ->
            runBlocking { Themes.all.forEach { c.load(it.background) } }
        }
    }
    private val art = FakeArt()
    private var preview by mutableStateOf<ThemeId?>(null)

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    /** Midnight is saved; focus is on Space (Down, Down, Right, Right from Midnight), so the whole launcher previews Space. */
    @Test fun pickerPreviewsTheFocusedTheme() {
        composeRule.setContent {
            val theme = Themes.byId(preview ?: ThemeId.Midnight)
            LauncherTheme(theme) {
                Backdrop(theme) {
                    ThemePickerPage(
                        theme = theme, title = androidx.compose.ui.res.stringResource(io.github.marozauartsiom.tvlauncher.R.string.item_theme),
                        chosen = ThemeId.Midnight, onPreview = { preview = it }, onChoose = {}, memory = FocusMemory(),
                        scheduleValue = androidx.compose.ui.res.stringResource(io.github.marozauartsiom.tvlauncher.R.string.off), onOpenSchedule = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        press(Key.DirectionDown); press(Key.DirectionDown); press(Key.DirectionRight); press(Key.DirectionRight)
        composeRule.onRoot().captureTo("theme-picker/space.png")
    }

    @Test fun pickerAtRestShowsTheSavedThemeWithItsCheck() {
        captureThemes("theme-picker", listOf(Themes.byKey("midnight"), Themes.byKey("sky"))) { theme ->
            Backdrop(theme) {
                ThemePickerPage(
                    theme = theme, title = androidx.compose.ui.res.stringResource(io.github.marozauartsiom.tvlauncher.R.string.item_theme),
                    chosen = theme.id, onPreview = {}, onChoose = {}, memory = FocusMemory(),
                    scheduleValue = androidx.compose.ui.res.stringResource(io.github.marozauartsiom.tvlauncher.R.string.off), onOpenSchedule = {},
                )
            }
        }
    }

    @Test fun schedule() {
        val themes = listOf("midnight", "sky", "valley").map(Themes::byKey)
        captureThemes("theme-schedule", themes) { theme ->
            Backdrop(theme) {
                SchedulePage(
                    theme, ThemeSchedule(enabled = true, dayTheme = ThemeId.Paper, nightTheme = ThemeId.Space),
                    SunTimes(LocalTime.of(7, 5), LocalTime.of(18, 12)), FocusMemory(), {}, {}, {},
                )
            }
        }
    }

    @Composable
    private fun Backdrop(theme: ThemeSpec, panel: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            ThemedBackground(theme, cache)
            HomeScreen(
                content = HomeFixtures.content, theme = theme, topBar = HomeFixtures.topBar, clock = { HomeFixtures.clock },
                art = art, resets = emptyFlow(), actions = HomeActions(), overlayOpen = true,
            )
            panel()
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.captureTo(path: String) =
    captureRoboImage("src/test/snapshots/$path")
