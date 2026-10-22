package io.github.marozauartsiom.tvlauncher.system.hdmi

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.tv.TvContract

/**
 * Vendor screens for choosing an input, tried when nothing handles the standard passthrough address.
 * **Unknown until the TV is probed** (`docs/device/`): add what the probe finds here and in the manifest `<queries>`.
 */
object VendorInputIntents {
    val picker: List<Intent> = emptyList()
}

/**
 * Switches the TV to an HDMI input: the passthrough address (`content://android.media.tv/passthrough/<id>`)
 * opened with `ACTION_VIEW`, which the TV's live-TV app handles; if nothing handles it, the vendor's input
 * picker. [canHandle] is `resolveActivity != null`, [start] is `Context.startActivity`.
 */
class InputLauncher(
    private val canHandle: (Intent) -> Boolean,
    private val start: (Intent) -> Unit,
    private val picker: List<Intent> = VendorInputIntents.picker,
) {
    /** The intents to try for [id], in order. */
    fun candidates(id: String): List<Intent> =
        listOf(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id))) + picker

    /** True if some screen was opened. */
    fun open(id: String): Boolean {
        val intent = candidates(id).firstOrNull { runCatching { canHandle(it) }.getOrDefault(false) } ?: return false
        return try {
            start(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
