# Compose for TV patterns

Versions: Compose BOM 2026.09.00 (Compose 1.12.1), tv-material 1.1.0. The full list is in
`docs/tech-stack.md`. Two of Google's official skills sit next to this one in
`.claude/skills/`. **`leanback-to-compose-tv-migration`** holds Google's own Compose-for-TV rules
and reference screens, and **`jetpack-compose-performance`** holds the performance audit
rules. Read the relevant one before writing non-trivial UI.

## Rules (Google's TV guidance + this project)

- Components come from `androidx.tv.material3`: `MaterialTheme`, `Card`/`CompactCard`,
  `Surface`, `Button`, `ListItem`, `Text`, `Icon`. **Import each class explicitly**
  (`import androidx.tv.material3.Card`), never `androidx.tv.material3.*`, so mobile M3 can't
  sneak in.
- `androidx.compose.material3` is allowed **only** for `TextField`/`OutlinedTextField`
  (tv-material has none). Wrap the field in a focusable TV `Surface` (see Text input below).
- Lists: `LazyRow`/`LazyColumn`/`LazyVerticalGrid` from `androidx.compose.foundation`.
  tv-foundation's `TvLazy*` were removed. For small bounded lists (≤ 10 items, such as
  quick tiles or the language list), use a plain `Row`/`Column`.
- Every lazy item gets a primitive `key` **and** a `contentType`.
- Screen files are named after the screen (`HomeScreen.kt`, `SettingsPanel.kt`), never `Main*`.
- Overscan: root containers use the safe-area padding (48 dp horizontal, 27 dp vertical).
- No `AndroidView` wrappers.
- Initial focus on every screen and panel: a `FocusRequester` on the primary element,
  requested in `LaunchedEffect`.
- Focus memory: `Modifier.focusRestorer()` **with no arguments** on each `LazyRow`/grid row.
  Don't pass fallback requesters: requesting focus on an off-screen lazy item throws
  `IllegalStateException`. Add `@OptIn(ExperimentalComposeUiApi::class)` if the BOM still
  marks it experimental.
- Don't hard-wire `focusProperties { up = … }` targets to items inside lazy lists. Rely on the
  2D focus search.
- Panels are focus groups. When a panel opens, focus moves into it. When it closes, focus
  returns to the element that opened it.

## Theme: `ThemeSpec` through a CompositionLocal

Port all nine themes from `design/mockups/themes.css` 1:1. Token names map from kebab-case
to camelCase (`item-focus-ring` → `itemFocusRing`).

```kotlin
enum class ThemeFamily { Dark, Light, Cartoon }

@Immutable
data class LauncherColors(
    val onSurface: Color, val onSurfaceVariant: Color,
    val chipBg: Color, val chipFg: Color, val chipMeta: Color,
    val titleBg: Color, val titleFg: Color, val titleMeta: Color,
    val labelBg: Color, val labelFg: Color,
    val focus: Color, val signal: Color, val wxSun: Color, val wxCloud: Color,
    val panelBg: Color, val panelFg: Color, val panelMuted: Color,
    val itemBg: Color, val itemFocusFill: Color, val itemFocusRing: Color,
)

@Immutable
data class FocusStyle(val ring: Dp, val scale: Float, val rotation: Float, val glow: Glow?)

@Immutable
data class ThemeSpec(
    val id: String,
    val family: ThemeFamily,
    val colors: LauncherColors,
    @DrawableRes val background: Int?,   // vector art, rasterised once to 960x540
    val font: FontFamily,                // Roboto, or Nunito for cartoon
    val cardRadius: Dp,
    val cardOutline: Dp,                 // 3 dp white sticker outline in cartoon, else 0
    val focus: FocusStyle,
)

val LocalLauncherTheme = staticCompositionLocalOf<ThemeSpec> { error("No theme") }

@Composable
fun LauncherTheme(spec: ThemeSpec, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLauncherTheme provides spec) {
        androidx.tv.material3.MaterialTheme(
            colorScheme = if (spec.family == ThemeFamily.Light) lightColorScheme() else darkColorScheme(),
            typography = launcherTypography(spec.font),   // tv-material scale, font swapped
            content = content,
        )
    }
}
```

- `staticCompositionLocalOf` is used because a theme change is rare and recomposing
  everything is fine.
