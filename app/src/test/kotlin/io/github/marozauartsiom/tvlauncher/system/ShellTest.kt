package io.github.marozauartsiom.tvlauncher.system

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.marozauartsiom.tvlauncher.LauncherActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp-land-television-xhdpi")
class ShellTest {
    private var now = 1_000_000L
    private fun guard() = CrashGuard(ApplicationProvider.getApplicationContext()) { now }

    @Test fun twoCrashesWithinTenMinutesTurnOnSafeMode() {
        val g = guard()
        assertFalse(g.safeMode())
        g.recordCrash(); assertFalse(g.safeMode())
        now += 60_000; g.recordCrash(); assertTrue(g.safeMode())
        g.markStable(); assertFalse(g.safeMode())
    }

    @Test fun oldCrashesAreForgotten() {
        val g = guard()
        g.recordCrash(); now += CrashGuard.WINDOW_MS + 1; g.recordCrash()
        assertFalse(g.safeMode()) // the second crash starts a new window
    }

    @Test fun screenOnWhileNobodyListensIsRemembered() {
        val w = ScreenOnWatcher(ApplicationProvider.getApplicationContext())
        w.onScreenOn()
        var calls = 0
        assertTrue(w.attach { calls++ })     // pending event reported on attach
        assertFalse(w.attach { calls++ })    // consumed
        w.onScreenOn(); assertEquals(1, calls) // live event reaches the listener
        w.detach(); w.onScreenOn(); assertEquals(1, calls)
    }

    @Test fun backNeverFinishesTheLauncher() {
        val controller = Robolectric.buildActivity(LauncherActivity::class.java).setup()
        val activity = controller.get()
        repeat(3) { activity.onBackPressedDispatcher.onBackPressed() }
        assertFalse(activity.isFinishing)
    }

    @Test fun homeIntentWhileInFrontDoesNotFinishEither() {
        val controller = Robolectric.buildActivity(LauncherActivity::class.java).setup()
        controller.newIntent(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        assertFalse(controller.get().isFinishing)
    }
}
