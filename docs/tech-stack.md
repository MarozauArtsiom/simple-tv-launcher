# Tech stack (source of truth)

What the launcher is built with, which versions, and what is deliberately left out. Every
version below was checked against Google Maven and Maven Central on **2026-10-22**. Before
scaffolding or upgrading, re-run
`.claude/skills/android-tv-launcher/scripts/latest-versions.sh` and update this file and
`gradle/libs.versions.toml` in the same commit. Use stable releases only: no alpha, beta or rc.

## Build toolchain

| Piece | Version | Why this one |
|---|---|---|
| Gradle wrapper | 9.8.2 | Latest stable. AGP 9.4 needs ≥ 9.6.0 |
| Android Gradle Plugin | 9.4.1 | Latest stable. Compose 1.12 with compileSdk 37 needs AGP ≥ 9.2 |
| Kotlin (Compose + serialization compiler plugins) | 2.4.21 | Latest stable. AGP 9 has **built-in Kotlin**: don't apply `org.jetbrains.kotlin.android` |
| JDK | Gradle runs on 21; bytecode targets 17 | AGP needs 17+. Robolectric tests on SDK 36+ need JDK 21 |
| compileSdk | 37 (package `platforms/android-37.0`) | Compose 1.12.1 and core 1.19.1 declare `minCompileSdk=37` |
| targetSdk | 36 | Well-understood behaviour. Raise deliberately later |
| minSdk | 30 until the device probe, then the TV's own API level | Libraries need ≥ 24. Only one device is targeted |
| Build tools | 36.0.0 | AGP 9.4 default |

Plugins: `com.android.application` 9.4.1, `org.jetbrains.kotlin.plugin.compose` 2.4.21,
`org.jetbrains.kotlin.plugin.serialization` 2.4.21, `io.github.takahirom.roborazzi` 1.77.0,
`androidx.baselineprofile` 1.5.0 (applied to `:app`), and `com.android.test` 9.4.1 for the
`:baselineprofile` module.

AGP 9 notes (details in the `agp-9-upgrade` skill):
- Built-in Kotlin. Configure the compiler with `kotlin { compilerOptions { … } }`, not
  `android.kotlinOptions`. `jvmTarget` follows `compileOptions.targetCompatibility` (17).
- Use the new DSL only. Don't set `android.builtInKotlin`, `android.newDsl`,
  `android.uniquePackageNames` or `android.enableAppCompileTimeRClass` in `gradle.properties`.
- No kapt. If an annotation processor is ever needed, use KSP ≥ 2.3.6. Plan for none.
- `buildFeatures { compose = true }`. BuildConfig is off by default; turn it on only if a
  field is needed.

## Libraries

| Library | Version | Used for |
|---|---|---|
| `androidx.compose:compose-bom` | 2026.09.00 | Compose 1.12.1 (ui, foundation, runtime); material3 1.4.0 |
| `androidx.compose.ui:ui`, `ui-graphics`, `foundation` | from BOM | Layout, lazy lists, focus |
| `androidx.compose.ui:ui-tooling-preview` / `ui-tooling` (debug) | from BOM | `@Preview` |
| `androidx.compose.material3:material3` | from BOM | **Only `TextField`/`OutlinedTextField`** for city search. tv-material has none |
| `androidx.tv:tv-material` | 1.1.0 | All TV components: `MaterialTheme`, `Card`, `Surface`, `Button`, `ListItem`, `Text` |
| `androidx.activity:activity-compose` | 1.13.0 | `ComponentActivity`, `setContent`, `BackHandler` |
| `androidx.lifecycle:lifecycle-runtime-compose` + `lifecycle-viewmodel-compose` | 2.11.0 | ViewModel, `collectAsStateWithLifecycle` |
| `androidx.core:core-ktx` | 1.19.1 | `Drawable.toBitmap`, context helpers |
| `androidx.datastore:datastore-preferences` | 1.3.0 | All settings and caches (favourites, theme, language, weather) |
| `androidx.palette:palette-ktx` | 1.1.0 | Tint for generated banners of icon-only apps |
| `androidx.profileinstaller:profileinstaller` | 1.4.1 | Ships the Baseline Profile |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | Threading |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.12.0 | Open-Meteo and IP-geolocation JSON; the remote's API and MCP JSON-RPC |
| `dev.mobile:dadb` | 2.0.0 | Phone remote: real key events through the TV's own adbd at 127.0.0.1:5555 (only while access is on) |
| `com.google.zxing:core` | 3.5.4 | The pairing QR code (matrix only; drawn with Compose) |

