# Launcher requirements (v1)

What the owner asked for, what we decided, and what still needs checking on the real TV.
Status: ✅ in the launcher, ⚠️ in the launcher with caveats, ❌ not a launcher job (solved elsewhere).

## 0. Principles (the owner's top priorities)

- **No ads.** No sponsored rows or service recommendations like on the stock Google TV home,
  and no ad or analytics SDKs. If the stock launcher is disabled (see `android-tv-launcher`),
  its ads disappear from the system entirely.
- **Network only on demand.** Internet and local-network access are fine for features the owner
  asked for, but only while that feature is in use. Nothing downloads in the background.
- **Minimal resources.** No background services, no scheduled jobs, no polling. Target
  ≤ 120 MB RAM while on screen, nothing extra while in the background, first frame in < 1 s.

## 1. Home screen: Favourites, then All apps ✅

- Top bar: clock and date on the left; HDMI inputs (with a "signal" dot) and Settings on the right.
- **Favourites always come first** (the owner's main requirement). One horizontal row,
  filled **only by hand**: long press on OK → "Add to favourites". Reorder with a "Move" mode
  (D-pad left/right, OK to drop). The order is exactly what the owner set; nothing automatic
  ever reorders or adds to it. Stored in DataStore.
- **When the TV is turned on**, focus lands on the first favourite. A cold start does this
  anyway; if the process survived standby, a screen-on callback resets focus on the next resume.
  When returning from an app with Back, focus stays on the tile you left from.
  Check on the TV: Xiaomi may have a "power on → last input/app" setting that skips Home entirely.
- **All apps**: a grid of banners, 5 per line, sorted by how often you launch them, then
  alphabetically (see §8). Phone-only apps without a TV banner get a generated banner
  (see `android-tv-launcher`).
- Mockup: `design/mockups/home.html`.

## 2. "Signal on HDMI N — switch?" prompt ⚠️

The owner wants it **only if it costs the TV nothing**.

- `TvInputManager.TvInputCallback.onInputStateChanged` delivers HDMI state changes as a binder
  callback. Nothing polls and no CPU is used while waiting. When an HDMI input moves to
  `INPUT_STATE_CONNECTED`, show a prompt: **Switch** (default focus) / **Not now**. It hides
  itself after 15 s, and Back closes it.
- Over the launcher: a normal in-app panel. Costs nothing extra.
- Over another app (YouTube and so on): this needs the overlay permission, granted once over adb
  (`appops set <pkg> SYSTEM_ALERT_WINDOW allow`), and the launcher process must still be alive.
  On a 2 GB TV the system often kills the launcher behind heavy apps, and then the event is
  missed. When the owner returns home, the launcher re-reads the input states and shows the prompt then.
- Not built by default: an always-on watcher, a tiny foreground service in its own process
  (about 15–25 MB RAM all the time). Build it only if missed prompts turn out to bother the owner.
- Caveats to verify on the TV:
  - Android reports *connection* state, not "picture present". For HDMI-CEC devices (consoles,
    set-top boxes, Apple TV) turning the device on usually flips STANDBY → CONNECTED, which
    matches "a signal appeared". For non-CEC devices it may only change when the cable is plugged in.
  - Xiaomi's HDMI input service must report these states to third-party apps. Check with
    `dumpsys tv_input` while plugging and powering a device.
  - If the TV's own CEC auto-switch ("One Touch Play") is on, the TV may switch by itself
    before our prompt appears. The owner chooses which behaviour to keep.
- Debounce: ignore repeats for the same input within 30 s. After "Not now", don't ask about
  that input again until it disconnects.

## 3. Easy background change ✅

- Launcher Settings → Background, with a live preview. Sources: built-in gradients/colours
  or images from a USB drive (copied into the app, so the drive can be removed). No downloads.
- Shown under a dark scrim so the UI stays legible. Decoded at 960×540, about 2 MB of RAM.

## 4. Screensaver ❌ (skipped: keep the system default)

- Decision: keep Google's default **Ambient mode**. Nothing to build or configure.
- The screensaver is a system feature that works with any launcher. If the owner ever wants
  nature videos without Google's cards, Aerial Views (free, open source) can be set over adb.
  See the `xiaomi-tv-a-pro-2025` skill.

## 5. Browser ❌ (not the launcher; install a TV browser)

- The Play Store on Google TV doesn't list Chrome or most browsers. A launcher can't change that.
- Decision: sideload **TV Bro** (open source, made for TV, with a D-pad mouse cursor and
  bookmarks) over adb, and pin it to Favourites.
- Not planned: a built-in WebView browser. It would be a lot of work, would have to run in a
  separate process to stay off the launcher's memory, and would still be worse than TV Bro.

## 6. Keyboard RU/EN ❌ (system, not the launcher)

- The on-screen keyboard is a separate system component (Gboard for TV). Any text field,
  including ours, uses it. Russian is added in Gboard's settings (Languages); switch with the
  globe key. The launcher doesn't take part.

## 7. Voice input from the remote ⚠️ (diagnose first)

- The launcher can't repair Google Assistant. Voice from the remote's mic goes Bluetooth
  remote → system voice service → Google app.
- Likely causes to check first:
  1. The remote is paired only over IR, not Bluetooth. The mic works only over Bluetooth.
     Check Settings → Remotes & Accessories.
  2. Microphone access is off: Settings → Privacy → Microphone, or the Google app's permission.
  3. The language or region is unsupported. Assistant on TV may refuse some UI languages or
     regions; plain voice search sometimes still works. Test by switching the UI to English.
- What the launcher could add after diagnosis: its own voice search for apps and the web,
  opened by the remote's mic button. This works only if the remote's mic audio is available to
  third-party apps on this TV, which we test with a small debug screen first. Recognition would
  use either Google's on-device recognizer, if it works, or offline Vosk (RU + EN models about
  45 MB each, run in a separate process only while listening). Because of the size, this would
  be a separate optional app, not part of the launcher APK.

