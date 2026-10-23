# Simple TV Launcher

A custom home-screen launcher for one specific TV: **Xiaomi TV A Pro 2025 55"**
(Google TV, 4K QLED, HDMI 2.1).

## Language
Reply in the language the user writes in. Code, identifiers, commit messages, docs and skill
files stay in English. UI strings ship in six languages (`app/src/main/res/values*/strings.xml`).

## Documents: the source of truth
| File | Holds |
|---|---|
| `docs/requirements.md` | What the owner asked for, every decision, and open questions |
| `docs/ui-spec.md` | Every screen and component: sizes, states, initial focus, Back behaviour, which mockups exist and which screens still need one |
| `docs/tech-stack.md` | Versions (verified 2026-10-22), libraries used and deliberately not used, architecture, testing, CI, SDK setup |
| `design/mockups/` | Approved HTML mockups; `themes.css` has the exact tokens of all nine themes |
| `docs/sow.md` | Scope, deliverables, acceptance criteria and milestones of v1 |
| `docs/tasks.md` | Ordered tasks T01–T27 with verification gates, plus the on-TV checklist |
| `docs/progress.md` | Log of finished tasks, "Decisions to review", blockers. Resume from the first unchecked task |
| `docs/run-prompt.md` | The single prompt that runs all of v1 (T01–T27) in one session |

Workflow: requirement → mockup → owner approves → code that matches `ui-spec.md` and the
mockup → Roborazzi screenshot test. Before scaffolding or upgrading, re-check versions with
`.claude/skills/android-tv-launcher/scripts/latest-versions.sh`.

## Skills: load before working
Load the matching skill before you write code or design anything. `.claude/skills/README.md`
lists them all.

| Skill | Load when |
|---|---|
| `android-tv-launcher` | any app code: manifest, app list, launching apps, HOME replacement, HDMI, weather, settings, build/install |
| `tv-ui-design` | any screen, component, colour, type, focus state, animation or mockup |
| `xiaomi-tv-a-pro-2025` | anything device-specific: hardware limits, adb, probing or testing on the real TV |
| `leanback-to-compose-tv-migration` (Google) | Compose-for-TV reference: focus, `focusRestorer`, text input, lazy lists |
| `jetpack-compose-performance` (Google) | writing or reviewing Compose UI for performance |
| `agp-9-upgrade`, `testing-setup`, `r8-analyzer`, `android-intent-security`, `android-cli` (Google) | build DSL, tests, release shrinking, manifest/intents, SDK and devices |

Project docs win over Google's generic advice (see `.claude/skills/README.md`).

## Stack (decided; details in `docs/tech-stack.md`)
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
- The environment allows `dl.google.com` (Android SDK + Google Maven) and the Open-Meteo hosts
  (verified 2026-10-22). If a build fails to download, re-check with
  `curl -sI https://dl.google.com/android/repository/repository2-3.xml`.
- The container is fresh each session, so the Android SDK isn't preinstalled. Install
  cmdline-tools into `~/android-sdk` (the build number is in `repository2-3.xml`). Note that
  `sdkmanager` now uses `platforms/android-NN` paths in `--list` and points to a newer
  `android sdk` CLI.
- Build with `scripts/gradlew-retry.sh <tasks>`: Maven Central answers HTTP 429 to cloud IPs,
  and the script retries with growing pauses. Set the SDK up first with
  `scripts/setup-android-sdk.sh` (idempotent, about 30 s).
- Robolectric tests need a 200 MB jar from Maven Central, which 429s. If a test fails with
  "Failed to fetch maven artifact ...android-all-instrumented:<ver>", run
  `scripts/prefetch-robolectric.sh <ver>` once, then rerun.
- The preinstalled `gradle` is 8.14 and cannot configure this project. Use `./gradlew` (9.8.2).
- There is no `/dev/kvm`, so the emulator cannot run here, and the TV is not reachable from the cloud.
- What does work here: writing code, HTML mockups rendered to 1920×1080 PNG with Playwright
  (see `tv-ui-design`), and CI builds on GitHub Actions.
- For quick on-device iteration, run Claude Code on a computer on the same LAN as the TV,
  so that `adb` can reach it.
