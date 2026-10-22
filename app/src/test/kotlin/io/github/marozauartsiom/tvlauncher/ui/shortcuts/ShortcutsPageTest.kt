package io.github.marozauartsiom.tvlauncher.ui.shortcuts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.data.prefs.SaveResult
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcuts
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ShortcutsPageTest : TvScreenshotTest() {
    private var shortcuts by mutableStateOf(
        listOf(Shortcut("n", "Новости", "https://news.example.com"), Shortcut("k", "Кино онлайн", "https://kino.example.org")),
    )
    private val saved = mutableListOf<Triple<String?, String, String>>()
    private val deleted = mutableListOf<String>()

    private fun show() {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                ShortcutsPage(
                    theme = Themes.default,
                    shortcuts = shortcuts,
                    onSave = { id, title, address ->
                        saved += Triple(id, title, address)
                        val url = Shortcuts.normalizeUrl(address)
                        if (title.isBlank() || url == null) SaveResult.Invalid
                        else SaveResult.Saved(Shortcut(id ?: "new", title, url)).also { saveResult ->
                            shortcuts = if (id == null) shortcuts + saveResult.shortcut else shortcuts.map { if (it.id == id) saveResult.shortcut else it }
                        }
                    },
                    onDelete = { id -> deleted += id; shortcuts = shortcuts.filterNot { it.id == id } },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    private fun back() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test fun listStartsOnAddAndShowsEveryShortcutWithItsAddress() {
        show()
        composeRule.onNodeWithText("Add shortcut").assertIsFocused()
        composeRule.onNodeWithText("Новости").assertExists()
        composeRule.onNodeWithText("news.example.com").assertExists()
        composeRule.onNodeWithText("kino.example.org").assertExists()
    }

    @Test fun addingAShortcutTypesNameAndAddressAndSaves() {
        show()
        press(Key.Enter) // Add shortcut
        composeRule.onNodeWithText("Name").assertExists()
        composeRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[0].performTextInput("Погода")
        composeRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[1].performTextInput("weather.example.net")
        composeRule.onNodeWithText("Save").performKeyInputOk()
        assertEquals(listOf(Triple<String?, String, String>(null, "Погода", "weather.example.net")), saved)
        // back on the list, with the new row
        composeRule.onNodeWithText("Add shortcut").assertExists()
        composeRule.onNodeWithText("weather.example.net").assertExists()
    }

    @Test fun anInvalidAddressShowsTheMessageAndStaysInTheEditor() {
        show()
        press(Key.Enter)
        composeRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[0].performTextInput("X")
        composeRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[1].performTextInput("javascript:alert(1)")
        composeRule.onNodeWithText("Save").performKeyInputOk()
        composeRule.onNodeWithText("Enter a name and a web address, for example example.com").assertExists()
        composeRule.onNodeWithText("Cancel").assertExists()
    }

    @Test fun editingAnExistingShortcutOffersDeleteAndKeepsItsId() {
        show()
        press(Key.DirectionDown) // Новости
        press(Key.Enter)
        composeRule.onNodeWithText("Delete").assertExists()
        composeRule.onAllNodes(androidx.compose.ui.test.hasSetTextAction())[0].performTextInput("!")
        composeRule.onNodeWithText("Save").performKeyInputOk()
        assertEquals("n", saved.single().first)
        assertEquals("Новости!", saved.single().second) // typing continues at the end of the old text
    }

    @Test fun deleteRemovesTheRowAndReturnsToTheList() {
        show()
        press(Key.DirectionDown, 2) // Кино онлайн
        press(Key.Enter)
        composeRule.onNodeWithText("Delete").performKeyInputOk()
        assertEquals(listOf("k"), deleted)
        composeRule.onNodeWithText("Кино онлайн").assertDoesNotExist()
        composeRule.onNodeWithText("Add shortcut").assertExists()
    }

    @Test fun newShortcutHasNoDeleteButton() {
        show()
        press(Key.Enter)
        composeRule.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test fun backLeavesTheEditorBeforeThePageAndFocusReturnsToTheRow() {
        show()
        press(Key.DirectionDown) // Новости
        press(Key.Enter)
        composeRule.onNodeWithText("Cancel").assertExists()
        back()
        composeRule.onNodeWithText("Add shortcut").assertExists()
        composeRule.onNode(hasText("Новости")).assertIsFocused()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performKeyInputOk() {
        performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) // what OK on the focused button does
        composeRule.waitForIdle()
    }
}