- In lazy items, read `LocalLauncherTheme.current` once in the parent and pass values down
  (Google's performance rule: no CompositionLocal reads inside lazy items).
- The **live preview** in the theme picker swaps the `spec` passed to `LauncherTheme` while a
  thumbnail is focused. OK persists it, and Back restores the saved spec.

## App banner card

```kotlin
@Composable
fun AppBannerCard(
    app: TvAppUi,
    theme: ThemeSpec,                    // passed down, not read from the Local per item
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(theme.cardRadius)
    var isFocused by remember { mutableStateOf(false) }
    Card(   // androidx.tv.material3.Card: owns focus, scale and border animation
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .size(160.dp, 90.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer { rotationZ = if (isFocused) theme.focus.rotation else 0f },  // cartoon tilt, read in draw
        shape = CardDefaults.shape(shape),
        scale = CardDefaults.scale(focusedScale = theme.focus.scale, pressedScale = 1.0f),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(theme.focus.ring, theme.colors.focus), shape = shape),
        ),
    ) {
        Image(
            bitmap = app.banner,               // pre-rasterised at 320×180 px off the main thread
            contentDescription = app.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

Focus state is kept per item and read inside the `graphicsLayer` lambda, so a focus change
re-draws without re-composing the row. Never keep focus in screen-level state.

## Row with focus memory

```kotlin
LazyRow(
    modifier = Modifier.focusRestorer(),
    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),   // room for ×1.12 growth
    horizontalArrangement = Arrangement.spacedBy(16.dp),
) {
    itemsIndexed(apps, key = { _, app -> app.packageName }, contentType = { _, _ -> "app" }) { i, app ->
        AppBannerCard(
            app = app, theme = theme, onClick = { onOpen(app) }, onLongClick = { onMenu(app) },
            modifier = if (i == 0) Modifier.focusRequester(firstItem) else Modifier,
        )
    }
}
```

Pivot scrolling: on TV, Compose's default `LocalBringIntoViewSpec` keeps the focused item
about 30% from the start. Override it only if the mockup asks for it:

```kotlin
class PivotSpec(private val parentFraction: Float = 0.3f, private val childFraction: Float = 0f) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - (parentFraction * containerSize - childFraction * size)
}
// CompositionLocalProvider(LocalBringIntoViewSpec provides remember { PivotSpec(0f) }) { LazyRow(...) }
```

## Home: initial focus, Back, Home key

```kotlin
@Composable
fun HomeScreen(state: HomeUiState, homePressed: Flow<Unit>, actions: HomeActions) {
    val listState = rememberLazyListState()
    val firstFavourite = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun resetToTop() = scope.launch {
        listState.scrollToItem(0)
        withFrameNanos { }                              // let the first row attach
        runCatching { firstFavourite.requestFocus() }   // never crash on a missing requester
    }

    LaunchedEffect(Unit) {                               // one effect, nested launches
        launch { snapshotFlow { state.isLoaded }.first { it }; resetToTop() }
        launch { homePressed.collect { resetToTop() } }  // onNewIntent(CATEGORY_HOME), screen on
    }
    BackHandler(enabled = state.overlay == Overlay.None) { resetToTop() }   // never exits

    LazyColumn(state = listState, contentPadding = PaddingValues(vertical = 27.dp)) {
        item(key = "top", contentType = "top") { TopBar(state.top, actions) }
        item(key = "fav", contentType = "row") { FavouritesRow(state.favourites, firstFavourite, actions) }
        items(state.gridRows, key = { it.id }, contentType = { "grid-row" }) { GridRow(it, actions) }
    }
}
```

The Activity forwards Home: `override fun onNewIntent(intent: Intent)`. If
`intent.hasCategory(Intent.CATEGORY_HOME)`, it emits into `homePressed`.

## Text input (city search)

The pattern comes from Google's TV skill. A bare `BasicTextField` doesn't open the IME on
D-pad OK, and Back would leave the screen.

```kotlin
Surface(   // androidx.tv.material3.Surface
    onClick = { fieldFocus.requestFocus() },
    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f),
    border = ClickableSurfaceDefaults.border(
        focusedBorder = Border(BorderStroke(3.dp, theme.colors.itemFocusRing)),
    ),
) {
    OutlinedTextField(   // androidx.compose.material3: the one allowed M3 component
        value = query, onValueChange = onQuery, singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .focusRequester(fieldFocus)
            .onPreviewKeyEvent { e ->   // Back leaves the field instead of the screen
                if (e.type == KeyEventType.KeyUp && (e.key == Key.Back || e.key == Key.Escape)) {
                    focusManager.moveFocus(FocusDirection.Down); true
                } else false
            },
    )
}
```

## Performance rules for this device

These are the essentials of the `jetpack-compose-performance` skill, applied here:
- Defer reads to the draw phase: animated or focus-driven values go into
  `graphicsLayer { }` or `offset { }` lambdas.
- Nothing heavy in composition: no PackageManager calls, no `Drawable.toBitmap`, no palette,
  no date formatter allocation without `remember`. Work goes to repositories on `IO`/`Default`.
- Collect flows once per screen, never per lazy item.
- Don't add `@Stable`/`@Immutable` to models that hold collections. Strong skipping already
  handles `List` by identity.
- Effects: combine `LaunchedEffect(Unit)` blocks; use `SideEffect(keys)` (Compose 1.12+) for
  synchronous work; use `rememberUpdatedState` for callbacks inside long-running effects.
- Release builds only for measuring (R8 on, Baseline Profile installed). Measure on the TV
  with `am start -W` and `dumpsys gfxinfo <pkg> framestats`.
