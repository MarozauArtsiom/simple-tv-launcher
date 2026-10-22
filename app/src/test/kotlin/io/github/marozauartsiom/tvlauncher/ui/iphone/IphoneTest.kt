package io.github.marozauartsiom.tvlauncher.ui.iphone

import android.content.Intent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.marozauartsiom.tvlauncher.data.apps.Airplay
import io.github.marozauartsiom.tvlauncher.data.apps.AppLauncher
import io.github.marozauartsiom.tvlauncher.data.apps.FakeSource
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AirplayTest {
    private val source = FakeSource()
    private val started = mutableListOf<Intent>()
    private val launcher = AppLauncher(source) { started += it }

    @Test fun anInstalledAirplayServerIsStarted() {
        source.intents[Airplay.PACKAGE] = Intent("airplay.MAIN")
        assertTrue(Airplay.open(launcher))
        assertEquals("airplay.MAIN", started.single().action)
    }

    @Test fun withoutItTheHowToCardIsNeeded() {
        assertFalse(Airplay.open(launcher))
        assertTrue(started.isEmpty())
    }

    @Test fun aRefusedStartAlsoShowsTheCard() {
        source.intents[Airplay.PACKAGE] = Intent("airplay.MAIN")
        assertFalse(Airplay.open(AppLauncher(source) { throw SecurityException() }))
    }

    @Test fun thePackageIsTheOpenSourceReceiver() = assertEquals("io.github.jqssun.airplay", Airplay.PACKAGE)
}

@OptIn(ExperimentalTestApi::class)
class IphoneHintDialogTest : TvScreenshotTest() {
    private var install = 0
    private var close = 0

    private fun show() {
        composeRule.setContent { LauncherTheme(Themes.default) { IphoneHintDialog(Themes.default, { install++ }, { close++ }) } }
        composeRule.waitForIdle()
    }

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    @Test fun explainsWhatToInstallAndHowToMirror() {
        show()
        composeRule.onNodeWithText("iPhone screen").assertExists()
        composeRule.onNodeWithText("Showing the screen needs the AirPlay Server app, which isn’t installed.").assertExists()
        composeRule.onNodeWithText("Put its .apk file on a USB drive and open “Install from USB”.").assertExists()
        composeRule.onNodeWithText("On the iPhone: Control Center → Screen Mirroring → pick this TV.").assertExists()
    }

    @Test fun installFromUsbHasFocusAndCloseIsNext() {
        show()
        composeRule.onNodeWithText("Install from USB").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, install)
        press(Key.DirectionRight)
        composeRule.onNodeWithText("Close").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, close)
    }
}

class IphoneScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }

    @Test fun hintInThreeThemesAndTwoLanguages() {
        captureThemes("iphone-hint", listOf("midnight", "sky", "valley").map(Themes::byKey)) { theme ->
            backdrop.Over(theme) { IphoneHintDialog(theme, {}, {}) }
        }
        // (languages below use the default theme)
    }

    @Test fun hintInTheLongestLanguages() {
        captureLocales("iphone-hint-locale", listOf("be", "fr")) { _ ->
            backdrop.Over(Themes.default) { IphoneHintDialog(Themes.default, {}, {}) }
        }
    }
}
