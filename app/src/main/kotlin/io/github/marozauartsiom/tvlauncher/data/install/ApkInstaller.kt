package io.github.marozauartsiom.tvlauncher.data.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Hands an archive to the system installer. The outcome comes later, as an [InstallResult]. */
interface ApkInstaller {
    /** True if the archive was committed to the system (the owner may still have to confirm); false if it could not be. */
    suspend fun commit(file: File, packageName: String): Boolean
}

/**
 * A `PackageInstaller` session: the file is streamed in and committed with a status `PendingIntent`.
 * The PendingIntent is explicit (our own receiver, our own package) and mutable, because the system must
 * add the status extras to it; nothing from the outside can use it to start anything of ours.
 */
class SessionApkInstaller(
    private val context: Context,
    private val receiver: Class<*>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ApkInstaller {
    override suspend fun commit(file: File, packageName: String): Boolean = withContext(io) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(packageName) }
        var sessionId = -1
        try {
            sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                file.inputStream().use { input ->
                    session.openWrite("base.apk", 0, file.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val intent = Intent(context, receiver).setPackage(context.packageName)
                val pending = PendingIntent.getBroadcast(
                    context, sessionId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pending.intentSender)
            }
            true
        } catch (e: IOException) {
            abandon(installer, sessionId); false
        } catch (e: SecurityException) {
            abandon(installer, sessionId); false
        } catch (e: IllegalStateException) {
            abandon(installer, sessionId); false
        } catch (e: IllegalArgumentException) {
            abandon(installer, sessionId); false
        }
    }

    private fun abandon(installer: PackageInstaller, id: Int) {
        if (id >= 0) runCatching { installer.abandonSession(id) }
    }
}
