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

## Decisions to review

Defaults taken during unattended runs that the owner should look at.

- T06: new screens were designed by Claude without prior owner review (as agreed for this run).
  Look at `design/mockups/screens/*.png`, especially the app menu and the USB installer.
- Gboard's keyboard height (~168 dp) is an estimate in the city-search mockup; check on the TV.

## Blockers

_(none yet)_
