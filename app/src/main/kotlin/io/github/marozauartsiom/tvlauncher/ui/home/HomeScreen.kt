package io.github.marozauartsiom.tvlauncher.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
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
import io.github.marozauartsiom.tvlauncher.ui.components.Icons
import io.github.marozauartsiom.tvlauncher.ui.components.rememberFocusProgress
import io.github.marozauartsiom.tvlauncher.ui.theme.ThemeSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What Home can ask the rest of the launcher to do. */
@Immutable
class HomeActions(
    val onOpen: (HomeTile) -> Unit,
    val onMenu: (HomeTile) -> Unit,
    val onWeather: () -> Unit,
    val onHdmi: (String) -> Unit,
    val onIphone: () -> Unit,
    val onSettings: () -> Unit,
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
) {
    val listState = rememberLazyListState()
    val favouritesState = rememberLazyListState()
    val firstFavourite = remember { FocusRequester() }
    val firstApp = remember { FocusRequester() }
    val current by rememberUpdatedState(content)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    suspend fun resetToTop() {
        val hasFavourites = current.favourites.isNotEmpty() && !safeMode
        listState.scrollToItem(0)
        if (hasFavourites) favouritesState.scrollToItem(0) // an unattached state would wait forever
        withFrameNanos { } // let the first items attach before asking for focus
        val target = if (hasFavourites) firstFavourite else firstApp
        runCatching { target.requestFocus() } // a missing requester must never crash Home
    }

    LaunchedEffect(Unit) {
        scope.launch { snapshotFlow { !current.isEmpty }.first { it }; resetToTop() }
        scope.launch { resets.collect { resetToTop() } }
    }

    val favourites = if (safeMode) emptyList() else content.favourites
    val rows = remember(content.all) { content.all.chunked(GridColumns) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
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
                        FavouritesRow(favourites, theme, art, actions, favouritesState, firstFavourite)
                    }
                }
            }
        }
        item(key = "all-title", contentType = "title") {
            Box(Modifier.padding(top = 12.dp)) {
                SectionTitle(stringResource(R.string.all_apps), count = content.all.size, theme = theme)
            }
        }
        itemsIndexed(rows, key = { _, row -> row.first().key.value }, contentType = { _, _ -> "grid-row" }) { index, row ->
            GridRow(row, theme, art, actions, firstApp.takeIf { index == 0 })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FavouritesRow(
    tiles: List<HomeTile>,
    theme: ThemeSpec,
    art: ArtSource,
    actions: HomeActions,
    state: androidx.compose.foundation.lazy.LazyListState,
    first: FocusRequester,
) {
    LazyRow(
        state = state,
        modifier = Modifier.focusRestorer(),
        contentPadding = PaddingValues(start = SafeHorizontal, end = SafeHorizontal, top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        itemsIndexed(tiles, key = { _, tile -> tile.key.value }, contentType = { _, _ -> "tile" }) { index, tile ->
            FavouriteItem(tile, theme, art, actions, if (index == 0) first else null)
        }
    }
}

/** A favourite: the card, and under it the name pill, which is visible only while the card has focus. */
@Composable
private fun FavouriteItem(tile: HomeTile, theme: ThemeSpec, art: ArtSource, actions: HomeActions, requester: FocusRequester?) {
    val focus = rememberFocusProgress()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TileCard(
            tile = tile,
            theme = theme,
            art = art,
            onClick = { actions.onOpen(tile) },
            onLongClick = { actions.onMenu(tile) },
            modifier = (if (requester != null) Modifier.focusRequester(requester) else Modifier).testTag("fav:${tile.key.value}"),
            focus = focus,
        )
        Box(Modifier.height(20.dp).graphicsLayer { alpha = focus.value }) {
            val pill = theme.hasLabelPill
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelLarge,
                color = theme.colors.labelFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = CardWidth)
                    .then(
                        if (pill) Modifier.background(theme.colors.labelBg, RoundedCornerShape(10.dp)) else Modifier,
                    )
                    .padding(horizontal = theme.labelPaddingHorizontal),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GridRow(
    tiles: List<HomeTile>,
    theme: ThemeSpec,
    art: ArtSource,
    actions: HomeActions,
    firstOfGrid: FocusRequester?,
) {
    Row(
        modifier = Modifier.focusRestorer().padding(start = SafeHorizontal, end = SafeHorizontal, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        tiles.forEachIndexed { column, tile ->
            TileCard(
                tile = tile,
                theme = theme,
                art = art,
                onClick = { actions.onOpen(tile) },
                onLongClick = { actions.onMenu(tile) },
                modifier = (if (column == 0 && firstOfGrid != null) Modifier.focusRequester(firstOfGrid) else Modifier)
                    .testTag("all:${tile.key.value}"),
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
