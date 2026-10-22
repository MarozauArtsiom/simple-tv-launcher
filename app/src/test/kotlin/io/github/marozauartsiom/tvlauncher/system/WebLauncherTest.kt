package io.github.marozauartsiom.tvlauncher.system

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WebLauncherTest {
    private val started = mutableListOf<Intent>()
    private val launcher = WebLauncher { started += it }

    @Test fun opensTheAddressInTheBrowserWithView() {
        assertEquals(WebLauncher.Result.Started, launcher.open("https://news.example.com"))
        val intent = started.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://news.example.com", intent.dataString)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test fun neverOpensAnythingButHttp() {
        listOf("javascript:alert(1)", "file:///etc/hosts", "intent://x#Intent;end", "content://a").forEach {
            assertEquals(WebLauncher.Result.Rejected, launcher.open(it))
        }
        assertTrue(started.isEmpty())
    }

    @Test fun noBrowserIsReportedNotThrown() {
        assertEquals(WebLauncher.Result.NoBrowser, WebLauncher { throw ActivityNotFoundException() }.open("https://a.example.com"))
    }
}
