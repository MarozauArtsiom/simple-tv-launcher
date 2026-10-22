package io.github.marozauartsiom.tvlauncher.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.background.PickedImage
import io.github.marozauartsiom.tvlauncher.data.prefs.BackgroundChoice
import io.github.marozauartsiom.tvlauncher.testing.PanelBackdrop
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.theme.BackgroundChoices
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Test

/** The background picker over Home, compared with `design/mockups/screens/settings-background-*.png`. */
@OptIn(ExperimentalTestApi::class)
class BackgroundPickerScreenshotTest : TvScreenshotTest() {
    private val backdrop by lazy { PanelBackdrop() }

    /** Six "USB pictures": the theme backgrounds, which are real 1920×1080 pictures. */
    private val pictures = listOf(R.drawable.bg_aurora, R.drawable.bg_sky, R.drawable.bg_sage, R.drawable.bg_valley, R.drawable.bg_ocean, R.drawable.bg_space)
    private val images = pictures.mapIndexed { i, _ -> PickedImage("/usb/$i.jpg", "$i.jpg") }

    private suspend fun thumb(image: PickedImage): ImageBitmap? {
        val res = pictures[image.name.substringBefore('.').toInt()]
        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 6 }
        return android.graphics.BitmapFactory.decodeResource(ApplicationProvider.getApplicationContext<android.content.Context>().resources, res, options)?.asImageBitmap()
    }

    @Composable
    private fun Page(theme: io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec, state: PicturesState, saved: BackgroundChoice = BackgroundChoice.FromTheme) {
        backdrop.Over(theme) {
            BackgroundPickerPage(
                theme = theme, saved = saved, pictures = state,
                adbCommand = "adb shell appops set --uid io.github.marozauartsiom.tvlauncher MANAGE_EXTERNAL_STORAGE allow",
                memory = FocusMemory(), thumbnail = { thumb(it) }, onPreview = {}, onChoose = {}, onCheckAccess = {},
            )
        }
    }

    @Test fun withPictures() {
        val themes = listOf("midnight", "sky", "valley").map(Themes::byKey)
        captureThemes("background-picker", themes, setup = {
            // focus the second picture, as in the mockup: Down x2 to the first picture, Right
            repeat(3) { composeRule.onRoot().performKeyInput { pressKey(Key.DirectionDown) }; composeRule.waitForIdle() }
            composeRule.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        }) { theme -> Page(theme, PicturesState.Loaded(images), BackgroundChoice.Solid(BackgroundChoices.swatchesFor(theme)[1])) }
    }

    @Test fun withoutAccess() {
        captureThemes("background-picker-noaccess", listOf(Themes.byKey("midnight"))) { theme -> Page(theme, PicturesState.NoAccess) }
    }
}
