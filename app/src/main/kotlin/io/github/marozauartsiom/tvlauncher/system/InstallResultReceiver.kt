package io.github.marozauartsiom.tvlauncher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import io.github.marozauartsiom.tvlauncher.LauncherApp
import io.github.marozauartsiom.tvlauncher.data.install.InstallResult
import io.github.marozauartsiom.tvlauncher.data.install.InstallResults

/**
 * Receives the installer's status for a session we committed. Not exported: the only way to reach it is the
 * explicit PendingIntent that was given to the system installer. When the system needs the owner's
 * confirmation, its confirmation screen is started (only if it is the installer's own action, started with
 * our flags only: no URI-permission grants can ride along).
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = InstallResults.fromStatus(
            status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE),
            message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
            packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME),
            confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java),
            ownPackage = context.packageName,
        )
        if (result is InstallResult.NeedsConfirm) {
            runCatching { context.startActivity(Intent(result.confirm).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        (context.applicationContext as? LauncherApp)?.graph?.installEvents?.tryEmit(result)
    }
}
