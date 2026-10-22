package io.github.marozauartsiom.tvlauncher.data.install

import androidx.compose.runtime.Immutable
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
sealed interface UsbState {
    data object Loading : UsbState
    /** All-files access is missing: nothing on the drive can be read. */
    data object NoStorageAccess : UsbState
    /** The launcher may not install apps yet ("unknown apps"). */
    data object NeedsUnknownApps : UsbState
    data object Empty : UsbState
    data class Ready(val items: List<ApkItem>) : UsbState
    data class Installing(val item: ApkItem) : UsbState
    data class Done(val item: ApkItem) : UsbState
    data class Failed(val item: ApkItem, val cancelled: Boolean) : UsbState
}

/**
 * The "Install from USB" flow: scan the drive, read each archive, install one through the system
 * installer, and follow the outcome. It does nothing until [refresh] is called by the open page.
 * A newer version of the launcher itself is installed the same way (the system replaces the app; the
 * process ends, so no "done" is shown for that one).
 */
class UsbInstaller(
    private val scanner: ApkFinder,
    private val inspector: ApkInspector,
    private val installer: ApkInstaller,
    private val hasStorageAccess: () -> Boolean,
    private val canInstallPackages: () -> Boolean,
    results: Flow<InstallResult>,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<UsbState>(UsbState.Loading)
    val state: StateFlow<UsbState> = _state.asStateFlow()

    init {
        scope.launch { results.collect(::onResult) }
    }

    /** Looks at the drive again (page opened, "Check again", "Done"). */
    fun refresh() {
        scope.launch { _state.value = load() }
    }

    private suspend fun load(): UsbState {
        if (!hasStorageAccess()) return UsbState.NoStorageAccess
        val items = scanner.scan().mapNotNull { file ->
            inspector.inspect(file)?.let { info -> ApkItem.of(info, inspector.installedVersion(info.packageName)) }
        }
        return when {
            items.isEmpty() -> UsbState.Empty
            else -> UsbState.Ready(items)
        }
    }

    /** Installs [item] if its action calls for it. */
    fun install(item: ApkItem) {
        if (item.action == ApkAction.Installed) return
        if (!canInstallPackages()) {
            _state.value = UsbState.NeedsUnknownApps
            return
        }
        _state.value = UsbState.Installing(item)
        scope.launch {
            if (!installer.commit(File(item.info.file.path), item.info.packageName)) {
                _state.value = UsbState.Failed(item, cancelled = false)
            }
        }
    }

    /** The system reported how an installation ended. Results that do not match what we are doing are ignored. */
    private fun onResult(result: InstallResult) {
        val current = _state.value as? UsbState.Installing ?: return
        when (result) {
            is InstallResult.NeedsConfirm -> Unit // the receiver shows the system dialog; we keep waiting
            is InstallResult.Success -> _state.value = UsbState.Done(current.item)
            InstallResult.Cancelled -> _state.value = UsbState.Failed(current.item, cancelled = true)
            is InstallResult.Failure -> _state.value = UsbState.Failed(current.item, cancelled = false)
        }
    }
}
