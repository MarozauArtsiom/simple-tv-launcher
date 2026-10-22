# Tasks — Simple TV Launcher v1

Execute in order. Each task lists what to read first, what to do, and how to check it's done.
After **every** task:
1. Run the gate: `./gradlew :app:assembleDebug :app:testDebugUnitTest` (plus
   `:app:verifyRoborazziDebug` once screenshot tests exist). It must be green.
2. Tick the task in this file and add a line to `docs/progress.md` (what was done, decisions
   taken, anything to review).
3. Commit with a clear message, and push at least at the end of each milestone.

Package root: `io.github.marozauartsiom.tvlauncher` (`app/src/main/kotlin/...`). Every UI
string lives in resources in all six languages from the start (`values/` = English, plus
`values-be`, `-ru`, `-pl`, `-de`, `-fr`).

---

## M0 — Setup

- [x] **T01 SDK setup script.** Read: `docs/tech-stack.md` §"Android SDK in a cloud session",
  the `android-cli` skill.
  Write an idempotent `scripts/setup-android-sdk.sh`. It installs the cmdline-tools into
  `~/android-sdk` (newest build number from `repository2-3.xml`, sorted numerically), accepts
  the licences, and installs `platforms;android-37.0`, `build-tools;36.0.0` and
  `platform-tools`. It then writes `local.properties` with `sdk.dir`, and skips anything
  already present. **Done:** running it twice works, and the second run is a no-op.

- [x] **T02 Re-verify versions.** Run
  `.claude/skills/android-tv-launcher/scripts/latest-versions.sh`. If a stable version
  changed, update `docs/tech-stack.md`, but keep the compatibility rules there (AGP ≥ 9.2 for
  compileSdk 37 Compose, Gradle ≥ AGP minimum, Kotlin plugins equal versions).
  **Done:** the versions table is current.

- [x] **T03 Gradle scaffold.** Read: `docs/tech-stack.md` (all of it), the `agp-9-upgrade`
  skill (new DSL, built-in Kotlin).
  - `settings.gradle.kts`: `pluginManagement` repos `google()`, `mavenCentral()`,
    `gradlePluginPortal()`; `dependencyResolutionManagement` with `google()` and
    `mavenCentral()`; modules `:app` (and `:baselineprofile` later, in T25).
  - `gradle/libs.versions.toml` with every version from `docs/tech-stack.md`.
  - Gradle wrapper 9.8.2: `gradle wrapper --gradle-version 9.8.2` with the preinstalled Gradle.
  - `gradle.properties`: `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`,
    `org.gradle.parallel=true`, `org.gradle.caching=true`,
    `org.gradle.configuration-cache=true`, `kotlin.code.style=official`. No AGP opt-out flags.
  - `:app`: `com.android.application`, `org.jetbrains.kotlin.plugin.compose`,
    `org.jetbrains.kotlin.plugin.serialization`. Namespace and applicationId
    `io.github.marozauartsiom.tvlauncher`; compileSdk 37, targetSdk 36, minSdk 30; Java 17;
    `buildFeatures.compose = true`; `androidResources.generateLocaleConfig = true` +
    `res/resources.properties` (`unqualifiedResLocale=en`). Debug gets the `.debug` suffix;
    release gets minify + shrinkResources + `proguard-android-optimize.txt`.
  - `.gitignore` (build/, .gradle/, local.properties, *.keystore, .idea/).
  - Retry on HTTP 429 from Maven Central or the plugin portal: rerun with a short pause.
  **Done:** `./gradlew :app:assembleDebug` builds an app with an empty Compose activity.

- [x] **T04 Manifest + launcher identity.** Read: the `android-tv-launcher` skill
  (Manifest, Launcher behaviour), and the `android-intent-security` skill.
  - Manifest exactly as in the skill: HOME + DEFAULT and LEANBACK_LAUNCHER filters,
    `<queries>`, `singleTask`, `stateNotNeeded`, `excludeFromRecents`, landscape,
    `configChanges`. Use `uses-feature` leanback required and touchscreen not required.
  - Add **no permissions yet**. Each feature task adds its own.
  - Icon (adaptive vector) and banner: write `design/mockups/brand/banner.html`, render it with
    the mockup renderer at 320×180 px, and save it as `res/drawable-xhdpi/banner.png`. The
    banner shows the launcher name "Simple TV".
  - Theme XML: no action bar, `windowBackground` = the Midnight background colour.
  **Done:** the merged manifest shows both filters; the build is green.

