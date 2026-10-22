package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.data.background.PickedImage
import io.github.marozauartsiom.tvlauncher.data.prefs.BackgroundChoice
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundChoices
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundPick
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class BackgroundPickerPageTest : TvScreenshotTest() {
    private val previews = mutableListOf<BackgroundPick?>()
    private val chosen = mutableListOf<BackgroundPick>()
    private var checks = 0
    private var showPage by mutableStateOf(true)
    private val images = (1..6).map { PickedImage("/usb/pic$it.jpg", "pic$it.jpg") }
    private val navy = BackgroundChoices_navy

    private fun show(saved: BackgroundChoice = BackgroundChoice.FromTheme, pictures: PicturesState = PicturesState.Loaded(images)) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                if (showPage) BackgroundPickerPage(
                    theme = Themes.default, saved = saved, pictures = pictures,
                    adbCommand = "adb shell appops set --uid io.github.marozauartsiom.tvlauncher MANAGE_EXTERNAL_STORAGE allow",
                    memory = FocusMemory(), thumbnail = { null as ImageBitmap? },
                    onPreview = { previews += it }, onChoose = { chosen += it }, onCheckAccess = { checks++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    @Test fun focusStartsOnFromThemeAndItIsMarkedSaved() {
        show()
        composeRule.onNodeWithText("From theme").assertIsFocused()
        composeRule.onNodeWithText("From theme").assertIsSelected()
    }

    @Test fun focusStartsOnTheSavedColour() {
        show(saved = BackgroundChoice.Solid(navy))
        composeRule.onAllSwatches()[1].assertIsFocused()
        composeRule.onAllSwatches()[1].assertIsSelected()
    }

    @Test fun focusingColoursAndPicturesPreviewsThem() {
        show()
        press(Key.DirectionDown) // first swatch
        press(Key.DirectionRight) // second
        assertEquals(BackgroundPick.Solid(navy), previews.last())
        press(Key.DirectionDown) // first picture
        assertEquals(BackgroundPick.Picture("/usb/pic1.jpg"), previews.last())
        press(Key.DirectionRight)
        assertEquals(BackgroundPick.Picture("/usb/pic2.jpg"), previews.last())
        assertEquals(emptyList<BackgroundPick>(), chosen)
    }

    @Test fun okKeepsWhatIsFocused() {
        show()
        press(Key.DirectionDown); press(Key.DirectionRight); press(Key.Enter)
        assertEquals(listOf<BackgroundPick>(BackgroundPick.Solid(navy)), chosen)
        press(Key.DirectionDown) // from the second swatch to the first picture above-below it
        press(Key.Enter)
        assertEquals(BackgroundPick.Picture("/usb/pic1.jpg"), chosen.last())
    }

    @Test fun picturesAreInRowsOfThreeWithTheirCount() {
        show()
        composeRule.onNodeWithText("Images from USB · 6").assertExists()
        press(Key.DirectionDown, 2) // first picture
        press(Key.DirectionDown) // row 2, first
        composeRule.onNodeWithContentDescription("pic4.jpg").assertIsFocused()
    }

    @Test fun noPicturesShowsAMessage() {
        show(pictures = PicturesState.Loaded(emptyList()))
        composeRule.onNodeWithText("No images on the USB drive").assertExists()
    }

    @Test fun withoutAccessTheAdbHintAndACheckButtonReplaceThePictures() {
        show(pictures = PicturesState.NoAccess)
        composeRule.onNodeWithText("Access to all files is needed. Run once on a computer:").assertExists()
        composeRule.onNodeWithText("adb shell appops set --uid io.github.marozauartsiom.tvlauncher MANAGE_EXTERNAL_STORAGE allow").assertExists()
        press(Key.DirectionDown, 2) // swatches, then the button
        composeRule.onNodeWithText("Check again").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, checks)
    }

    @Test fun leavingThePageEndsThePreview() {
        show()
        press(Key.DirectionDown)
        showPage = false
        composeRule.waitForIdle()
        assertEquals(null, previews.last())
    }

    /** The six swatches in order (tags, because the focused one moves in z-order and so in the semantics list). */
    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllSwatches() =
        BackgroundChoices.darkSwatches.map { onNodeWithTag("swatch:$it") }
}

private const val BackgroundChoices_navy = 0xFF1B2A44.toInt()
