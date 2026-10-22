package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.marozauartsiom.tvlauncher.data.prefs.BackgroundChoice
import java.io.File

/** Resolves the saved background choice against the theme that is showing. */
object BackgroundChoices {
    /** Dark colours for themes with light text. */
    val darkSwatches = listOf(0xFF0B0D12, 0xFF1B2A44, 0xFF3B2A6B, 0xFF24523A, 0xFF7A2E2E, 0xFF5C4A1F).map { it.toInt() }

    /** Light colours for themes with dark text. */
    val lightSwatches = listOf(0xFFFBF9F4, 0xFFE3EEFA, 0xFFEEE6FA, 0xFFE2F1E7, 0xFFFAE5E5, 0xFFF5ECCB).map { it.toInt() }

    /** The six colours offered with [theme]: the set that keeps its text readable. */
    fun swatchesFor(theme: ThemeSpec): List<Int> = if (hasLightText(theme)) darkSwatches else lightSwatches

    fun hasLightText(theme: ThemeSpec): Boolean = theme.colors.onSurface.luminance() > 0.5f

    /** A colour can be used if the theme's text stays readable on it (contrast ≥ 3:1). */
    fun readable(theme: ThemeSpec, color: Color): Boolean {
        val a = theme.colors.onSurface.luminance() + 0.05f
        val b = color.luminance() + 0.05f
        return maxOf(a, b) / minOf(a, b) >= 3f
    }

    /**
     * What to draw for [choice]. A colour that would make the theme's text unreadable (a dark colour
     * chosen under one theme, then a light theme switched on) and a picture that is gone fall back
     * to the theme's own background.
     */
    fun resolve(theme: ThemeSpec, choice: BackgroundChoice, fileOf: (String) -> File?): BackgroundSpec = when (choice) {
        BackgroundChoice.FromTheme -> theme.background
        is BackgroundChoice.Solid -> Color(choice.argb).takeIf { readable(theme, it) }?.let(::SolidBackground) ?: theme.background
        is BackgroundChoice.Image -> fileOf(choice.fileName)?.let { FileBackground(it.absolutePath, scrimFor(theme)) } ?: theme.background
    }

    /** A picture can be any brightness, so a scrim of the opposite tone keeps the theme's text readable. */
    fun scrimFor(theme: ThemeSpec): List<Pair<Float, Color>> =
        if (hasLightText(theme)) {
            listOf(0f to Color(0x260B0D12), 0.38f to Color(0x990B0D12), 0.72f to Color(0xE60B0D12))
        } else {
            listOf(0f to Color(0x73FBF9F4), 0.4f to Color(0xA6FBF9F4), 0.75f to Color(0xCCFBF9F4))
        }

    /** What the picker shows while a swatch or a USB picture is only previewed. */
    fun preview(theme: ThemeSpec, pick: BackgroundPick?): BackgroundSpec? = when (pick) {
        null -> null
        BackgroundPick.FromTheme -> theme.background
        is BackgroundPick.Solid -> SolidBackground(Color(pick.argb))
        is BackgroundPick.Picture -> FileBackground(pick.path, scrimFor(theme))
    }
}

/** Something the owner is looking at in the background picker. */
sealed interface BackgroundPick {
    data object FromTheme : BackgroundPick
    data class Solid(val argb: Int) : BackgroundPick
    data class Picture(val path: String) : BackgroundPick
}
