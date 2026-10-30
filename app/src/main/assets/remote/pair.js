// Pairing page for an AI assistant (opened on a computer): code from the TV → a key → ready-made MCP settings.
'use strict';
const $ = (id) => document.getElementById(id);
let S = {};
// On the home network the page is /pair; through the tunnel it is /s/<secret>/pair, and everything it calls sits under the same prefix.
const base = location.pathname.replace(/\/(pair\/?)?$/, '');
const looked = fetch(base + '/api/look').then((r) => r.json()).then((look) => {
  if (look.theme) document.documentElement.dataset.theme = look.theme;
  if (look.lang) document.documentElement.lang = look.lang;
  S = look.strings || {};
  document.querySelectorAll('[data-s]').forEach((el) => { if (S[el.dataset.s]) el.textContent = S[el.dataset.s]; });
  document.querySelectorAll('[data-ph]').forEach((el) => { if (S[el.dataset.ph]) el.placeholder = S[el.dataset.ph]; });
}).catch(() => {});

$('go').addEventListener('click', async () => {
  $('err').hidden = true;
  const body = { code: $('code').value.replace(/\D/g, ''), kind: 'assistant', name: $('name').value.trim() || S.web_ai_default_name || 'AI assistant' };
  const r = await fetch(base + '/api/pair', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }).catch(() => null);
  const j = r ? await r.json().catch(() => ({})) : {};
  if (!r || !r.ok || !j.key) {
    $('err').textContent = j.error === 'locked' ? (S.web_pair_locked || 'Too many tries. Wait {s} s.').replace('{s}', j.seconds)
      : j.error === 'off' ? (S.web_ai_off || 'AI access is off on the TV.') : (S.web_pair_wrong || 'Wrong code.');
    $('err').hidden = false; return;
  }
  const url = `${location.origin}${base}/mcp`;
  $('cmd').textContent = `claude mcp add --transport http tv ${url} --header "Authorization: Bearer ${j.key}"`;
  $('cfg').textContent = JSON.stringify({ mcpServers: { tv: { type: 'http', url, headers: { Authorization: `Bearer ${j.key}` } } } }, null, 2);
  if (base) {
    // Through the tunnel: the address carries the key, for connectors that can't send headers (the Claude app).
    $('addr').textContent = `${url}/${j.key}`;
    const prompt = S.web_chat_prompt || 'Use my TV connector to control my TV.';
    $('openChat').href = 'https://claude.ai/new?q=' + encodeURIComponent(prompt); // fills the first message where Claude supports it
    $('firstMessage').textContent = `«${prompt}»`;
    $('chat').hidden = false;
  }
  $('form').hidden = true; $('done').hidden = false;
});
// The page is plain http on the home network, where browsers hide navigator.clipboard (it needs https).
// So: the clipboard API when there is one, else the old execCommand('copy'), else select the text for Ctrl+C.
function copy(button) {
  const pre = $(button.dataset.copy);
  const text = pre.textContent;
  const label = button.querySelector('span');
  const say = (key, fallback) => {
    label.textContent = S[key] || fallback;
    setTimeout(() => { label.textContent = S.web_copy || 'Copy'; }, 2500);
  };
  const legacy = () => {
    const t = document.createElement('textarea');
    t.value = text; t.setAttribute('readonly', '');
    t.style.cssText = 'position:fixed;top:0;left:0;opacity:0;user-select:text;-webkit-user-select:text';
    document.body.appendChild(t); t.focus(); t.select(); t.setSelectionRange(0, text.length);
    let ok = false;
    try { ok = document.execCommand('copy'); } catch (e) { ok = false; }
    t.remove();
    return ok;
  };
  const selectIt = () => {
    const range = document.createRange(); range.selectNodeContents(pre);
    const sel = getSelection(); sel.removeAllRanges(); sel.addRange(range);
    say('web_copy_manual', 'Selected: press Ctrl+C');
  };
  const fallback = () => (legacy() ? say('web_copied', 'Copied') : selectIt());
  if (navigator.clipboard && window.isSecureContext) navigator.clipboard.writeText(text).then(() => say('web_copied', 'Copied'), fallback);
  else fallback();
}
document.querySelectorAll('[data-copy]').forEach((b) => b.addEventListener('click', () => copy(b)));

// The QR code on the TV carries the code (?c=): fill it in and connect at once; it leaves the address bar.
const fromQr = (new URLSearchParams(location.search).get('c') || '').replace(/\D/g, '');
if (fromQr.length === 6) {
  history.replaceState(null, '', location.pathname);
  $('code').value = fromQr;
  looked.then(() => $('go').click()); // after the strings arrived: the chat prompt is in them
}
