package io.github.marozauartsiom.tvlauncher.system.remote.control

import android.graphics.Rect
import java.util.Locale

/** What [NodeOps] needs from an on-screen element. The real one wraps `AccessibilityNodeInfo`; tests use fakes. */
interface UiNode {
    val className: String
    val text: String?
    val description: String?
    val bounds: Rect
    val isVisible: Boolean
    val isClickable: Boolean
    val isLongClickable: Boolean
    val isScrollable: Boolean
    val isEditable: Boolean
    val isFocusable: Boolean
    val isFocused: Boolean
    val children: List<UiNode>
    val parent: UiNode?
}

/**
 * Finding the element under a finger, the field to type into, and a readable outline of the screen. The TV
 * refuses injected touch gestures, so a "tap" acts on the element under the point instead.
 */
object NodeOps {
    /** The deepest visible node containing the point, searching the topmost window first. */
    fun nodeAt(roots: List<UiNode>, x: Int, y: Int): UiNode? {
        fun deepest(n: UiNode): UiNode? {
            if (!n.isVisible || !n.bounds.contains(x, y)) return null
            for (c in n.children.asReversed()) deepest(c)?.let { return it }
            return n
        }
        for (root in roots) deepest(root)?.let { return it }
        return null
    }

    fun ancestorOrSelf(node: UiNode?, predicate: (UiNode) -> Boolean): UiNode? {
        var n = node
        while (n != null && !predicate(n)) n = n.parent
        return n
    }

    /** What a tap at the point should press: the clickable node under it (or its clickable ancestor). */
    fun tapTarget(roots: List<UiNode>, x: Int, y: Int): UiNode? = ancestorOrSelf(nodeAt(roots, x, y)) { it.isClickable }

    fun longTapTarget(roots: List<UiNode>, x: Int, y: Int): UiNode? = ancestorOrSelf(nodeAt(roots, x, y)) { it.isLongClickable }

    /** What a swipe at the point scrolls; null when nothing there scrolls. */
    fun scrollTarget(roots: List<UiNode>, x: Int, y: Int): UiNode? = ancestorOrSelf(nodeAt(roots, x, y)) { it.isScrollable }
        ?: roots.firstNotNullOfOrNull { firstMatching(it) { n -> n.isScrollable } }

    /** The field to type into: the focused editable one, else the editable one under the point, else the first visible one. */
    fun editTarget(roots: List<UiNode>, x: Int?, y: Int?): UiNode? {
        roots.firstNotNullOfOrNull { firstMatching(it) { n -> n.isEditable && n.isFocused } }?.let { return it }
        if (x != null && y != null) ancestorOrSelf(nodeAt(roots, x, y)) { it.isEditable }?.let { return it }
        return roots.firstNotNullOfOrNull { firstMatching(it) { n -> n.isEditable } }
    }

    /** The clickable node whose own text or description contains [query] (case-insensitive), preferring exact matches. */
    fun findByText(roots: List<UiNode>, query: String): UiNode? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        val all = roots.flatMap { flatten(it) }.filter { it.isVisible }
        fun label(n: UiNode) = listOfNotNull(n.text, n.description).joinToString(" ").lowercase()
        val hit = all.firstOrNull { label(it).trim() == q } ?: all.firstOrNull { label(it).contains(q) } ?: return null
        return ancestorOrSelf(hit) { it.isClickable } ?: hit
    }

    fun firstMatching(n: UiNode, predicate: (UiNode) -> Boolean): UiNode? {
        if (n.isVisible && predicate(n)) return n
        for (c in n.children) firstMatching(c, predicate)?.let { return it }
        return null
    }

    fun flatten(n: UiNode): List<UiNode> = listOf(n) + n.children.flatMap { flatten(it) }

    /**
     * A compact outline of what is on screen for an AI: one line per element that has text or can be pressed,
     * with its centre as fractions of the screen (what `tap` takes) and flags. At most [maxLines] lines.
     */
    fun outline(roots: List<UiNode>, screenW: Int, screenH: Int, maxLines: Int = 150): String {
        val sb = StringBuilder()
        var lines = 0
        fun walk(n: UiNode, depth: Int) {
            if (lines >= maxLines || !n.isVisible) return
            val label = listOfNotNull(n.text?.trim()?.takeIf { it.isNotEmpty() }, n.description?.trim()?.takeIf { it.isNotEmpty() && it != n.text?.trim() }).joinToString(" / ")
            val pressable = n.isClickable || n.isEditable || n.isScrollable
            if (label.isNotEmpty() || pressable) {
                val b = n.bounds
                val cx = (b.centerX().toFloat() / screenW).coerceIn(0f, 1f)
                val cy = (b.centerY().toFloat() / screenH).coerceIn(0f, 1f)
                val flags = buildList {
                    if (n.isClickable) add("button")
                    if (n.isEditable) add("text field")
                    if (n.isScrollable) add("scrollable")
                    if (n.isFocused) add("focused")
                }.joinToString(", ")
                sb.append("  ".repeat(depth.coerceAtMost(8)))
                    .append(if (label.isEmpty()) "(${n.className.substringAfterLast('.')})" else "\"${label.take(120)}\"")
                    .append(" at ").append(String.format(Locale.US, "%.3f, %.3f", cx, cy))
                if (flags.isNotEmpty()) sb.append(" [").append(flags).append(']')
                sb.append('\n')
                lines++
            }
            n.children.forEach { walk(it, depth + 1) }
        }
        roots.forEach { walk(it, 0) }
        if (lines >= maxLines) sb.append("… (more elements not shown)\n")
        return sb.toString().ifEmpty { "(this app shows nothing the accessibility service can read; use screenshot and keys)\n" }
    }
}
