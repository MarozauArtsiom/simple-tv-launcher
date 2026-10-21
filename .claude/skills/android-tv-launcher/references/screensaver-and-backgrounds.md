# Screensaver (nature photos) and launcher backgrounds

## Screensaver = `DreamService`

Put the screensaver in the launcher APK, but in its own process, so its bitmaps never
weigh on the launcher and a crash can't take Home down.

```xml
<service
    android:name=".dream.NatureDreamService"
    android:exported="true"
    android:label="@string/dream_name"
    android:permission="android.permission.BIND_DREAM_SERVICE"
    android:process=":dream">
    <intent-filter>
        <action android:name="android.service.dreams.DreamService" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</service>
```

```kotlin
class NatureDreamService : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false     // any remote key wakes the TV back to what was on screen
        isFullscreen = true
        isScreenBright = true
        setContentView(SlideshowView(this))   // lightweight: two image layers + clock + credit
    }
    override fun onDreamingStarted() { /* start the slideshow timer */ }
    override fun onDreamingStopped() { /* stop the timer, recycle bitmaps */ }
}
```

Slideshow rules:
- Change the photo every 45–60 s with a 1–2 s crossfade. An optional, very slow Ken Burns
  pan uses one layer transform, which this GPU handles fine.
- Decode at the screen size (1920×1080, ARGB_8888, about 8 MB). Hold at most two bitmaps,
  only during the crossfade.
- Always show the credit (author · licence · "Wikimedia Commons"), labelMedium, bottom-left
  inside the safe area. Show a small clock bottom-right.
- Move the clock and credit a few dp every few minutes. The panel is LCD/QLED with no
  burn-in, but static text over hours still looks bad.

### Turning it on (Google TV has no UI for third-party screensavers)

```bash
adb shell settings put secure screensaver_components <pkg>/<pkg>.dream.NatureDreamService
adb shell settings get secure screensaver_components                       # verify
adb shell settings put system screen_off_timeout 600000                    # idle time before it starts, ms (ATV 12+: min 360000)
# restore Google's Ambient mode:
adb shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
```

Before changing anything, record the original value from the device in `docs/device/`.
Xiaomi may use a different default component.

## Image source: Wikimedia Commons featured pictures

The source is free, needs no API key, and every file has an open licence (most are CC BY /
CC BY-SA, so attribution is mandatory). Wikimedia's API policy requires a descriptive
`User-Agent` (app name + contact).

```
https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2
  &generator=categorymembers&gcmtitle=Category:Featured_pictures_of_landscapes
  &gcmtype=file&gcmlimit=50
  &prop=imageinfo&iiprop=url|size|extmetadata&iiurlwidth=1920
  &iiextmetadatafilter=Artist|LicenseShortName|LicenseUrl|ObjectName
```

- Keep only landscape images (`width / height >= 1.5`) at least 1920 px wide. Download
  `thumburl`, the server-scaled 1920 px version, and never the original (that can be 50+ MB).
- `Artist` is HTML. Strip the tags before showing it.
- Page with `continue.gcmcontinue`. Mix a few categories (landscapes, mountains, forests,
  water). Check the exact category names when implementing: the cloud session can't reach
  Commons, so check from a machine that can.
- Optional second source later: Unsplash API (needs the owner's access key and attribution).

### Prefetch and cache

- A WorkManager periodic job runs once a day with `NetworkType.UNMETERED` and
  `requiresStorageNotLow`. It keeps a pool of about 30 photos (about 10–20 MB) in `cacheDir`,
  replacing the oldest. Metadata (credit, licence) is stored in a JSON index next to them.
- The dream reads only from the cache and never waits on the network. With an empty cache
  it shows the built-in gradient backgrounds.

## Launcher background

- Launcher Settings → Background, with a live preview. Choices:
  - built-in gradients or solid colours (shipped as tiny vectors or code, no PNGs);
  - "Photo of the day", one image from the screensaver cache, rotating daily;
  - images from a USB drive through MediaStore (`READ_MEDIA_IMAGES` on API 33+,
    `READ_EXTERNAL_STORAGE` below). Google TV has no system photo picker, so we draw our own
    grid of thumbnails.
- Decode the background at 960×540 (about 2 MB). It sits under a dark scrim and the UI, so
  it doesn't need to be sharp, and a softer background helps legibility.
- Change it with a 300 ms crossfade. Persist the choice in DataStore.
