# HTML mockups at TV scale

Mockups live in `design/mockups/`. Each one is a plain HTML file with no build step that
links the shared `tokens.css`. The renderer opens it at 960×540 CSS px with
`deviceScaleFactor: 2`, so:

- 1 CSS px = 1 dp, and `font-size` in px = sp;
- the PNG is 1920×1080, the same frame the TV's UI layer draws.

```bash
node .claude/skills/tv-ui-design/scripts/render-mockup.cjs design/mockups/home.html          # design/mockups/home.png
node .claude/skills/tv-ui-design/scripts/render-mockup.cjs design/mockups/home.html --grid   # + safe area & 12-col grid
```

Look at every render yourself (Read the PNG) before you show it. Check for clipped
focus states, text under 14 px, and anything outside the safe area.

## Conventions

- Themes: `home.html?t=<id>` picks a theme from `themes.css` (midnight, graphite, aurora, paper,
  sky, sage, valley, ocean, space). The renderer passes the query string through, e.g.
  `render-mockup.cjs "design/mockups/home.html?t=space" out.png`. Layout code in `home.css` must
  read colours, background, font and focus style only from theme variables.
- Background art is static SVG in `assets/bg/`. In the app it becomes one 960×540 bitmap, so a
  busy cartoon scene costs the same per frame as a plain colour.

- Draw the focused state as a static `.focused` class. A mockup shows one frame, so put
  exactly one `.focused` element per file. To compare focus positions, make variants such
  as `home.focus-row2.html`.
- Use only the token variables. Layout and sizes live in `tokens.css`, and per-theme colours
  and styles live in `themes.css`. A new value is added to all nine themes in `themes.css`, to
  the `ThemeSpec` port in `ui/theme/`, and to the token list in `docs/ui-spec.md` §2, all in
  the same change.
- Fonts: `var(--font)` from the theme (Roboto, or the bundled Nunito for the cartoon themes).
  Don't load web fonts the app won't ship.
- App art: use plain coloured blocks with the app name. Don't copy real app logos into
  mockups.

## `design/mockups/tokens.css` (create it with the first mockup)

```css
:root {
  --background: #0B0D12;
  --surface: #161A22;
  --surface-variant: #1E232D;
  --on-surface: #E8EAF0;
  --on-surface-variant: #A3A9B7;
  --focus: #FFFFFF;
  --on-focus: #0B0D12;
  --accent: #8AB4F8;
  --error: #F28B82;
  --signal: #7BD88F;

  --safe-h: 48px;
  --safe-v: 27px;
  --card-gap: 16px;
  --banner-w: 160px;
  --banner-h: 90px;
  --card-radius: 12px;
  --focus-border: 3px;
  --focus-scale: 1.1;
}

* { box-sizing: border-box; margin: 0; }
html, body { width: 960px; height: 540px; overflow: hidden; }
body {
  background: var(--background);
  color: var(--on-surface);
  font-family: Roboto, system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* tv-material type scale (px == sp) */
.display-small  { font-size: 36px; line-height: 44px; font-weight: 400; }
.headline-small { font-size: 24px; line-height: 32px; font-weight: 400; }
.title-large    { font-size: 22px; line-height: 28px; font-weight: 400; }
.row-title      { font-size: 18px; line-height: 24px; font-weight: 500; }
.title-medium   { font-size: 16px; line-height: 24px; font-weight: 500; }
.body-medium    { font-size: 14px; line-height: 20px; font-weight: 400; }
.label-large    { font-size: 14px; line-height: 20px; font-weight: 500; }
.label-medium   { font-size: 12px; line-height: 16px; font-weight: 500; }

.card {
  width: var(--banner-w); height: var(--banner-h); flex: none;
  border-radius: var(--card-radius); background: var(--surface-variant);
  display: grid; place-items: center; overflow: hidden;
}
.card.focused {
  transform: scale(var(--focus-scale));
  outline: var(--focus-border) solid var(--focus);
}
```

## Starter mockup

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <link rel="stylesheet" href="tokens.css">
  <style>
    .top { display: flex; justify-content: space-between; align-items: center;
           padding: var(--safe-v) var(--safe-h) 0; }
    .rows { margin-top: 32px; display: grid; gap: 24px; }
    .row-title { padding-left: var(--safe-h); margin-bottom: 12px; }
    .row { display: flex; gap: var(--card-gap); padding: 8px var(--safe-h); overflow: hidden; }
  </style>
</head>
<body>
  <header class="top">
    <span class="display-small">20:41</span>
    <span class="label-large" style="color: var(--on-surface-variant)">HDMI 1 · Settings</span>
  </header>
  <main class="rows">
    <section>
      <h2 class="row-title">Favourites</h2>
      <div class="row">
        <div class="card focused" style="background:#3B2A6B"><span class="title-medium">App 1</span></div>
        <div class="card" style="background:#1F4D5C"><span class="title-medium">App 2</span></div>
        <div class="card" style="background:#5C3A1F"><span class="title-medium">App 3</span></div>
        <div class="card" style="background:#24452B"><span class="title-medium">App 4</span></div>
        <div class="card" style="background:#4A1F3A"><span class="title-medium">App 5</span></div>
        <div class="card" style="background:#2B3550"><span class="title-medium">App 6</span></div>
      </div>
    </section>
  </main>
</body>
</html>
```
