# Statement of Work — Simple TV Launcher v1

## 1. Purpose

Build an ad-free, lightweight home-screen launcher for the owner's **Xiaomi TV A Pro 2025 55"**
(Google TV). It replaces the stock Google TV home, which shows sponsored rows and service
recommendations. The launcher shows only the owner's own apps and the features listed below,
and it spends as little of the TV's 2 GB RAM and weak GPU as possible.

## 2. Inputs (already agreed)

| Document | Role |
|---|---|
| `docs/requirements.md` | Every requirement and decision (§0–§13) |
| `docs/ui-spec.md` | Screens, components, states, focus and Back behaviour, themes, languages |
| `docs/tech-stack.md` | Versions, libraries, architecture, testing, CI |
| `design/mockups/` | Approved mockups and `themes.css` (tokens of the 9 themes) |
| `.claude/skills/` | Project skills plus 7 official Android skills |

## 3. Scope

### In scope (v1)

1. **Home:** clock and date; a top bar with weather, HDMI inputs (signal dot), an iPhone button
   and a settings gear; a **Favourites** row (manual order only, always first); an **All
   apps** grid (5 columns, sorted by launch count); app banners with generated banners for
   icon-only apps; focus memory; Back, Home and TV-on all return to the first favourite.
2. **Shortcut tiles** for websites (added to All apps and Favourites like apps).
   **App menu** (long press OK): add to or remove from favourites, move, hide, app info,
   uninstall.
3. **HDMI:** event-driven "signal on HDMI N — switch?" prompt inside the launcher, plus over
   other apps when the overlay permission is granted. HDMI chips open the input.
4. **Weather:** IP-based location by default, manual city search, current conditions plus 12 h
   and 5 days, refreshed only while Home is visible.
5. **Settings panel:** TV quick tiles (Wi-Fi, Bluetooth, Sound, Picture, Sleep timer,
   Android settings) and launcher pages: Theme, Background, Language, Weather, HDMI
   prompt, Hidden apps, Install from USB, About.
6. **Themes:** all 9 (3 dark, 3 light, 3 cartoon), live preview, optional day/night schedule.
7. **Languages:** Беларуская, Русский, English, Polski, Deutsch, Français, and "Как на
   телевизоре" (the default, falling back to English).
8. **Background:** the theme's art, a solid colour, or an image from USB.
9. **USB APK installer**, which also handles updates of the launcher itself.
10. **Quality:** unit tests, Compose behaviour tests, Roborazzi screenshots of key screens in
    all 9 themes and in `be`/`fr`, an R8 release build, a GitHub Actions CI with the APK as an
    artifact, and a prepared Baseline Profile module.
11. **Owner guide:** how to install, make it the default Home, and grant the adb permissions.

### Out of scope (decided elsewhere)

- Screensaver: Google's default Ambient mode stays.
- Browser: TV Bro. Keyboard: Gboard. iPhone mirroring: the AirPlay Server app (the launcher only
  opens it).
- Voice input: waits for the owner's diagnostics (`requirements.md` §7).
- "Continue watching" rows, remote button remapping, weather/news beyond the widget, any ads,
  analytics or background services.

## 4. Deliverables

| # | Deliverable | Where |
|---|---|---|
| D1 | Android project (Gradle, `:app`, `:baselineprofile`) | repo root |
| D2 | Mockups for the screens not yet designed | `design/mockups/` + renders |
| D3 | Source code of every feature in §3 | `app/src/main/` |
| D4 | Strings in 6 languages | `app/src/main/res/values*/` |
| D5 | Tests and screenshot goldens | `app/src/test/` |
| D6 | CI workflow | `.github/workflows/android.yml` |
| D7 | Debug APK (CI artifact) and a reproducible release build | GitHub Actions |
| D8 | Owner guide and updated docs | `README.md`, `docs/` |

## 5. Acceptance criteria

1. `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug` passes
   locally and in CI.
2. Every screen in `docs/ui-spec.md` exists, matches its mockup (Roborazzi PNGs reviewed
   against the HTML renders), and works with the D-pad alone: initial focus, focus memory,
   Back/Home behaviour as specified.
3. All 9 themes and all 6 languages are selectable and render without clipping (screenshot
   tests for `be` and `fr`).
4. The manifest declares only what's needed (`INTERNET`, `REQUEST_INSTALL_PACKAGES`,
   `REQUEST_DELETE_PACKAGES`, `SYSTEM_ALERT_WINDOW`, `MANAGE_EXTERNAL_STORAGE`). No ad,
   analytics or tracking libraries are present, and nothing runs in the background.
5. The release APK is minified (R8) and its size is reported. The target is under 10 MB.
6. The docs (`requirements.md`, `ui-spec.md`, `tech-stack.md`, `README.md`) describe what
   was actually built.

Checked on the TV after delivery (it can't be checked from the cloud): first frame < 1 s,
≤ 120 MB PSS, HDMI events, the default-Home method, real banners, and Belarusian dates.

## 6. Assumptions and decisions for an unattended run

- **Mockups for new screens:** the run designs them in the existing style, following
  `ui-spec.md`, and builds them without waiting. Launching the run counts as the owner's
  approval. The owner reviews the renders afterwards, and screenshot tests make later
  changes cheap.
- **Defaults when something is open:** take the documented default, record it in
  `docs/progress.md` under "Decisions to review", and continue. Stop only if a task cannot be
  done at all.
- **Device facts are unknown** until the probe runs. Use minSdk 30 and guarded intents with
  fallbacks (vendor settings screens, the HDMI passthrough handler).
- **Cloud environment:** the Android SDK is installed by `scripts/setup-android-sdk.sh`.
  Network hosts are already allowed (Google Maven, Maven Central, Gradle, Open-Meteo).
  Maven Central and the Gradle Plugin Portal sometimes answer 429, so retry.
- **Translations:** written by Claude. The owner reviews Belarusian.

## 7. Milestones

| Milestone | Tasks (see `docs/tasks.md`) | Exit check |
|---|---|---|
| M0 Setup | T01–T05 | Empty TV app builds; one screenshot test and CI pass |
| M1 Design | T06 | Mockups for all remaining screens rendered and indexed |
| M2 Foundations | T07–T10 | Themes, settings store, languages and activity shell work in tests |
| M3 Home | T11–T13 | Home + app menu complete, screenshot-tested in 9 themes |
| M4 Features | T14–T23 | Settings, themes, languages, background, weather, HDMI, iPhone, hidden apps, USB installer, About |
| M5 Hardening | T24–T27 | R8 release, baseline-profile module, security review, docs and owner guide |

After M5, the owner runs the "On the TV" checklist in `docs/tasks.md`.
