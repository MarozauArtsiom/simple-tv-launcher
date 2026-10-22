# Launcher requirements (v1)

What the owner asked for, what we decided, and what still needs checking on the real TV.
Status: ✅ in the launcher, ⚠️ in the launcher with caveats, ❌ not a launcher job (solved elsewhere).

## 0. Principles (the owner's top priorities)

- **No ads.** The launcher has no `INTERNET` permission, so it cannot load ads or send data.
  Side benefit: if the stock Google TV launcher is disabled (see `android-tv-launcher`),
  its sponsored rows disappear too.
- **Minimal resources.** No background services, no scheduled jobs, no polling. Target
  ≤ 120 MB RAM while on screen, nothing extra while in the background, first frame in < 1 s.

## 1. Home screen: Favourites, then All apps ✅

- Top bar: clock and date on the left; HDMI inputs (with a "signal" dot) and Settings on the right.
- **Favourites**: one horizontal row. Add or remove with a long press on OK, then "Add to favourites".
  Reorder with a "Move" mode (D-pad left/right, OK to drop). Stored in DataStore.
- **All apps**: a grid of banners, 5 per line, sorted alphabetically. Phone-only apps without
  a TV banner get a generated banner (see `android-tv-launcher`).
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
  45 MB each, run in a separate process only while listening). Because of the size and the
  offline principle, this would be a separate optional app, not part of the launcher APK.

## 8. Candidate features (proposed, awaiting the owner's choice)

All of these are offline and do no background work.

| Idea | Cost | Notes |
|---|---|---|
| Hide apps + adb clean-up of unused preinstalled apps | none in the launcher | `pm disable-user`, reversible with `pm enable`. Frees RAM and background activity; measure before and after |
| Install APKs from a USB drive (also updates the launcher itself) | none until used | `REQUEST_INSTALL_PACKAGES`, and the owner allows "unknown apps" once |
| "All apps" sorted by how often you launch them | tiny | Own launch counters in DataStore; no usage-stats permission |
| Shortcut tiles: websites (open in TV Bro), deep links into apps | tiny | `ACTION_VIEW` with a URL or deep link |
| Quick actions in the top bar: Wi-Fi, Bluetooth, sound output, picture mode, sleep timer | tiny | Open system/Xiaomi settings pages; which ones exist must be checked on the TV |
| PIN lock on chosen apps | small | Soft lock: only blocks launching from our launcher |

Not possible or against the principles: weather/news (needs internet); a "Continue watching" row
(other apps' Watch Next needs a system-only permission); remote button remapping (needs an
always-on accessibility service, so use Button Mapper if ever needed).

## Open questions for the owner

- Over other apps: is the prompt worth granting the overlay permission over adb (one command)?
- What are the results of the voice checks in section 7?
- Visual style: is the mockup direction right (dark, photo background, white focus)?