- [x] **T05 Test infra + CI.** Read: `docs/tech-stack.md` §Testing and §CI, and the
  `testing-setup` skill (project decisions win).
  - Test dependencies: JUnit 4, Robolectric 4.17, Roborazzi 1.77.0 (plugin + `roborazzi`,
    `roborazzi-compose`, `roborazzi-junit-rule`), Compose ui-test-junit4 and
    ui-test-manifest, coroutines-test. `testOptions.unitTests.isIncludeAndroidResources = true`.
  - Roborazzi: `outputDir = src/test/snapshots`, `separateOutputDirs = true`.
  - A shared annotation/base class for TV screenshots: `@RunWith(AndroidJUnit4)`,
    `@GraphicsMode(NATIVE)`, `@Config(sdk = [36], qualifiers = "w960dp-h540dp-land-television-xhdpi")`.
    A helper renders a composable for each of the 9 themes, and for a given locale.
  - One smoke screenshot test; record it (`recordRoborazziDebug`) and **look at the PNG**.
  - `.github/workflows/android.yml`: ubuntu, JDK 21, Gradle cache. It runs
    `assembleDebug testDebugUnitTest verifyRoborazziDebug` and uploads the debug APK and any
    failing screenshot diffs.
  **Done:** the gate (with verifyRoborazzi) is green; the workflow file is valid YAML.

## M1 — Design the remaining screens