The phone remote's HTTP server is our own (~150 lines, `system/remote/http/`), not Ktor or NanoHTTPD: one request
per connection, hard size limits, private source addresses only, and nothing running while both switches are off.

Tests:

| Library | Version | Used for |
|---|---|---|
| `junit:junit` | 4.13.2 | Unit tests |
| `org.robolectric:robolectric` | 4.17 | Android framework on the JVM (supports SDK 37) |
| `io.github.takahirom.roborazzi:roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule` | 1.77.0 | **Screenshot tests of real Compose UI on the JVM**, so no emulator is needed in the cloud |
| `androidx.compose.ui:ui-test-junit4` (+ `ui-test-manifest`, debug) | from BOM | Compose behaviour tests on Robolectric |
| `androidx.test.ext:junit` | 1.3.0 | `AndroidJUnit4` runner |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | ViewModel and repository tests |
| `androidx.benchmark:benchmark-macro-junit4` | 1.5.0 | Baseline Profile and startup benchmark, run on the TV |
| `androidx.test.uiautomator:uiautomator` | 2.4.0 | Drives the TV in benchmarks |

## Deliberately not used

| Not used | Instead | Why |
|---|---|---|
| Leanback library, XML views | Compose + tv-material | Decided stack. Leanback is legacy |
| `androidx.compose.material3` components (except text fields) | tv-material | M3 mobile components have touch semantics and no TV focus states |
| Hilt / Koin / Dagger | Manual DI: one `AppGraph` built in `LauncherApp` | Small app. No kapt/KSP, faster cold start, smaller APK. Interfaces + fakes keep it testable |
| Navigation Compose / Navigation 3 | State-driven overlays (`sealed interface Overlay`) plus `BackHandler` | One screen with side panels. A navigation library adds weight and nothing we need |
| Room / SQLite | DataStore Preferences | Only small key-value data |
| Retrofit / OkHttp / Ktor | `HttpURLConnection` | Two tiny GET requests. Keeps the APK small |
| Coil / Glide | `PackageManager` drawables, `ImageDecoder` with downsampling | No internet images. App art and theme backgrounds are local |
| Gson / Moshi | kotlinx.serialization | Kotlin-native, no reflection |
| WorkManager, AlarmManager, services | Lifecycle-scoped coroutines | Principle: nothing runs in the background |
| AppCompat | `LocaleManager` (API 33+) or context wrapping | Per-app language without the AppCompat weight |
| Firebase, analytics, crash-reporting, ad SDKs | Logcat only | Principle: no ads, no tracking |
| Compose Styles API | `ThemeSpec` + `CompositionLocal` | Experimental (alpha-only). Stable-only policy |

## Architecture

- **Modules:** `:app`, and `:baselineprofile` (`com.android.test`, a D-pad journey: start, Favourites, All apps grid,
  Settings). The profile is recorded on the TV with `./gradlew :app:generateBaselineProfile` and committed to
  `app/src/release/generated/baselineProfiles`; until then the release build has none.
- **applicationId / namespace:** `io.github.marozauartsiom.tvlauncher`.
- **Packages** (inside the namespace):
  - `LauncherApp`, `LauncherActivity`, `AppGraph` (manual DI container)
  - `data/apps` — query, banners, launch counters, `LauncherApps` callback
  - `data/inputs` — HDMI inputs, `HdmiWatcher`
  - `data/weather` — Open-Meteo client, IP geolocation, cache
  - `data/prefs` — the single DataStore and typed settings
  - `ui/theme` — `ThemeSpec` for the 14 themes, fonts, backgrounds
  - `ui/home`, `ui/settings`, `ui/weather`, `ui/hdmi`, `ui/components`
