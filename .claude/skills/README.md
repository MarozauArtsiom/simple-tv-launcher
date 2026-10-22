# Skills in this repo

## Project skills (written for this launcher)

| Skill | Scope |
|---|---|
| `android-tv-launcher` | Launcher engineering: manifest, apps, HDMI, weather, settings, themes and languages, default Home |
| `tv-ui-design` | TV UI rules, Compose-for-TV patterns, HTML mockups |
| `xiaomi-tv-a-pro-2025` | The target TV: limits, adb, probing, profiling |

## Official Android skills (vendored from Google)

Copied unchanged from <https://github.com/android/skills>, commit
`95ea931a1ad8f9b2be132ae0cfb2aad964d71118` (2026-10-21), under the Apache License 2.0
(`LICENSE.txt` in each folder).

| Skill | Use it for |
|---|---|
| `jetpack-compose-performance` | Recomposition, lazy lists, animation and effects audits |
| `leanback-to-compose-tv-migration` | Google's Compose-for-TV rules and reference screens (focus, `focusRestorer`, text input, lazy containers). We don't migrate anything; it is used as a TV reference |
| `agp-9-upgrade` | AGP 9 build DSL, built-in Kotlin, KSP and BuildConfig recipes |
| `testing-setup` | Test strategy and harness setup |
| `android-cli` | Google's `android` CLI: SDK install, devices, screenshots, docs search |
| `r8-analyzer` | Keep rules and release size |
| `android-intent-security` | Exported components and intent handling (HOME activity, launch intents) |

**Project decisions win.** If a Google skill disagrees with `docs/tech-stack.md`,
`docs/ui-spec.md` or a project skill, follow the project docs. Known differences:
- No Hilt or any DI framework; manual DI instead.
- Screenshot tests use Roborazzi, not the deprecated preview-screenshot plugin.
- No Coil: there are no network images.
- No Media3 playback screens.

**Updating:** `git clone --depth 1 https://github.com/android/skills` into a scratch folder,
read the diff of these seven folders, copy them over, and update the commit hash above. If the
Android CLI is installed, `android skills update` does the same thing.
