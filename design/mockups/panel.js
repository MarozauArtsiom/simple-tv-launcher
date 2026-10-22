// Shared setup for panel mockups: ?t=<theme> styles the panel and the home screen behind it.
const THEME_NAMES = {
  midnight: 'Полночь', graphite: 'Графит', aurora: 'Сияние',
  paper: 'Бумага', sky: 'Небо', sage: 'Шалфей',
  valley: 'Солнечная долина', ocean: 'Подводный мир', space: 'Космос',
};
const panelTheme = new URLSearchParams(location.search).get('t') || 'midnight';
document.documentElement.dataset.theme = panelTheme;
addEventListener('DOMContentLoaded', () => {
  const behind = document.querySelector('iframe.behind');
  if (behind) {
    const src = behind.dataset.src || 'home.html?nofocus';
    behind.src = `${src}${src.includes('?') ? '&' : '?'}t=${panelTheme}`;
  }
  document.querySelectorAll('[data-theme-name]').forEach((el) => { el.textContent = THEME_NAMES[panelTheme]; });
});
