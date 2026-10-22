# Progress log

One line per finished task: date, task id, what was done, anything notable.
`docs/tasks.md` has the checkboxes; this file has the story.

## Log

- 2026-10-22 — Planning done: requirements, UI spec, tech stack, SOW and task list written;
  mockups for Home (9 themes), HDMI prompt, weather, settings, theme and language pickers
  approved by the owner.
- 2026-10-22 — T01 `scripts/setup-android-sdk.sh` (idempotent; platform 37.0, build-tools 36.0.0).
- 2026-10-22 — T02 versions re-verified, no changes.
- 2026-10-22 — T03 Gradle scaffold (wrapper 9.8.2, AGP 9.4.1, Kotlin 2.4.21, catalog, `:app`).
  `./gradlew :app:assembleDebug` is green. The wrapper must be bootstrapped in an empty folder
  because the preinstalled Gradle 8.14 can't configure AGP 9.4. Added `scripts/gradlew-retry.sh`
  for Maven Central HTTP 429 responses.
- 2026-10-22 — T04 manifest (HOME + LEANBACK_LAUNCHER, queries, singleTask, landscape, no
  permissions yet), adaptive icon, 320×180 banner rendered from `design/mockups/brand/banner.html`.
  The renderer gained `--size=WxH --scale=N`. Intent-security notes for later: never forward
  incoming intents; the installer's status `PendingIntent` must be mutable (system fills extras),
  so keep it explicit and package-bound.
- 2026-10-22 — T05 test infra: Robolectric 4.17 + Roborazzi 1.77 + Compose UI tests; `TvScreenshotTest`
  base class (960×540 dp, television, xhdpi → 1920×1080 PNG); smoke golden recorded and viewed;
  GitHub Actions workflow `.github/workflows/android.yml`. Gotchas: Robolectric needs JDK
  `--add-opens` flags (set in `app/build.gradle.kts`), and its android-all jar download dies on
  Maven Central HTTP 429, so use `scripts/prefetch-robolectric.sh <version>` once per session.
  M0 complete.
- 2026-10-22 — T06 ten new screens designed and rendered in midnight/sky/valley (all viewed):
  empty-favourites hint, app menu + move mode, city search, background picker, hidden apps, USB
  installer (4 states), About, theme schedule, HDMI settings, iPhone hint. `ui-spec.md` §5 and §9
  updated. M1 complete.
- 2026-10-22 — T07 theme engine: `ThemeSpec` and nine themes generated from `themes.css` values
  (`Themes.kt`), `LauncherTheme` (tv-material scheme and typography, Nunito variable font bundled),
  `ThemedBackground` + `BackgroundCache` (960×540 decode, two bitmaps max, 300 ms crossfade),
  `themeShadow`, WebP backgrounds and thumbnails (~440 KB), `ThemeCssSyncTest` guards drift against
  the CSS, nine-theme sample screenshots viewed and match the mockups.
- 2026-10-22 — Gap found: shortcut tiles (approved feature 4) were not in the task list. Added T13b.
- 2026-10-22 — T08 `Settings` (one Preferences DataStore, typed `SettingsData`, favourites as ordered
  `ItemKey`s `pkg:`/`url:`, hidden set, launch counters, theme/schedule/background/language,
  weather location and cache, HDMI options). Damaged values fall back to defaults. 8 tests.
- 2026-10-22 — T09 languages: 136 string entries in be/ru/en/pl/de/fr via `scripts/add-strings.py`
  (add strings for ALL languages with it), `StringsConsistencyTest` (same keys, placeholders and
  complete plural forms), `LocaleController` (platform per-app locale on API 33+, context wrapping
  + recreate below; SharedPreferences mirror for `attachBaseContext`), `DateText`, and a six-language
  screenshot sample (viewed: nothing clipped).
- 2026-10-22 — T10 app shell: `LauncherApp`/`AppGraph` (manual DI, all lazy), `LauncherActivity`
  (locale wrap, HOME `onNewIntent`, screen-on reset via `ScreenOnWatcher`), `Overlay` state machine +
  `LauncherViewModel` (Back never exits), `CrashGuard` (two crashes in ten minutes → safe mode),
  `LauncherRoot` placeholder. 10 tests. M2 complete.