## 8. Extra features (approved) ✅

None of these do background work.

1. **Hide apps + clean-up of preinstalled apps.** Hidden apps disappear from "All apps"
   (Settings lists them, to bring them back). Separately, adb `pm disable-user --user 0 <pkg>`
   for unused preinstalled apps (reversible with `pm enable`). This frees RAM and background
   activity; the list is built from the device probe, and the gain is measured before and after.
2. **Install APKs from a USB drive**, including updates of the launcher itself.
   `REQUEST_INSTALL_PACKAGES` + `PackageInstaller`; the owner allows "unknown apps" for the
   launcher once in system settings.
3. **"All apps" sorted by launch count** (own counters in DataStore, no usage-stats permission).
   Favourites are never affected.
4. **Shortcut tiles**: a website that opens in TV Bro, or a deep link into an app
   (`ACTION_VIEW`). They can be added to Favourites like apps.
5. **Quick actions in the top bar**: Wi-Fi, Bluetooth, sound output, picture mode, sleep timer.
   They open the matching system or Xiaomi settings page; which pages exist is checked on the TV.

Not chosen: PIN lock on apps.
Not possible: a "Continue watching" row
(other apps' Watch Next needs a system-only permission); remote button remapping (needs an
always-on accessibility service, so use Button Mapper if ever needed).

## 9. iPhone screen on the TV ⚠️ (separate open-source app; the launcher adds a button)

The owner has an iPhone. Google TV has no AirPlay, and the TV's Chromecast and Miracast don't
help with mirroring an iPhone screen.

- **Screen mirroring: AirPlay Server** (`io.github.jqssun.airplay`, github.com/jqssun/android-airplay-server).
  Open source (GPL-3.0), built on UxPlay. Supports Android TV and the D-pad, H.264/H.265
  mirroring, and audio. It's on Google Play, F-Droid and GitHub. On the iPhone: Control Centre →
  Screen Mirroring → pick the TV.
- The launcher's top bar gets an **"iPhone screen"** button that opens AirPlay Server, so the
  receiver runs only when wanted.
- Check on the TV: stability and latency on this hardware, and whether the app keeps a service
  running after it is closed (the resources principle). DRM content (the Apple TV app, Netflix)
  won't mirror; for those, use the TV's own app.
- **Photos and videos without mirroring:** the Google Photos and YouTube apps on the iPhone have a
  Cast button, which works with the TV's Chromecast built-in. Nothing to build.
- Not built into the launcher: an AirPlay receiver is native code, a reverse-engineered protocol and
  a constantly running discovery service. A separate app keeps the launcher light. Fallback idea
  if AirPlay Server disappoints: a launcher "Photos from phone" screen that shows a QR code. The
  iPhone opens it in Safari and uploads photos over the local network, and the small server runs
  only while that screen is open.

## 10. Weather ✅

- A weather chip in the top bar (icon, temperature, city). OK opens a side panel with the
  current conditions, the next 12 hours, 5 days, and a **"Change city"** button.
- **Location by default: automatic**, from the internet connection (IP geolocation). The TV has
  no GPS, and this way there's no permission dialog. If the city is wrong (for example, because
  of a VPN), pick it once by hand: a search by name in Russian. "Automatic" stays one press away.
