package io.github.marozauartsiom.tvlauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.marozauartsiom.tvlauncher.R
import io.github.marozauartsiom.tvlauncher.data.apps.ArtSource
import io.github.marozauartsiom.tvlauncher.data.prefs.ItemKey
import io.github.marozauartsiom.tvlauncher.ui.components.FocusMemory
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.rememberFocusProgress
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What Home can ask the rest of the launcher to do. */
@Immutable
data class HomeActions(
    val onOpen: (HomeTile) -> Unit = {},
    /** Long press on a tile; the rectangle is where it is on screen, for placing the menu. */
    val onMenu: (HomeTile, Rect?) -> Unit = { _, _ -> },
    val onWeather: () -> Unit = {},
    val onHdmi: (String) -> Unit = {},
    val onIphone: () -> Unit = {},
    val onSettings: () -> Unit = {},
    /** Move mode: swap the tile one place left (-1) or right (+1). */
    val onMoveStep: (ItemKey, Int) -> Unit = { _, _ -> },
    val onMoveDone: () -> Unit = {},
    val onMoveCancel: () -> Unit = {},
)

private const val GridColumns = 5
private val CardGap = 16.dp

/**
 * The home screen: top bar, Favourites (manual order) and All apps (5 columns).
 *
 * Focus: the first favourite (or the first app when there are none) gets focus when the first tiles
 * appear and again on every reset in [resets] (Home key, screen on, Back at the top). The favourites
 * row and each grid row remember their last item with `focusRestorer`.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    content: HomeContent,
    theme: ThemeSpec,
    topBar: TopBarState,
    clock: () -> ClockText,
    art: ArtSource,
    resets: Flow<Unit>,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    safeMode: Boolean = false,
    /** A panel or menu is open over Home: Home cannot take focus, and gets it back when this turns false. */
    overlayOpen: Boolean = false,
    /** The favourite being moved (move mode), or null. */
    moveKey: ItemKey? = null,
) {
    val listState = rememberLazyListState()
    val favouritesState = rememberLazyListState()
    val focus = remember { FocusMemory() }
    val current by rememberUpdatedState(content)
    val overlayNow by rememberUpdatedState(overlayOpen)
    val resetWhileCovered = remember { BooleanArray(1) } // a reset arrived while a panel was open
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    suspend fun resetToTop() {
        val hasFavourites = current.favourites.isNotEmpty() && !safeMode
        listState.scrollToItem(0)
        if (hasFavourites) favouritesState.scrollToItem(0) // an unattached state would wait forever
        withFrameNanos { } // let the first items attach before asking for focus
        if (overlayNow) { resetWhileCovered[0] = true; return } // the panel keeps focus; Home takes it when the panel closes
        val target = if (hasFavourites) favouriteId(current.favourites.first()) else gridId(current.all.firstOrNull())
        if (target != null) focus.request(target) // a missing requester must never crash Home
    }

    LaunchedEffect(Unit) {
        scope.launch { snapshotFlow { !current.isEmpty }.first { it }; resetToTop() }
        scope.launch { resets.collect { resetToTop() } }
    }

    // When a panel or menu closes, focus goes back to where it was; if that is gone, to the top.
    var wasOverlayOpen by remember { mutableStateOf(overlayOpen) }
    LaunchedEffect(overlayOpen) {
        if (wasOverlayOpen && !overlayOpen) {
            withFrameNanos { }
            val reset = resetWhileCovered[0] // the Home key was pressed while the panel was open
            resetWhileCovered[0] = false
            if (reset || !focus.restore()) resetToTop()
        }
        wasOverlayOpen = overlayOpen
    }

    BackHandler(enabled = moveKey != null) { actions.onMoveCancel() }

    // The tile a menu is open for keeps its focus look while focus is in the menu.
    val pinnedId = remember(overlayOpen) { if (overlayOpen) focus.last else null }

    val favourites = if (safeMode) emptyList() else content.favourites
    val rows = remember(content.all) { content.all.chunked(GridColumns) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().focusProperties { canFocus = !overlayOpen },
        contentPadding = PaddingValues(bottom = SafeVertical),
    ) {
        item(key = "top", contentType = "top") {
            TopBar(
                state = if (safeMode) TopBarState(showIphone = false) else topBar,
                clock = clock,
                theme = theme,
                onWeather = actions.onWeather,
                onHdmi = actions.onHdmi,
                onIphone = actions.onIphone,
                onSettings = actions.onSettings,
                focus = focus,
            )
        }
        if (safeMode) {
            item(key = "safe", contentType = "note") {
                Text(
                    text = stringResource(R.string.safe_mode_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = SafeHorizontal, end = SafeHorizontal, top = 16.dp),
                )
            }
        } else {
            item(key = "favourites", contentType = "favourites") {
                Column(Modifier.padding(top = 24.dp)) {
                    SectionTitle(stringResource(R.string.favourites), count = null, theme = theme)
                    if (favourites.isEmpty()) {
                        EmptyFavouritesHint(theme)
                    } else {
                        FavouritesRow(favourites, theme, art, actions, favouritesState, focus, moveKey, pinnedId)
                    }
                }
            }
        }
        item(key = "all-title", contentType = "title") {
            Box(Modifier.padding(top = 12.dp)) {
                SectionTitle(stringResource(R.string.all_apps), count = content.all.size, theme = theme)
            }
        }
        items(rows, key = { row -> row.first().key.value }, contentType = { "grid-row" }) { row ->
            GridRow(row, theme, art, actions, focus, pinnedId)
        }
    }
}

