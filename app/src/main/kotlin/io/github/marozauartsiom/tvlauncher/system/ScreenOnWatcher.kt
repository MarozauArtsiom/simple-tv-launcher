package io.github.marozauartsiom.tvlauncher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Tells the UI when the TV's screen turns on, so Home can go back to the first favourite. A
 * system callback only: it does nothing until the broadcast arrives. If no activity is listening
 * (the process is alive but Home isn't resumed), the event is remembered for the next resume.
 */
class ScreenOnWatcher(private val context: Context) {
    private var listener: (() -> Unit)? = null
    private var pending = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) onScreenOn()
        }
    }

    fun register() {
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun onScreenOn() {
        val l = listener
        if (l != null) l() else pending = true
    }

    /** Called from onResume. Returns true if the screen turned on while nobody was listening. */
    fun attach(onScreenOn: () -> Unit): Boolean {
        listener = onScreenOn
        return pending.also { pending = false }
    }

    fun detach() {
        listener = null
    }
}
