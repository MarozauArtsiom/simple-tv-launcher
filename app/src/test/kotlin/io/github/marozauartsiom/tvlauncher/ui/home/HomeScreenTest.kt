package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.SemanticsNodeInteraction
import io.github.marozauartsiom.tvlauncher.data.prefs.SettingsData
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/** Behaviour of Home with the D-pad: initial focus, moving between the bar, rows and grid, focus memory, resets. */
@OptIn(ExperimentalTestApi::class)
class HomeScreenTest : TvScreenshotTest() {
    private val resets = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val opened = mutableListOf<String>()
    private val menus = mutableListOf<String>()
    private var settingsClicks = 0

    private val actions = HomeActions(
        onOpen = { opened += it.label },
        onMenu = { menus += it.label },
        onWeather = {}, onHdmi = {}, onIphone = {},
        onSettings = { settingsClicks++ },
    )

    private fun show(content: HomeContent = HomeFixtures.content, safeMode: Boolean = false) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                HomeScreen(content, Themes.default, HomeFixtures.topBar, { HomeFixtures.clock }, FakeArt(), resets, actions, safeMode = safeMode)
            }
        }
        composeRule.waitForIdle()
    }

    /** A favourite (every favourite is also in All apps, so the two rows are told apart by tag). */
    private fun fav(name: String): SemanticsNodeInteraction = composeRule.onNodeWithTag("fav:" + HomeFixtures.key(name).value)
    private fun grid(name: String): SemanticsNodeInteraction = composeRule.onNodeWithTag("all:" + HomeFixtures.key(name).value)
    private fun press(key: Key, times: Int = 1) {
        repeat(times) { composeRule.onRoot().performKeyInput { pressKey(key) }; composeRule.waitForIdle() }
    }

    @Test fun firstFavouriteHasFocusWhenHomeAppears() {
        show()
        fav("YouTube").assertIsFocused()
    }

    @Test fun rightAndLeftMoveAlongTheFavourites() {
        show()
        press(Key.DirectionRight)
        fav("Кинопоиск").assertIsFocused()
        press(Key.DirectionLeft)
        fav("YouTube").assertIsFocused()
    }

    @Test fun downFromFavouritesEntersTheGridAndUpGoesBack() {
        show()
        press(Key.DirectionDown)
        grid("Disney+").assertIsFocused() // first grid tile, below the first favourite
        press(Key.DirectionUp)
        fav("YouTube").assertIsFocused()
    }

    @Test fun upFromFavouritesGoesToTheTopBar() {
        show()
        press(Key.DirectionUp)
        // The nearest chip above the first favourite is the weather chip.
        composeRule.onNode(hasContentDescription("Weather: +7°", substring = true)).assertIsFocused()
    }

    @Test fun favouritesRowRemembersItsLastTile() {
        show()
        press(Key.DirectionRight, 2) // TV Bro
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        fav("TV Bro").assertIsFocused()
    }

    @Test fun gridRowRemembersItsLastTile() {
        show()
        press(Key.DirectionDown)
        press(Key.DirectionRight, 2) // third tile of the first grid row
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        val third = HomeFixtures.content.all[2].label
        grid(third).assertIsFocused()
    }

    @Test fun resetGoesBackToTheFirstFavouriteFromAnywhere() {
        show()
        press(Key.DirectionDown, 3)
        fav("YouTube").assertIsNotFocused()
        resets.tryEmit(Unit) // Home key, screen on, Back
        composeRule.waitForIdle()
        fav("YouTube").assertIsFocused()
    }

    @Test fun okOpensTheFocusedApp() {
        show()
        press(Key.DirectionRight)
        press(Key.Enter)
        assertEquals(listOf("Кинопоиск"), opened)
    }

    @Test fun longPressOnOkOpensTheMenuInsteadOfTheApp() {
        show()
        composeRule.onRoot().performKeyInput {
            keyDown(Key.Enter)
            advanceEventTime(800)
            keyUp(Key.Enter)
        }
        composeRule.waitForIdle()
        assertEquals(listOf("YouTube"), menus)
        assertEquals(emptyList<String>(), opened)
    }

    @Test fun gearOpensSettings() {
        show()
        press(Key.DirectionUp)
        press(Key.DirectionRight, 4)
        composeRule.onNodeWithContentDescription("Settings").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, settingsClicks)
    }

    @Test fun withoutFavouritesTheHintShowsAndFocusStartsInTheGrid() {
        val content = HomeLists.build(HomeFixtures.apps, SettingsData(), locale = java.util.Locale.ENGLISH)
        show(content)
        composeRule.onNodeWithText("Nothing here yet").assertIsDisplayed()
        grid(content.all.first().label).assertIsFocused()
    }

    @Test fun safeModeShowsOnlyTheGridAndSettings() {
        show(safeMode = true)
        composeRule.onNodeWithText("Favorites").assertDoesNotExist()
        composeRule.onNode(hasContentDescription("Show the iPhone screen")).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        grid(HomeFixtures.content.all.first().label).assertIsFocused()
    }
}
