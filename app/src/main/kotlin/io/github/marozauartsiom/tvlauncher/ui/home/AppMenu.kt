package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.ui.components.HeldKeyGuard
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItem
import io.github.marozauartsiom.tvlauncher.ui.components.PanelItemHeight
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec

enum class MenuAction { ToggleFavourite, Move, Hide, Info, Uninstall }

/** What the menu is about. [anchor] is the tile's rectangle on screen in pixels. */
data class AppMenuState(
    val tile: HomeTile,
    val isFavourite: Boolean,
    val anchor: androidx.compose.ui.geometry.Rect?,
)

private val MenuWidth = 290.dp
private val MenuPadding = 8.dp
private val ItemGap = 4.dp

private data class MenuEntry(val action: MenuAction, val titleRes: Int, val icon: ImageVector)

private fun entries(state: AppMenuState): List<MenuEntry> = buildList {
    add(
        if (state.isFavourite) MenuEntry(MenuAction.ToggleFavourite, R.string.menu_remove_favourite, Icons.StarOutline)
        else MenuEntry(MenuAction.ToggleFavourite, R.string.menu_add_favourite, Icons.Star),
    )
    if (state.isFavourite) add(MenuEntry(MenuAction.Move, R.string.menu_move, Icons.Move))
    if (state.tile.app != null) {
        add(MenuEntry(MenuAction.Hide, R.string.menu_hide, Icons.Hidden))
        add(MenuEntry(MenuAction.Info, R.string.menu_app_info, Icons.Info))
        add(MenuEntry(MenuAction.Uninstall, R.string.menu_uninstall, Icons.Delete))
    }
}

/**
 * The long-press menu: a 290 dp popup next to the tile, under it, or above it when there is no room.
 * Focus starts on the first item and stays in the menu; Back closes it (handled by the shell).
 *
 * The OK key that opened the menu is probably still held, and its repeats and release arrive here.
 * They are swallowed until the first release or the first fresh press, so the first item is not
 * chosen by accident.
 */
@Composable
fun AppMenu(
    state: AppMenuState,
    theme: ThemeSpec,
    onAction: (MenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = remember(state.tile.key, state.isFavourite) { entries(state) }
    val first = remember { FocusRequester() }
    val heldKey = remember { HeldKeyGuard() }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        withFrameNanos { } // the items are composed during layout, a frame later than this effect
        runCatching { first.requestFocus() }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .onPreviewKeyEvent { event ->
                val ok = event.key == Key.Enter || event.key == Key.DirectionCenter || event.key == Key.NumPadEnter
                ok && heldKey.swallows(event.type == KeyEventType.KeyDown, event.nativeKeyEvent.repeatCount)
            },
    ) {
        val height = PanelItemHeight * items.size + ItemGap * (items.size - 1) + MenuPadding * 2
        val (x, y) = menuPosition(state.anchor, density.density, maxWidth.value, maxHeight.value, height.value)
        Column(
            modifier = Modifier
                .offset(x.dp, y.dp)
                .width(MenuWidth)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .background(theme.colors.panelBg, RoundedCornerShape(16.dp))
                .padding(MenuPadding)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(ItemGap),
        ) {
            items.forEachIndexed { index, entry ->
                PanelItem(
                    title = stringResource(entry.titleRes),
                    theme = theme,
                    icon = entry.icon,
                    transparent = true,
                    onClick = { onAction(entry.action) },
                    modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}

/**
 * Top-left corner of the menu in dp. Below the tile by default, above it when it would run past the
 * bottom safe area; horizontally aligned with the tile and kept 16 dp inside the screen.
 */
internal fun menuPosition(anchor: androidx.compose.ui.geometry.Rect?, density: Float, screenW: Float, screenH: Float, menuH: Float): Pair<Float, Float> {
    val menuW = MenuWidth.value
    if (anchor == null) return (screenW - menuW) / 2f to (screenH - menuH) / 2f
    val left = anchor.left / density
    val top = anchor.top / density
    val bottom = anchor.bottom / density
    val x = (left - 8f).coerceIn(16f, (screenW - menuW - 16f).coerceAtLeast(16f))
    val below = bottom + 8f // for a favourite the anchor already includes its name pill
    val y = if (below + menuH <= screenH - 27f) below else (top - 8f - menuH).coerceAtLeast(27f)
    return x to y
}