- 2026-10-22 — T11 apps: `PackageSource` interface (+ `PackageManagerSource`), `AppLists` (dedupe with
  leanback priority, own package excluded, hidden filter, usage sort with locale collator),
  `AppRepository` (JSON snapshot for instant start, 500 ms debounced package events, no polling,
  failing query keeps the last list), `AppLauncher` (leanback intent → launch intent → NotFound,
  platform errors mapped), `BannerLoader` (320×180 tiles, generated tinted tile for icon-only apps,
  16 MB LRU by bytes). 19 tests. Caches are freed at TRIM_MEMORY_BACKGROUND, not on every UI_HIDDEN,
  so Home reappears instantly after an app.
- 2026-10-22 — T12 Home: `HomeScreen` (LazyColumn: top bar, favourites LazyRow, 5-column grid rows), `TileCard`
  (theme frame: sticker outline + hard shadow at rest, ring/tilt/glow in focus, all driven by an animated
  focus value read only in draw lambdas), `TopBar` (clock via `ACTION_TIME_TICK` only while started, chips),
  `HomeContentSource` (apps + settings → tiles), empty-favourites hint, safe-mode screen. 13 behaviour tests
  (initial focus, D-pad moves, focus memory in both rows, reset, OK vs long OK, empty hint, safe mode) and
  Roborazzi Home in 9 themes + empty favourites + `be`/`fr`; all viewed and compared with `design/mockups/themes/`
  (within 1–2 px; soft shadows are not drawn by Robolectric, they show on the TV).
- 2026-10-22 — T13 app menu and move mode: `AppMenu` (290 dp popup below the tile, flips above near the bottom;
  Home cannot take focus while it is open and gets it back on close via `HomeFocus`), `FavouritesEditor` (add,
  remove, swap with the visible neighbour, restore on Back; every step is written at once), `PackageActions`
  (app details and uninstall with `REQUEST_DELETE_PACKAGES`), `PanelItem` (the list row reused by Settings).
  Held-OK guard: the OK press that opened the menu cannot choose its first item. 36 new tests; screenshots of the
  menu (3 themes + a grid tile) and move mode (3 themes) viewed against `design/mockups/screens/`. M3 complete.
- 2026-10-22 — T13b website shortcuts: mockup `shortcuts.html` (list + editor, 3 themes) approved by launch; `Shortcuts`
  rules (only http/https with a host; bare `example.com` becomes `https://`), `ShortcutsEditor` (delete also
  removes the favourite), `WebLauncher` (`ACTION_VIEW`, BROWSABLE), `SidePanel`/`PanelScroll`/`PanelRow`/`PanelButton`/
  `PanelTextField` building blocks that Settings reuses, `ShortcutsPage`. Fixes found on the way: Home no longer
  steals focus from a panel on reset, and a reset while a panel is open applies when it closes. The page is
  reachable from Settings once T14 lands.
- 2026-10-22 — T14 settings panel: `SettingsPanel` (quick tiles 3 per row + launcher rows with their values, bottom
  fade, focus returns to the open row when a page closes), `SystemSettings` (Wi-Fi, Bluetooth, Sound, Android;
  Picture → vendor screen else display settings; Sleep timer → vendor screen else hidden; every intent checked
  with `resolveActivity`, `<queries>` added), gear chip opens it, Home gets focus back on the gear on close.
  Rows for pages that do not exist yet do nothing (`SettingsPage.hasUi`), so there are no invisible dead ends.
  11 intent-chain tests, 7 panel tests, 3-theme screenshots viewed.
- 2026-10-22 — T15 theme picker and schedule: 3×3 thumbnails (live preview of the whole launcher on focus, OK saves with
  a check badge, leaving the page clears the preview), "Change by time of day" page (switch, day and night rows
  that open the picker in choose mode, sunset/sunrise note), `themeAt` schedule logic (07:00/20:00 until the weather
  data supplies sun times in T18), clock-minute re-check while Home is visible. Back chain: pick → schedule → picker → panel.
