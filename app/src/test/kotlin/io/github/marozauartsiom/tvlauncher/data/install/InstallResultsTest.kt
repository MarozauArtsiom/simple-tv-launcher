package io.github.marozauartsiom.tvlauncher.data.install

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallResultsTest {
    private val own = "io.github.marozauartsiom.tvlauncher"
    private fun status(code: Int, confirm: Intent? = null, message: String? = null) =
        InstallResults.fromStatus(code, message, "pkg.x", confirm, own)

    @Test fun successCarriesThePackage() = assertEquals(InstallResult.Success("pkg.x"), status(PackageInstaller.STATUS_SUCCESS))

    @Test fun abortedIsACancellation() = assertEquals(InstallResult.Cancelled, status(PackageInstaller.STATUS_FAILURE_ABORTED))

    @Test fun otherStatusesFailWithTheMessage() {
        assertEquals(InstallResult.Failure("bad"), status(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, message = "bad"))
        assertEquals(InstallResult.Failure(null), status(PackageInstaller.STATUS_FAILURE_STORAGE))
    }

    @Test fun installersConfirmationScreenIsPassedOn() {
        val confirm = Intent("android.content.pm.action.CONFIRM_INSTALL")
        assertTrue(status(PackageInstaller.STATUS_PENDING_USER_ACTION, confirm) is InstallResult.NeedsConfirm)
    }

    @Test fun anyOtherIntentIsRefused() {
        val other = Intent(Intent.ACTION_VIEW)
        assertEquals(InstallResult.Failure(null), status(PackageInstaller.STATUS_PENDING_USER_ACTION, other))
        assertEquals(InstallResult.Failure(null), status(PackageInstaller.STATUS_PENDING_USER_ACTION, null))
    }

    @Test fun aConfirmationAimedAtOurOwnComponentIsRefused() {
        val aimed = Intent("android.content.pm.action.CONFIRM_INSTALL").setComponent(ComponentName(own, "$own.LauncherActivity"))
        assertEquals(InstallResult.Failure(null), status(PackageInstaller.STATUS_PENDING_USER_ACTION, aimed))
    }
}
