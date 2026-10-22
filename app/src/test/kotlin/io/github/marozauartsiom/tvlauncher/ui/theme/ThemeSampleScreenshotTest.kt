package io.github.marozauartsiom.tvlauncher.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.test.core.app.ApplicationProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

/** A rough Home sample (background, clock, title, five cards) in every theme: checks tokens, fonts and backgrounds. */
class ThemeSampleScreenshotTest : TvScreenshotTest() {
    @Test
    fun sampleInAllThemes() {
        val cache = BackgroundCache(ApplicationProvider.getApplicationContext(), Dispatchers.Unconfined)
        runBlocking { Themes.all.forEach { cache.load(it.background) } } // warm: no async work during capture
        captureThemes("theme-sample") { theme -> ThemeSample(theme, cache) }
    }
}

@Composable
private fun ThemeSample(theme: ThemeSpec, cache: BackgroundCache) {
    val c = theme.colors
    Box(Modifier.fillMaxSize()) {
        ThemedBackground(theme, cache)
        Column(Modifier.padding(horizontal = 48.dp, vertical = 27.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("20:41", style = MaterialTheme.typography.displaySmall, color = c.onSurface, fontWeight = theme.clockWeight)
            val titleShape = RoundedCornerShape(50)
            Text(
                text = if (theme.titleUppercase) "ИЗБРАННОЕ" else "Избранное",
                style = MaterialTheme.typography.titleMedium,
                color = c.titleFg,
                fontWeight = theme.titleWeight,
                modifier = Modifier.background(c.titleBg, titleShape).padding(theme.titlePadding),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf(0xFF7A2E2E, 0xFF5A3A1A, 0xFF2C4A6B, 0xFF24523A, 0xFF5C4A1F).forEachIndexed { i, rgb ->
                    val shape = RoundedCornerShape(theme.cardRadius)
                    val focused = i == 0
                    Box(
                        Modifier
                            .size(160.dp, 90.dp)
                            .graphicsLayer { if (focused) { scaleX = theme.focus.scale; scaleY = theme.focus.scale; rotationZ = theme.focus.rotation } }
                            .themeShadow(if (focused) theme.focus.glow ?: theme.cardShadow else theme.cardShadow, theme.cardRadius)
                            .clip(shape)
                            .background(Color(rgb))
                            .let { if (theme.cardOutline.value > 0f) it.border(theme.cardOutline, Color.White, shape) else it }
                            .let { if (focused) it.border(theme.focus.ring, c.focus, shape) else it },
                        contentAlignment = Alignment.Center,
                    ) { Text("App ${i + 1}", color = Color(0xFFF4F5F8), fontWeight = if (theme.font == ThemeFont.Nunito) FontWeight.ExtraBold else FontWeight.Medium) }
                }
            }
        }
    }
}
