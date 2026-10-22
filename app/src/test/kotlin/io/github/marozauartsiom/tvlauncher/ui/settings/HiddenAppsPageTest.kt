package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.data.apps.TvApp
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.home.FakeArt
import io.github.marozauartsiom.tvlauncher.ui.home.HomeFixtures
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HiddenAppsPageTest : TvScreenshotTest() {
    private var apps by mutableStateOf(HomeFixtures.hiddenApps.sortedBy { it.label })
    private val shown = mutableListOf<String>()
    private val memory = FocusMemory() // one per page, as in the app (not one per recomposition)

    private fun show(list: List<TvApp> = apps) {
        apps = list
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                HiddenAppsPage(Themes.default, apps, FakeArt(), memory, onShow = { app -> shown += app.label; apps = apps - app })
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    @Test fun listsEachHiddenAppWithAShowAction() {
        show()
        composeRule.onNodeWithText("Google Play Фильмы").assertExists()
        composeRule.onNodeWithText("Xiaomi Mi Home").assertExists()
        composeRule.onNodeWithText("Hidden apps are not shown on the home screen. Press OK to bring an app back.").assertExists()
    }

    @Test fun focusStartsOnTheFirstRow() {
        show()
        composeRule.onNodeWithText("Google Play Фильмы").assertIsFocused()
    }

    @Test fun okBringsTheAppBackAndTheRowGoes() {
        show()
        press(Key.Enter)
        assertEquals(listOf("Google Play Фильмы"), shown)
        composeRule.onNodeWithText("Google Play Фильмы").assertDoesNotExist()
        composeRule.onNodeWithText("Xiaomi Mi Home").assertIsFocused() // focus moved to the row that took its place
    }

    @Test fun removingTheLastRowShowsTheEmptyMessage() {
        show()
        press(Key.Enter)
        press(Key.Enter)
        composeRule.onNodeWithText("No hidden apps").assertExists()
        assertEquals(listOf("Google Play Фильмы", "Xiaomi Mi Home"), shown)
    }

    @Test fun removingTheLowerRowFocusesTheOneAbove() {
        show()
        press(Key.DirectionDown)
        press(Key.Enter)
        composeRule.onNodeWithText("Google Play Фильмы").assertIsFocused()
    }

    @Test fun noHiddenAppsSaysSo() {
        show(emptyList())
        composeRule.onNodeWithText("No hidden apps").assertExists()
    }
}

class HiddenAppsScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }
    private val art = FakeArt()

    @Test fun hiddenApps() {
        captureThemes("hidden-apps", listOf("midnight", "sky", "valley").map(Themes::byKey)) { theme ->
            backdrop.Over(theme) { HiddenAppsPage(theme, HomeFixtures.hiddenApps.sortedBy { it.label }, art, FocusMemory(), {}) }
        }
    }

    @Test fun emptyInTheLongestLanguages() {
        captureLocales("hidden-apps-locale", listOf("be", "fr")) { _ ->
            backdrop.Over(Themes.default) { HiddenAppsPage(Themes.default, emptyList(), art, FocusMemory(), {}) }
        }
    }
}
