# Launcher backgrounds

Backgrounds are built in or come from the TV's own storage. Nothing is downloaded in the
background.

- Launcher Settings → Background, with a live preview. Choices:
  - built-in gradients or solid colours (shipped as tiny vectors or code, no PNGs);
  - images from a USB drive through MediaStore (`READ_MEDIA_IMAGES` on API 33+,
    `READ_EXTERNAL_STORAGE` below). Google TV has no system photo picker, so we draw our own
    grid of thumbnails, decoded small (about 160×90 dp) and only while the picker is open.
- Copy the chosen image into app storage, downscaled to 960×540, so the background still
  works after the USB drive is removed.
- Decode the background at 960×540 (about 2 MB). It sits under a dark scrim and the UI, so
  it doesn't need to be sharp, and a softer background helps legibility.
- Change it with a 300 ms crossfade. Persist the choice in DataStore.

## Screensaver: not ours

The screensaver is a separate system feature (a `DreamService` chosen in system settings).
It works with any launcher, so the launcher doesn't ship one. Options for the owner:
- Google's Ambient mode (already on the TV). To hide personal cards: Settings → Accounts &
  Sign In → account → Google Assistant → "Never show proactively".
- Aerial Views: free and open source (github.com/theothernt/AerialViews). Nature aerial
  videos, or photos from USB. It is set over adb; see the `xiaomi-tv-a-pro-2025` skill.
