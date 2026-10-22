package io.github.marozauartsiom.tvlauncher.system

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings

/** The quick tiles at the top of the Settings panel. */
enum class SystemTile { Wifi, Bluetooth, Sound, Picture, Sleep, Android }

/**
 * Vendor screens that the stock Android settings do not have. They are tried before the generic
 * fallback, and only if they resolve on this TV.
 *
 * **Unknown until the TV is probed** (`docs/device/`, see the `xiaomi-tv-a-pro-2025` skill): add the
 * intents the probe finds here and in the manifest `<queries>`. With empty lists, Picture opens the
 * generic display settings and the Sleep timer tile is hidden.
 */
object VendorIntents {
    val picture: List<Intent> = emptyList()
    val sleep: List<Intent> = emptyList()
}

/**
 * Opens system settings screens. Each tile has a chain of candidate intents; the first that
 * [canHandle] is used, and a tile with no candidate is not offered. [start] is
 * `Context.startActivity`.
 */
class SystemSettings(
    private val canHandle: (Intent) -> Boolean,
    private val start: (Intent) -> Unit,
    private val vendorPicture: List<Intent> = VendorIntents.picture,
    private val vendorSleep: List<Intent> = VendorIntents.sleep,
) {
    private fun candidates(tile: SystemTile): List<Intent> = when (tile) {
        SystemTile.Wifi -> listOf(Intent(Settings.ACTION_WIFI_SETTINGS))
        SystemTile.Bluetooth -> listOf(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        SystemTile.Sound -> listOf(Intent(Settings.ACTION_SOUND_SETTINGS))
        SystemTile.Picture -> vendorPicture + Intent(Settings.ACTION_DISPLAY_SETTINGS)
        SystemTile.Sleep -> vendorSleep
        SystemTile.Android -> listOf(Intent(Settings.ACTION_SETTINGS))
    }

    /** The intent this TV can open for [tile], or null. */
    fun resolve(tile: SystemTile): Intent? = candidates(tile).firstOrNull { runCatching { canHandle(it) }.getOrDefault(false) }

    /** Tiles to show, in order. */
    fun available(): List<SystemTile> = SystemTile.entries.filter { resolve(it) != null }

    /** Opens the screen; false if there is none or the system refused. */
    fun open(tile: SystemTile): Boolean {
        val intent = resolve(tile) ?: return false
        return try {
            start(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
