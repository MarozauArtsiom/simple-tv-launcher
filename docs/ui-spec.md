# UI specification (source of truth)

What every screen looks like and how it behaves. The general rules (canvas, focus, motion,
contrast) are in the `tv-ui-design` skill; this file is the concrete spec. Exact colour and
style values live in `design/mockups/themes.css`, and approved mockups in `design/mockups/`.
**A screen that has no approved mockup gets one before it is coded.**

## 1. Canvas and layout

- 960×540 dp canvas (rendered at 1920×1080, xhdpi). Safe area: 48 dp left/right,
  27 dp top/bottom. Backgrounds bleed to the edges.
- Spacing scale: 4, 8, 12, 16, 20, 24, 32, 48 dp.
- App banner: **160×90 dp**, gaps 16 dp. 5 banners fill the 864 dp content width exactly.
- Vertical rhythm of Home: top bar 27→71 dp; "Favourites" title +24; favourites row
  (8 dp padding for focus growth + 90 + label 20); "All apps" title +12; grid rows of 90 dp with
  16 dp gaps. The third grid row peeks out at the bottom, which signals that the page scrolls.

## 2. Themes

Fourteen themes ship. The family decides font and card style, and the theme decides colours and art.

| # | id | Name | Family | Background | Focus colour | Card radius | Focus on cards |
|---|---|---|---|---|---|---|---|
| 1 | midnight | Midnight | dark | nature photo/art + dark scrim | #FFFFFF | 12 | ×1.1, 3 dp ring |
| 2 | graphite | Graphite | dark | near-black radial gradient | #FFC46B | 6 | ×1.1, 3 dp ring; titles uppercase, tracked 0.12em, 14 sp |
| 3 | aurora | Aurora | dark | blurred aurora fields | #FFFFFF | 16 | ×1.1, 3 dp ring + violet glow |
| 4 | paper | Paper | light | warm off-white | #1F1D1A | 10 | ×1.1, 3 dp ring + soft shadow |
| 5 | sky | Sky | light | sky + clouds | #1A6BFF | 16 | ×1.1, 3 dp ring + blue shadow |
| 6 | sage | Sage | light | sage hills | #2F6B45 | 14 | ×1.1, 3 dp ring + green shadow |
| 7 | valley | Sunny valley | cartoon | hills, house, smiling sun | #FFD400 | 20 | ×1.12, 5 dp ring, −2° tilt, hard shadow |
| 8 | ocean | Underwater | cartoon | fish, bubbles, seaweed | #FF7A59 | 20 | ×1.12, 5 dp ring, −2° tilt, hard shadow |
| 9 | space | Space | cartoon | rocket, moon, ringed planet | #FFE14D | 20 | ×1.12, 5 dp ring, −2° tilt, yellow glow |
| 10 | gloom | Gloomy hill | dark cartoon | huge moon striped by clouds, ravens, spiral hill, twisted trees, iron fence | #EDE3C4 | 20 | ×1.12, 5 dp ring, −2° tilt, pale glow |
| 11 | haunted | Haunted house | dark cartoon | sickly moon with bats, crooked house, graveyard, ghost, spider | #8CFF5A | 20 | ×1.12, 5 dp ring, −2° tilt, green glow |
| 12 | monsters | Monster party | dark cartoon | violet night, party lights, moon in a party hat, monsters peeking over the apps | #FF4FA3 | 20 | ×1.12, 5 dp ring, −2° tilt, pink glow |
| 13 | halloween-night | Halloween night | halloween (dark) | orange moon with a witch, glowing pumpkins, cat on a fence, bunting | #FF8A1F | 20 | ×1.12, 5 dp ring, −2° tilt, orange glow |
| 14 | halloween-day | Halloween day | halloween (light) | autumn sky, leaves, friendly ghost, smiling pumpkins, witch hat | #7A3CE0 | 20 | ×1.12, 5 dp ring, −2° tilt, violet shadow |

