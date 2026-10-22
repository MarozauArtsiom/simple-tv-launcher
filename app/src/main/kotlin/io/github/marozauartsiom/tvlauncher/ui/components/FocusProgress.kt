package io.github.marozauartsiom.tvlauncher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.flow.collectLatest

/**
 * Focus as a 0..1 animation (in 150 ms, out 120 ms). It is only read inside draw and layer
 * lambdas, so a focus change redraws the element without recomposing it.
 */
@Stable
class FocusProgress {
    var focused by mutableStateOf(false)
        private set
    internal val animation = Animatable(0f)

    val value: Float get() = animation.value

    /** Put this before the focusable in the modifier chain. */
    fun track(): Modifier = Modifier.onFocusChanged { focused = it.hasFocus }
}

@Composable
fun rememberFocusProgress(): FocusProgress {
    val progress = remember { FocusProgress() }
    LaunchedEffect(progress) {
        snapshotFlow { progress.focused }.collectLatest { focused ->
            progress.animation.animateTo(if (focused) 1f else 0f, tween(if (focused) 150 else 120, easing = FastOutSlowInEasing))
        }
    }
    return progress
}
