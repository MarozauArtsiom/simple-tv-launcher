package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fails when `ui/theme/Themes.kt` drifts from the approved tokens in `design/mockups/themes.css`. */
class ThemeCssSyncTest {
    private val css = File("../design/mockups/themes.css").readText()

    private val tokens: Map<ThemeId, Map<String, String>> = run {
        val blockRegex = Regex("""((?::root, )?(?:\[data-theme="[\w-]+"](?:,\s*)?)+)\s*\{(.*?)\n\}""", RegexOption.DOT_MATCHES_ALL)
        val varRegex = Regex("""--([\w-]+):\s*([^;]+);""")
        var base = mapOf<String, String>()
        var family = mapOf<String, String>()
        var familyNames = setOf<String>()
        val per = mutableMapOf<String, Map<String, String>>()
        for (m in blockRegex.findAll(css)) {
            val names = Regex("""data-theme="([\w-]+)"""").findAll(m.groupValues[1]).map { it.groupValues[1] }.toList()
            val vars = varRegex.findAll(m.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2].trim() }
            when {
                m.groupValues[1].startsWith(":root") -> base = vars
                names.size > 1 -> { family = vars; familyNames = names.toSet() }
                else -> per[names.single()] = vars
            }
        }
        ThemeId.entries.associateWith { id ->
            val merged = base.toMutableMap()
            if (id.key in familyNames) merged.putAll(family)
            merged.putAll(per[id.key].orEmpty())
            merged.keys.toList().forEach { k ->
                Regex("""var\(--([\w-]+)\)""").matchEntire(merged.getValue(k))?.let { merged[k] = merged.getValue(it.groupValues[1]) }
            }
            merged
        }
    }

    private fun parseColor(v: String): Color = when {
        v == "transparent" -> Color.Transparent
        v == "currentColor" -> Color.Unspecified
        v.startsWith("#") -> Color(0xFF000000.toInt() or v.drop(1).toInt(16))
        else -> {
            val n = Regex("""rgba\((\d+), (\d+), (\d+), (\.?[\d.]+)\)""").matchEntire(v)!!.groupValues
            Color(n[1].toInt(), n[2].toInt(), n[3].toInt(), (n[4].toFloat() * 255).toInt().coerceIn(0, 255))
        }
    }

    private fun assertColor(theme: ThemeId, token: String, actual: Color) {
        val raw = tokens.getValue(theme).getValue(token)
        // --card-outline is a box-shadow ("0 0 0 3px #FFFFFF"): its colour is the last word
        val expected = parseColor(if (token == "card-outline") raw.substringAfterLast(' ') else raw)
        if (expected == Color.Unspecified || actual == Color.Unspecified) {
            assertEquals("$theme $token", expected, actual)
            return
        }
        val a = actual.toArgb(); val e = expected.toArgb()
        for (shift in listOf(24, 16, 8, 0)) {
            val d = abs(((a ushr shift) and 0xFF) - ((e ushr shift) and 0xFF))
            assertTrue("$theme --$token: expected ${"%08X".format(e)} but was ${"%08X".format(a)}", d <= 1)
        }
    }

    private fun px(theme: ThemeId, token: String) = tokens.getValue(theme).getValue(token).removeSuffix("px").toFloat()

    @Test
    fun allFourteenThemesExist() {
        assertEquals(14, ThemeId.entries.size)
        assertEquals(ThemeId.entries, Themes.all.map { it.id })
    }

    @Test
    fun colorsMatchTheCss() {
        for (t in Themes.all) {
            val c = t.colors
            val pairs = mapOf(
                "on-surface" to c.onSurface, "on-surface-variant" to c.onSurfaceVariant,
                "chip-bg" to c.chipBg, "chip-fg" to c.chipFg, "chip-meta" to c.chipMeta,
                "title-bg" to c.titleBg, "title-fg" to c.titleFg, "title-meta" to c.titleMeta,
                "label-bg" to c.labelBg, "label-fg" to c.labelFg, "card-outline" to c.cardOutline,
                "focus" to c.focus, "signal" to c.signal, "wx-sun" to c.weatherSun, "wx-cloud" to c.weatherCloud,
                "panel-bg" to c.panelBg, "panel-fg" to c.panelFg, "panel-muted" to c.panelMuted,
                "item-bg" to c.itemBg, "item-focus-fill" to c.itemFocusFill, "item-focus-ring" to c.itemFocusRing,
                "btn-fg" to c.buttonFg,
            )
            pairs.forEach { (token, actual) -> assertColor(t.id, token, actual) }
        }
    }

    @Test
    fun shapesAndFocusMatchTheCss() {
        for (t in Themes.all) {
            assertEquals("${t.id} card radius", px(t.id, "card-radius"), t.cardRadius.value, 0.01f)
            assertEquals("${t.id} focus ring", px(t.id, "focus-border"), t.focus.ring.value, 0.01f)
            assertEquals("${t.id} focus scale", tokens.getValue(t.id).getValue("focus-scale").toFloat(), t.focus.scale, 0.001f)
            assertEquals("${t.id} rotation", tokens.getValue(t.id).getValue("focus-rotate").removeSuffix("deg").toFloat(), t.focus.rotation, 0.001f)
            assertEquals("${t.id} panel radius", px(t.id, "panel-radius"), t.panelRadius.value, 0.01f)
            assertEquals("${t.id} uppercase", tokens.getValue(t.id).getValue("title-case") == "uppercase", t.titleUppercase)
            assertEquals("${t.id} font", "Nunito" in tokens.getValue(t.id).getValue("font"), t.font == ThemeFont.Nunito)
            val family = when (t.id.key) {
                "valley", "ocean", "space" -> ThemeFamily.Cartoon
                "gloom", "haunted", "monsters" -> ThemeFamily.DarkCartoon
                "halloween-night", "halloween-day" -> ThemeFamily.Halloween
                "paper", "sky", "sage" -> ThemeFamily.Light
                else -> ThemeFamily.Dark
            }
            assertEquals("${t.id} family", family, t.family)
        }
    }

    @Test
    fun everyBackgroundAssetIsReferencedByExactlyOneTheme() {
        val images = Themes.all.map { it.background }.filterIsInstance<ImageBackground>()
        assertEquals(images.size, images.map { it.resId }.toSet().size)
        assertEquals(12, images.size) // Graphite and Paper draw gradients in code
    }

    @Test
    fun lookupFallsBackToTheDefault() {
        assertEquals(Themes.Midnight, Themes.byKey(null))
        assertEquals(Themes.Midnight, Themes.byKey("nonsense"))
        assertEquals(Themes.Space, Themes.byKey("space"))
    }
}
