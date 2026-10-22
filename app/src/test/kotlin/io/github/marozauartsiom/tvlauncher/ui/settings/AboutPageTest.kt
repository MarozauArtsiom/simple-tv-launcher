package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.SettingsPage
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AboutPageTest : TvScreenshotTest() {
    private fun show() {
        composeRule.setContent { LauncherTheme(Themes.default) { AboutPage(Themes.default, "1.0.0", 1, FocusMemory()) } }
        composeRule.waitForIdle()
    }

    @Test fun showsVersionPromiseAndTheThreeLicences() {
        show()
        composeRule.onNodeWithText("Version 1.0.0 (1)").assertExists()
        composeRule.onNodeWithText("A home screen made for this TV only. No ads, no tracking, no background services.").assertExists()
        composeRule.onNodeWithText("Open-Meteo.com · CC BY 4.0").assertExists()
        composeRule.onNodeWithText("SIL OFL 1.1").assertExists()
        composeRule.onNodeWithText("Apache 2.0").assertExists()
    }

    @Test fun focusStartsOnTheFirstLicenceAndMovesDown() {
        show()
        composeRule.onNodeWithText("Weather data").assertIsFocused()
        composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nunito font").assertIsFocused()
    }

    @Test fun theSettingsListOpensIt() = assertTrue(SettingsPage.About.hasUi && SettingsPage.Usb.hasUi)
}

class AboutScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }

    @Test fun about() {
        captureThemes("settings-about", listOf("midnight", "sky", "valley").map(Themes::byKey)) { theme ->
            backdrop.Over(theme) { AboutPage(theme, "1.0.0", 1, FocusMemory()) }
        }
    }

    @Test fun inTheLongestLanguages() {
        captureLocales("settings-about-locale", listOf("be", "fr")) { _ ->
            backdrop.Over(Themes.default) { AboutPage(Themes.default, "1.0.0", 1, FocusMemory()) }
        }
    }
}
