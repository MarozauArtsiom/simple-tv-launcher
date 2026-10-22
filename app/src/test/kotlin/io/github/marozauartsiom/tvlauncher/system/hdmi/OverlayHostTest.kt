package io.github.marozauartsiom.tvlauncher.system.hdmi

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class OverlayHostTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun showAddsOneOverlayWindowAndHideRemovesIt() {
        val host = OverlayHost(context)
        host.show { Box(Modifier.size(10.dp)) }
        assertTrue(host.isShowing)
        host.hide()
        assertFalse(host.isShowing)
    }

    @Test fun showingAgainReplacesTheFirstWindow() {
        val host = OverlayHost(context)
        host.show { Box(Modifier.size(10.dp)) }
        host.show { Box(Modifier.size(20.dp)) }
        assertTrue(host.isShowing)
        host.hide()
        assertFalse(host.isShowing)
    }

    @Test fun theContentCanDismissItself() {
        val host = OverlayHost(context)
        var dismiss: (() -> Unit)? = null
        host.show { d -> dismiss = d; Box(Modifier.size(10.dp)) }
        shadowOf(android.os.Looper.getMainLooper()).idle() // composition runs when the window is attached
        dismiss!!.invoke()
        assertFalse(host.isShowing)
        dismiss!!.invoke() // a second call is harmless
    }

    @Test fun hideWithoutAWindowDoesNothing() {
        OverlayHost(context).hide()
    }
}