- [x] **T06 Mockups for screens not yet designed.** Read: the `tv-ui-design` skill, and
  `docs/ui-spec.md` §4–§5 ("Not yet designed").
  Following the existing mockups and `settings.css`, create HTML mockups for:
  1. the empty-favourites hint card;
  2. the app menu (long press) and the "move" mode in Favourites;
  3. city search (text field, results, "Автоматически" entry);
  4. the background picker (from theme / colours / USB images grid);
  5. hidden apps (list with "show again");
  6. the USB installer (APK list, permission-missing state, installing state);
  7. About (version, licences, Open-Meteo credit);
  8. the theme day/night schedule page;
  9. the HDMI prompt settings page (on/off, "over other apps" with a hint when the
     permission is missing);
  10. the iPhone hint (shown when AirPlay Server isn't installed).

  Render each one in `midnight`, `sky` and `valley`, add it to the mockup index in
  `docs/ui-spec.md` §9, and move each screen out of "Not yet designed" into §5 with its
  focus and Back rules. **Done:** every PNG has been looked at, nothing is clipped, and the
  index is updated.

## M2 — Foundations

- [x] **T07 Theme engine.** Read: `docs/ui-spec.md` §2–§4, `compose-tv-patterns.md`
  (ThemeSpec), and `design/mockups/themes.css`.
  - Port all 9 themes 1:1 into `ui/theme/Themes.kt` (`ThemeSpec`, `LauncherColors`,
    `FocusStyle`), including the panel and item tokens. `LauncherTheme(spec)` provides the
    CompositionLocal and the tv-material scheme and typography.
  - Fonts: bundle Nunito (`res/font/`, from `design/mockups/assets/fonts/`). Ship its OFL text
    in `assets/licenses/`.
  - Backgrounds: render `design/mockups/assets/bg/*.svg` and the Midnight art with Playwright to
    1920×1080, encode as WebP (quality ≈ 85), and save to `res/drawable-nodpi/bg_<id>.webp`.
    Graphite and Paper backgrounds are drawn as gradients in code. Theme thumbnails come from
    `design/mockups/themes/<id>.png`, scaled to 296×166 WebP.
  - `BackgroundPainter`: decodes once off the main thread at 960×540, crossfades in 300 ms, and
    keeps at most two bitmaps.
  **Done:** a screenshot test of a sample screen in all 9 themes matches the theme renders
  in `design/mockups/themes/`.

- [x] **T08 Settings store.** Read: `references/settings-themes-i18n.md` (keys).
  `data/prefs/Settings.kt`: one Preferences DataStore with typed `Flow`s and setters for
  favourites (ordered list), hidden apps, launch counts, theme, theme schedule, background,
  language, weather location, weather cache and the HDMI prompt options.
  **Done:** unit tests for read/write and defaults (theme = midnight, language = follow TV).

- [x] **T09 Languages.** Read: `references/settings-themes-i18n.md` §Languages and
  `docs/ui-spec.md` §7.
  - String resources for every screen designed so far, in all 6 languages, with plurals
    (`one`/`few`/`many`/`other`). The language list labels are written natively:
    "Беларуская" first.
  - `LocaleController`: on API 33+ use `LocaleManager.applicationLocales`; below that, wrap the
    context in `attachBaseContext` and recreate. "Follow the TV" means an empty list, falling
    back to English. Dates: `DateTimeFormatter("EEEE, d MMMM")` per locale with the first
    letter capitalised.
  **Done:** unit tests for date strings (ru, be, de, fr, en, pl) and for the fallback;
  screenshot of a sample screen in `be` and `fr`.

- [x] **T10 App shell.** Read: the `android-tv-launcher` skill (Launcher behaviour) and
  `docs/tech-stack.md` §Architecture.
  `LauncherApp` + `AppGraph` (manual DI), and `LauncherActivity`: Compose content,
  `onNewIntent(CATEGORY_HOME)` → home-pressed events, and an `ACTION_SCREEN_ON` receiver
  registered at runtime → reset on next resume. The overlay state machine
  (`None | Settings(page) | Weather | Hdmi | AppMenu | …`) handles `BackHandler` rules: Back
  never finishes. If anything fails at startup, a safe minimal UI still renders (app grid plus
  Settings).
  **Done:** Robolectric tests for the Back/Home/screen-on reset logic.

## M3 — Home

- [x] **T11 App repository.** Read: the `android-tv-launcher` skill (Listing apps, Banners,
  Launching, Reacting to installs).
  `data/apps/`: query LEANBACK_LAUNCHER + LAUNCHER and dedupe (leanback wins); labels;
  `BannerLoader` (activity banner → app banner → icon; rasterised at 320×180 px on `Default`;
  LRU of about 16 MB; generated banner with a palette tint); a `LauncherApps.Callback` with
  500 ms debounce; launching with fallbacks and error handling; launch counters; hidden
  filter; a JSON snapshot cache for instant cold start; `onTrimMemory` clears the caches.
  PackageManager is wrapped behind an interface with a fake for tests.
  **Done:** unit tests for dedupe, sorting (launch count, then alphabetical), the hidden
  filter and the cache round-trip.

- [x] **T12 Home screen.** Read: `docs/ui-spec.md` §1, §4, §5 Home; the `tv-ui-design` skill;
  `compose-tv-patterns.md`; the `leanback-to-compose-tv-migration` and
  `jetpack-compose-performance` skills.
  - TopBar: clock (minute tick via `ACTION_TIME_TICK` while visible), localised date, weather
    chip (placeholder until T18), HDMI chips (data from T19), iPhone chip, gear.
  - FavouritesRow (manual order, label under the focused card, `focusRestorer`, first item
    requester), AppGrid (5 columns, rows of `LazyColumn` items, `focusRestorer` per row),
    empty-favourites hint (T06 mockup), cartoon tilt and sticker style from the theme.
  - Focus rules from the spec: initial focus on the first favourite, Up to the top bar, and
    Back, Home and screen-on reset to the first favourite.
  **Done:** Compose behaviour tests (D-pad navigation, focus restore, Back reset); Roborazzi
  Home in 9 themes + `be` + `fr`, compared by eye with `design/mockups/themes/*.png`.

- [ ] **T13 App menu + move mode.** Read: the T06 mockup.
  Long-press OK opens a menu with: add to / remove from favourites, move (Favourites only:
  Left/Right moves, OK drops, Back cancels), hide, app info
  (`ACTION_APPLICATION_DETAILS_SETTINGS`), uninstall (`ACTION_DELETE` +
  `REQUEST_DELETE_PACKAGES`). Favourites persist in their manual order.
  **Done:** behaviour tests for move and persistence; screenshot.

## M4 — Features

- [ ] **T13b Shortcut tiles.** (Added during the run: approved in `requirements.md` §8 item 4, but missing from the first task list.)
  A shortcut is a tile that opens a web address (in TV Bro, or the default browser through `ACTION_VIEW`). Settings gets
  "Ярлыки сайтов": a list with "Добавить" (a title field and an address field, using the text-input pattern),
  edit and delete. Shortcuts appear in "All apps" with a generated banner (first letter + colour from the
  address) and can be added to Favourites from the app menu like apps. Keys in storage: `url:<id>` next to `pkg:<package>`.
  Mockup first (`design/mockups/shortcuts.html`, midnight/sky/valley), and add it to `docs/ui-spec.md`.
  **Done:** tests for add/edit/delete and for launching; screenshot of the list and the editor.

- [ ] **T14 Settings panel.** Read: `docs/ui-spec.md` §5 Settings and
  `design/mockups/settings.html`.
  The gear opens the panel. TV tiles open settings screens, each guarded with
  `resolveActivity` and a fallback chain: Wi-Fi `ACTION_WIFI_SETTINGS`; Bluetooth
  `ACTION_BLUETOOTH_SETTINGS`; Sound `ACTION_SOUND_SETTINGS`; Picture → vendor picture
  settings if any resolve, otherwise `ACTION_DISPLAY_SETTINGS`; Sleep timer → vendor screen if
  one resolves, otherwise hide the tile; Android → `ACTION_SETTINGS`. Launcher items lead to
  the sub-pages from T15–T23. Focus and Back follow the spec.
  **Done:** screenshot in 3 themes; tests for the intent fallbacks with a fake resolver.

- [ ] **T15 Theme picker + schedule.** Read: the theme-picker mockup and the T06 schedule
  mockup.
  The 3×3 thumbnails do a live preview on focus, OK saves, Back reverts. The schedule page
  sets a day and a night theme, switched at sunrise/sunset from the weather data or at
  07:00/20:00, checked only on resume or on the minute tick while Home is visible.
  **Done:** tests for preview/revert and the schedule logic; screenshot.

- [ ] **T16 Language picker.** The order and default from `docs/ui-spec.md` §5; applies
  immediately; initial focus on the saved choice. **Done:** test + screenshot.

- [ ] **T17 Background picker.** Read: the T06 mockup and `references/backgrounds.md`.
  Choices: from theme, a set of solid colours, or images from USB. USB images need
  `MANAGE_EXTERNAL_STORAGE` (granted over adb). When `Environment.isExternalStorageManager()`
  is false, show a hint with the exact adb command. Scan `StorageManager` volumes for
  jpg/png/webp, build thumbnails on `Default`, and copy the chosen image downscaled to
  960×540 into app storage. **Done:** tests for scanning (fake filesystem); screenshot.

- [ ] **T18 Weather.** Read: `references/weather.md`.
  Add the `INTERNET` permission. `GeoIp` (ipinfo.io → ipapi.co), `OpenMeteoClient` (forecast
  + geocoding with `language` = UI language), `WeatherRepository` (DataStore cache, refresh
  only while Home is `STARTED`, every 30 min at most, one retry, stale after 6 h). WMO code →
  vector icon + localised text. Chip + panel (no source or location-mode text) + city search
  (TextField pattern from `compose-tv-patterns.md`, Back interception).
  **Done:** unit tests with JSON fixtures (forecast, geocoding, IP) and a fake HTTP
  layer; screenshots of the panel and search in 3 themes. One live request from the cloud
  as a smoke check.

- [ ] **T19 HDMI.** Read: the `android-tv-launcher` skill §HDMI and the T06 settings mockup.
  `InputRepository` (`TvInputManager`, labels, hidden inputs filtered), `HdmiWatcher`
  (callback, 30 s debounce, "not now" until disconnect, catch-up on resume), chips with the
  signal dot, opening an input (passthrough `ACTION_VIEW`, with a fallback), the in-launcher
  prompt (15 s countdown), and the overlay prompt via `TYPE_APPLICATION_OVERLAY` when
  `Settings.canDrawOverlays` (add `SYSTEM_ALERT_WINDOW`). Plus the settings page.
  **Done:** watcher unit tests with fake states; screenshot of the prompt.

- [ ] **T20 iPhone button.** If `io.github.jqssun.airplay` is installed, launch it; otherwise
  show the hint from T06 (how to install). **Done:** test for both branches.

- [ ] **T21 Hidden apps page.** Lists hidden apps with "show again". **Done:** test +
  screenshot.

- [ ] **T22 USB APK installer.** Read: the T06 mockup.
  Add `REQUEST_INSTALL_PACKAGES`. List `.apk` files on USB volumes (same storage access as
  T17), read the label and version with `getPackageArchiveInfo`, and install through a
  `PackageInstaller` session with a status `PendingIntent`. Show the "allow unknown apps"
  hint when `canRequestPackageInstalls()` is false, and handle installing, success and error
  states. A newer version of the launcher itself installs as an update.
  **Done:** tests with fakes; screenshot of each state.

- [ ] **T23 About page.** Version, build, licences (Nunito OFL; "Данные о погоде: Open-Meteo.com,
  CC BY 4.0"), and a short "no ads, no tracking" line. **Done:** screenshot.

## M5 — Hardening and handover

- [ ] **T24 Security and permission review.** Run the `android-intent-security` checklist
  on the manifest and every intent we send or receive. The only exported component is
  `LauncherActivity` (HOME/LEANBACK), plus receivers registered at runtime as not exported.
  **Done:** findings fixed or documented in `docs/progress.md`.

- [ ] **T25 Performance + Baseline Profile.** Read: the `jetpack-compose-performance` skill
  and the `r8-analyzer` skill.
  Audit the UI code against the performance checklist and fix findings. Add the
  `:baselineprofile` module (`com.android.test`, `androidx.baselineprofile` plugin, a
  startup + scroll journey with UiAutomator). Generating the profile needs the TV, so it goes
  on the TV checklist. Build release and report the APK size (target < 10 MB). Review keep
  rules with `r8-analyzer`.
  **Done:** `./gradlew :app:assembleRelease` is green (unsigned is fine) and the size is noted.

- [ ] **T26 Owner guide.** Write `README.md` in Russian: what the launcher is; installing the
  APK (adb, or from USB once installed); making it the default Home (methods from
  `references/default-home.md`, with the restore commands); the one-time adb grants
  (overlay, all-files access, "unknown apps"); where to download TV Bro and AirPlay Server;
  updating; troubleshooting (black screen → re-enable the stock launcher).
  **Done:** every command in the guide is copied from the skills and docs, not invented.

- [ ] **T27 Docs sync + final gate.** Update `docs/requirements.md` statuses,
  `docs/ui-spec.md` (mockup index, final behaviour) and `docs/tech-stack.md` (anything that
  changed). Finish `docs/progress.md` with a summary and the "Decisions to review" list.
  The full gate passes, CI is green, and everything is pushed.

---

## On the TV (owner, or Claude Code running on a computer in the same network)

Not part of the cloud run. Do these after M5:

1. Enable adb on the TV, then run `.claude/skills/xiaomi-tv-a-pro-2025/scripts/probe-device.sh`
   and commit `docs/device/`. Set minSdk to the TV's API level.
2. `adb install` the APK, then make it the default Home with the method that works
   (`references/default-home.md`). Record the result in `docs/device/default-home.md`.
3. Grant the permissions: `appops set <pkg> SYSTEM_ALERT_WINDOW allow`, and
   `appops set --uid <pkg> MANAGE_EXTERNAL_STORAGE allow`; allow "unknown apps" for the launcher.
4. Check: first frame (`am start -W`) < 1 s, `dumpsys meminfo` ≤ 120 MB, focus feels instant,
   HDMI prompt on plug/power-on, real banners, Belarusian dates, weather "Auto" city.
5. Generate the Baseline Profile on the TV (`:baselineprofile`), commit it, and rebuild the release.
6. Record the remote's keycodes (`getevent -lt`) and the voice diagnostics (`requirements.md` §7).
