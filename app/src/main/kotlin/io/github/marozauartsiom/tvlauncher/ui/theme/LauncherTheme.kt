package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import io.github.marozauartsiom.tvlauncher.R

val LocalLauncherTheme = staticCompositionLocalOf<ThemeSpec> { error("LauncherTheme is not provided") }

object Launcher {
    val theme: ThemeSpec
        @Composable @ReadOnlyComposable get() = LocalLauncherTheme.current
    val colors: LauncherColors
        @Composable @ReadOnlyComposable get() = LocalLauncherTheme.current.colors
}

/** Nunito is bundled as one variable font; each weight is a variation of it. */
private val Nunito = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(
            R.font.nunito,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

fun ThemeFont.family(): FontFamily = when (this) {
    ThemeFont.Roboto -> FontFamily.Default
    ThemeFont.Nunito -> Nunito
}

/** tv-material's type scale with the theme's font swapped in. */
fun launcherTypography(family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        displayLarge = base.displayLarge.f(), displayMedium = base.displayMedium.f(), displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(), headlineMedium = base.headlineMedium.f(), headlineSmall = base.headlineSmall.f(),
        titleLarge = base.titleLarge.f(), titleMedium = base.titleMedium.f(), titleSmall = base.titleSmall.f(),
        bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f(),
    )
}

@Composable
fun LauncherTheme(spec: ThemeSpec, content: @Composable () -> Unit) {
    val c = spec.colors
    val scheme = if (spec.family == ThemeFamily.Light) {
        lightColorScheme(
            surface = c.panelBg, onSurface = c.onSurface, surfaceVariant = c.itemBg,
            onSurfaceVariant = c.onSurfaceVariant, background = c.panelBg, onBackground = c.onSurface, border = c.focus,
        )
    } else {
        darkColorScheme(
            surface = c.panelBg, onSurface = c.onSurface, surfaceVariant = c.itemBg,
            onSurfaceVariant = c.onSurfaceVariant, background = c.panelBg, onBackground = c.onSurface, border = c.focus,
        )
    }
    CompositionLocalProvider(LocalLauncherTheme provides spec) {
        MaterialTheme(colorScheme = scheme, typography = launcherTypography(spec.font.family()), content = content)
    }
}
