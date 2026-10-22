package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppMenuTest : TvScreenshotTest() {
    private val chosen = mutableListOf<MenuAction>()
    private val tile = HomeFixtures.content.all.first { it.label == "YouTube" }

    private fun show(isFavourite: Boolean, tile: HomeTile = this.tile) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                AppMenu(AppMenuState(tile, isFavourite, Rect(96f, 278f, 416f, 458f)), Themes.default, { chosen += it })
            }
        }
        composeRule.waitForIdle()
    }

    private fun press(key: Key) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }

    @Test fun anAppThatIsNotAFavouriteCanBeAddedHiddenInspectedAndUninstalled() {
        show(isFavourite = false)
        listOf("Add to favorites", "Hide", "App info", "Uninstall").forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText("Move").assertDoesNotExist()
        composeRule.onNodeWithText("Remove from favorites").assertDoesNotExist()
    }

    @Test fun aFavouriteCanBeRemovedAndMoved() {
        show(isFavourite = true)
        listOf("Remove from favorites", "Move", "Hide", "App info", "Uninstall").forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test fun aWebsiteShortcutHasNoPackageActions() {
        val shortcut = HomeTile(io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey.shortcut("s"), "News", shortcut = Shortcut("s", "News", "https://example.org"))
        show(isFavourite = true, tile = shortcut)
        composeRule.onNodeWithText("Remove from favorites").assertExists()
        composeRule.onNodeWithText("Move").assertExists()
        composeRule.onNodeWithText("Hide").assertDoesNotExist()
        composeRule.onNodeWithText("Uninstall").assertDoesNotExist()
    }

    @Test fun focusStartsOnTheFirstItemAndOkChoosesIt() {
        show(isFavourite = true)
        composeRule.onNodeWithText("Remove from favorites").assertIsFocused()
        press(Key.Enter)
        assertEquals(listOf(MenuAction.ToggleFavourite), chosen)
    }

    @Test fun downThenOkChoosesTheSecondItem() {
        show(isFavourite = true)
        press(Key.DirectionDown)
        composeRule.onNodeWithText("Move").assertIsFocused()
        press(Key.Enter)
        assertEquals(listOf(MenuAction.Move), chosen)
    }

    @Test fun focusStaysInsideTheMenu() {
        show(isFavourite = false) // Add, Hide, Info, Uninstall
        press(Key.DirectionUp)
        composeRule.onNodeWithText("Add to favorites").assertIsFocused()
        repeat(6) { press(Key.DirectionDown) }
        composeRule.onNodeWithText("Uninstall").assertIsFocused()
    }

    @Test fun aFreshPressChoosesAnItemWhenOkWasAlreadyReleased() {
        show(isFavourite = true)
        press(Key.Enter)
        assertEquals(listOf(MenuAction.ToggleFavourite), chosen)
    }

    @Test fun menuOpensBelowTheTileAndFlipsAboveNearTheBottom() {
        val density = 2f
        val (_, below) = menuPosition(Rect(96f, 278f, 416f, 458f), density, 960f, 540f, 252f)
        assertEquals(229f + 8f, below, 0.01f)
        val (_, above) = menuPosition(Rect(96f, 1000f, 416f, 1180f), density, 960f, 540f, 252f)
        assertEquals(500f - 8f - 252f, above, 0.01f)
        val (x, _) = menuPosition(Rect(1700f, 278f, 2020f, 458f), density, 960f, 540f, 252f)
        assertEquals("kept inside the screen", 960f - 290f - 16f, x, 0.01f)
    }
}

class HeldKeyGuardTest {
    @org.junit.Test fun repeatsAndReleaseOfTheOpeningPressAreSwallowed() {
        val guard = io.github.marozauartsiom.tvlauncher.ui.components.HeldKeyGuard()
        assertTrue(guard.swallows(isDown = true, repeatCount = 3))
        assertTrue(guard.swallows(isDown = true, repeatCount = 4))
        assertTrue(guard.swallows(isDown = false, repeatCount = 0)) // the release
        assertEquals(false, guard.swallows(isDown = true, repeatCount = 0)) // next press is real
        assertEquals(false, guard.swallows(isDown = false, repeatCount = 0))
    }

    @org.junit.Test fun aFreshPressMeansTheKeyWasAlreadyReleased() {
        val guard = io.github.marozauartsiom.tvlauncher.ui.components.HeldKeyGuard()
        assertEquals(false, guard.swallows(isDown = true, repeatCount = 0))
        assertEquals(false, guard.swallows(isDown = false, repeatCount = 0))
    }
}