- **Token groups** (names as in `themes.css`):
  - text: `on-surface`, `on-surface-variant`
  - chips: `chip-bg`, `chip-fg`, `chip-meta`, `chip-shadow`
  - section titles: `title-bg`, `title-fg`, `title-meta`, `title-pad`, `title-case`, `title-tracking`, `title-weight`
  - focused-card label: `label-bg`, `label-fg`, `label-pad`
  - cards: `card-radius`, `card-outline`, `card-shadow`
  - focus: `focus`, `focus-border`, `focus-scale`, `focus-rotate`, `focus-glow`
  - panels: `panel-bg`, `panel-fg`, `panel-muted`, `panel-radius`, `item-bg`, `item-focus-fill`, `item-focus-ring`
  - misc: `signal`, `wx-sun`, `wx-cloud`, `bg`, `scrim`, `font`
- **Fonts:** Roboto (system) for dark and light; Nunito (bundled, OFL) for cartoon, with
  ExtraBold (800) for titles, the clock and card text.
- **Cartoon family:** cards always carry a 3 dp white "sticker" outline and a hard drop shadow.
  Section titles and the focused-card label sit in white pills (yellow pills in Space). Chips
  are white with dark text and a coloured weather icon.
- **Dark cartoon and Halloween:** the same shapes, font and focus style as cartoon. The dark ones
  (Gloomy hill, Haunted house, Monster party, Halloween night) use dark chips, dark panels and a tinted
  outline (`card-outline` colour) instead of white; pills take the theme accent. Halloween day is a light
  cartoon theme like Sunny valley. The two Halloween themes are meant as a day/night pair for
  the schedule.
- **Switching:** Settings → Theme. Focusing a thumbnail previews it live (home and panel); OK
  saves; Back restores the saved theme. Optional "Change by time of day" picks a day theme
  and a night theme, switching at sunrise/sunset (07:00/20:00 without weather). It is
  checked only while Home is visible.
- Background art is static. It is rasterised once to 960×540, and a theme change crossfades
  it in 300 ms.

## 3. Type

- tv-material type scale. Clock: displaySmall 36 sp (800 in cartoon). Date: titleMedium 16 sp
  muted. Section titles: 18 sp/500 (800 in cartoon; 14 sp uppercase in Graphite). Card text
  and list titles: titleMedium 16 sp/500. Values and meta: bodyMedium 14 sp. Chip labels:
  labelLarge 14 sp/500. Panel titles: 24 sp.
- Nothing the user must read is under 14 sp. Everything is one line with an ellipsis.

## 4. Components

| Component | Size | States |
|---|---|---|
| App banner card | 160×90 dp, theme radius | Unfocused: art (+ cartoon outline/shadow). Focused: theme focus style (§2). Pressed: scale back to 1.0. The label under the card shows only in Favourites, only when focused |
| Generated banner (app without TV banner) | same | Icon 36 dp (radius 10) above the app name (14 sp), on a tint from the icon (palette) |
| Section title | text, or a pill (cartoon) | Static. "All apps" has a muted "· N" count |
| Top-bar chip | height 36, radius 18, padding 14 | Types: weather (icon + "+7°" + "· City"), Inputs (one button for all HDMI inputs, green dot when any is connected; OK opens a popup under the bar listing each input with "Connected" / "Nothing connected", OK switches), iPhone (phone icon), settings: a round 36×36 button with the owner's photo (Settings → Profile photo) or a person icon, like the account button of the stock home. Focused: theme focus ring 3 dp + ×1.05 |
| Side panel | 400 dp wide (560 for the theme picker), full height, pad 27/48/27/32, radius 0 (24 in cartoon) | Opens from the right over a dim of 45% black (15% in the theme picker, so the preview shows) |
| List item | height 44, radius 10, padding 12, icon 20, title 16 sp, value 14 sp muted, chevron | Focused: `item-focus-fill` + 3 dp `item-focus-ring` + ×1.03 (**never a full white fill**). Selected: check icon in `signal` colour |
| Quick tile | 3 columns × 58 dp, radius 12, icon 20 + label 14 sp | Focus like list items |
| Button | height 40, pill, padding 20 | Focused: fill in the theme focus colour + ×1.05 (e.g. "Change city", "Switch") |
| Theme thumbnail | 148×83 dp, radius 10, name pill | Focused: 3 dp ring + ×1.06. Saved theme: green check badge |
| HDMI prompt | 460 dp wide, bottom-right inside the safe area, radius 16 | Default focus on "Switch". Auto-hides after 15 s |

