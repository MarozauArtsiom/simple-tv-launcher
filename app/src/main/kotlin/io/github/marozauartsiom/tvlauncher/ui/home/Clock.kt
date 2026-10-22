package io.github.marozauartsiom.tvlauncher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleStartEffect
import io.github.marozauartsiom.tvlauncher.i18n.DateText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Immutable
data class ClockText(val time: String, val date: String, val at: LocalDateTime? = null)

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun clockText(now: LocalDateTime, languageTag: String) =
    ClockText(TimeFormat.format(now), DateText.format(now.toLocalDate(), languageTag), now)

/**
 * The time and date for the top bar. It is refreshed by the system's once-a-minute `ACTION_TIME_TICK`
 * broadcast, registered only while the screen is started, so nothing ticks while an app is in front.
 */
@Composable
fun rememberClock(languageTag: String): State<ClockText> {
    val context = LocalContext.current
    val state = remember(languageTag) { mutableStateOf(clockText(LocalDateTime.now(), languageTag)) }
    LifecycleStartEffect(languageTag) {
        state.value = clockText(LocalDateTime.now(), languageTag)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                state.value = clockText(LocalDateTime.now(), languageTag)
            }
        }
        val filter = IntentFilter(Intent.ACTION_TIME_TICK).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
    return state
}
