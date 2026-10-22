package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo

/**
 * Gives the bounds of the node it is attached to, on demand. Unlike `onGloballyPositioned` it installs no
 * layout callback, so a screen full of tiles pays nothing while scrolling; the position is read only when
 * asked (a long press).
 */
class BoundsProbe {
    private var node: ProbeNode? = null

    val modifier: Modifier = ProbeElement(this)

    /** The attached node's bounds in the window root, or null when it is not on screen. */
    fun bounds(): Rect? = node?.takeIf { it.isAttached }?.requireLayoutCoordinates()?.takeIf { it.isAttached }?.boundsInRoot()

    private class ProbeNode(var probe: BoundsProbe) : Modifier.Node() {
        override fun onAttach() { probe.node = this }
        override fun onDetach() { if (probe.node === this) probe.node = null }
    }

    private class ProbeElement(private val probe: BoundsProbe) : ModifierNodeElement<ProbeNode>() {
        override fun create() = ProbeNode(probe)
        override fun update(node: ProbeNode) { node.probe = probe }
        override fun hashCode() = System.identityHashCode(probe)
        override fun equals(other: Any?) = other is ProbeElement && other.probe === probe
        override fun InspectorInfo.inspectableProperties() { name = "boundsProbe" }
    }
}
