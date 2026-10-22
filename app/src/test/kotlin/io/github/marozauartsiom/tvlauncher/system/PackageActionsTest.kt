package io.github.marozauartsiom.tvlauncher.system

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class PackageActionsTest {
    private val started = mutableListOf<Intent>()
    private val actions = PackageActions { started += it }

    @Test fun infoOpensTheAppDetailsPageOfThatPackage() {
        assertTrue(actions.showInfo("com.example.tv"))
        val intent = started.single()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:com.example.tv", intent.dataString)
    }

    @Test fun uninstallAsksTheSystemToConfirm() {
        assertTrue(actions.uninstall("com.example.tv"))
        val intent = started.single()
        assertEquals(Intent.ACTION_DELETE, intent.action)
        assertEquals("package:com.example.tv", intent.dataString)
    }

    @Test fun aTvWithoutTheScreenReportsFalseInsteadOfCrashing() {
        val failing = PackageActions { throw ActivityNotFoundException() }
        assertFalse(failing.showInfo("a"))
        assertFalse(PackageActions { throw SecurityException() }.uninstall("a"))
    }
}
