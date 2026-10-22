package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Remembers which tile or chip on Home had focus last, and gives back a requester for any of them,
 * so focus can return there when a panel or menu closes. Ids look like `fav:pkg:com.x`, `all:pkg:com.x`
 * or `chip:settings`.
 */
class HomeFocus {
    private val requesters = HashMap<String, FocusRequester>()

    var last: String? = null
        private set

    fun requester(id: String): FocusRequester = requesters.getOrPut(id) { FocusRequester() }

    /** Attach to the focusable with this id: provides its requester and records when it gets focus. */
    fun track(id: String): Modifier =
        Modifier.focusRequester(requester(id)).onFocusChanged { if (it.hasFocus) last = id }

    /** Tries to focus [id]; false if it is not on screen (removed, scrolled out, hidden). */
    fun request(id: String): Boolean = runCatching { requester(id).requestFocus() }.getOrDefault(false)

    fun restore(): Boolean = last?.let(::request) ?: false

    fun forget() { last = null }
}
