package io.github.marozauartsiom.tvlauncher.ui.hdmi

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.data.prefs.HdmiSettings
import io.github.marozauartsiom.tvlauncher.system.hdmi.HdmiInput
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.settings.HdmiSettingsPage
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

private val Ps5 = HdmiInput("hdmi2", "HDMI 2", "PlayStation 5")

@OptIn(ExperimentalTestApi::class)
class HdmiPromptTest : TvScreenshotTest() {
    private var switched = 0
    private var dismissed = 0

    private fun show(input: HdmiInput = Ps5) {
        composeRule.setContent {
            LauncherTheme(Themes.default) { HdmiPrompt(Themes.default, input, { switched++ }, { dismissed++ }) }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    @Test fun showsTheInputAndTheDeviceAndSwitchHasFocus() {
        show()
        composeRule.onNodeWithText("Signal on HDMI 2").assertExists()
        composeRule.onNodeWithText("PlayStation 5").assertExists()
        composeRule.onNodeWithText("Switch").assertIsFocused()
        composeRule.onNodeWithText("Hides in 15 s").assertExists()
    }

    @Test fun okSwitches() {
        show()
        press(Key.Enter)
        assertEquals(1, switched)
        assertEquals(0, dismissed)
    }

    @Test fun notNowDismisses() {
        show()
        press(Key.DirectionRight)
        composeRule.onNodeWithText("Not now").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, dismissed)
        assertEquals(0, switched)
    }

    @Test fun backMeansNotNow() {
        show()
        press(Key.Back)
        assertEquals(1, dismissed)
    }

    @Test fun theCountdownTicksAndHidesTheCardAfterFifteenSeconds() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { LauncherTheme(Themes.default) { HdmiPrompt(Themes.default, Ps5, { switched++ }, { dismissed++ }) } }
        composeRule.mainClock.advanceTimeBy(5_500)
        composeRule.onNodeWithText("Hides in 10 s").assertExists()
        composeRule.mainClock.advanceTimeBy(9_000)
        assertEquals(0, dismissed)
        composeRule.mainClock.advanceTimeBy(1_500)
        assertEquals(1, dismissed)
    }

    @Test fun aSourceNameIsOptional() {
        show(HdmiInput("hdmi1", "HDMI 1", null))
        composeRule.onNodeWithText("Signal on HDMI 1").assertExists()
    }
}

@OptIn(ExperimentalTestApi::class)
class HdmiSettingsPageTest : TvScreenshotTest() {
    private val changes = mutableListOf<HdmiSettings>()
    private var checks = 0
    private val inputs = listOf(HdmiInput("hdmi1", "HDMI 1", null), HdmiInput("hdmi2", "HDMI 2", null), HdmiInput("hdmi3", "HDMI 3", null))

    private fun show(settings: HdmiSettings = HdmiSettings(), canDraw: Boolean = true) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                HdmiSettingsPage(
                    Themes.default, settings, inputs, canDraw, "adb shell appops set pkg SYSTEM_ALERT_WINDOW allow", FocusMemory(),
                    onChange = { changes += it }, onCheckPermission = { checks++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    @Test fun okTurnsTheAskOffAndOn() {
        show()
        composeRule.onNodeWithText("Ask about a new signal").assertIsFocused()
        press(Key.Enter)
        assertEquals(false, changes.last().promptEnabled)
    }

    @Test fun overAppsNeedsThePermissionAndShowsTheCommandWithoutIt() {
        show(canDraw = false)
        composeRule.onNodeWithText("adb shell appops set pkg SYSTEM_ALERT_WINDOW allow").assertExists()
        press(Key.DirectionDown, 2)
        composeRule.onNodeWithText("Check again").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, checks)
    }

    @Test fun noCommandWhenThePermissionIsGranted() {
        show(canDraw = true)
        composeRule.onNodeWithText("Check again").assertDoesNotExist()
    }

    @Test fun eachInputHasItsOwnSwitch() {
        show()
        composeRule.onNodeWithText("Ask for inputs").assertExists()
        press(Key.DirectionDown, 2) // past the two top switches (the permission is granted, so no note)
        composeRule.onNodeWithText("HDMI 1").assertIsFocused()
        press(Key.Enter)
        assertEquals(setOf("hdmi1"), changes.last().mutedInputs)
        press(Key.Enter)
        assertEquals(emptySet<String>(), changes.last().mutedInputs - "hdmi1")
    }
}

@OptIn(ExperimentalTestApi::class)
class HdmiScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }
    private val threeThemes by lazy { listOf("midnight", "sky", "valley").map(Themes::byKey) }
    private val inputs = listOf(HdmiInput("hdmi1", "HDMI 1", null), HdmiInput("hdmi2", "HDMI 2", null), HdmiInput("hdmi3", "HDMI 3", null))

    @Test fun promptOverHome() {
        captureThemes("hdmi-prompt", threeThemes) { theme ->
            backdrop.Over(theme) {
                Box(Modifier.fillMaxSize()) {
                    HdmiPrompt(theme, Ps5, {}, {}, Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 27.dp))
                }
            }
        }
    }

    @Test fun settingsWithoutThePermission() {
        captureThemes("hdmi-settings", threeThemes) { theme ->
            backdrop.Over(theme) {
                HdmiSettingsPage(
                    theme, HdmiSettings(mutedInputs = setOf("hdmi3")), inputs, false,
                    "adb shell appops set io.github.marozauartsiom.tvlauncher SYSTEM_ALERT_WINDOW allow", FocusMemory(), {}, {},
                )
            }
        }
    }

    @Test fun promptInTheLongestLanguages() {
        captureLocales("hdmi-prompt-locale", listOf("be", "fr")) { _ ->
            backdrop.Over(Themes.default) {
                Box(Modifier.fillMaxSize()) {
                    HdmiPrompt(Themes.default, Ps5, {}, {}, Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 27.dp))
                }
            }
        }
    }
}
