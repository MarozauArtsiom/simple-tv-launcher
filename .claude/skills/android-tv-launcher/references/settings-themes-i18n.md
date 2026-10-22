# Settings, themes and languages

Mockups: `design/mockups/settings*.html`, rendered PNGs in `design/mockups/settings/`.

## Settings panel

The gear chip in the top bar opens a right-hand side panel, 400 dp wide, styled by the
current theme's panel tokens. Initial focus goes to the first tile.

- **TV** (quick actions, 3 × 2 tiles): Wi-Fi, Bluetooth, Sound, Picture, Sleep timer, Android
  settings. Each one opens the matching system or Xiaomi settings screen. Which intents exist on
  this TV comes from the device probe.
- **Launcher** (list, scrolls): Theme, Background, Language, Weather, HDMI signal prompt,
  Hidden apps, Install from USB, About (version, licences, Open-Meteo credit).
- Back closes the sub-screen, then the panel. Focus returns to the gear chip.

Persist everything in one Preferences DataStore. Keys: `theme`, `theme_schedule`
(off | day theme + night theme), `background` (theme | colour | image path), `language`
(empty = follow the TV), `weather_location`, `hdmi_prompt`, `hidden_apps`.

## Themes

All nine themes ship: midnight, graphite, aurora, paper, sky, sage, valley, ocean and space.
The tokens are in `design/mockups/themes.css`, which is the source of truth for the Kotlin
values.

```kotlin
data class ThemeSpec(
    val id: String,
    val family: Family,             // DARK, LIGHT, CARTOON
    val colors: LauncherColors,     // onSurface, chips, titles, signal, panel*, item* …
    @DrawableRes val background: Int?,   // vector art; null = gradient drawn in code
    val font: FontFamily,           // Roboto, or Nunito for the cartoon family
    val focus: FocusStyle,          // colour, border width, scale, rotation, glow
    val cards: CardStyle,           // radius, sticker outline, shadow
)
```

- Provide the active `ThemeSpec` through a `CompositionLocal`. Switching themes recomposes
  only the chrome; app banners are untouched.
- Background: rasterise the vector once on `Dispatchers.Default` into a 960×540 bitmap, cache
  it, and crossfade (300 ms) on change. Keep only one background bitmap in memory, two during
  the crossfade.
- **Theme picker** (560 dp panel): a 3 × 3 grid of static preview thumbnails (WebP,
  ~20 KB each) grouped by family. **Live preview**: focusing a thumbnail applies that theme
  to the home screen behind the panel and to the panel itself. OK saves it, and Back reverts to
  the saved theme. A check badge marks the saved theme.
- **Change by time of day** (off by default): pick a day theme and a night theme. The check
  runs only when the home screen resumes and on the minute tick while it is visible, never in
  the background. Switch at sunrise and sunset from the weather data
  (`daily=sunrise,sunset`), or at 07:00 / 20:00 without weather.
- Fonts: Roboto (system) and Nunito (bundled, OFL, ~270 KB). Both cover Russian, Belarusian
  (і, ў), Polish, German and French letters. Nunito's coverage was checked with fc-query.

## Languages

The launcher's own language, independent of the TV's: follow the TV (default), Русский, English,
Polski, Deutsch, Français, Беларуская. Show each one by its native name.

- Resources: `values/` (English, the fallback), plus `values-ru`, `values-pl`, `values-de`,
  `values-fr` and `values-be`. Declare them in `res/xml/locales_config.xml` and point
  `android:localeConfig` at it.
- Applying:
  - API 33+: `getSystemService(LocaleManager::class.java).applicationLocales =
    LocaleList.forLanguageTags("be")`. An empty `LocaleList` means "follow the TV".
  - Below 33: store the choice in DataStore, wrap the context in
    `attachBaseContext` (`createConfigurationContext` with the chosen locale), and `recreate()`.
    Check the TV's API level in `docs/device/profile.md` before picking one path.
- Plurals: ru, be and pl need `one`/`few`/`many`/`other` (for example "14 приложений",
  "2 программы").
- Dates: `DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)` for ru, be and pl, plus each
  language's own order (de "Mittwoch, 21. Oktober", fr "mercredi 21 octobre",
  en "Wednesday, October 21"). Capitalise the first letter. Android's ICU has Belarusian data;
  the mockup browser didn't, so the mockups spell the dates out.
- Weather: condition texts come from string resources. City names come from Open-Meteo
  geocoding with `language=<app language>` (fall back to `en` if a name is missing).
- Strings run up to about 40% longer in fr, de and pl ("Toutes les applications"). Every
  title is one line with an ellipsis. Check each new screen with the longest language.
- Translations are written in-house. The owner reviews them, especially Belarusian.
