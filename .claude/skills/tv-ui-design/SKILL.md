---
name: tv-ui-design
description: Design rules for the 10-foot TV UI of this launcher. Covers the 960x540 dp canvas and safe areas, layout grid, type scale, the fourteen themes (tokens in design/mockups/themes.css), QLED-aware colour, focus states per component, D-pad navigation model, motion budget for a weak GPU, Compose-for-TV component patterns, and how to render HTML mockups as 1920x1080 PNGs with Playwright. The concrete per-screen spec is docs/ui-spec.md. Load before designing, reviewing or coding any screen or component.
---

# TV UI design

Three sources, each with one job:
- **This skill** holds the rules (how anything on a TV must look and behave).
- **`docs/ui-spec.md`** holds what each screen and component is: sizes, states, initial focus,
  Back behaviour, and which screens still need a mockup.
- **`design/mockups/themes.css`** holds the exact token values of all fourteen themes.

The user sits about 3 m from a 55" screen, holding a remote with a D-pad, OK, Back and
Home. Nothing on screen can be tapped, and only one thing has focus at a time. Every
design decision follows from that.

## Canvas and units

- Design in **dp on a 960×540 canvas** (16:9). The UI is usually drawn at 1920×1080
  (xhdpi, 2 px per dp) and the 4K panel upscales it. Confirm with
  `adb shell wm size` / `wm density`. Don't design for 3840×2160 pixels.
- **Safe area:** 48 dp left and right, 27 dp top and bottom. Backgrounds and hero art
  bleed to the edges. Text, focusable items and row starts stay inside.
- Grid: 12 columns, 52 dp columns, 20 dp gutters, 58 dp outer margin (Google TV grid).
  Card widths for N cards per row with that grid: 1 = 844, 2 = 412, 3 = 268, 4 = 196,
  5 = 124 dp.
- Spacing scale: 4, 8, 12, 16, 20, 24, 32, 48 dp. Nothing off-scale.
- Leave room for focus growth. A card scaled ×1.1 grows 5% on each side, so gaps and row
  padding must hold that without overlap or clipping. For a 160×90 dp card that means at
  least 8 dp on each side and 4.5 dp above and below; use 16 dp gaps.

## Type

Use the tv-material defaults (sp): displayLarge 57/64, displayMedium 45/52, displaySmall
36/44, headlineLarge 32/40, headlineMedium 28/36, headlineSmall 24/32, titleLarge 22/28,
titleMedium 16/24 (Medium), titleSmall 14/20 (Medium), bodyLarge 16/24, bodyMedium 14/20,
bodySmall 12/16, labelLarge 14/20 (Medium), labelMedium 12/16 (Medium), labelSmall 11/16
(Medium).

- **14 sp is the minimum** for anything the user has to read. 11 to 12 sp only for
  non-essential meta text.
- At small sizes, use Medium (500) weight. Never Light or Thin: thin strokes break up
  from across the room.
- Card titles fit on one line with an ellipsis. Show a title only on the focused card if
  the art already carries the name (app banners do).
- Use the system sans (Roboto). No decorative fonts. The one exception is the cartoon themes,
  which use Nunito: a rounded, very legible sans with Cyrillic, under the OFL licence, bundled
  in the app at about 270 KB (`design/mockups/assets/fonts/`).

## Colour (fourteen themes, QLED-aware)

The owner chose to ship fourteen themes (3 dark, 3 light, 3 cartoon, 3 dark cartoon, and a Halloween
pair: one dark, one light). Every colour comes from
a theme token. The token list is in `docs/ui-spec.md` §2 and the values are in
`design/mockups/themes.css`. Never hard-code a colour in a component.

- Text contrast is at least 4.5:1 against its real background in **every** theme. The
  focused element contrasts at least 3:1 with its unfocused state.
- The focus indicator is the most prominent thing on screen in each theme: white in Midnight
  and Aurora, amber in Graphite, dark ink in Paper, blue in Sky, green in Sage, and yellow or
  coral in the cartoon themes.
