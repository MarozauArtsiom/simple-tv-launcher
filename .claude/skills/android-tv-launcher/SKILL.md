---
name: android-tv-launcher
description: How to build a custom HOME launcher for Google TV / Android TV in Kotlin + Compose. Covers the manifest (HOME + LEANBACK_LAUNCHER, package visibility), listing installed TV apps with banners, launching them, reacting to installs/removals, HDMI inputs and the event-driven "signal on HDMI" prompt, offline launcher backgrounds, launcher-specific Back/Home behaviour, making it the default Home on Google TV, and build/install over adb. Use for any non-visual code in this repo.
---

# Android TV launcher: engineering guide

Target: one device, the Xiaomi TV A Pro 2025 (Google TV). Device facts and adb workflow
are in the `xiaomi-tv-a-pro-2025` skill, and visual rules are in `tv-ui-design`.

## Project shape

```
app/src/main/
  AndroidManifest.xml
  java/<pkg>/
    LauncherActivity.kt        single activity, hosts Compose
    LauncherApp.kt             Application: keep onCreate nearly empty
    data/apps/AppRepository.kt     query + cache + change callbacks
    data/apps/AppIconLoader.kt     banner/icon -> ImageBitmap, LRU by bytes
    data/inputs/InputRepository.kt HDMI inputs (TvInputManager)
    data/prefs/                    DataStore: favourites, order, hidden apps
    ui/home/HomeScreen.kt + HomeViewModel.kt
    ui/components/               cards, rows, top bar
    ui/theme/                    tokens from tv-ui-design (colours, type, spacing, motion)
```

- Use ViewModel + `StateFlow` and collect it with `collectAsStateWithLifecycle()`.
- Create dependencies by hand in `LauncherApp`. Don't add a DI framework until it hurts.
- Pick library versions at scaffold time from Google Maven and Maven Central. Don't guess
  them. As of May 2026 the stable versions were `androidx.tv:tv-material:1.1.0` and
  `tv-foundation:1.0.0`. tv-foundation's `TvLazyRow`/`TvLazyColumn` were removed, so use
  `androidx.compose.foundation.lazy.LazyRow`/`LazyColumn`.
- Set `minSdk 28` unless the device probe shows otherwise. Set `targetSdk`/`compileSdk` to
  the latest stable.
- In release builds, turn on R8 (`isMinifyEnabled = true`, `isShrinkResources = true`)
  because the TV only has 8 GB of storage.

## Manifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- No INTERNET permission, on purpose: the launcher stays offline (no ads, no tracking). -->
    <uses-feature android:name="android.software.leanback" android:required="true" />
    <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

    <!-- Package visibility (API 30+): see every launchable app without QUERY_ALL_PACKAGES. -->
    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
        </intent>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:name=".LauncherApp"
        android:banner="@drawable/banner"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.Launcher">

        <activity
            android:name=".LauncherActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:stateNotNeeded="true"
            android:clearTaskOnLaunch="true"
            android:excludeFromRecents="true"
            android:resumeWhilePausing="true"
            android:screenOrientation="landscape"
            android:configChanges="keyboard|keyboardHidden|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|density|fontScale"
            android:windowSoftInputMode="adjustNothing">

            <!-- Lets the system offer us as Home. -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>

            <!-- Shows us in the stock Google TV app list, so we can be opened before we are default. -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

Theme (`res/values/themes.xml`): a dark `android:Theme.DeviceDefault.NoActionBar` parent, with
`android:windowBackground` set to the launcher background colour so a cold start never
flashes white. On Android 12+ TV, don't add a `SplashScreen` animation: it isn't supported.

Banner: 320×180 px in `drawable-xhdpi`, with the app name drawn into the image.

## Listing apps

`LauncherApps.getActivityList()` only returns `CATEGORY_LAUNCHER`, which misses TV-only apps.
Query both categories and dedupe by package. Prefer the leanback entry.

```kotlin
data class TvApp(
    val packageName: String,
    val component: ComponentName,
    val label: String,
    val isLeanback: Boolean,
)

fun queryTvApps(pm: PackageManager, self: String): List<TvApp> {
    fun query(category: String) = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(category), 0,
    )
    val leanback = query(Intent.CATEGORY_LEANBACK_LAUNCHER).map { it to true }
    val phone = query(Intent.CATEGORY_LAUNCHER).map { it to false }
    return (leanback + phone)
        .distinctBy { (ri, _) -> ri.activityInfo.packageName }   // leanback wins (it comes first)
        .filter { (ri, _) -> ri.activityInfo.packageName != self }
        .map { (ri, isLeanback) ->
            val ai = ri.activityInfo
            TvApp(ai.packageName, ComponentName(ai.packageName, ai.name), ri.loadLabel(pm).toString(), isLeanback)
        }
        .sortedBy { it.label.lowercase() }
}
```

