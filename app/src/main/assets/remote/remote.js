// The phone page of Simple TV Launcher. Plain JS, no frameworks. Talks only to the TV that served it.
// The key lives in sessionStorage: the TV forgets every key when it turns off, so nothing here outlives that.
'use strict';
const $ = (id) => document.getElementById(id);
const params = new URLSearchParams(location.search);
const demo = params.get('demo'); // design renders only: ?demo=pair|remote|screen|off&t=<theme>
let S = {};
let key = sessionStorage.getItem('tvkey');
let mode = 'remote';
let lastTap = null; // where the finger last tapped on the picture: typed text goes to the field there
let theme = null;

function applyLook(look) {
  if (look.theme && look.theme !== theme) { theme = look.theme; document.documentElement.dataset.theme = theme; }
  if (look.lang) document.documentElement.lang = look.lang;
  if (look.strings) {
    S = look.strings;
    document.querySelectorAll('[data-s]').forEach((el) => { if (S[el.dataset.s]) el.textContent = S[el.dataset.s]; });
    document.querySelectorAll('[data-ph]').forEach((el) => { if (S[el.dataset.ph]) el.placeholder = S[el.dataset.ph]; });
    if (S.web_title) document.title = S.web_title;
  }
  const meta = document.querySelector('meta[name=theme-color]');
  if (meta) meta.content = getComputedStyle(document.documentElement).getPropertyValue('--panel-bg').trim() || '#101216';
}

function show(view) {
  $('pair').hidden = view !== 'pair';
  $('off').hidden = view !== 'off';
  $('main').hidden = view !== 'main';
  $('modes').hidden = view !== 'main';
}

async function call(path, body) {
  if (demo) return null;
  const r = await fetch(path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { Authorization: 'Bearer ' + key, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).catch(() => null);
  if (r && r.status === 401) { disconnected(); return null; }
  return r;
}

function disconnected() {
  key = null; sessionStorage.removeItem('tvkey'); setMode('remote'); show('off');
}

async function pair(code) {
  $('pairError').hidden = true;
  const r = await fetch('/api/pair', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ code, kind: 'phone' }) }).catch(() => null);
  const j = r ? await r.json().catch(() => ({})) : {};
  if (r && r.ok && j.key) { key = j.key; sessionStorage.setItem('tvkey', key); show('main'); poll(); return; }
  const msg = j.error === 'locked' ? (S.web_pair_locked || 'Too many tries. Wait {s} s.').replace('{s}', j.seconds)
    : j.error === 'off' ? (S.web_off_hint || 'Phone remote is off on the TV.')
    : (S.web_pair_wrong || 'Wrong code.');
  $('pairError').textContent = msg; $('pairError').hidden = false;
}

// ---- keys, taps, text ----
document.querySelectorAll('[data-key]').forEach((b) => b.addEventListener('click', () => {
  if (navigator.vibrate) navigator.vibrate(8);
  call('/api/key', { key: b.dataset.key });
}));

let typing = null;
$('text').addEventListener('input', () => {
  clearTimeout(typing);
  typing = setTimeout(() => call('/api/text', { text: $('text').value, ...(lastTap || {}) }), 250);
});
$('text').addEventListener('keydown', (e) => { if (e.key === 'Enter') { e.preventDefault(); enter(); } });
$('enter').addEventListener('click', enter);
async function enter() {
  clearTimeout(typing);
  await call('/api/text', { text: $('text').value, ...(lastTap || {}) });
  call('/api/enter', { ...(lastTap || {}) });
  $('text').value = '';
}

