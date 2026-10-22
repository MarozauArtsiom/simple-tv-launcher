package io.github.marozauartsiom.tvlauncher.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shell state that outlives configuration changes: the open overlay and the "go back to the first
 * favourite" requests (Home pressed, screen turned on, Back at the top). Back never leaves the app.
 */
class LauncherViewModel : ViewModel() {
    private val _overlay = MutableStateFlow<Overlay>(Overlay.None)
    val overlay: StateFlow<Overlay> = _overlay.asStateFlow()

    private val _resets = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emits when Home should scroll to the top and focus the first favourite. */
    val resets: SharedFlow<Unit> = _resets.asSharedFlow()

    fun open(overlay: Overlay) { _overlay.value = overlay }

    /** Handles Back. Returns true if an overlay consumed it; at the top level it asks Home to reset. */
    fun onBack(): Boolean {
        val current = _overlay.value
        if (current == Overlay.None) {
            requestReset()
            return false
        }
        _overlay.value = current.back()
        return true
    }

    /** The Home key (or the screen turning on): close everything and go to the first favourite. */
    fun onHomePressed() {
        _overlay.value = Overlay.None
        requestReset()
    }

    fun requestReset() { _resets.tryEmit(Unit) }
}
