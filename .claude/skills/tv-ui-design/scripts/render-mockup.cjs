#!/usr/bin/env node
// Renders an HTML mockup the way the TV draws the UI: a 960x540 dp canvas at 2 px per dp,
// saved as a 1920x1080 PNG. Inside the mockup, 1 CSS px == 1 dp and font-size px == sp.
//
// Usage: node render-mockup.cjs <mockup.html[?query]> [out.png] [--grid]
//   A query string is passed to the page, e.g. home.html?t=space picks a theme.
//   --size=WxH  viewport in CSS px (default 960x540); --scale=N  device scale factor (default 2).
//   --grid  overlays the safe area (48 dp sides, 27 dp top/bottom) and the 12-column grid.

const path = require('path');
const { pathToFileURL } = require('url');

function loadPlaywright() {
  try {
    return require('playwright');
  } catch {
    return require('/opt/node-tools/node_modules/playwright'); // global install in cloud sessions
  }
}

const GRID_CSS = `
  body::before { content: ''; position: fixed; inset: 27px 48px; z-index: 2147483647;
    outline: 1px dashed rgba(255, 0, 255, .9); pointer-events: none; }
  body::after { content: ''; position: fixed; top: 0; bottom: 0; left: 58px; right: 58px; z-index: 2147483646;
    pointer-events: none; background: repeating-linear-gradient(to right,
      rgba(0, 200, 255, .12) 0 52px, transparent 52px 72px); }
`;

async function main() {
  const args = process.argv.slice(2);
  const grid = args.includes('--grid');
  const opt = (name, fallback) => (args.find((a) => a.startsWith(`--${name}=`)) || '').split('=')[1] || fallback;
  const [vw, vh] = opt('size', '960x540').split('x').map(Number);
  const scale = Number(opt('scale', '2'));
  const [input, outArg] = args.filter((a) => !a.startsWith('--'));
  if (!input) {
    console.error('Usage: node render-mockup.cjs <mockup.html> [out.png] [--grid]');
    process.exit(2);
  }
  const [file, query] = input.split('?');
  const suffix = (query ? '.' + query.replace(/[^\w-]+/g, '-') : '') + (grid ? '.grid' : '');
  const output = outArg || file.replace(/\.html?$/i, suffix + '.png');

  const { chromium } = loadPlaywright();
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: vw, height: vh }, deviceScaleFactor: scale });
    const url = pathToFileURL(path.resolve(file)).href + (query ? '?' + query : '');
    await page.goto(url, { waitUntil: 'networkidle' });
    if (grid) await page.addStyleTag({ content: GRID_CSS });
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: output });
    console.log(path.resolve(output));
  } finally {
    await browser.close();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
