# Making our launcher the default Home on Google TV

Google TV ships `com.google.android.apps.tv.launcherx` as Home. Usually there is no picker
in Settings, and the Home key always goes to it. There are four ways around this, from
least to most invasive. Use the first one that keeps the Home key on our launcher after a
reboot.

Before starting:
- Run the device probe (`xiaomi-tv-a-pro-2025` skill). Write down the current Home
  handlers and the HOME role holder, so you can restore them exactly.
- Keep adb connected while experimenting. Every step below can be undone from adb.

```bash
# Who can handle HOME, and who holds the role now
adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell cmd role get-role-holders android.app.role.HOME
```

## 1. Settings UI (no adb)

Look under Settings → Apps → Default apps → Home app (or similar). Most Google TV builds
don't have it, but check this Xiaomi build first.

## 2. Assign the HOME role over adb (non-destructive)

```bash
adb shell cmd role add-role-holder --user 0 android.app.role.HOME <pkg>
# older equivalent:
adb shell cmd package set-home-activity --user 0 <pkg>/<pkg>.LauncherActivity
adb shell cmd role get-role-holders android.app.role.HOME   # verify
```

Then press Home on the remote, reboot, and press Home again. Some Google TV builds keep
sending Home to launcherx anyway, or reset the role on boot. If that happens, go to step 3.

Undo: `adb shell cmd role add-role-holder --user 0 android.app.role.HOME com.google.android.apps.tv.launcherx`

## 3. Disable the stock launcher (works almost everywhere; side effects)

```bash
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith   # fallback "setup" Home that re-enables launcherx
```

With them disabled, our launcher is the only HOME handler left, so the system uses it.

Known side effects (check each one on the device):
- Google TV features from launcherx are gone: the For You and Live tabs, profiles,
  recommendations, and possibly the side panel or quick settings opened from the remote.
  Our launcher has to provide a Settings entry point.
- Some remote buttons (for example YouTube) can stop working, because they are routed
  through launcherx.
- System OTA updates may re-enable the packages. Re-check after updates.

Undo:
```bash
adb shell pm enable com.google.android.apps.tv.launcherx
adb shell pm enable com.google.android.tungsten.setupwraith
```

Test the undo commands once before relying on this method. If our launcher crash-loops
while the stock one is disabled, the TV shows a black screen until you run the undo
commands. That is why the launcher must never crash on startup (see SKILL.md).

## 4. Accessibility redirect (no adb, flickers)

This is the approach Projectivy and Button Mapper use. An `AccessibilityService` watches
`TYPE_WINDOW_STATE_CHANGED`. When the foreground package becomes launcherx, it starts our
`LauncherActivity`. The downsides:
- The stock home flashes for a moment.
- The user has to enable the service in Settings → Accessibility.
- It depends on window events, which can break with Google TV updates.

Use this only if 2 and 3 are not acceptable.

## Record the outcome

After you find a working method, write the method, the commands, and any side effects
you saw into `docs/device/default-home.md`, so later sessions don't repeat the experiments.
