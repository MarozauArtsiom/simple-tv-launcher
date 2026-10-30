package io.github.marozauartsiom.tvlauncher.ui.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.system.hdmi.OverlayHost
import io.github.marozauartsiom.tvlauncher.system.remote.RemoteBadgeHost
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.theme.LauncherTheme
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

/** "Remote · AI is controlling": a small pill at the top of the screen while anyone is connected. */
@Composable
fun RemoteBadge(theme: ThemeSpec, phone: Boolean, ai: Boolean, modifier: Modifier = Modifier) {
    val c = theme.colors
    Row(
        modifier.shadow(4.dp, RoundedCornerShape(16.dp)).background(c.chipBg, RoundedCornerShape(16.dp)).height(32.dp).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(c.signal, CircleShape))
        if (phone) Icon(Icons.Phone, null, tint = c.chipFg, modifier = Modifier.size(16.dp))
        if (ai) Icon(Icons.Sparkle, null, tint = c.chipFg, modifier = Modifier.size(16.dp))
        val text = listOfNotNull(
            stringResource(R.string.remote_badge_phone).takeIf { phone },
            stringResource(R.string.remote_badge_ai).takeIf { ai },
        ).joinToString(" · ")
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = c.chipFg, maxLines = 1)
    }
}

/**
 * The badge as a passive window (no focus, no touches). Needs the "draw over other apps" permission the HDMI
 * card uses; without it there is no badge (Settings still lists who is connected).
 *
 * - On Home the top bar's Remote chip says who is connected, so the badge is hidden there; only when the owner hid
 *   that chip ([homeShowsStatus] false) does the badge stay up on Home instead.
 * - Over any other app it stays hidden, and only [flash]es for a moment (with a fade and slide) when an
 *   assistant does something, so the owner sees that the TV is being controlled without a permanent pill.
 *
 * All methods run on the main thread.
 */
class OverlayRemoteBadge(
    private val context: () -> Context,
    private val theme: () -> ThemeSpec,
    private val canDraw: () -> Boolean,
    private val homeInFront: () -> Boolean,
    private val homeShowsStatus: () -> Boolean = { false },
) : RemoteBadgeHost {
    private val handler = Handler(Looper.getMainLooper())
    private var host: OverlayHost? = null
    private var visible: MutableTransitionState<Boolean>? = null
    private var shownPhone = false
    private var shownAi = false
    private var phone = false
    private var ai = false
    private var flashing = false

    override fun show(phone: Boolean, ai: Boolean) {
        this.phone = phone
        this.ai = ai
        if (!phone && !ai) flashing = false
        refresh()
    }

    override fun hide() {
        phone = false
        ai = false
        flashing = false
        handler.removeCallbacksAndMessages(null)
        remove()
    }

    override fun refresh() {
        if (!phone && !ai) { remove(); return }
        val onHome = homeInFront()
        when {
            onHome && homeShowsStatus() -> fadeOut()
            onHome -> display(phone, ai)
            flashing -> display(phone = false, ai = true)
            else -> fadeOut()
        }
    }

    override fun flash() {
        if (!ai || homeInFront()) return
        flashing = true
        handler.removeCallbacksAndMessages(null)
        refresh()
        handler.postDelayed({ flashing = false; refresh() }, FLASH_MS)
    }

    private fun display(phone: Boolean, ai: Boolean) {
        visible?.let { if (host != null && shownPhone == phone && shownAi == ai) { it.targetState = true; return } }
        if (!canDraw()) return
        remove()
        val t = theme()
        val state = MutableTransitionState(false).apply { targetState = true } // composes hidden, then animates in
        host = OverlayHost(context()).also { h ->
            h.show(focusable = false, gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL, xDp = 0, yDp = 6) {
                LauncherTheme(t) {
                    AnimatedVisibility(
                        visibleState = state,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(250)) { -it },
                        exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { -it },
                    ) { RemoteBadge(t, phone, ai) }
                }
            }
        }
        visible = state
        shownPhone = phone
        shownAi = ai
    }

    /** Plays the exit animation, then removes the window. */
    private fun fadeOut() {
        val state = visible ?: return remove()
        state.targetState = false
        handler.postDelayed({ if (visible === state && !state.targetState) remove() }, EXIT_MS)
    }

    private fun remove() {
        host?.hide()
        host = null
        visible = null
    }

    private companion object {
        const val FLASH_MS = 2_500L
        const val EXIT_MS = 300L
    }
}
