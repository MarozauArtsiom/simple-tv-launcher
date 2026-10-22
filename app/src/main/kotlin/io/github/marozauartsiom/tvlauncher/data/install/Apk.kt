package io.github.marozauartsiom.tvlauncher.data.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An `.apk` file found on a USB drive. */
@Immutable
data class ApkFile(val path: String, val name: String, val sizeBytes: Long)

/** What the archive says about itself. */
@Immutable
data class ApkInfo(val file: ApkFile, val packageName: String, val label: String, val versionName: String, val versionCode: Long)

/** What pressing OK on an archive will do. */
enum class ApkAction {
    Install,
    /** The archive is newer than what is installed. */
    Update,
    /** The same version, or a newer one, is already installed: nothing to do (and a downgrade would be refused anyway). */
    Installed,
}

@Immutable
data class ApkItem(val info: ApkInfo, val action: ApkAction) {
    companion object {
        fun of(info: ApkInfo, installedVersionCode: Long?): ApkItem = ApkItem(
            info,
            when {
                installedVersionCode == null -> ApkAction.Install
                info.versionCode > installedVersionCode -> ApkAction.Update
                else -> ApkAction.Installed
            },
        )
    }
}

/** Finds archives to offer. Fakes replace it in tests. */
fun interface ApkFinder {
    suspend fun scan(): List<ApkFile>
}

/** Finds `.apk` files on removable volumes (same storage access as the background pictures). */
class ApkScanner(private val roots: () -> List<File>, private val io: CoroutineDispatcher = Dispatchers.IO) : ApkFinder {
    /** Up to [MAX_FILES] archives from the top three folder levels of each volume, sorted by name. */
    override suspend fun scan(): List<ApkFile> = withContext(io) {
        val found = ArrayList<ApkFile>()
        for (root in roots()) walk(root, 0, found)
        found.sortedBy { it.name.lowercase() }.take(MAX_FILES)
    }

    private fun walk(dir: File, depth: Int, into: MutableList<ApkFile>) {
        if (into.size >= MAX_FILES * 2) return
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (child in children) {
            if (child.name.startsWith(".")) continue
            if (child.isDirectory) {
                if (depth < MAX_DEPTH && child.name !in SKIPPED_DIRS) walk(child, depth + 1, into)
            } else if (child.isFile && child.extension.equals("apk", ignoreCase = true) && child.length() in MIN_BYTES..MAX_BYTES) {
                into += ApkFile(child.path, child.name, child.length())
            }
        }
    }

    companion object {
        const val MAX_FILES = 40
        private const val MAX_DEPTH = 2
        private const val MIN_BYTES = 1024L
        private const val MAX_BYTES = 1L * 1024 * 1024 * 1024
        private val SKIPPED_DIRS = setOf("Android", "LOST.DIR", "System Volume Information", "\$RECYCLE.BIN")
    }
}

/** Reads an archive's own manifest. Fakes replace it in tests. */
interface ApkInspector {
    /** null if the file is not a readable APK. */
    fun inspect(file: ApkFile): ApkInfo?

    /** The installed version code of [packageName], or null if it is not installed. */
    fun installedVersion(packageName: String): Long?
}

class PackageManagerInspector(private val context: Context) : ApkInspector {
    private val pm: PackageManager get() = context.packageManager

    override fun inspect(file: ApkFile): ApkInfo? = runCatching {
        val info = pm.getPackageArchiveInfo(file.path, 0) ?: return null
        val app = info.applicationInfo?.apply { sourceDir = file.path; publicSourceDir = file.path }
        val label = app?.let { pm.getApplicationLabel(it).toString() }?.takeIf { it.isNotBlank() } ?: info.packageName
        ApkInfo(file, info.packageName, label, info.versionName.orEmpty(), info.longVersionCode)
    }.getOrNull()

    override fun installedVersion(packageName: String): Long? = runCatching { pm.getPackageInfo(packageName, 0).longVersionCode }.getOrNull()
}

/** The outcome the system reports after a session was committed. */
sealed interface InstallResult {
    data class Success(val packageName: String?) : InstallResult
    data object Cancelled : InstallResult
    data class Failure(val message: String?) : InstallResult
    /** The system wants the owner to confirm: start [confirm] (already checked to be the installer's confirmation screen). */
    data class NeedsConfirm(val confirm: Intent) : InstallResult
}

object InstallResults {
    /** The only screen we agree to start on the installer's behalf. */
    private const val CONFIRM_ACTION = "android.content.pm.action.CONFIRM_INSTALL"

    /**
     * Turns the installer's status broadcast into an [InstallResult]. A confirmation intent is passed on
     * only if it is the installer's own confirmation action and does not point at one of our components
     * (the broadcast comes through a PendingIntent only the installer holds; this is defence in depth).
     */
    fun fromStatus(status: Int, message: String?, packageName: String?, confirm: Intent?, ownPackage: String): InstallResult = when (status) {
        PackageInstaller.STATUS_SUCCESS -> InstallResult.Success(packageName)
        PackageInstaller.STATUS_PENDING_USER_ACTION ->
            if (confirm != null && confirm.action == CONFIRM_ACTION && confirm.component?.packageName != ownPackage) {
                InstallResult.NeedsConfirm(confirm)
            } else {
                InstallResult.Failure(null)
            }
        PackageInstaller.STATUS_FAILURE_ABORTED -> InstallResult.Cancelled
        else -> InstallResult.Failure(message)
    }
}