- PackageManager calls are binder IPC, so run them on `Dispatchers.IO`, never during composition.
- Cache the last list (DataStore/JSON) and render it at once on cold start, then refresh in
  the background. The launcher process gets killed often on a 2 GB device.
- Phone-only apps (`isLeanback == false`) often have no banner and may not work with a
  D-pad. Show them in a separate row ("Other apps") or hide them by default.

## Banners and icons

Order of preference: activity banner, then app banner, then activity icon, then app icon.

```kotlin
fun loadArt(pm: PackageManager, app: TvApp): Pair<Drawable?, Boolean /*isBanner*/> {
    val ai = pm.getActivityInfo(app.component, 0)
    ai.loadBanner(pm)?.let { return it to true }
    pm.getApplicationBanner(app.packageName)?.let { return it to true }
    return ai.loadIcon(pm) to false
}
```

- Rasterise once with `drawable.toBitmap(w, h)` (core-ktx) at the exact on-screen pixel
  size. The banner card is 160×90 dp, which is 320×180 px at xhdpi. Keep the results as
  `ImageBitmap` in an `LruCache` sized in bytes (about 16 MB).
- For apps with only an icon, build the banner yourself: centre the icon on a surface
  tinted from the icon's dominant colour (`androidx.palette`), with the label underneath.
  See `tv-ui-design`.
- Clear the cache in `onTrimMemory(TRIM_MEMORY_UI_HIDDEN or higher)`.

## Launching

```kotlin
fun launch(context: Context, app: TvApp) {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(app.packageName)
        ?: pm.getLaunchIntentForPackage(app.packageName)
        ?: Intent(Intent.ACTION_MAIN).setComponent(app.component)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // App was removed or disabled between refreshes. Refresh the list and show a short message.
    } catch (e: SecurityException) {
        // Non-exported activity. Same handling.
    }
}
```

## Reacting to installs and removals

Use `LauncherApps.Callback`, because manifest-declared `PACKAGE_*` broadcasts are restricted:

```kotlin
val launcherApps = context.getSystemService(LauncherApps::class.java)
launcherApps.registerCallback(object : LauncherApps.Callback() {
    override fun onPackageAdded(pkg: String, user: UserHandle) = refresh()
    override fun onPackageRemoved(pkg: String, user: UserHandle) = refresh()
    override fun onPackageChanged(pkg: String, user: UserHandle) = refresh()
    override fun onPackagesAvailable(p: Array<out String>, u: UserHandle, r: Boolean) = refresh()
    override fun onPackagesUnavailable(p: Array<out String>, u: UserHandle, r: Boolean) = refresh()
}, Handler(Looper.getMainLooper()))
```

Debounce `refresh()` by about 500 ms, because updates fire several callbacks in a row.

## HDMI inputs

```kotlin
val tim = context.getSystemService(TvInputManager::class.java)
val hdmi = tim.tvInputList.filter { it.type == TvInputInfo.TYPE_HDMI && !it.isHidden(context) }
// label: it.loadCustomLabel(context) ?: it.loadLabel(context)
fun openInput(context: Context, input: TvInputInfo) {
    val uri = TvContract.buildChannelUriForPassthroughInput(input.id)
    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
```

Which app handles the `ACTION_VIEW` passthrough URI depends on the vendor (Xiaomi's Live TV
app). Check it on the device with
`adb shell cmd package query-activities -a android.intent.action.VIEW -d content://android.media.tv/passthrough/x`.
If nothing resolves, fall back to opening the vendor's input picker, which you find through the
device probe.

### "Signal on HDMI N" prompt (event-driven, zero polling)

