package io.github.marozauartsiom.tvlauncher.ui.home

import android.content.pm.PackageManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.Dp
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.Shadows.shadowOf

/**
 * How Home scrolls when focus moves, on a device that reports itself as a TV. Compose picks a
 * different default scroll rule there (a pivot), which Robolectric doesn't get unless the leanback
 * feature is switched on, so these tests turn it on.
 */
@OptIn(ExperimentalTestApi::class)
class HomeScrollTest : TvScreenshotTest() {
    private val resets = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Before fun beATv() {
        shadowOf(composeRule.activity.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
    }

    private fun show() {
        composeRule.setContent {
            LauncherTheme(Themes.default) {
                HomeScreen(HomeFixtures.content, Themes.default, HomeFixtures.topBar, { HomeFixtures.clock }, FakeArt(), resets, HomeActions())
            }
        }
        composeRule.waitForIdle()
    }

    private fun grid(index: Int): SemanticsNodeInteraction = composeRule.onNodeWithTag(gridId(HomeFixtures.content.all[index])!!)
    private fun top(node: SemanticsNodeInteraction): Dp = node.getBoundsInRoot().top
    private fun press(key: Key) = composeRule.onRoot().performKeyInput { pressKey(key) }

    /** Tops of a tile on every frame while focus moves and the focus animation runs. */
    private fun topsWhile(key: Key, watched: SemanticsNodeInteraction): List<Dp> {
        composeRule.mainClock.autoAdvance = false
        press(key)
        val tops = (0 until 30).map { composeRule.mainClock.advanceTimeByFrame(); top(watched) }
        composeRule.mainClock.autoAdvance = true
        return tops
    }

    @Test fun movingAlongAGridRowDoesNotScroll() {
        show()
        press(Key.DirectionDown); press(Key.DirectionDown) // into the second grid row
        composeRule.waitForIdle()
        val neighbour = grid(GRID_COLUMNS + 4)
        val before = top(neighbour)
        topsWhile(Key.DirectionRight, neighbour).forEach { assertEquals(before.value, it.value, 0.5f) }
    }

    @Test fun movingIntoAVisibleRowDoesNotScroll() {
        show()
        press(Key.DirectionDown) // first grid row, fully on screen
        composeRule.waitForIdle()
        val other = grid(GRID_COLUMNS + 3) // a tile that neither gains nor loses focus, so it doesn't scale itself
        val before = top(other)
        topsWhile(Key.DirectionDown, other).forEach { assertEquals(before.value, it.value, 0.5f) }
    }

    @Test fun aRowBelowTheScreenIsBroughtFullyIntoView() {
        show()
        repeat(3) { press(Key.DirectionDown); composeRule.waitForIdle() } // third grid row, below the screen at first
        val bounds = grid(2 * GRID_COLUMNS).getBoundsInRoot()
        val screen = composeRule.onRoot().getBoundsInRoot()
        assert(bounds.bottom <= screen.bottom) { "row still cut off: ${bounds.bottom} > ${screen.bottom}" }
    }

    @Test fun goingBackUpShowsTheFavouritesTitleAgain() {
        show()
        repeat(3) { press(Key.DirectionDown); composeRule.waitForIdle() }
        repeat(3) { press(Key.DirectionUp); composeRule.waitForIdle() }
        composeRule.onNodeWithText("Favorites").assertIsDisplayed()
    }

    private companion object {
        const val GRID_COLUMNS = 5
    }
}