- 2026-10-22 — T16 language picker: seven rows (Беларуская … Français, then "Same as the TV" with the language it
  resolves to), check on the saved choice, OK applies at once (`LocaleController.apply`: platform per-app locale on
  API 33+, activity recreation below), focus starts on the saved choice. `be`/`fr` screenshots viewed.
- 2026-10-22 — T17 background picker: "From theme", six colours, pictures found on USB (`BackgroundImages.scan`: jpg/jpeg/png/webp,
  two folder levels, 36 max; thumbnails 296×166 decoded only for the open picker), live preview of the whole screen on
  focus, OK copies the picture centre-cropped to 960×540 WebP into app storage (EXIF rotation honoured; the previous copy is
  deleted), `ThemedBackground` now draws theme art, a colour or a picture with a 300 ms crossfade. Without
  `MANAGE_EXTERNAL_STORAGE` the picker shows the exact adb command and a "Check again" button (`PermissionNote`, reused by T22).
  Fixed on the way: Down from the wide "From theme" row used to land on the last swatch. 22 new tests; screenshots viewed.
- 2026-10-22 — T18 weather (first network feature, `INTERNET` added): `HttpFetcher` (HttpURLConnection, https only, 10 s
  timeouts, 512 KB cap), `OpenMeteoClient` (forecast incl. sunrise/sunset, city search in the UI language), `GeoIp` (ipinfo.io →
  ipapi.co, refined through geocoding to a localised name), `WeatherRepository` (refresh only while Home is STARTED, at most every
  30 min, one retry after 3 s, failure keeps the cache, IP place kept 24 h, "—°" after 6 h), WMO code → picture + wording,
  top-bar chip, panel, city search (Back leaves the field first), Settings → Weather opens the search. The theme schedule now uses
  the real sunrise/sunset when the forecast is from today. 54 tests on the real Open-Meteo JSON (fetched from the cloud) and fakes.
  Live smoke check from the cloud: forecast 200 (first attempt timed out, as the skill warned), geocoding 200, ipinfo.io 200
  (reports the data-centre city), ipapi.co 429 (as the skill warned).
- 2026-10-22 — T19 HDMI: `TvInputManagerSource` (system callback, hidden inputs filtered), `HdmiWatcher` (asks when an input
  goes to CONNECTED from anything else; 30 s debounce per input; "not now"/timeout is remembered until the input
  disconnects; refresh on resume catches missed events; the owner's switches decide), `HdmiController` (routes: launcher in front →
  card inside Home; another app + overlay permission + allowed → `OverlayHost` window; otherwise it waits and is dropped if the
  signal is gone when Home returns), `InputLauncher` (passthrough `ACTION_VIEW`, vendor picker fallback), `HdmiPrompt` (460 dp card,
  15 s countdown, Back = not now), top-bar chips with the signal dot and the Settings → HDMI page (switches, adb hint, one
  switch per input). Registered 2 s after process start, nothing polls. 41 tests; screenshots viewed against the mockups.
  Process note: when several Gradle tasks are given with `--tests` filters, tests of the first task may be skipped; the full
  gate is what counts.
- 2026-10-22 — T20 iPhone button: `Airplay.open` starts `io.github.jqssun.airplay` if it is installed, otherwise the how-to card
  (`IphoneHintDialog`: what to put on a USB drive, how to mirror from the iPhone; "Install from USB" has focus and opens the USB
  installer page once T22 exists). 4 + 2 tests, screenshots in 3 themes and `be`/`fr`.
- 2026-10-22 — T21 hidden apps page: rows with a mini banner, name and "Show"; OK unhides, the row goes and focus moves to the row
  in its place (or above); empty state; the Settings row counts only installed hidden apps. 6 tests, screenshots viewed.