internal fun favouriteId(tile: HomeTile) = "fav:${tile.key.value}"
internal fun gridId(tile: HomeTile?) = tile?.let { "all:${it.key.value}" }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FavouritesRow(
    tiles: List<HomeTile>,
    theme: ThemeSpec,
    art: ArtSource,
    actions: HomeActions,
    state: androidx.compose.foundation.lazy.LazyListState,
    focus: FocusMemory,
    moveKey: ItemKey?,
    pinnedId: String?,
) {
    // In move mode the tile follows the owner's presses, so keep it on screen as it changes place.
    val movingIndex = tiles.indexOfFirst { it.key == moveKey }
    LaunchedEffect(moveKey, movingIndex) {
        if (movingIndex >= 0 && state.layoutInfo.visibleItemsInfo.none { it.index == movingIndex }) state.animateScrollToItem(movingIndex)
    }
    LazyRow(
        state = state,
        modifier = Modifier.focusRestorer(),
        contentPadding = PaddingValues(start = SafeHorizontal, end = SafeHorizontal, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        items(tiles, key = { tile -> tile.key.value }, contentType = { "tile" }) { tile ->
            FavouriteItem(tile, theme, art, actions, focus, moving = tile.key == moveKey, pinned = favouriteId(tile) == pinnedId)
        }
    }
}

/** A favourite: the card, and under it the name pill, which is visible only while the card has focus. */
@Composable
private fun FavouriteItem(
    tile: HomeTile,
    theme: ThemeSpec,
    art: ArtSource,
    actions: HomeActions,
    homeFocus: FocusMemory,
    moving: Boolean,
    pinned: Boolean,
) {
    val focus = rememberFocusProgress()
    val labelGap = with(LocalDensity.current) { (10 + 20 - 8).dp.toPx() } // the menu opens below the name pill, 8 dp further
    val id = favouriteId(tile)
    // Entering move mode from the grid or the menu: take focus on this tile.
    LaunchedEffect(moving) { if (moving) homeFocus.request(id) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TileCard(
            tile = tile,
            theme = theme,
            art = art,
            onClick = { actions.onOpen(tile) },
            onLongClick = { rect -> actions.onMenu(tile, rect?.let { it.copy(bottom = it.bottom + labelGap) }) },
            modifier = homeFocus.track(id)
                .then(if (moving) Modifier.moveKeys(tile.key, actions) else Modifier)
                .testTag(id),
            focus = focus,
            moving = moving,
            pinned = pinned,
        )
        Box(Modifier.height(20.dp).graphicsLayer { alpha = if (moving || pinned) 1f else focus.value }) {
            if (moving) MovePill(theme) else NamePill(tile.label, theme)
        }
    }
}

@Composable
private fun NamePill(label: String, theme: ThemeSpec) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = theme.colors.labelFg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = CardWidth)
            .then(if (theme.hasLabelPill) Modifier.background(theme.colors.labelBg, RoundedCornerShape(10.dp)) else Modifier)
            .padding(horizontal = theme.labelPaddingHorizontal),
    )
}

