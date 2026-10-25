package io.github.marozauartsiom.tvlauncher.system.hdmi

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * One small window over other apps (`TYPE_APPLICATION_OVERLAY`, needs `SYSTEM_ALERT_WINDOW`), in the
 * bottom-right corner of the safe area. It takes focus so the remote's D-pad and OK reach it. At most one
 * is shown at a time, and it exists only while the prompt is on screen. Must be used on the main thread.
 *
 * [context] should carry the launcher's language so strings resolve like in the app.
 */
class OverlayHost(private val context: Context) {
    private var view: ComposeView? = null
    private var owner: Owner? = null

    val isShowing: Boolean get() = view != null

    /**
     * [focusable] false makes a passive window (no focus, no touches) for status badges; [gravity] and the
     * offsets in dp place it (default: bottom-right of the safe area).
     */
    fun show(
        focusable: Boolean = true,
        gravity: Int = Gravity.BOTTOM or Gravity.END,
        xDp: Int = 48,
        yDp: Int = 27,
        content: @Composable (dismiss: () -> Unit) -> Unit,
    ) {
        hide()
        val windowManager = context.getSystemService(WindowManager::class.java)
        val owner = Owner().also { it.start() }
        val density = context.resources.displayMetrics.density
        val compose = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { content { hide() } }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                (if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            x = (xDp * density).toInt()
            y = (yDp * density).toInt()
        }
        try {
            windowManager.addView(compose, params)
            view = compose
            this.owner = owner
        } catch (e: RuntimeException) { // the permission was withdrawn, or the window token is bad: no overlay, no crash
            android.util.Log.w("OverlayHost", "no overlay: $e")
            owner.destroy()
        }
    }

    fun hide() {
        val current = view ?: return
        view = null
        runCatching { context.getSystemService(WindowManager::class.java).removeViewImmediate(current) }
        owner?.destroy()
        owner = null
    }

    /** The lifecycle and saved-state owner a ComposeView needs when it is not in an Activity. */
    private class Owner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)

        init {
            controller.performAttach()
            controller.performRestore(null)
        }

        fun start() { registry.currentState = Lifecycle.State.RESUMED }
        fun destroy() { registry.currentState = Lifecycle.State.DESTROYED }

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
    }
}
