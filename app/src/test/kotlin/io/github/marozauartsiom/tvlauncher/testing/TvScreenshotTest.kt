package io.github.marozauartsiom.tvlauncher.testing

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.test.core.app.ApplicationProvider
import io.github.marozauartsiom.tvlauncher.i18n.LocaleController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import io.github.marozauartsiom.tvlauncher.ui.theme.Themes
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Base class for TV screenshot and behaviour tests. It renders at the TV's UI size
 * (960 x 540 dp at xhdpi, so 1920 x 1080 px) with the `television` UI mode, like the real device.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = TV_QUALIFIERS)
abstract class TvScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Renders [content] once per theme (switching the theme in place, like the live preview does) and
     * records `<snapshots>/<name>/<themeId>.png`.
     */
    protected fun captureThemes(
        name: String,
        themes: List<ThemeSpec> = Themes.all,
        content: @Composable (ThemeSpec) -> Unit,
    ) {
        var current by mutableStateOf(themes.first())
        composeRule.setContent { LauncherTheme(current) { content(current) } }
        for (theme in themes) {
            current = theme
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("src/test/snapshots/$name/${theme.id.key}.png")
        }
    }

    /**
     * Renders [content] once per language (resources resolved through a locale-wrapped context)
     * and records `<snapshots>/<name>/<tag>.png`.
     */
    protected fun captureLocales(
        name: String,
        tags: List<String> = listOf("be", "ru", "en", "pl", "de", "fr"),
        theme: ThemeSpec = Themes.default,
        content: @Composable (String) -> Unit,
    ) {
        var current by mutableStateOf(tags.first())
        val base: Context = ApplicationProvider.getApplicationContext()
        composeRule.setContent {
            val localized = LocaleController.localizedTo(base, current)
            CompositionLocalProvider(LocalContext provides localized) { LauncherTheme(theme) { content(current) } }
        }
        for (tag in tags) {
            current = tag
            composeRule.waitForIdle()
            composeRule.onRoot().captureRoboImage("src/test/snapshots/$name/$tag.png")
        }
    }
}

const val TV_QUALIFIERS = "w960dp-h540dp-land-television-xhdpi"
