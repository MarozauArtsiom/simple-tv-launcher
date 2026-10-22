package io.github.marozauartsiom.tvlauncher.system

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings

/**
 * System screens for one app: its details page and the uninstall dialog. Only explicit, fixed
 * actions with a `package:` URI are built here, nothing from outside is forwarded. [start] is
 * `Context.startActivity`. Returns false when the TV has no screen for the action.
 */
class PackageActions(private val start: (Intent) -> Unit) {
    fun showInfo(packageName: String): Boolean =
        launch(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(packageName)))

    /** Needs `REQUEST_DELETE_PACKAGES`; the system asks the owner to confirm. */
    fun uninstall(packageName: String): Boolean = launch(Intent(Intent.ACTION_DELETE, packageUri(packageName)))

    private fun packageUri(packageName: String) = Uri.fromParts("package", packageName, null)

    private fun launch(intent: Intent): Boolean = try {
        start(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