- Data from Open-Meteo: free, no API key, no ads. The panel shows only the weather: no text
  about the source or the location mode (the owner's request). The licence credit lives in
  launcher Settings → About.
- Network use: only while the home screen is on screen, at most every 30 minutes, a few KB per
  update. The last result is cached and shown instantly. Nothing runs in the background.
- This is the first network feature, so the launcher gets the `INTERNET` permission.
- Details: `android-tv-launcher` skill, `references/weather.md`. Mockup: `design/mockups/weather-panel.html`.

## 11. Themes ✅ (all fourteen ship, switchable)

- The owner liked all nine, so all of them ship: dark (Midnight, Graphite, Aurora), light (Paper,
  Sky, Sage), and cartoon (Sunny valley, Underwater, Space).
- 2026-10-23: the owner asked for three dark cartoon themes (gloomy, horror, dark but cheerful) and a
  Halloween pair (dark and light): Gloomy hill, Haunted house, Monster party, Halloween night and Halloween day.
  The first "gloomy" draft read as sad (rain, a crying cloud), so it became a Burton-like hill; the
  first monster party had white chips and too little colour, so it became darker with a three-colour
  palette. Each new theme is a separate family row in the picker.
- Settings → Theme: thumbnails grouped by family with **live preview**. The focused theme is applied
  to the screen at once, OK saves it, and Back cancels.
- Optional "change by time of day" (off by default): a day theme and a night theme, switched at
  sunrise and sunset, checked only while the home screen is visible.
- Panels (settings, weather, prompts) follow the theme too. Backgrounds are static images, so no
  theme costs more than another.
- Mockups: `design/mockups/themes/*.png`, `design/mockups/settings/theme-picker.png`.

## 12. Settings ✅

- The gear in the top bar opens the settings panel.
  - **TV** (quick tiles): Wi-Fi, Bluetooth, Sound, Picture, Sleep timer, Android settings.
  - **Launcher**: Theme, Background, Language, Weather, HDMI signal prompt, Hidden apps,
    Install from USB, About.
- "Background" defaults to the theme's own art. A custom colour or an image from USB overrides it.
- Mockup: `design/mockups/settings/settings-*.png`.

## 13. Languages ✅

- The launcher's own language, in this order (the owner's request): **Беларуская** first, then
  Русский, English, Polski, Deutsch, Français, and "Same as the TV" last. Each language is shown
  by its native name.
- **Default: the TV's language** ("Same as the TV"). If the TV's language is not one of the six,
  the launcher uses **English**. Android always reports the system language, so no extra
  permission is needed.
- Everything the launcher shows switches: titles, dates, weather texts and city names. App names
  come from the apps themselves.
- Translations are written in-house; the owner checks them, especially Belarusian.
- Mockups: `design/mockups/settings/language-picker.png`, `design/mockups/settings/languages.png`.
- Details: `android-tv-launcher` skill, `references/settings-themes-i18n.md`.

## 14. Phone remote and AI access ✅ (built 2026-10-25, tasks T33–T44; owner checks in T43 left)

The owner asked to control the TV from a phone, and to let any AI assistant in the home (Claude or
another one) do what a person asks: "play this YouTube video", "open Kinopub and find Dark", "search the
web for…". Anyone at home may connect their own assistant.

### What it is

One small web server inside the launcher, with two front ends on top of one "TV control" layer:

1. **Phone remote**: a web page for the phone's browser (iPhone Safari; nothing to install, can be
   added to the home screen).
   - **Remote** (default): D-pad, OK, Back, Home, and the phone keyboard typing into the field selected
     on the TV.
   - **Screen** (extension, switched on from the page): a live picture of the TV (about one frame a
     second). Tap on the picture presses the element under the finger, long-press works as a long
     press, a swipe scrolls, and text goes into the tapped field.
   - **Same theme as the launcher** (owner's request): the page uses the TV's current theme: its tokens
     from `themes.css` (colours, radii, focus colour, pills, Nunito for cartoon themes), its background
     art behind the top of the page, and its button style. When the theme changes on the TV (by hand
     or by the day/night schedule), the open page follows without a reload. Its language is the
     launcher's language.
2. **AI access (MCP)**: an MCP server (Streamable HTTP) that an assistant on a computer or phone in the
   same network connects to. Tools: `list_apps`, `open_app`, `youtube_search`, `youtube_play`,
   `open_url`, `web_search`, `screenshot`, `read_screen`, `tap`, `scroll`, `type_text`, `press_key`,
   `power_off`, `sleep_timer`.

### Hard requirements

- **Local network only.** The server listens on the TV's Wi-Fi address and rejects any address that is
  not private. No cloud relay, no port forwarding, no remote access from outside the home.
- **Off by default.** Turned on in Settings → "Phone remote & AI access" (two switches on one page).
- **TV off = access gone.** Keys live only in memory, never on disk. The server closes and every key is
  erased when the screen turns off (standby, sleep timer, "Turn off TV"), on reboot or power loss, when
  the launcher process dies, when the accessibility helper is turned off, and on an app update. After the
  TV is on again, access is off and has to be granted from scratch with a new code. An assistant
  can't turn the TV on. (Google's Ambient screensaver keeps the screen on, so access continues there.)
