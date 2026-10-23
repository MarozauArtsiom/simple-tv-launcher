package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp

enum class ThemeFamily { Dark, Light, Cartoon, DarkCartoon, Halloween }

enum class ThemeId(val key: String) {
    Midnight("midnight"), Graphite("graphite"), Aurora("aurora"),
    Paper("paper"), Sky("sky"), Sage("sage"),
    Valley("valley"), Ocean("ocean"), Space("space"),
    Gloom("gloom"), Haunted("haunted"), Monsters("monsters"),
    HalloweenNight("halloween-night"), HalloweenDay("halloween-day"),
}

enum class ThemeFont { Roboto, Nunito }

/** A CSS-style box shadow. `blur == 0.dp` is a hard shadow (cartoon themes). */
@Immutable
data class ShadowSpec(val color: Color, val offsetY: Dp, val blur: Dp, val spread: Dp)

@Immutable
data class FocusStyle(val ring: Dp, val scale: Float, val rotation: Float, val glow: ShadowSpec?)

/**
 * Colours of one theme. Names match the CSS tokens in `design/mockups/themes.css`.
 * [Color.Unspecified] for the weather colours means "use the chip text colour".
 */
@Immutable
data class LauncherColors(
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val chipBg: Color,
    val chipFg: Color,
    val chipMeta: Color,
    val chipRing: Color,
    val titleBg: Color,
    val titleFg: Color,
    val titleMeta: Color,
    val labelBg: Color,
    val labelFg: Color,
    /** Colour of the "sticker" outline drawn around every card ([ThemeSpec.cardOutline] wide). */
    val cardOutline: Color,
    val focus: Color,
    val signal: Color,
    val weatherSun: Color,
    val weatherCloud: Color,
    val panelBg: Color,
    val panelFg: Color,
    val panelMuted: Color,
    val itemBg: Color,
    val itemFocusFill: Color,
    val itemFocusRing: Color,
    val buttonFg: Color,
)

sealed interface BackgroundSpec

/** The owner's colour (Settings → Background), no art and no scrim. */
@Immutable
data class SolidBackground(val color: Color) : BackgroundSpec

/** The owner's picture, already 960x540 in app storage (or a USB file while previewing), under a scrim chosen for the theme. */
@Immutable
data class FileBackground(val path: String, val scrim: List<Pair<Float, Color>>) : BackgroundSpec

/** A bitmap background (stored as 1920x1080 WebP, decoded at 960x540) under an optional vertical scrim. */
@Immutable
data class ImageBackground(
    @DrawableRes val resId: Int,
    val scrim: List<Pair<Float, Color>>,
) : BackgroundSpec

/** An elliptical radial gradient drawn in code. Centre is a fraction of the screen; radii are in dp on the 960x540 canvas. */
@Immutable
data class RadialBackground(
    val inner: Color,
    val outer: Color,
    val centerX: Float,
    val centerY: Float,
    val radiusX: Dp,
    val radiusY: Dp,
    val stop: Float,
) : BackgroundSpec

@Immutable
data class ThemeSpec(
    val id: ThemeId,
    val family: ThemeFamily,
    @StringRes val nameRes: Int,
    @DrawableRes val thumbnailRes: Int,
    val background: BackgroundSpec,
    val font: ThemeFont,
    val colors: LauncherColors,
    val cardRadius: Dp,
    /** Width of the "sticker" outline around every card (cartoon themes, colour [LauncherColors.cardOutline]), 0 otherwise. */
    val cardOutline: Dp,
    val cardShadow: ShadowSpec?,
    val chipRingWidth: Dp,
    val chipShadow: ShadowSpec?,
    val focus: FocusStyle,
    val panelRadius: Dp,
    val clockWeight: FontWeight,
    val titleWeight: FontWeight,
    val titleUppercase: Boolean,
    val titleTrackingEm: Float,
    val titlePadding: PaddingValues,
    val labelPaddingHorizontal: Dp,
) {
    val hasTitlePill: Boolean get() = colors.titleBg != Color.Transparent
    val hasLabelPill: Boolean get() = colors.labelBg != Color.Transparent
}