/** "◀ ▶ Move · OK to finish" in the theme's focus colour, instead of the name. */
@Composable
private fun MovePill(theme: ThemeSpec) {
    Text(
        text = stringResource(R.string.move_hint),
        style = MaterialTheme.typography.labelLarge,
        color = theme.colors.buttonFg,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .background(theme.colors.focus, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp)
            .wrapContentWidth(unbounded = true),
    )
}

/** Move mode on the moving tile: Left/Right swap places, OK drops it, Up/Down stay put. */
private fun Modifier.moveKeys(key: ItemKey, actions: HomeActions): Modifier = onPreviewKeyEvent { event ->
    val ok = event.key == Key.Enter || event.key == Key.DirectionCenter || event.key == Key.NumPadEnter
    when {
        event.key == Key.DirectionLeft -> { if (event.type == KeyEventType.KeyDown) actions.onMoveStep(key, -1); true }
        event.key == Key.DirectionRight -> { if (event.type == KeyEventType.KeyDown) actions.onMoveStep(key, +1); true }
        event.key == Key.DirectionUp || event.key == Key.DirectionDown -> true
        ok -> { if (event.type == KeyEventType.KeyUp) actions.onMoveDone(); true }
        else -> false
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GridRow(
    tiles: List<HomeTile>,
    theme: ThemeSpec,
    art: ArtSource,
    actions: HomeActions,
    homeFocus: FocusMemory,
    pinnedId: String?,
) {
    Row(
        modifier = Modifier.focusRestorer().padding(start = SafeHorizontal, end = SafeHorizontal, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        tiles.forEach { tile ->
            val id = gridId(tile)!!
            TileCard(
                tile = tile,
                theme = theme,
                art = art,
                onClick = { actions.onOpen(tile) },
                onLongClick = { rect -> actions.onMenu(tile, rect) },
                modifier = homeFocus.track(id).testTag(id),
                pinned = id == pinnedId,
            )
        }
    }
}

/** "Favourites" / "All apps · 14". A plain title, or a pill in the cartoon themes. */
@Composable
private fun SectionTitle(title: String, count: Int?, theme: ThemeSpec) {
    val shape = remember { RoundedCornerShape(percent = 50) }
    val size = if (theme.titleUppercase) 14.sp else 18.sp
    Row(
        modifier = Modifier
            .padding(start = SafeHorizontal, bottom = 12.dp)
            .then(if (theme.hasTitlePill) Modifier.background(theme.colors.titleBg, shape) else Modifier)
            .padding(theme.titlePadding)
            .heightIn(min = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val style = MaterialTheme.typography.titleMedium.copy(
            fontSize = size,
            lineHeight = 24.sp,
            fontWeight = theme.titleWeight,
            letterSpacing = theme.titleTrackingEm.em,
        )
        Text(if (theme.titleUppercase) title.uppercase() else title, style = style, color = theme.colors.titleFg, maxLines = 1)
        if (count != null) {
            Text(
                stringResource(R.string.all_apps_count, count),
                style = style.copy(fontWeight = FontWeight.Normal),
                color = theme.colors.titleMeta,
                maxLines = 1,
            )
        }
    }
}

/** Shown in the Favourites row when it is empty. Not focusable: focus goes to the first app below. */
@Composable
private fun EmptyFavouritesHint(theme: ThemeSpec) {
    val shape = remember(theme.cardRadius) { RoundedCornerShape(theme.cardRadius) }
    val c = theme.colors
    Row(
        modifier = Modifier
            .padding(start = SafeHorizontal, top = 8.dp)
            .size(width = 864.dp, height = CardHeight)
            .background(c.chipBg, shape)
            .drawBehind { drawRing(c.chipMeta, 2.dp.toPx(), theme.cardRadius.toPx(), inside = true) }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Star, contentDescription = null, tint = c.chipFg, modifier = Modifier.size(32.dp))
        Column {
            Text(stringResource(R.string.favourites_empty_title), style = MaterialTheme.typography.titleMedium, color = c.chipFg, maxLines = 1)
            Text(stringResource(R.string.favourites_empty_hint), style = MaterialTheme.typography.bodyMedium, color = c.chipMeta, maxLines = 2)
        }
    }
}
