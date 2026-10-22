package io.github.marozauartsiom.tvlauncher.testing

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Base class for TV screenshot and behaviour tests. It renders at the TV's UI size
 * (960 x 540 dp at xhdpi, so 1920 x 1080 px) with the `television` UI mode, like the real device.
 * Theme and locale helpers are added next to this class as those features land.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = TV_QUALIFIERS)
abstract class TvScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()
}

const val TV_QUALIFIERS = "w960dp-h540dp-land-television-xhdpi"
