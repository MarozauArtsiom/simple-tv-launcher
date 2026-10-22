---
name: tv-ui-design
description: Design system and rules for the 10-foot TV UI of this launcher. Covers the 960x540 dp canvas and safe areas, layout grid, type scale, dark colours tuned for a QLED panel, focus states, D-pad navigation model, motion budget for a weak GPU, Compose-for-TV component patterns, and how to render HTML mockups as 1920x1080 PNGs with Playwright. Load before designing, reviewing or coding any screen or component.
---

# TV UI design

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

## Colour (dark, QLED-aware)

Starting tokens, placeholders until the owner picks a style:

| Token | Value | Use |
|---|---|---|
| `background` | `#0B0D12` | full-screen background (not pure black) |
| `surface` | `#161A22` | panels, top bar |
| `surfaceVariant` | `#1E232D` | unfocused cards and buttons |
| `onSurface` | `#E8EAF0` | primary text (not pure white) |
| `onSurfaceVariant` | `#A3A9B7` | secondary text |
| `focus` | `#FFFFFF` | focus border, focused button fill |
| `onFocus` | `#0B0D12` | text on a focused (white) button |
| `accent` | `#8AB4F8` | small highlights only: indicators, progress |
| `error` | `#F28B82` | errors |
| `signal` | `#7BD88F` | "HDMI has a signal" dot |

- Text contrast is at least 4.5:1. The focused element contrasts at least 3:1 with its
  unfocused neighbours.
- Use pure white only for the focus indicator, so focus always reads as the brightest
  thing on screen.
- The panel covers about 94% of DCI-P3, and in Vivid mode it oversaturates sRGB colours.
  Keep saturation moderate and keep big areas desaturated. Bright accents go on small
  elements only.
- Avoid 1 dp hairlines, fine stripes and checkerboards (they cause moiré and shimmer after
  upscaling). Avoid long, subtle full-screen gradients, because 8-bit UI surfaces band;
  if you need one, keep it short or add a little noise.
- The UI is SDR. Don't design for HDR brightness.

## Focus

- Exactly one focused item, always. On first frame, on return from an app, and after Back
  or Home, focus goes to a predictable place: the first app, or the last focused one.
- Focused state = **scale + white border**:
  - Small cards (banners, about 160 dp wide): ×1.1, 3 dp border.
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

Starter template and conventions: [references/mockups.md](references/mockups.md). The nine theme
candidates live in `design/mockups/themes.css`. Render one with `home.html?t=<id>`; comparison
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
- [ ] Colours come from tokens. No pure white text, no pure black background.
