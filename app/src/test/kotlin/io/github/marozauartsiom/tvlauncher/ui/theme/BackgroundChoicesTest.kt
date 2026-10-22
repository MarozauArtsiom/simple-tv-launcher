package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.marozauartsiom.tvlauncher.data.prefs.BackgroundChoice
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class BackgroundChoicesTest {
    private val midnight = Themes.byKey("midnight")
    private val paper = Themes.byKey("paper")
    private val valley = Themes.byKey("valley")
    private val space = Themes.byKey("space")
    private val none: (String) -> File? = { null }

    @Test fun fromThemeIsTheThemesOwnArt() {
        assertEquals(midnight.background, BackgroundChoices.resolve(midnight, BackgroundChoice.FromTheme, none))
    }

    @Test fun aReadableColourIsUsedAsIs() {
        val navy = 0xFF1B2A44.toInt()
        assertEquals(SolidBackground(Color(navy)), BackgroundChoices.resolve(midnight, BackgroundChoice.Solid(navy), none))
    }

    @Test fun aColourThatHidesTheTextFallsBackToTheTheme() {
        val navy = 0xFF1B2A44.toInt()
        assertEquals(paper.background, BackgroundChoices.resolve(paper, BackgroundChoice.Solid(navy), none)) // dark text on dark
        val cream = 0xFFFBF9F4.toInt()
        assertEquals(midnight.background, BackgroundChoices.resolve(midnight, BackgroundChoice.Solid(cream), none)) // light text on light
    }

    @Test fun swatchesMatchTheTextTone() {
        assertEquals(BackgroundChoices.darkSwatches, BackgroundChoices.swatchesFor(midnight))
        assertEquals(BackgroundChoices.darkSwatches, BackgroundChoices.swatchesFor(space))
        assertEquals(BackgroundChoices.lightSwatches, BackgroundChoices.swatchesFor(paper))
        assertEquals(BackgroundChoices.lightSwatches, BackgroundChoices.swatchesFor(valley))
    }

    @Test fun everyOfferedSwatchIsReadableInItsTheme() {
        Themes.all.forEach { theme ->
            BackgroundChoices.swatchesFor(theme).forEach {
                assertTrue("${theme.id} ${Integer.toHexString(it)}", BackgroundChoices.readable(theme, Color(it)))
            }
        }
    }

    @Test fun aStoredPictureGetsAScrimAndAMissingOneFallsBack() {
        val file = File("/data/bg_1.webp")
        val spec = BackgroundChoices.resolve(midnight, BackgroundChoice.Image("bg_1.webp")) { file } as FileBackground
        assertEquals(file.absolutePath, spec.path)
        assertTrue(spec.scrim.isNotEmpty())
        assertEquals(midnight.background, BackgroundChoices.resolve(midnight, BackgroundChoice.Image("bg_1.webp"), none))
    }

    @Test fun theScrimIsDarkUnderLightTextAndLightUnderDarkText() {
        val dark = BackgroundChoices.scrimFor(midnight).last().second
        val light = BackgroundChoices.scrimFor(paper).last().second
        assertTrue(dark.red < 0.2f)
        assertTrue(light.red > 0.8f)
        assertFalse(BackgroundChoices.hasLightText(paper))
    }

    @Test fun previewsResolveWithoutTouchingTheSavedChoice() {
        assertEquals(null, BackgroundChoices.preview(midnight, null))
        assertEquals(midnight.background, BackgroundChoices.preview(midnight, BackgroundPick.FromTheme))
        assertEquals(SolidBackground(Color(0xFF123456.toInt())), BackgroundChoices.preview(midnight, BackgroundPick.Solid(0xFF123456.toInt())))
        assertTrue(BackgroundChoices.preview(midnight, BackgroundPick.Picture("/usb/a.jpg")) is FileBackground)
    }
}