## 5. Screens

For each screen: initial focus, Back, Home, and the mockup.

### Home — `design/mockups/home.html`, `themes/*.png`
- Order: top bar → **Favourites** (one row, manual order only, always first) → **All apps**
  (grid, 5 per row, sorted by launch count, then alphabetically).
- Initial focus: the first favourite. When there are no favourites, the first app in the grid.
- Focus memory: the favourites row and each grid row restore their last item (`focusRestorer`).
  Up from the first row goes to the top bar.
- The remote's Menu key opens Settings from anywhere on Home.
- Top bar order: weather · Inputs · Timer · Remote · iPhone · avatar. Every chip but the avatar can be hidden in
  Settings → Top bar buttons; a running sleep timer shows anyway. **Remote** (remote-control icon, a signal dot while
  the phone remote or AI access is on) opens Settings → Remote & AI assistant directly; Back closes it. **Timer** (moon icon, "Timer" or the minutes left)
  opens a popup under the bar: Off, 15, 30, 45, 60, 90, 120 min; the running choice is focused and checked.
- Settings: under the TV tiles a full-width **Turn off TV** row → dialog "Turn the TV off completely?" Yes
  (focused) / No. Without the power helper both the dialog and the timer popup show its adb command instead.
- **Hidden** section (only when something is hidden): the last section, a chip "Hidden · N" with a chevron.
  Collapsed by default and after every reset (Home key, TV on, Back); its tiles are not composed, so Down
  from the header goes nowhere. OK toggles it; open, it is a 5-column grid like All apps. The app menu of a
  hidden tile offers "Show on Home", App info and Uninstall. Settings → Hidden apps stays as a second way back.
- The section that holds focus (Favourites or All apps) is drawn as is; the other one fades to 50 %
  in 150 ms (title and tiles). Focus in the top bar fades every section below; the top bar never fades.
  The clock side of the top bar gives way when it is crowded, so the avatar button always stays on screen. The clock and
  date stay whole; the weather chip takes what is left (the place name is ellipsized first) and is left out when not even
  the temperature fits (no half-cut chip). No blur (see T31).
- Scrolling follows focus calmly (`CalmScrollSpec`, not the TV pivot): a tile already on screen
  never scrolls the list, even while it grows; a tile off screen is brought just inside the edge
  (56 dp from the top so the section title shows, 27 dp from the bottom, 48 dp at the row ends).
- Back: close any overlay, else scroll to the top and focus the first favourite. Never exit.
  Home pressed while on Home does the same. TV turned on: first favourite. Back from an
  app: the tile the user left from.
- Long-press OK on a tile opens the app menu (see App menu below).
- Empty favourites: a hint card in the favourites row explaining the long press (see Empty
  favourites below).
- "All apps" is re-sorted by launch count only on Home key, screen on and Back at the top, never
  while the owner is looking at it.
- Safe mode (the launcher crashed twice within 10 minutes): default theme, clock, settings button and
  the app grid only, with one line of explanation. Nothing from the weather, HDMI or favourites
  features is drawn.

### Weather panel — `design/mockups/weather-panel.html`
- Opens from the weather chip. City (24 sp); now: icon 44 dp + temperature + condition +
  "feels like · wind"; 4 × 3-hour forecast; 5 days; "Change city" button.
- **No text about the data source or the location mode** (the owner's request; the credit is in
  Settings → About).
- Initial focus: "Change city". Back closes the panel and returns focus to the chip.