// ---- the picture of the TV: tap, long press, swipe ----
const scr = $('screen');
let down = null;
const frac = (e) => { const r = scr.getBoundingClientRect(); return { x: Math.min(1, Math.max(0, (e.clientX - r.left) / r.width)), y: Math.min(1, Math.max(0, (e.clientY - r.top) / r.height)) }; };
const dot = (e) => { const r = scr.getBoundingClientRect(); const d = $('dot'); d.style.left = (e.clientX - r.left) + 'px'; d.style.top = (e.clientY - r.top) + 'px'; d.style.display = 'block'; };
scr.addEventListener('pointerdown', (e) => { scr.setPointerCapture(e.pointerId); down = { ...frac(e), t: performance.now() }; dot(e); });
scr.addEventListener('pointermove', (e) => { if (down) dot(e); });
scr.addEventListener('pointercancel', () => { down = null; $('dot').style.display = 'none'; });
scr.addEventListener('pointerup', (e) => {
  if (!down) return;
  $('dot').style.display = 'none';
  const up = frac(e), dx = up.x - down.x, dy = up.y - down.y, held = performance.now() - down.t;
  const r4 = (v) => Math.round(v * 10000) / 10000;
  if (Math.hypot(dx, dy) < 0.03) {
    lastTap = { x: r4(down.x), y: r4(down.y) };
    if (navigator.vibrate) navigator.vibrate(8);
    call('/api/tap', { ...lastTap, long: held > 450 });
  } else {
    const dir = Math.abs(dy) > Math.abs(dx) ? (dy < 0 ? 'down' : 'up') : (dx < 0 ? 'right' : 'left');
    call('/api/scroll', { x: r4(down.x), y: r4(down.y), dir });
  }
  down = null;
  setTimeout(frame, 400);
});

// ---- modes ----
function setMode(m) {
  mode = m;
  document.querySelectorAll('.mode').forEach((b) => b.classList.toggle('on', b.dataset.mode === m));
  $('screenCard').hidden = m !== 'screen';
  if (m === 'screen') frame();
}
document.querySelectorAll('.mode').forEach((b) => b.addEventListener('click', () => setMode(b.dataset.mode)));

// One picture about every second: the TV can't take them faster (and only while this mode is open).
let frameTimer = null, frameBusy = false;
async function frame() {
  clearTimeout(frameTimer);
  if (mode !== 'screen' || !key || document.hidden || frameBusy) { if (mode === 'screen') frameTimer = setTimeout(frame, 1200); return; }
  frameBusy = true;
  const r = await call('/api/frame');
  if (r && r.ok && r.status === 200) {
    const blob = await r.blob();
    const img = $('frame'); const old = img.src;
    img.src = URL.createObjectURL(blob);
    if (old.startsWith('blob:')) URL.revokeObjectURL(old);
  }
  frameBusy = false;
  frameTimer = setTimeout(frame, 1100);
}

// The TV's theme and language may change while the page is open (by hand or by the day/night schedule).
let pollTimer = null;
async function poll() {
  clearTimeout(pollTimer);
  if (!key) return;
  const r = await call('/api/state');
  if (r && r.ok) applyLook(await r.json());
  pollTimer = setTimeout(poll, 4000);
}
document.addEventListener('visibilitychange', () => { if (!document.hidden && key) { poll(); if (mode === 'screen') frame(); } });

$('pairBtn').addEventListener('click', () => pair($('code').value.replace(/\D/g, '')));
$('code').addEventListener('input', () => { const v = $('code').value.replace(/\D/g, ''); if (v.length === 6) pair(v); });
$('again').addEventListener('click', () => { $('code').value = ''; show('pair'); });
$('disconnect').addEventListener('click', async () => { await call('/api/disconnect', {}); disconnected(); });

// No zoom: double taps on the keys are fast presses, not a zoom gesture (Safari ignores user-scalable=no).
document.addEventListener('dblclick', (e) => e.preventDefault(), { passive: false });
document.addEventListener('gesturestart', (e) => e.preventDefault());

async function start() {
  if (demo) {
    const t = params.get('t') || 'midnight';
    const strings = await fetch('demo-strings.json').then((r) => r.json()).catch(() => ({}));
    applyLook({ theme: t, lang: params.get('lang') || 'ru', strings });
    if (demo === 'screen') { key = 'demo'; show('main'); setMode('screen'); $('frame').src = 'demo-screen.png'; return; }
    if (demo === 'remote') { key = 'demo'; show('main'); return; }
    if (demo === 'off') { show('off'); return; }
    show('pair'); $('code').value = '48291'; return;
  }
  const look = await fetch('/api/look').then((r) => r.json()).catch(() => ({}));
  applyLook(look);
  // The QR code opens /?c=<code> (older ones used #c=). A code from the QR wins over a key left from an
  // earlier visit: that key died when the TV turned off.
  const fromQr = (params.get('c') || new URLSearchParams(location.hash.slice(1)).get('c') || '').replace(/\D/g, '');
  if (fromQr || location.hash) history.replaceState(null, '', location.pathname); // the code leaves the address bar at once
  if (fromQr.length === 6) { show('pair'); $('code').value = fromQr; pair(fromQr); return; }
  if (key) { show('main'); poll(); return; }
  show('pair');
}
start();