- Dark themes: off-white text (#E8EAF0-ish), never a pure black background. The one exception is
  Space, which uses white text because its focus colour is yellow.
- Light themes are bright in a dark room, which is why the optional day/night schedule exists.
- The panel covers about 94% of DCI-P3, and in Vivid mode it oversaturates sRGB colours.
  Keep saturation moderate on big areas. The cartoon themes are deliberately bright, but their
  large sky and sea areas stay soft.
- Avoid 1 dp hairlines, fine stripes and checkerboards (they cause moiré after upscaling). Avoid
  long, subtle full-screen gradients, which band on 8-bit surfaces: keep them short or add noise.
- The UI is SDR. Don't design for HDR brightness.

## Focus

- Exactly one focused item, always. On first frame, on return from an app, and after Back
  or Home, focus goes to a predictable place: the first app, or the last focused one.
- Focused state = **scale + ring in the theme's focus colour** (`docs/ui-spec.md` §2, §4):
  - Small cards (banners, about 160 dp wide): ×1.1 with a 3 dp ring. The cartoon family uses
    ×1.12, a 5 dp ring and a −2° tilt.
  - Medium (about 260 dp): ×1.05.
  - Large or full-width: ×1.025.
- Glow and shadow cost a lot on this GPU, so use them sparingly, only on the focused item.
- Optionally dim unfocused content slightly (alpha 0.85). Never change layout on focus,
  because that causes reflow.
- Pressed state: shrink back a little (about ×1.0) while OK is held.
- List items and tiles in panels: a 3 dp ring in the theme's focus colour, a slightly lighter
  fill and ×1.03. No full white fill: the owner found white rows odd. Buttons may still fill.

## Navigation model

- A vertical stack of horizontal rows: categories go down, items go across. A top bar
  (clock, inputs, settings) sits above the first row and is reached with Up.
- Each row remembers its last focused item (`focusRestorer`). Moving Up or Down returns
  to that item instead of the geometrically nearest one.
- No wrap-around inside rows. Left at the start of a row does nothing.
- Focused item positions: rows scroll so the focused item stays near a fixed pivot (the
  Compose default on TV is 30% from the start). Don't let focus run to the screen edge.
- Back: close any open menu, then scroll to the top and focus the first app. Back never
  exits the launcher.
- Long-press OK on an app opens a context menu: Move, Hide, App info, Uninstall.
- Any action is at most 2 to 3 presses from the default focus.

## Motion (Mali-G52 MC1 budget)

- Animate only `graphicsLayer` properties (scale, alpha, translation). Never animate size,
  padding or layout.
- Focus in: 150 ms `FastOutSlowInEasing`. Focus out: 120 ms. Content fade: 200 ms. Screen
  transitions: 250 ms max.
- No `RenderEffect` blur on large areas, no animated gradients, no parallax on many layers,
  and no more than one large animation at a time.
- Launcher background: decode at 960×540 px under a dark scrim, crossfade ≤ 300 ms.
- No animation is better than a janky one. Check with
  `adb shell dumpsys gfxinfo <pkg> framestats` on the real TV.

## Component patterns (Compose for TV)

Theme setup, focusable cards, rows with focus memory, initial focus, Back handling and
performance rules for Compose on this device:
[references/compose-tv-patterns.md](references/compose-tv-patterns.md).

## Mockups before code

The emulator can't run in the cloud. To iterate on a design, write an HTML mockup where
**1 CSS px = 1 dp** on a 960×540 page, and render it to a 1920×1080 PNG:

```bash
node .claude/skills/tv-ui-design/scripts/render-mockup.cjs design/mockups/home.html          # → home.png
node .claude/skills/tv-ui-design/scripts/render-mockup.cjs design/mockups/home.html --grid   # safe-area overlay
```

Starter template and conventions: [references/mockups.md](references/mockups.md). All fourteen
themes live in `design/mockups/themes.css`. Render one with `home.html?t=<id>`; comparison
sheets are in `design/mockups/themes/`. Show
the PNG to the user (it reads as the real TV frame), get agreement, then implement it in
Compose with the same token values.

## Review checklist

- [ ] Everything readable is ≥ 14 sp and inside the safe area.
- [ ] Every interactive element is reachable with the D-pad, has a visible focus state,
      and doesn't clip when scaled.
- [ ] There is a defined initial focus, and focus is restored after returning from an app.
- [ ] Back and Home on the root screen reset to the top; the launcher never exits.
- [ ] Only `graphicsLayer` animations, all ≤ 250 ms, no blur.
- [ ] Colours, radii and focus styles come from theme tokens. Nothing is hard-coded.
- [ ] Checked in all 14 themes: every family looks right, and contrast holds.
- [ ] Checked in `fr` and `be` (the longest strings and the Belarusian alphabet). Nothing is
      clipped and every title is one line.
- [ ] The screen has an approved mockup and matches `docs/ui-spec.md`.