```kotlin
class HdmiWatcher(context: Context, private val onSignal: (TvInputInfo) -> Unit) {
    private val tim = context.getSystemService(TvInputManager::class.java)
    private val states = mutableMapOf<String, Int>()
    private val callback = object : TvInputManager.TvInputCallback() {
        override fun onInputStateChanged(inputId: String, state: Int) {
            val before = states.put(inputId, state)
            if (state == TvInputManager.INPUT_STATE_CONNECTED && before != TvInputManager.INPUT_STATE_CONNECTED) {
                tim.getTvInputInfo(inputId)?.takeIf { it.type == TvInputInfo.TYPE_HDMI }?.let(onSignal)
            }
        }
    }
    fun start() {
        tim.tvInputList.forEach { states[it.id] = tim.getInputState(it.id) }
        tim.registerCallback(callback, Handler(Looper.getMainLooper()))
    }
    fun stop() = tim.unregisterCallback(callback)
}
```

- Own it at process level (in `LauncherApp`), not in the Activity, so it keeps working while
  the launcher is in the background and its process is alive.
- When the launcher comes back to the front, compare `getInputState` with the last known
  states. This catches events missed while the process was dead.
- To show the prompt over other apps, add a `TYPE_APPLICATION_OVERLAY` window. It must be
  focusable so the D-pad reaches the buttons. It needs `SYSTEM_ALERT_WINDOW`, granted over adb with
  `adb shell appops set <pkg> SYSTEM_ALERT_WINDOW allow`. Without it, prompt only inside the launcher.
- Debounce: ignore repeats within 30 s. After "Not now", don't ask about that input again until
  it disconnects.
- States mean connection (hotplug, or CEC power state), not "picture present". Check on the
  device what Xiaomi's HDMI service reports: run `adb shell dumpsys tv_input` while
  plugging in and powering on a device.

## Backgrounds

Built-in gradients, or images from USB, all offline:
[references/backgrounds.md](references/backgrounds.md).

## Not the launcher's job

- **Browser:** the Google TV Play Store doesn't list browsers. Sideload TV Bro
  (github.com/truefedex/tv-bro). Web search in the launcher opens URLs with `ACTION_VIEW`.
- **Screensaver:** a system feature that works with any launcher. Use Google's Ambient mode or
  Aerial Views. See [references/backgrounds.md](references/backgrounds.md).
- **Keyboard:** the system IME (Gboard) is used by every text field. Russian/English is set in Gboard.
- **Google Assistant:** it can't be fixed from a launcher. See the voice diagnostics in the
  `xiaomi-tv-a-pro-2025` skill and `docs/requirements.md` §7.

## Launcher behaviour (differs from a normal app)

- **Back on the root screen:** don't finish. A HOME activity has nowhere to go back to. Use
  `BackHandler` to scroll to the top and focus the first item. If already there, do nothing.
- **Home pressed while we are already in front:** it arrives as `onNewIntent` with
  `CATEGORY_HOME`. Treat it like Back: reset scroll and focus, close menus.
- **TV turned on → first favourite focused.** Favourites are always the first row, in the
  owner's manual order. Nothing automatic reorders them; launch-count sorting applies only to
  "All apps". In `LauncherApp`, register `Intent.ACTION_SCREEN_ON` at runtime (a callback, not
  background work), set a flag, and on the next `onResume` reset scroll and focus to the first
  favourite. A cold start does the same by default. Returning from an app with Back keeps
  focus on the tile you left from.
- **Installing APKs from USB:** declare `REQUEST_INSTALL_PACKAGES`, then stream the file into a
  `PackageInstaller` session and commit it with a `PendingIntent` status receiver. The system
  shows its own confirmation. The first time, the owner allows "unknown apps" for the launcher.
- **Never block startup:** show the cached list in the first frame. Keep
  `LauncherApp.onCreate` free of I/O.
- **Never crash in a loop.** If the stock launcher is disabled (see below), a crash loop
  leaves a black screen. Wrap app-list and banner loading in `runCatching`. Make the
  minimal UI (a Settings button plus the app grid) render even with an empty or failing
  repository.
- **System shortcuts:** `Settings.ACTION_SETTINGS`, `Settings.ACTION_WIFI_SETTINGS`, and
  the HDMI inputs above. Always guard `startActivity` with `resolveActivity` or a try/catch.

## Becoming the default Home on Google TV

Google TV hides or locks the Home-app picker, so this takes adb. Methods, safest first,
with exact commands and how to roll back: [references/default-home.md](references/default-home.md).

## Build, install, run

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n <pkg>/.LauncherActivity      # -W prints cold-start timings
adb logcat --pid=$(adb shell pidof -s <pkg>)
adb exec-out screencap -p > shot.png                   # screenshot of the real UI
```

Without network access to `dl.google.com` (as in the cloud session), Gradle can't resolve
AGP or AndroidX. Build in CI (GitHub Actions) or on a local machine instead.
