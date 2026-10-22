package io.github.marozauartsiom.tvlauncher.ui.components

/**
 * A long press on OK opens a menu while the key is still held. The key's repeats and its release
 * then arrive in the new menu and would choose its first item. This swallows them: everything about
 * OK is ignored until the first release, or until a fresh press (not a repeat) shows the key had
 * already been let go.
 */
class HeldKeyGuard {
    private var armed = false

    /** True if this OK/Enter event belongs to the press that opened the menu and must be ignored. */
    fun swallows(isDown: Boolean, repeatCount: Int): Boolean = when {
        armed -> false
        isDown && repeatCount == 0 -> { armed = true; false }
        !isDown -> { armed = true; true }
        else -> true
    }
}