- 2026-10-22 — T22 USB APK installer: `ApkScanner` (3 folder levels, ≤ 40 files, skips hidden/system folders), `PackageManagerInspector`
  (`getPackageArchiveInfo`), `SessionApkInstaller` (PackageInstaller session, explicit mutable PendingIntent), non-exported
  `InstallResultReceiver` (starts only the installer's own `CONFIRM_INSTALL` screen, never one aimed at our components) and the
  `UsbInstaller` state machine (Install / Update / Installed labels; a downgrade is never offered). Page states: list, empty, no
  storage access, unknown apps not allowed (own adb command), installing, done (Open / Done), failed / cancelled. Scans only when the
  page opens or on "Check again". Tests with fakes + temp dirs, screenshots of every state viewed against the mockup.

## Decisions to review

Defaults taken during unattended runs that the owner should look at.

- T06: new screens were designed by Claude without prior owner review (as agreed for this run).
  Look at `design/mockups/screens/*.png`, especially the app menu and the USB installer.
- T07: soft shadows and glows use Android's elevation shadow tinted with the theme colour, not an
  exact CSS box-shadow, so glows look slightly different from the mockups. Hard cartoon shadows
  are exact.
- T09: all translations (especially Belarusian) were written by Claude. Please read
  `app/src/main/res/values-be/strings.xml`.
- T12: "All apps" order (launch counts) is re-applied only on Home key, screen on or Back at the top, so tiles
  never move under the focus when you return from an app. Shortcut tiles are already supported by Home
  (letter on a colour from the address); their editor comes in T13b.
- T12: safe mode (two crashes within 10 minutes) shows the default theme, only the clock, the settings gear and
  the app grid, with a one-line explanation. Weather, HDMI and iPhone chips appear when T18–T20 supply data.
- T13: the menu's focused row uses the theme's focus fill (slightly lighter) with the ring; the mockup's
  menu rows had no fill by a CSS accident. "Uninstall" is offered for every app; for system apps the system
  dialog says it cannot remove them.
- T13: Settings, Weather, HDMI and iPhone chips do nothing until their own tasks (T14, T18–T20) land.
- T14: vendor intents for **Picture** and **Sleep timer** are unknown until the TV is probed (`VendorIntents` in
  `system/SystemSettings.kt` is empty). Until then Picture opens the generic display settings and the Sleep timer
  tile is hidden. After the probe on the TV, add the intents there and to the manifest `<queries>`.
- T15: while "Change by time of day" is on, the schedule decides the theme and the saved theme is used again only when
  it is switched off. The Theme row in Settings shows the theme in force.
- T17: only colours that keep the theme's text readable are offered (dark set for light text, light set for dark text); if a
  theme switch would make a saved colour unreadable, the theme's own background is shown instead. Pictures get a scrim of the
  opposite tone. Please check a bright photo under a few themes on the TV.
- T17: the background picker lists at most 36 pictures from the first two folder levels of each USB drive.
- T18: the chip is hidden until the first forecast arrives (no data, no chip). If automatic location fails (no internet, or a
  VPN shows the wrong city) the owner sets the city in Settings → Weather. City names stay in the language they were fetched in
  until the next refresh of the place.
- T18: the search lists at most three matches (the keyboard covers the rest of the screen); typing more narrows the search.
- T19: **to verify on the TV** — what the HDMI inputs are called (chips use the labels Android reports), whether Xiaomi reports
  CONNECTED when a device powers on (CEC) or only on plug-in, which app opens the passthrough address, and whether the overlay card
  takes the remote's focus. The CEC device name is not in the public API, so the card shows only "Signal on <input label>".
  The vendor input picker fallback (`VendorInputIntents.picker`) is empty until the probe finds it.
- Gboard's keyboard height (~168 dp) is an estimate in the city-search mockup; check on the TV.
- T22: `REQUEST_INSTALL_PACKAGES` is not grantable from a dialog on every Google TV build, so the page shows the adb command
  (`appops set --uid … REQUEST_INSTALL_PACKAGES allow`) like the other special permissions. Check on the TV whether the system's own
  "allow unknown apps" screen is reachable and prefer it if so.

## Blockers

_(none yet)_
