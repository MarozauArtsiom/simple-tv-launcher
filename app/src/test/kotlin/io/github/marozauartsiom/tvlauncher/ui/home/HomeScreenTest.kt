package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
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
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
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

    private val steps = mutableListOf<Pair<String, Int>>()
    private var moveDone = 0
    private var moveCancelled = 0

    private val actions = HomeActions(
        onOpen = { opened += it.label },
        onMenu = { tile, _ -> menus += tile.label },
        onSettings = { settingsClicks++ },
        onMoveStep = { key, delta -> steps += key.value to delta },
        onMoveDone = { moveDone++ },
        onMoveCancel = { moveCancelled++ },
    )

    private fun show(
        content: HomeContent = HomeFixtures.content,
        safeMode: Boolean = false,
        moveKey: ItemKey? = null,
        overlayOpen: Boolean = false,
    ) {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                HomeScreen(
                    content, Themes.default, HomeFixtures.topBar, { HomeFixtures.clock }, FakeArt(), resets, actions,
                    safeMode = safeMode, moveKey = moveKey, overlayOpen = overlayOpen,
                )
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

    @Test fun menuKeyOpensSettingsFromAnywhere() {
        show()
        press(Key.DirectionDown, 2)
        press(Key.Menu)
        assertEquals(1, settingsClicks)
    }

    @Test fun hiddenSectionIsCollapsedUntilOpenedAndClosesOnReset() {
        val settings = HomeFixtures.settings.copy(hidden = HomeFixtures.hiddenApps.map { it.packageName }.toSet())
        show(HomeLists.build(HomeFixtures.apps + HomeFixtures.hiddenApps, settings, locale = java.util.Locale.ENGLISH))
        val hiddenTile = composeRule.onNodeWithTag(hiddenId(HomeTile(HomeFixtures.key("Xiaomi Mi Home"), "Xiaomi Mi Home"))!!)
        press(Key.DirectionDown, 4) // favourites → three grid rows → the header
        composeRule.onNodeWithTag(HiddenHeaderId).assertIsFocused()
        press(Key.DirectionDown) // collapsed: nothing to go to
        composeRule.onNodeWithTag(HiddenHeaderId).assertIsFocused()
        hiddenTile.assertDoesNotExist()
        press(Key.Enter)
        press(Key.DirectionDown)
        hiddenTile.assertExists()
        resets.tryEmit(Unit)
        composeRule.waitForIdle()
        hiddenTile.assertDoesNotExist()
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

    // ---- move mode ----

    @Test fun inMoveModeLeftAndRightAskForASwapAndDoNotMoveFocus() {
        show(moveKey = HomeFixtures.key("YouTube"))
        fav("YouTube").assertIsFocused() // the moving tile takes focus
        press(Key.DirectionRight)
        press(Key.DirectionLeft)
        assertEquals(listOf(HomeFixtures.key("YouTube").value to 1, HomeFixtures.key("YouTube").value to -1), steps)
        fav("YouTube").assertIsFocused()
    }

    @Test fun inMoveModeUpAndDownStayOnTheTile() {
        show(moveKey = HomeFixtures.key("YouTube"))
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        fav("YouTube").assertIsFocused()
    }

    @Test fun okDropsTheMovedTileAndDoesNotOpenTheApp() {
        show(moveKey = HomeFixtures.key("YouTube"))
        press(Key.Enter)
        assertEquals(1, moveDone)
        assertEquals(emptyList<String>(), opened)
    }

    @Test fun backInMoveModeCancels() {
        show(moveKey = HomeFixtures.key("YouTube"))
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        assertEquals(1, moveCancelled)
    }

    @Test fun movingTileShowsThePillInsteadOfItsName() {
        show(moveKey = HomeFixtures.key("YouTube"))
        composeRule.onNodeWithText("◀ ▶ Move · OK to finish").assertIsDisplayed()
    }

    // ---- panels over Home ----

    @Test fun homeCannotTakeFocusWhileAMenuIsOpenAndGetsItBackOnClose() {
        var open by androidx.compose.runtime.mutableStateOf(false)
        val menuFocus = androidx.compose.ui.focus.FocusRequester()
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                HomeScreen(HomeFixtures.content, Themes.default, HomeFixtures.topBar, { HomeFixtures.clock }, FakeArt(), resets, actions, overlayOpen = open)
                if (open) {
                    // stands in for a menu: one focusable element that takes focus when it appears
                    androidx.compose.foundation.layout.Box(
                        androidx.compose.ui.Modifier
                            .size(20.dp)
                            .focusRequester(menuFocus)
                            .focusable()
                            .testTag("menu"),
                    )
                    androidx.compose.runtime.LaunchedEffect(Unit) { androidx.compose.runtime.withFrameNanos { }; menuFocus.requestFocus() }
                }
            }
        }
        composeRule.waitForIdle()
        press(Key.DirectionRight, 2)
        fav("TV Bro").assertIsFocused()

        open = true
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu").assertIsFocused()
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        composeRule.onNodeWithTag("menu").assertIsFocused() // Home did not take the focus

        open = false
        composeRule.waitForIdle()
        fav("TV Bro").assertIsFocused()
    }

    // ---- website shortcuts ----

    @Test fun aShortcutIsATileLikeAnyOtherAndOpensWithOk() {
        val shortcut = io.github.marozauartsiom.tvlauncher.data.prefs.Shortcut("s1", "Новости сайта", "https://news.example.com")
        val key = ItemKey.shortcut("s1")
        val settings = SettingsData(favourites = listOf(key), shortcuts = listOf(shortcut))
        val content = HomeLists.build(HomeFixtures.apps, settings, locale = java.util.Locale.ENGLISH)
        show(content)
        composeRule.onNodeWithTag("fav:${key.value}").assertIsFocused() // the first favourite
        composeRule.onNodeWithTag("fav:${key.value}").assert(hasText("Н")) // the first letter on the tile
        press(Key.Enter)
        assertEquals(listOf("Новости сайта"), opened)
    }
}