- **Pairing.** The TV shows a 6-digit code (and a QR code for the phone). Each phone or assistant
  exchanges it once for its own random 128-bit key. Five wrong codes → 60 s lockout. The phone's key
  travels in the QR fragment (`#`), never in a URL that a server could log.
- **One phone at a time.** A new phone takes over; the old page shows "Disconnected".
  **Several assistants** may be connected; the TV lists them by name, each can be disconnected, and
  there is "Disconnect all".
- **Visible.** While anyone is connected, a small badge shows on the TV over any app ("📱 Remote",
  "AI is controlling").
- **Auto-off** after 15 minutes without requests.
- **No dangerous tools:** no install or uninstall, no system settings, no shell passthrough.
- **Resources:** nothing runs while it is off. While on: the server is about 1 MB; screenshots are taken
  only while a client asks for them. An owner-approved exception to "nothing in the background".

### What the TV can do (spike on the Xiaomi TV A Pro 2025, Android 11, 2026-10-25)

| Capability | Result |
|---|---|
| Accessibility helper keeps working after the update adds capabilities | ✅ no new adb command |
| Screen picture through the accessibility helper (`takeScreenshot`) | ✅ but at most one per ~1.1 s (about 0.9 fps); 120–160 ms of work, ~65 KB JPEG at 960×540 |
| Injected touch gestures (`dispatchGesture`) | ❌ refused instantly by this firmware (touch from `adb shell input tap` works) |
| Tap / long-press / scroll on the accessibility node under a point | ✅ TV Bro (also inside web pages), Kinopub, the launcher |
| Text into a field (`ACTION_SET_TEXT`, any language) + Enter (`ACTION_IME_ENTER`) | ✅ Google search in TV Bro, including Cyrillic |
| D-pad by moving accessibility focus; Back/Home global actions | ✅ Kinopub, browser, launcher; Back/Home everywhere |
| YouTube | ❌ its UI is not exposed to accessibility (taps and focus moves do nothing) |
| Real key events through the TV's own adbd from inside the app (loopback adb, `dadb`) | ✅ everywhere, YouTube included; connected without an on-screen prompt; ~1 s per command |
| `input text` through loopback adb | ✅ Latin only; Cyrillic via clipboard paste doesn't reach YouTube's field |
| YouTube deep links (`youtube.com/results?search_query=…`, `watch?v=…`) | ✅ open the TV app directly |

### Decisions

- **Picture:** accessibility screenshots (no prompt, no foreground service), about 1 fps. A smoother
  mode through MediaProjection (system prompt every time) only if the owner asks later.
- **Taps:** act on the node under the finger; injected gestures don't work on this TV.
- **Keys:** in the launcher, delivered in-process; elsewhere, real key events through loopback adb;
  when adb is unavailable, accessibility focus moves and global actions as a fallback.
- **Text:** `ACTION_SET_TEXT` into the tapped or focused field (any language), then Enter; for apps
  without accessible fields (YouTube), `input text` (Latin). Cyrillic YouTube searches go through the
  `youtube_search` deep link.
- **Loopback adb** needs network debugging to stay on (it is on for this TV). Library: `dadb`
  (Apache-2.0, depends on okio). If adb is off, the page and the AI tools say so and keep working in
  the reduced mode.
- **Accessibility helper:** gains `canTakeScreenshot`; its label and description change from "reads
  nothing" to "turns the TV off; while Phone remote or AI access is on, it sees the screen and presses
  buttons for you".
- **Not planned:** access from outside the home network, video streaming, voice.

## Status after the first unattended run

Everything marked ✅ above is built and covered by unit, behaviour and screenshot tests (`docs/progress.md` has
the log and "Decisions to review"). What only the TV can confirm is listed in `docs/tasks.md` ("On the TV"):
vendor intents (Picture, Sleep timer, input picker), HDMI behaviour, the default-Home method, memory and
startup budgets, Belarusian dates, the Baseline Profile and the voice diagnostics (§7).

## Open questions for the owner

- Hidden section on Home (T32): answered — keep both the section and Settings → Hidden apps.

- Account avatar (T30): which photo should it show? The Google account photo is not available to a
  launcher without Google sign-in; the default is a picture from USB or the first letter of a name.

- Over other apps: is the prompt worth granting the overlay permission over adb (one command)?
- What are the results of the voice checks in section 7?
- Visual style: is the mockup direction right (dark, photo background, white focus)?
