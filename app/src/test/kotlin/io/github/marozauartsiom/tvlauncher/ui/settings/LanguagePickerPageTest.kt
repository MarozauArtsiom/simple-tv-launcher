package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.assertTextContains
import io.github.marozauartsiom.tvlauncher.data.prefs.LanguageChoice
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LanguagePickerPageTest : TvScreenshotTest() {
    private val chosen = mutableListOf<LanguageChoice>()

    private fun show(selected: LanguageChoice = LanguageChoice.Follow, tvTag: String = "ru") {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                LanguagePickerPage(Themes.default, selected, tvTag, FocusMemory(), { chosen += it })
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    @Test fun orderIsBelarusianFirstAndFollowTheTvLast() {
        show()
        val titles = composeRule.onAllNodes(isFocusable()).fetchSemanticsNodes().map { node ->
            node.config[androidx.compose.ui.semantics.SemanticsProperties.Text].joinToString(" ") { it.text }
        }
        assertEquals(
            listOf("Беларуская", "Русский", "English", "Polski", "Deutsch", "Français", "Same as the TV Русский"),
            titles,
        )
    }

    @Test fun defaultSelectionIsFollowTheTvAndItHasFocusAndCheck() {
        show()
        composeRule.onNodeWithText("Same as the TV").assertIsFocused()
        composeRule.onNodeWithText("Same as the TV").assertIsSelected()
        composeRule.onNodeWithText("English").assertExists()
    }

    @Test fun theTvRowShowsWhichLanguageItResolvesTo() {
        show(tvTag = "en")
        composeRule.onNodeWithText("Same as the TV").assertTextContains("English", substring = true)
    }

    @Test fun focusStartsOnTheSavedFixedLanguage() {
        show(selected = LanguageChoice.Fixed("pl"))
        composeRule.onNodeWithText("Polski").assertIsFocused()
        composeRule.onNodeWithText("Polski").assertIsSelected()
    }

    @Test fun okChoosesTheFocusedLanguage() {
        show(selected = LanguageChoice.Fixed("ru"))
        press(Key.DirectionDown) // English
        press(Key.Enter)
        assertEquals(listOf<LanguageChoice>(LanguageChoice.Fixed("en")), chosen)
        press(Key.DirectionDown, 4) // Follow the TV
        press(Key.Enter)
        assertEquals(LanguageChoice.Follow, chosen.last())
    }
}

class LanguagePickerScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }

    @Test fun languagePickerInTheLongestLanguages() {
        captureLocales("language-picker", listOf("be", "fr"), theme = Themes.default) { _ ->
            backdrop.Over(Themes.default) {
                LanguagePickerPage(Themes.default, LanguageChoice.Follow, "ru", FocusMemory(), {})
            }
        }
    }
}
