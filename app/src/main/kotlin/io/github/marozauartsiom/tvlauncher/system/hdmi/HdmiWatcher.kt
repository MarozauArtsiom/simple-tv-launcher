package io.github.marozauartsiom.tvlauncher.system.hdmi

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** An HDMI chip's data: the input, and whether it has a signal (connected, not just plugged in standby). */
@Immutable
data class HdmiStatus(val input: HdmiInput, val connected: Boolean)

/**
 * Decides when "signal on HDMI N" is worth asking about. It listens to state changes (the system calls
 * it; it never polls) and calls [onSignal] when an input goes to CONNECTED from something else, unless
 *
 * - [allow] says no (the owner switched the prompt off, in general or for that input),
 * - the owner already said "not now" for that input and it has not disconnected since, or
 * - the input was announced less than [debounceMs] ago (a flaky cable or a TV that repeats itself).
 *
 * [refresh] re-reads every state, which catches what happened while the launcher's process was away.
 */
class HdmiWatcher(
    private val inputs: TvInputs,
    private val clock: () -> Long,
    private val allow: (HdmiInput) -> Boolean = { true },
    private val debounceMs: Long = DEBOUNCE_MS,
    private val onSignal: (HdmiInput) -> Unit,
) {
    private val states = HashMap<String, Int?>()
    private val lastAnnounced = HashMap<String, Long>()
    private val dismissed = HashSet<String>()
    private var unregister: (() -> Unit)? = null

    private val _status = MutableStateFlow<List<HdmiStatus>>(emptyList())
    /** The inputs and their signal, for the top-bar chips. Changes only when the system reports a change. */
    val status: StateFlow<List<HdmiStatus>> = _status.asStateFlow()

    /** Starts listening. What is connected right now is not news, so it does not prompt. */
    fun start() {
        if (unregister != null) return
        inputs.hdmiInputs().forEach { states[it.id] = inputs.state(it.id) }
        unregister = inputs.register(::onChange)
        publish()
    }

    fun stop() {
        unregister?.invoke()
        unregister = null
    }

    /** The owner said "not now" (or the prompt timed out): no more questions about [id] until it disconnects. */
    fun dismiss(id: String) { dismissed += id }

    /** Re-reads every input's state (when Home comes back to the front). */
    fun refresh() {
        inputs.hdmiInputs().forEach { onChange(it.id, inputs.state(it.id)) }
        publish()
    }

    private fun onChange(id: String, state: Int?) {
        val before = states.put(id, state)
        if (state != TvInputs.CONNECTED) {
            if (state == TvInputs.DISCONNECTED || state == null) dismissed -= id
            publish()
            return
        }
        publish()
        if (before == TvInputs.CONNECTED) return
        val input = inputs.hdmiInputs().firstOrNull { it.id == id } ?: return
        if (id in dismissed || !allow(input)) return
        val now = clock()
        val last = lastAnnounced[id]
        if (last != null && now - last < debounceMs) return
        lastAnnounced[id] = now
        onSignal(input)
    }

    private fun publish() {
        _status.value = inputs.hdmiInputs().map { HdmiStatus(it, states[it.id] == TvInputs.CONNECTED) }
    }

    companion object {
        const val DEBOUNCE_MS = 30_000L
    }
}
