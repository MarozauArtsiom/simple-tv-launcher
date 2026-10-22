package io.github.marozauartsiom.tvlauncher.system.hdmi

import io.github.marozauartsiom.tvlauncher.data.prefs.HdmiSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ties the watcher to the rest of the launcher and decides where a prompt appears:
 *
 * - launcher in front: [pending] is set and Home shows the in-launcher card;
 * - another app in front, overlay permission granted and the owner allows it: [showOverOtherApps];
 * - otherwise [pending] waits and Home shows it when it comes back (if the signal is still there).
 *
 * It lives as long as the process and does nothing until the system reports a change.
 */
class HdmiController(
    private val inputs: TvInputs,
    private val settings: () -> HdmiSettings,
    clock: () -> Long,
    private val canDrawOverlays: () -> Boolean,
    private val showOverOtherApps: (HdmiInput) -> Unit,
) {
    private val watcher = HdmiWatcher(
        inputs = inputs,
        clock = clock,
        allow = { input -> settings().let { it.promptEnabled && input.id !in it.mutedInputs } },
        onSignal = ::route,
    )

    /** The inputs and whether each has a signal, for the top-bar chips. */
    val status: StateFlow<List<HdmiStatus>> get() = watcher.status

    private val _pending = MutableStateFlow<HdmiInput?>(null)
    /** A signal Home should ask about as soon as it can. Cleared by [consume]. */
    val pending: StateFlow<HdmiInput?> = _pending.asStateFlow()

    @Volatile
    var launcherInFront: Boolean = false
        private set

    fun start() = watcher.start()

    fun stop() = watcher.stop()

    /** Home is on screen: re-read the inputs (catches what happened while we were away) and drop stale signals. */
    fun onHomeVisible() {
        launcherInFront = true
        watcher.refresh()
        _pending.value?.let { if (inputs.state(it.id) != TvInputs.CONNECTED) _pending.value = null }
    }

    fun onHomeHidden() { launcherInFront = false }

    /** Home has taken the pending signal and shows (or decided not to show) its card. */
    fun consume(input: HdmiInput) { if (_pending.value?.id == input.id) _pending.value = null }

    /** "Switch" or "Not now" or a timeout: no more questions about [id] until it disconnects. */
    fun dismiss(id: String) = watcher.dismiss(id)

    private fun route(input: HdmiInput) {
        val overOtherApps = !launcherInFront && settings().overOtherApps && canDrawOverlays()
        if (overOtherApps) showOverOtherApps(input) else _pending.value = input
    }
}
