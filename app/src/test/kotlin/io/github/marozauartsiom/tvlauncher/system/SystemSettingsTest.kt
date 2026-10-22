package io.github.marozauartsiom.tvlauncher.system

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SystemSettingsTest {
    private val started = mutableListOf<Intent>()
    private val vendorPicture = Intent("vendor.PICTURE")
    private val vendorSleep = Intent("vendor.SLEEP")

    /** A TV on which only the listed actions resolve. */
    private fun tv(vararg actions: String, failStart: Boolean = false) = SystemSettings(
        canHandle = { it.action in actions },
        start = { if (failStart) throw ActivityNotFoundException() else started += it },
        vendorPicture = listOf(vendorPicture),
        vendorSleep = listOf(vendorSleep),
    )

    private val standard = arrayOf(
        Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_BLUETOOTH_SETTINGS, Settings.ACTION_SOUND_SETTINGS,
        Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SETTINGS,
    )

    @Test fun eachTileOpensItsOwnSystemScreen() {
        val settings = tv(*standard)
        assertEquals(Settings.ACTION_WIFI_SETTINGS, settings.resolve(SystemTile.Wifi)?.action)
        assertEquals(Settings.ACTION_BLUETOOTH_SETTINGS, settings.resolve(SystemTile.Bluetooth)?.action)
        assertEquals(Settings.ACTION_SOUND_SETTINGS, settings.resolve(SystemTile.Sound)?.action)
        assertEquals(Settings.ACTION_SETTINGS, settings.resolve(SystemTile.Android)?.action)
    }

    @Test fun pictureUsesTheVendorScreenWhenItExists() {
        assertEquals("vendor.PICTURE", tv(*standard, "vendor.PICTURE").resolve(SystemTile.Picture)?.action)
    }

    @Test fun pictureFallsBackToDisplaySettings() {
        assertEquals(Settings.ACTION_DISPLAY_SETTINGS, tv(*standard).resolve(SystemTile.Picture)?.action)
    }

    @Test fun sleepTimerExistsOnlyWithAVendorScreen() {
        assertNull(tv(*standard).resolve(SystemTile.Sleep))
        assertFalse(SystemTile.Sleep in tv(*standard).available())
        assertEquals("vendor.SLEEP", tv(*standard, "vendor.SLEEP").resolve(SystemTile.Sleep)?.action)
    }

    @Test fun tilesWithoutAnyScreenAreNotOffered() {
        val settings = tv(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_SETTINGS)
        assertEquals(listOf(SystemTile.Wifi, SystemTile.Android), settings.available())
    }

    @Test fun availableKeepsTheTileOrder() {
        assertEquals(
            listOf(SystemTile.Wifi, SystemTile.Bluetooth, SystemTile.Sound, SystemTile.Picture, SystemTile.Sleep, SystemTile.Android),
            tv(*standard, "vendor.SLEEP").available(),
        )
    }

    @Test fun openStartsTheResolvedIntentInANewTask() {
        assertTrue(tv(*standard).open(SystemTile.Wifi))
        val intent = started.single()
        assertEquals(Settings.ACTION_WIFI_SETTINGS, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun openReportsFalseWhenThereIsNoScreenOrTheSystemRefuses() {
        assertFalse(tv().open(SystemTile.Wifi))
        assertFalse(tv(*standard, failStart = true).open(SystemTile.Wifi))
        assertTrue(started.isEmpty())
    }

    @Test fun aResolverThatThrowsCountsAsNotAvailable() {
        val settings = SystemSettings(canHandle = { error("binder died") }, start = {})
        assertEquals(emptyList<SystemTile>(), settings.available())
    }

    @Test fun withoutProbedVendorIntentsPictureIsGenericAndSleepIsHidden() {
        val settings = SystemSettings(canHandle = { true }, start = {})
        assertEquals(Settings.ACTION_DISPLAY_SETTINGS, settings.resolve(SystemTile.Picture)?.action)
        assertNull(settings.resolve(SystemTile.Sleep))
    }
}