- **State:** one ViewModel per surface (`HomeViewModel`, `SettingsViewModel`) exposing
  `StateFlow<UiState>`. UI events go up as lambdas, and one-off effects travel through a
  `Channel`. Collect with `collectAsStateWithLifecycle()`.
- **Navigation:** `HomeViewModel` holds
  `overlay: Overlay = None | Settings(page) | Weather | Hdmi(input) | AppMenu(app)`.
  `BackHandler` pops one level. With `None`, Back resets the home screen and never exits.
- **Threading:** binder and PackageManager calls on `Dispatchers.IO`, bitmaps on `Default`,
  nothing heavy on Main or in composition.
- **Resilience:** every system call is wrapped in `runCatching` and degrades gracefully. The
  launcher must never crash-loop, because it may be the only Home on the TV.
- **Strings:** all of them in resources. `values/` holds English (the fallback), plus
  `values-be`, `values-ru`, `values-pl`, `values-de` and `values-fr`.
  `androidResources { generateLocaleConfig = true }` + `res/resources.properties`
  (`unqualifiedResLocale=en`) generates the locale config.

## Build types

- `debug`: `applicationIdSuffix = ".debug"`, so it can sit next to the release build on the TV.
- `release`: `isMinifyEnabled = true`, `isShrinkResources = true`,
  `proguard-android-optimize.txt`. Use the `r8-analyzer` skill when touching keep rules.
- Signing, debug: every debug build (local and CI) uses the committed throwaway key `app/debug.keystore`
  (password `android`), so a newer debug APK installs over an older one. The owner installs this build.
- Signing, release: a single release keystore kept **outside the repo** (path and passwords from
  `~/.gradle/gradle.properties` locally, GitHub secrets in CI). Not wired yet: `assembleRelease` produces an
  unsigned APK (about 3.0 MB with R8) that CI only builds as a check. Updates from USB only work
  while the signature stays the same, so a release keystore must never be lost.

## Testing

| Layer | Tool | Runs in the cloud |
|---|---|---|
| Logic (repositories, sorting, weather parsing, theme schedule) | JUnit 4 + fakes + coroutines-test | yes |
| UI behaviour (focus order, focus restore, Back/Home reset) | Compose UI test on Robolectric, D-pad via `performKeyInput { pressKey(Key.DirectionRight) }` | yes |
| Screenshots | Roborazzi on Robolectric, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36], qualifiers = "w960dp-h540dp-land-television-xhdpi")`. Every key screen in all 14 themes, plus `be` and `fr` (the longest strings) | yes |
| Startup and Baseline Profile | Macrobenchmark + UiAutomator (`:baselineprofile`, compiles in the cloud) | no, needs the TV |

- Goldens live in `app/src/test/snapshots/`. Turn on `roborazzi { separateOutputDirs.set(true) }`
  to avoid the Gradle 9 race on `roborazziImageInput`.
- Screenshot tests should match the approved HTML mockups in `design/mockups/`.
- The `testing-setup` skill is general advice. Where it differs (it says to install Hilt and
  to use the deprecated preview-screenshot plugin on AGP < 9.5), this file wins.

## CI (GitHub Actions)

On every push: JDK 21, the Gradle cache, then
`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug`. Upload the
debug APK as an artifact, then `assembleRelease` (R8) as a check. A tag-triggered signed release is not
implemented: it needs the owner's keystore as a GitHub secret.

## Android SDK in a cloud session

The container is fresh every session. Install only when a build is needed, and ask the owner
first if it hasn't been agreed:

```bash
# Android CLI (Google's tool for agents; see the android-cli skill)
curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh | bash
android sdk install platforms/android-37.0 build-tools/36.0.0 platform-tools
# or the classic way with cmdline-tools in ~/android-sdk:
sdkmanager "platforms;android-37.0" "build-tools;36.0.0" "platform-tools"
```

Then `echo "sdk.dir=$HOME/android-sdk" > local.properties` (never committed).
