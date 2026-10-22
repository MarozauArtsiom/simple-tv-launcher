package io.github.marozauartsiom.tvlauncher.system.hdmi

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable

/**
 * One HDMI input as the owner sees it. [deviceName] is the connected device's name when it is known
 * (the public API only gives the input's label, which for a CEC device is usually its name).
 */
@Immutable
data class HdmiInput(val id: String, val label: String, val deviceName: String?)

/** What the launcher needs from Android's TV input service, so tests can fake it. */
interface TvInputs {
    /** HDMI inputs that are not hidden in the TV's own settings, labelled. */
    fun hdmiInputs(): List<HdmiInput>

    /** The input's connection state (`TvInputManager.INPUT_STATE_*`), or null if unknown. */
    fun state(id: String): Int?

    /** Calls [onChange] when an input changes state, or one is added or removed. Returns a function that unregisters. */
    fun register(onChange: (inputId: String, state: Int?) -> Unit): () -> Unit

    companion object {
        const val CONNECTED = TvInputManager.INPUT_STATE_CONNECTED
        const val STANDBY = TvInputManager.INPUT_STATE_CONNECTED_STANDBY
        const val DISCONNECTED = TvInputManager.INPUT_STATE_DISCONNECTED
    }
}

/**
 * The real thing. State changes arrive as a binder callback: nothing polls, and nothing runs while no
 * input changes. Android reports *connection* state (plug, or the device's CEC power state), not whether a
 * picture is present.
 */
class TvInputManagerSource(private val context: Context) : TvInputs {
    private val manager: TvInputManager? get() = context.getSystemService(TvInputManager::class.java)

    override fun hdmiInputs(): List<HdmiInput> = runCatching {
        manager?.tvInputList.orEmpty()
            .filter { it.type == TvInputInfo.TYPE_HDMI && !it.isHidden(context) }
            .map { info ->
                val label = (info.loadCustomLabel(context)?.toString()?.takeIf { it.isNotBlank() } ?: info.loadLabel(context)?.toString()).orEmpty()
                HdmiInput(info.id, label.ifBlank { info.id }, deviceName = null)
            }
    }.getOrDefault(emptyList())

    override fun state(id: String): Int? = runCatching { manager?.getInputState(id) }.getOrNull()

    override fun register(onChange: (String, Int?) -> Unit): () -> Unit {
        val tim = manager ?: return {}
        val callback = object : TvInputManager.TvInputCallback() {
            override fun onInputStateChanged(inputId: String, state: Int) = onChange(inputId, state)
            override fun onInputAdded(inputId: String) = onChange(inputId, state(inputId))
            override fun onInputRemoved(inputId: String) = onChange(inputId, null)
            override fun onInputUpdated(inputId: String) = onChange(inputId, state(inputId))
        }
        return runCatching {
            tim.registerCallback(callback, Handler(Looper.getMainLooper()))
            val unregister: () -> Unit = { runCatching { tim.unregisterCallback(callback) } }
            unregister
        }.getOrDefault({})
    }
}
