package io.github.marozauartsiom.tvlauncher.system

import android.app.Application
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class InstallResultReceiverTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun status(confirm: Intent?) = Intent().putExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_PENDING_USER_ACTION)
        .apply { if (confirm != null) putExtra(Intent.EXTRA_INTENT, confirm) }

    @Test fun theInstallersConfirmationScreenIsStartedWithOnlyOurFlags() {
        val confirm = Intent("android.content.pm.action.CONFIRM_INSTALL")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        InstallResultReceiver().onReceive(app, status(confirm))
        val started = shadowOf(app).nextStartedActivity
        assertEquals("android.content.pm.action.CONFIRM_INSTALL", started.action)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, started.flags)
    }

    @Test fun anyOtherIntentIsNeverStarted() {
        InstallResultReceiver().onReceive(app, status(Intent(Intent.ACTION_VIEW)))
        assertNull(shadowOf(app).nextStartedActivity)
        InstallResultReceiver().onReceive(app, status(null))
        assertNull(shadowOf(app).nextStartedActivity)
    }
}
