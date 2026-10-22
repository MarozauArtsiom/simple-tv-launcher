# Simple TV Launcher

A custom home-screen launcher for one specific TV: **Xiaomi TV A Pro 2025 55"**
(Google TV, 4K QLED, HDMI 2.1).

## Language
The owner writes in Russian, so reply in Russian. Code, identifiers, commit messages and
skill files stay in English.

## Skills: load before working
Project skills live in `.claude/skills/`. Load the matching one before you write code or
design anything:

| Skill | Load when |
|---|---|
| `android-tv-launcher` | any app code: manifest, app list, launching apps, HOME replacement, HDMI inputs, build/install |
| `tv-ui-design` | any screen, component, colour, type, focus state, animation or mockup |
| `xiaomi-tv-a-pro-2025` | anything device-specific: hardware limits, adb, probing or testing on the real TV |

## Stack (decided)
- Kotlin, one activity, Jetpack Compose + `androidx.tv:tv-material` (Material 3 for TV).
- No Leanback library and no XML views for UI.
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).
- Sideloaded over adb, not published to Google Play.

## Principles (the owner's top priorities)
- **No ads, no tracking.** Unlike the stock Google TV home: no sponsored rows, no service
  recommendations, no ad or analytics SDKs. Only the owner's own apps and features.
- **The network is allowed** for features the owner asked for, and only while that feature
  is in use (on demand, never in the background). Add `INTERNET` to the manifest together
  with the first feature that needs it.
- **As few system resources as possible.** Nothing runs in the background: no services,
  no scheduled jobs, no polling. System events (app installs, HDMI state) arrive as
  callbacks. Budget: ≤ 120 MB PSS while on screen, first frame in < 1 s, small APK.
- Prefer existing system features and small open-source apps over building things into
  the launcher. Screensaver: Google's default Ambient mode. Browser: TV Bro. Keyboard: Gboard.
  iPhone screen mirroring: AirPlay Server (open source).

## Cloud session limits
Check these before promising a build or a device test:
- Android builds need `dl.google.com` (Android SDK + Google Maven; `maven.google.com`
  redirects there). Test with `curl -sI https://dl.google.com/android/repository/repository2-3.xml`.
  A 403 means the environment's network policy still blocks it.
- There is no `/dev/kvm`, so the emulator cannot run here, and the TV is not reachable from the cloud.
- What does work here: writing code, HTML mockups rendered to 1920×1080 PNG with Playwright
  (see `tv-ui-design`), and CI builds on GitHub Actions.
- For quick on-device iteration, run Claude Code on a computer on the same LAN as the TV,
  so that `adb` can reach it.
