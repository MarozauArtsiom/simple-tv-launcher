package io.github.marozauartsiom.tvlauncher.ui.remote

import android.content.Context
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
 * The badge as a passive window over every app (no focus, no touches). Needs the "draw over other apps"
 * permission the HDMI card uses; without it there is no badge (Settings still lists who is connected).
 */
class OverlayRemoteBadge(
    private val context: () -> Context,
    private val theme: () -> ThemeSpec,
    private val canDraw: () -> Boolean,
) : RemoteBadgeHost {
    private var host: OverlayHost? = null
    private var shown: Pair<Boolean, Boolean>? = null

    override fun show(phone: Boolean, ai: Boolean) {
        if (shown == phone to ai) return
        if (!canDraw()) return
        hide()
        val t = theme()
        host = OverlayHost(context()).also { h ->
            h.show(focusable = false, gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL, xDp = 0, yDp = 6) {
                LauncherTheme(t) { RemoteBadge(t, phone, ai) }
            }
        }
        shown = phone to ai
    }

    override fun hide() {
        host?.hide()
        host = null
        shown = null
    }
}
