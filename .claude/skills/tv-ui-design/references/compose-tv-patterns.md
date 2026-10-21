# Compose for TV patterns

Use `androidx.tv.material3.*` for themed and focusable components (`MaterialTheme`, `Card`,
`Surface`, `Button`, `Text`, `ListItem`, `NavigationDrawer`, `TabRow`, `Carousel`). Use
`androidx.compose.foundation.lazy.*` for lists. Don't mix in `androidx.compose.material3`
components: they have touch semantics and no TV focus states.

Focus APIs have moved between Compose releases (`focusRestorer` parameters, and
`focusProperties` `enter`/`exit` renamed to `onEnter`/`onExit`). Check signatures against
the Compose BOM in use before copying.

## Theme from tokens

```kotlin
object Tokens {
    val Background = Color(0xFF0B0D12)
    val Surface = Color(0xFF161A22)
    val SurfaceVariant = Color(0xFF1E232D)
    val OnSurface = Color(0xFFE8EAF0)
    val OnSurfaceVariant = Color(0xFFA3A9B7)
    val Focus = Color(0xFFFFFFFF)
    val OnFocus = Color(0xFF0B0D12)
    val Accent = Color(0xFF8AB4F8)
    val Signal = Color(0xFF7BD88F)

    val SafeHorizontal = 48.dp
    val SafeVertical = 27.dp
    val CardGap = 16.dp
    val BannerWidth = 160.dp
    val BannerHeight = 90.dp
    val CardRadius = 12.dp

    const val FocusInMs = 150
    const val FocusOutMs = 120
}

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(   // androidx.tv.material3.MaterialTheme
        colorScheme = darkColorScheme(   // androidx.tv.material3.darkColorScheme
            background = Tokens.Background,
            surface = Tokens.Surface,
            surfaceVariant = Tokens.SurfaceVariant,
            onSurface = Tokens.OnSurface,
            onSurfaceVariant = Tokens.OnSurfaceVariant,
            primary = Tokens.Accent,
            border = Tokens.Focus,
        ),
        content = content,
    )
}
```

Keep `design/mockups/tokens.css` (see mockups.md) in sync with `Tokens`. When a value
changes, change it in both places in the same commit.

## App banner card

```kotlin
@Composable
fun AppBannerCard(
    app: TvAppUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Tokens.CardRadius)
    Card(   // androidx.tv.material3.Card: owns focus, scale and border animation
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(Tokens.BannerWidth, Tokens.BannerHeight),
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = 1.1f, pressedScale = 1.0f),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Tokens.Focus), shape = shape),
        ),
        colors = CardDefaults.colors(containerColor = Tokens.SurfaceVariant),
    ) {
        Image(
            bitmap = app.banner,               // pre-rasterised at 320×180 px, never decoded here
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

The focus state comes from the Card's own interaction source, so focus changes don't
recompose the parent row. If you need to know about focus outside the card (to show the
label under it, for example), hoist it with `Modifier.onFocusChanged` and keep the state
per item. Never keep it in the screen-level state.

## Row with focus memory

```kotlin
@Composable
fun AppRow(
    title: String,
    apps: List<TvAppUi>,
    onOpen: (TvAppUi) -> Unit,
    onMenu: (TvAppUi) -> Unit,
    modifier: Modifier = Modifier,
    firstItemRequester: FocusRequester? = null,
) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = Tokens.OnSurface,
            modifier = Modifier.padding(start = Tokens.SafeHorizontal, bottom = 12.dp),
        )
        LazyRow(
            modifier = Modifier.focusRestorer(),   // Up/Down returns to the last focused card
            contentPadding = PaddingValues(horizontal = Tokens.SafeHorizontal, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(Tokens.CardGap),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }, contentType = { _, _ -> "app" }) { i, app ->
                AppBannerCard(
                    app = app,
                    onClick = { onOpen(app) },
                    onLongClick = { onMenu(app) },
                    modifier = if (i == 0 && firstItemRequester != null) {
                        Modifier.focusRequester(firstItemRequester)
                    } else Modifier,
                )
            }
        }
    }
}
```

- Always give lazy items stable `key`s, so focus and scroll survive list refreshes.
- Pivot scrolling: on TV devices, Compose's default `LocalBringIntoViewSpec` keeps the
  focused item at about 30% of the row. To change it, provide a custom
  `BringIntoViewSpec` through `LocalBringIntoViewSpec`:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
class PivotSpec(private val parentFraction: Float = 0.3f, private val childFraction: Float = 0f) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - (parentFraction * containerSize - childFraction * size)
}
// CompositionLocalProvider(LocalBringIntoViewSpec provides remember { PivotSpec(0f) }) { LazyRow(...) }
```

## Screen: initial focus, Back, Home

```kotlin
@Composable
fun HomeScreen(state: HomeUiState, homePressed: Flow<Unit>, actions: HomeActions) {
    val listState = rememberLazyListState()
    val firstApp = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun resetToTop() = scope.launch {
        listState.scrollToItem(0)
        withFrameNanos { }                       // let the first row attach
        runCatching { firstApp.requestFocus() }  // requester may not be attached yet
    }

    LaunchedEffect(state.apps.isNotEmpty()) { if (state.apps.isNotEmpty()) resetToTop() }
    LaunchedEffect(homePressed) { homePressed.collect { resetToTop() } }   // from onNewIntent(CATEGORY_HOME)
    BackHandler { resetToTop() }                 // the launcher never finishes on Back

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(Tokens.Background),
        contentPadding = PaddingValues(vertical = Tokens.SafeVertical),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item(key = "top") { TopBar(actions) }
        item(key = "favorites") { AppRow("Favourites", state.favorites, actions::open, actions::menu, firstItemRequester = firstApp) }
        item(key = "all") { AppRow("Apps", state.apps, actions::open, actions::menu) }
    }
}
```

In the Activity, forward Home presses:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.onHomePressed()   // emits into homePressed
}
```

## Performance rules for this device

- Read state as late as possible. Pass lambdas (`{ scale }`) into `graphicsLayer { }`
  instead of reading animated values during composition.
- Nothing heavy in composition: no PackageManager calls, no `Drawable.toBitmap`, no
  palette extraction. Do all of it in the repository on `Dispatchers.IO` or `Default`.
- Don't create `Brush` objects or other expensive things on every recomposition. `remember` them.
- Nested scrolling is fine as LazyColumn of LazyRows. Don't nest same-direction lazies.
- Add a Baseline Profile (`androidx.baselineprofile`, `profileinstaller`) once the home
  screen settles. It noticeably cuts cold start and first-scroll jank on a quad A55.
- Measure on the device: `adb shell am start -W` for start time, and
  `dumpsys gfxinfo <pkg> framestats` for jank.
