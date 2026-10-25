package io.github.marozauartsiom.tvlauncher.system.remote

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.marozauartsiom.tvlauncher.system.remote.control.NodeOps
import io.github.marozauartsiom.tvlauncher.system.remote.control.UiNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NodeOpsTest {
    private class Node(
        override val className: String,
        override val text: String? = null,
        override val description: String? = null,
        l: Int, t: Int, r: Int, b: Int,
        override val isClickable: Boolean = false,
        override val isLongClickable: Boolean = false,
        override val isScrollable: Boolean = false,
        override val isEditable: Boolean = false,
        override val isFocused: Boolean = false,
        override val isVisible: Boolean = true,
        kids: List<Node> = emptyList(),
    ) : UiNode {
        override val bounds = Rect(l, t, r, b)
        override val isFocusable get() = isClickable || isEditable
        override val children: List<UiNode> = kids
        override var parent: UiNode? = null
        init { kids.forEach { it.parent = this } }
    }

    private val label = Node("TextView", text = "Синьора Вольпе", l = 110, t = 210, r = 290, b = 240)
    private val card = Node("FrameLayout", l = 100, t = 100, r = 300, b = 250, isClickable = true, isLongClickable = true, kids = listOf(label))
    private val search = Node("EditText", description = "Search", l = 0, t = 0, r = 960, b = 60, isEditable = true)
    private val hidden = Node("Button", text = "Hidden", l = 100, t = 100, r = 300, b = 250, isClickable = true, isVisible = false)
    private val list = Node("RecyclerView", l = 0, t = 80, r = 960, b = 540, isScrollable = true, kids = listOf(card, hidden))
    private val root = Node("FrameLayout", l = 0, t = 0, r = 960, b = 540, kids = listOf(search, list))
    private val roots = listOf(root)

    @Test fun tapPressesTheClickableAncestorAndSkipsHiddenNodes() {
        assertEquals(label, NodeOps.nodeAt(roots, 150, 220))
        assertEquals(card, NodeOps.tapTarget(roots, 150, 220))
        assertEquals(card, NodeOps.longTapTarget(roots, 150, 220))
        assertNull(NodeOps.tapTarget(roots, 700, 400))
    }

    @Test fun topmostWindowWins() {
        val dialog = Node("Button", text = "OK", l = 0, t = 0, r = 960, b = 540, isClickable = true)
        assertEquals(dialog, NodeOps.tapTarget(listOf(dialog, root), 150, 220))
    }

    @Test fun scrollFindsTheScrollableUnderTheFingerOrAnyOther() {
        assertEquals(list, NodeOps.scrollTarget(roots, 150, 220))
        assertEquals(list, NodeOps.scrollTarget(roots, 10, 10))
    }

    @Test fun typingGoesToTheFocusedFieldFirst() {
        assertEquals(search, NodeOps.editTarget(roots, null, null))
        val focused = Node("EditText", l = 0, t = 300, r = 100, b = 340, isEditable = true, isFocused = true)
        val two = listOf(Node("FrameLayout", l = 0, t = 0, r = 960, b = 540, kids = listOf(search, focused)))
        assertEquals(focused, NodeOps.editTarget(two, 10, 10))
    }

    @Test fun findByTextReturnsWhatToPress() {
        assertEquals(card, NodeOps.findByText(roots, "синьора"))
        assertEquals(search, NodeOps.findByText(roots, "SEARCH"))
        assertNull(NodeOps.findByText(roots, "Hidden"))
        assertNull(NodeOps.findByText(roots, " "))
    }

    @Test fun outlineListsLabelsWithCentresAndFlags() {
        val out = NodeOps.outline(roots, 960, 540)
        assertTrue(out, out.contains("\"Search\" at 0.500, 0.056 [button, text field]".replace("button, ", "")))
        assertTrue(out, out.contains("\"Синьора Вольпе\" at 0.208, 0.417"))
        assertTrue(out, out.contains("(FrameLayout) at 0.208, 0.324 [button]"))
        assertTrue(!out.contains("Hidden"))
        assertTrue(NodeOps.outline(roots, 960, 540, maxLines = 1).endsWith("(more elements not shown)\n"))
    }
}
