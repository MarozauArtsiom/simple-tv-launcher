package io.github.marozauartsiom.tvlauncher.system.power

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * The only way a sideloaded app can turn the TV off: an accessibility service the owner enables once over adb.
 * The phone remote and AI access also act through it (screen picture, taps, text), only while they are on.
 * It asks for no events and does nothing until [powerOff] is called (the Settings button or the sleep timer):
 * then it opens the system power menu and presses its "Power off" item itself. If the item is not found
 * within [SEARCH_MS] the menu just stays open for the owner.
 */
class PowerService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        // The phone remote and AI access work through this helper: without it, everything turns off.
        (application as? io.github.marozauartsiom.tvlauncher.LauncherApp)?.graph?.remoteIfStarted?.shutDown()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private fun start() {
        if (!performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)) return
        val deadline = System.currentTimeMillis() + SEARCH_MS
        fun attempt() {
            if (clickPowerOff()) return
            if (System.currentTimeMillis() < deadline) handler.postDelayed(::attempt, STEP_MS)
        }
        handler.postDelayed(::attempt, STEP_MS)
    }

    /** Looks through every window on screen (the power menu is a system window) for a "Power off" item. */
    private fun clickPowerOff(): Boolean {
        val roots = runCatching { windows.mapNotNull { it.root } }.getOrDefault(emptyList()) + listOfNotNull(rootInActiveWindow)
        for (root in roots) {
            for (label in PowerOffLabels.all) {
                val node = root.findAccessibilityNodeInfosByText(label).firstOrNull { PowerOffLabels.matches(it.text?.toString()) }
                    ?: continue
                if (clickable(node)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
            }
        }
        return false
    }

    private fun clickable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent
        return n
    }

    companion object {
        private const val SEARCH_MS = 3_000L
        private const val STEP_MS = 250L

        @Volatile private var instance: PowerService? = null

        /** True when the owner has enabled the service (see the README). */
        val isEnabled: Boolean get() = instance != null

        /** The running helper, for the phone remote and AI access (screen picture, taps, text, keys). */
        val service: android.accessibilityservice.AccessibilityService? get() = instance

        /** Starts powering off; false if the service is not enabled. */
        fun powerOff(): Boolean {
            val service = instance ?: return false
            service.handler.post { service.start() }
            return true
        }
    }
}

/** Names of the power menu's "off" item in the TV's languages; matched whole, ignoring case, so "Restart" never matches. */
object PowerOffLabels {
    val all = listOf("Выключить", "Выключение", "Выключыць", "Power off", "Shut down", "Turn off", "Wyłącz", "Ausschalten", "Éteindre")

    fun matches(text: String?): Boolean = text != null && all.any { it.equals(text.trim(), ignoreCase = true) }
}