### HDMI prompt — `design/mockups/hdmi-prompt.html`
- "Signal on HDMI 2" + the device name when known; buttons "Switch" / "Not now"; a countdown
  in muted text. Initial focus: "Switch". Back = "Not now"; the card hides itself after 15 s.
  Over Home it sits in the bottom-right corner of the safe area over a 40 % dim; over another app it is a
  window of its own (needs the overlay permission). While it is open Home takes no focus. Once dismissed
  for any reason, the same input is not asked about again until it disconnects.

### Settings panel — `design/mockups/settings.html`, `settings/settings-*.png`
- Title "Settings". Section "Television": Wi-Fi, Bluetooth, Sound, Picture, Sleep timer,
  Android (tiles the TV can't open are hidden). Section "Launcher": Theme, Background, Profile photo,
  Top bar buttons, Language, Weather, HDMI signal, Hidden apps, Website shortcuts, Remote & AI assistant,
  Install from USB, About.
- Initial focus: the Wi-Fi tile. Back: out of a sub-page, then close the panel and return focus
  to the settings (avatar) button. Coming back from a sub-page, focus returns to the row that opened it.
- A tile appears only if this TV has a screen for it. Picture falls back to the display settings;
  the Sleep timer is hidden unless a vendor screen is known. "Website shortcuts" is a launcher row
  after "Hidden apps".

### Theme picker — `design/mockups/settings-theme.html`, `settings/theme-picker.png`
- One row of thumbnails per family heading (Dark / Light / Cartoon / Dark cartoon / Halloween), then
  the item "Change by time of day". The rows scroll.
- Initial focus: the saved theme. Focus = live preview, OK = save, Back = revert and close.

### Language picker — `design/mockups/settings-language.html`, `settings/language-picker.png`
- Order: **Беларуская**, Русский, English, Polski, Deutsch, Français, **Same as the TV**
  (shows the TV language as its value). Each language by its native name.
- Default selection: "Same as the TV". If the TV language isn't one of the six, English.
- Initial focus: the saved choice (check mark). OK applies at once (the UI re-renders in
  the new language). Back closes.

### Empty favourites — `design/mockups/home-empty-favs.html`
- When there are no favourites, the Favourites row shows one hint card (star icon, "Nothing here
  yet", how to add: long-press OK, then "Add to favorites"). The card isn't focusable. Initial focus
  goes to the first app in the grid.

### App menu — `design/mockups/app-menu.html`, move mode `favourites-move.html`
- Long-press OK on any tile opens a 290 dp popup under the tile (flipped above it when there
  is no room). Items: Add to favorites / Remove from favorites, Move (only for favourites),
  Hide, App info, Uninstall (the last three only for apps; a website shortcut has just the
  favourites items). Initial focus is the first item. Back closes it, and focus
  returns to the tile. While the menu is open the tile keeps its focus look and Home takes no
  focus. The OK press that opened the menu (still held) cannot choose the first item.
- Move mode: the tile gets a dashed ring and a pill "◀ ▶ Move · OK to finish". Left/Right
  swap the tile with its neighbour, OK drops it, and Back cancels (restores the old order).

### Website shortcuts — `design/mockups/shortcuts.html?state=list|edit`
- Settings → "Website shortcuts". List: a first row "Add shortcut", then one two-line row per shortcut
  (72×40 tile with the first letter, name, address without `https://`) and a muted hint note. OK on a row
  edits it. Initial focus: "Add shortcut". Back closes the panel.
- Editor: "Name" and "Address" fields (the text-input pattern: OK starts typing, Back stops it), then
  buttons Save / Delete (existing only) / Cancel. Save with a bad name or address (anything but
  http/https with a host) shows one warning line and stays. Back leaves the editor first; focus returns to
  the row that was open.
- On Home a shortcut is a normal tile (letter on a tinted colour picked from the address, name below the
  letter), in All apps and, from the app menu, in Favourites. OK opens the address with `ACTION_VIEW`.

### City search — `design/mockups/weather-search.html`
- A 400 dp panel: title "City", a search field (focus ring, caret), and up to three results
  above the on-screen keyboard (Gboard takes the bottom ~168 dp). The first row is always
  "Detect automatically" (clears the manual city). Each result: city (16 sp) plus region and
  country (14 sp muted). OK on a result saves it and returns to the weather panel.
- The field intercepts Back and Escape: the first press leaves the field (focus goes to the
  list), and the second closes the panel.

### Background picker — `design/mockups/settings-background.html`
- A 560 dp panel: "From theme" (check when selected), six colour swatches, and a 3-column grid of
  images found on USB (title shows the count). Focus on a swatch or picture previews it on the
  screen behind, OK saves, and Back reverts. Without USB access, the grid is replaced by the
  adb hint note (as on the USB installer screen).
- The six colours depend on the theme's text: dark colours for themes with light text, light colours for
  themes with dark text. A saved colour that the current theme's text cannot be read on is not applied.
  A picture gets a scrim (dark under light text, light under dark text). Down from "From theme" goes to the
  saved colour (or the first). Pictures: up to 36, three per row, from the first two folder levels of each
  removable volume; OK stores a 960×540 copy in app storage.

### Hidden apps — `design/mockups/settings-hidden.html`
- A list with a mini banner (72×40), the app name and "Show". OK unhides and removes the row.
  An empty list shows "No hidden apps" in muted text.

### USB installer — `design/mockups/settings-usb.html?state=list|nopermission|installing|done`
- List: `.apk` files from USB volumes (mini icon, name, "version · size", action "Install", or
  "Update" when the package is installed with a lower version). Empty: "No .apk files on the USB drive".
- No permission: a warning note with the exact adb command and a "Check again" button.
- Installing: one row "Installing…" plus a hint to confirm in the system dialog.
- Done: a row with a check, then buttons "Open" and "Done".
- Built beyond the mockup (same components, screenshot-tested in `app/src/test/snapshots/settings-usb-*`):
  "unknown apps not allowed" (a note with its own adb command and "Check again"), and "failed" / "cancelled"
  (the archive row with the message and "Check again"). An archive that is already installed (same or newer
  version) reads "Installed" and does nothing on OK. The drive is scanned only when the page opens or on
  "Check again" / "Done".

### Profile photo — no separate mockup (same parts as the background picker)
- "No photo" (person icon, check mark when chosen), then "Images from USB" as the background picker's
  3-column thumbnails, or the all-files note. OK keeps a 160 px square copy, sets the avatar and goes back.

### About — `design/mockups/settings-about.html`
- App icon, name, version and build, one sentence on privacy, and the licence list as two-line
  rows (Open-Meteo CC BY 4.0, Nunito SIL OFL 1.1, Android components Apache 2.0).

### Theme schedule — `design/mockups/settings-schedule.html`
- A switch "Change automatically", and two theme rows (day, evening/night) that open the theme
  picker. A note shows today's sunset and sunrise, or says it uses 07:00 and 20:00 when no weather.

### HDMI prompt settings — `design/mockups/settings-hdmi.html`
- Switches: ask about a new signal, show it over other apps (with the adb hint while the
  permission is missing), and one switch per HDMI input.

### Top bar buttons — no separate mockup (switch rows like HDMI settings), golden `top-bar-page/`
- One switch row per chip (icon, name, switch): Weather, Inputs, Sleep timer, Remote, iPhone screen (AirPlay); a
  muted note below. Initial focus: Weather. The settings row shows "N of 5" while something is hidden.

### Remote & AI assistant — `design/mockups/settings-remote.html` (`?badge` for the badge), golden `remote-page/`
- Switches "Phone remote" and "Control via AI assistant"; while one is on, a card with the QR code (`http://<ip>:8765/?c=<code>`,
  or `/pair` with only AI on), the code in two groups of three (32 sp) and the address; connected clients with
  "Disconnect" ("Disconnect everyone" from two); the AI hint, the keys status and the auto-off note. Without the
  helper: its adb command and "Check again". Initial focus: the first switch.
- Under it a third switch, "…also from anywhere" (only while the AI switch is on). While on: "Connecting…", then a
  card that replaces the home-network one (unless the phone remote is on): QR code
  (`https://<random>.lhr.life/s/<secret>/pair?c=<code>`), the code in big digits, the host and a plain warning
  (anyone with the address and the code controls the TV; new address every time; traffic passes the free relay).
  If the tunnel fails: one line, switch on again for a new address.
- Indicator on Home: the top bar's Remote chip itself says who is connected ("Remote · AI is controlling", sparkle icon
  while an assistant is connected, dot while a switch is on). Only if the owner hid that chip does the overlay badge stay
  up on Home. Badge: a pill at the top centre, not focusable, not touchable; over other apps it is hidden and slides in
  for 2.5 s each time an assistant runs a tool.
- Phone page (`assets/remote/`): the TV's theme tokens and art, pairing, Remote and Screen modes, the typing row;
  no zoom (double tap or pinch).

### iPhone hint — `design/mockups/iphone-hint.html`
- A centred dialog (520 dp) shown when AirPlay Server isn't installed: what to install and how
  to mirror from the iPhone; buttons "Install from USB" (focused) and "Close".

## 6. Motion

Focus in 150 ms / out 120 ms (FastOutSlowIn). Panels slide in from the right in 200 ms.
Theme and background crossfade 300 ms. Only scale, alpha, translation and rotation through
`graphicsLayer`. No blur, no animated gradients.

## 7. Languages and text

- Six languages: be, ru, en, pl, de, fr. Strings live in resources only. Belarusian is listed
  first everywhere.
- The longest strings come from fr/pl/de ("Toutes les applications"). Each new screen is
  checked in `fr` and `be` (screenshot tests).
- Dates are localised with each language's word order, and the first letter is capitalised.
  Plurals use the `one`/`few`/`many`/`other` forms (be, ru, pl).
- City names come from geocoding in the UI language. App names come from the apps.

## 8. Accessibility

- Contrast: text ≥ 4.5:1 against its actual background in every theme; the focus indicator
  ≥ 3:1 against the unfocused state.
- State is never shown by colour alone: the HDMI signal dot also has a content description,
  and selected items get a check mark.
- Every focusable element has a localised `contentDescription` or text for TalkBack.

## 9. Mockup index

| Mockup | Source | Render |
|---|---|---|
| Home, 14 themes | `home.html?t=<id>` | `themes/<id>.png`, `themes/sheet-*.png` |
| Home in 6 languages | `home.html?lang=<code>` | `settings/languages.png` |
| HDMI prompt | `hdmi-prompt.html` | `hdmi-prompt.png` |
| Weather panel | `weather-panel.html` | `weather-panel.png` |
| Settings | `settings.html?t=<id>` | `settings/settings-{midnight,sky,valley}.png` |
| Theme picker | `settings-theme.html?t=space` | `settings/theme-picker.png` |
| Language picker | `settings-language.html` | `settings/language-picker.png` |
| Empty favourites, app menu, move mode | `home-empty-favs.html`, `app-menu.html`, `favourites-move.html` | `screens/<name>-<theme>.png` |
| City search, background picker, hidden apps | `weather-search.html`, `settings-background.html`, `settings-hidden.html` | `screens/` |
| USB installer (4 states), About, theme schedule | `settings-usb.html?state=…`, `settings-about.html`, `settings-schedule.html` | `screens/` |
| HDMI settings, iPhone hint | `settings-hdmi.html`, `iphone-hint.html` | `screens/` |
| Website shortcuts (list, editor) | `shortcuts.html?state=list|edit` | `screens/shortcuts-{list,edit}-<theme>.png` |
| Remote & AI assistant (TV), badge | `settings-remote.html`, `settings-remote.html?badge` | goldens `remote-page/`, `remote-badge/` |
| Phone page (pair, remote, screen, off), AI pairing | `app/src/main/assets/remote/index.html?demo=…&t=…`, `pair.html` | `screens/remote-phone-*.png`, `screens/remote-pair-ai.png` |

`screens/` holds midnight, sky and valley renders of the batch above (`render-screens.sh`).
