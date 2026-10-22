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

## Decisions to review

Defaults taken during unattended runs that the owner should look at.

_(none yet)_

## Blockers

_(none yet)_
