---
name: xiaomi-tv-a-pro-2025
description: Device profile of the target TV (Xiaomi TV A Pro 2025 55", Google TV, MediaTek MT9611, 2 GB RAM, Mali-G52) and what it means for the launcher's performance and memory budget. Also covers how to connect adb, probe the real device into docs/device/, read remote keycodes, install builds, take screenshots and profile on the TV. Load for anything device-specific or when testing on hardware.
---

# Target device: Xiaomi TV A Pro 2025, 55"

## Known specs (public sources; confirm with the probe)

| | |
|---|---|
| OS | Google TV. Xiaomi doesn't publish the Android TV OS version, so read `ro.build.version.sdk` |
| SoC | MediaTek **MT9611** (per Xiaomi's FAQ for the 55" 2025) |
| CPU / GPU | 4× Cortex-A55 / Mali-G52 (MC1 on most regional pages, MP2 on one) |
| RAM / storage | **2 GB** / 8 GB |
| Panel | 55" 4K UHD 3840×2160 QLED, about 94% DCI-P3, HDR10 / HLG, MEMC |
| UI surface | probably 1920×1080 at density 320 (960×540 dp); confirm with `wm size`/`wm density` |
| Audio | 2× 10 W, Dolby Audio, DTS:X |
| I/O | HDMI 2.1 (ALLM, eARC), USB, Wi-Fi, Bluetooth |

Once `docs/device/profile.md` exists, it is the source of truth and overrides this table.

## What this hardware means for the launcher

- **Memory is the tightest limit.** Google TV itself uses most of the 2 GB. Keep the
  launcher's PSS at or below about 120 MB (`adb shell dumpsys meminfo <pkg>`), and keep
  the bitmap cache at or below about 16 MB. Release caches in `onTrimMemory`. Expect the
  process to be killed while big apps (YouTube, Netflix) run.
- **Cold start happens all the time.** After an app exits, the launcher often starts
  from scratch. Target: first frame with the cached app list in under 1 s
  (`am start -W` TotalTime). Use a Baseline Profile.
- **The GPU is weak.** One Mali-G52 core drives 1080p UI plus the 4K upscale. Follow the
  motion budget in `tv-ui-design`: no blur, no large translucent layers, animations only
  through graphicsLayer.
- **Storage is small.** Keep the APK small: R8, no bundled big images, vector icons.
- **The CPU is a quad A55** (in-order, slow single core). Keep work off the main thread
  and avoid reflection-heavy libraries.

## Connecting adb

1. Settings → System → About → **Android TV OS build**: press OK 7 times to enable Developer options.
2. Settings → System → Developer options → turn on **USB debugging**. On Google TV 12+
   there is also **Wireless debugging** with pairing.
3. From a computer on the same network:
   ```bash
   adb connect <tv-ip>:5555                  # classic network adb
   # or, with Wireless debugging:
   adb pair <tv-ip>:<pair-port> <code>  &&  adb connect <tv-ip>:<port>
   ```
   Accept the "Allow debugging" prompt on the TV, and tick "Always allow".

The cloud session can't reach the TV. Device work happens on the owner's computer,
ideally with Claude Code running locally so it can run adb itself.

## Probe the device (do this once, commit the result)

```bash
.claude/skills/xiaomi-tv-a-pro-2025/scripts/probe-device.sh [serial]
```

This writes these files to `docs/device/`:
- `profile.md`: build props, Android version and SDK, display mode and density, memory,
  HOME handlers and the role holder, the HDMI passthrough handler, and features.
- `packages-system.txt` and `packages-user.txt`: Xiaomi and Google packages, including the
  stock launcher and the Live TV app.
- `tv-input.txt`: HDMI inputs and their ids.

Then record the remote's keycodes by hand: run `adb shell getevent -lt`, press each
button (D-pad, OK, Back, Home, Settings, Input, the app buttons, mic), and write
`docs/device/remote.md`. Long-press OK and the Settings/Input keys are good candidates for
the context menu and quick actions.

## Everyday commands

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n <pkg>/.LauncherActivity           # cold-start timing
adb exec-out screencap -p > shot.png                        # UI screenshot (DRM video shows black)
adb shell dumpsys meminfo <pkg>                             # memory
adb shell dumpsys gfxinfo <pkg> reset; <use the UI>; adb shell dumpsys gfxinfo <pkg> framestats  # jank
adb shell input keyevent KEYCODE_HOME                        # simulate remote: DPAD_UP/DOWN/LEFT/RIGHT, DPAD_CENTER, BACK
adb logcat --pid=$(adb shell pidof -s <pkg>)
```

Making the launcher the default Home (role assignment, or disabling launcherx) and rolling
it back: `android-tv-launcher` skill, `references/default-home.md`.
