package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.system.SystemTile
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.SettingsPage
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsPanelTest : TvScreenshotTest() {
    private val tiles = mutableListOf<SystemTile>()
    private val pages = mutableListOf<SettingsPage>()
    private val memory = FocusMemory()

    private fun show(available: List<SystemTile> = SystemTile.entries) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                SettingsPanel(Themes.default, available, SettingsFixtures.values, memory, { tiles += it }, { pages += it })
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    @Test fun focusStartsOnTheFirstTile() {
        show()
        composeRule.onNodeWithText("Wi-Fi").assertIsFocused()
    }

    @Test fun tilesAreAGridOfThreeAndOkOpensTheFocusedOne() {
        show()
        press(Key.DirectionRight, 2)
        composeRule.onNodeWithText("Sound").assertIsFocused()
        press(Key.DirectionDown)
        composeRule.onNodeWithText("Android").assertIsFocused() // second row, third column
        press(Key.Enter)
        assertEquals(listOf(SystemTile.Android), tiles)
    }

    @Test fun anUnavailableTileIsNotShown() {
        show(SystemTile.entries - SystemTile.Sleep)
        composeRule.onNodeWithText("Sleep timer").assertDoesNotExist()
        composeRule.onNodeWithText("Picture").assertExists()
    }

    @Test fun downFromTilesGoesToTheLauncherList() {
        show()
        press(Key.DirectionDown, 2)
        composeRule.onNodeWithText("Theme").assertIsFocused()
        press(Key.Enter)
        assertEquals(listOf(SettingsPage.Theme), pages)
    }

    @Test fun rowsShowTheirCurrentValues() {
        show()
        composeRule.onNodeWithText("Midnight").assertExists()
        composeRule.onNodeWithText("From theme").assertExists()
        composeRule.onNodeWithText("Minsk").assertExists()
        composeRule.onNodeWithText("2").assertExists() // hidden apps
    }

    @Test fun comingBackFromAPageFocusesTheRowThatWasOpen() {
        var open by androidx.compose.runtime.mutableStateOf(true)
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                if (open) SettingsPanel(Themes.default, SystemTile.entries, SettingsFixtures.values, memory, {}, {})
            }
        }
        composeRule.waitForIdle()
        press(Key.DirectionDown, 3) // Background
        composeRule.onNodeWithText("Background").assertIsFocused()
        open = false // a page replaces the panel ...
        composeRule.waitForIdle()
        open = true // ... and Back brings the panel again, as a new composition with the same memory
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Background").assertIsFocused()
    }

    @Test fun withoutAnyTilesFocusStartsOnTheFirstRow() {
        show(emptyList())
        composeRule.onNodeWithText("Theme").assertIsFocused()
    }
}

object SettingsFixtures {
    val values = SettingsValues(
        theme = "Midnight", background = "From theme", language = "Русский", weather = "Minsk",
        hdmi = "On", hiddenCount = 2, shortcutsCount = 0,
    )
}
