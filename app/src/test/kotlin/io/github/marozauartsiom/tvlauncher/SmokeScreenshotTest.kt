package io.github.marozauartsiom.tvlauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.onRoot
import androidx.tv.material3.Text
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.marozauartsiom.tvlauncher.testing.TvScreenshotTest
import org.junit.Test

class SmokeScreenshotTest : TvScreenshotTest() {
    @Test
    fun rendersAtTvSize() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF0B0D12)), contentAlignment = Alignment.Center) {
                Text("Simple TV", color = Color(0xFFE8EAF0))
            }
        }
        composeRule.onRoot().captureRoboImage()
    }
}
