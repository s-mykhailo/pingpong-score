import { navigate, type Route } from '../router';
import { onPendingCountChange } from '../offline/sync';

const TABS: { route: Route; label: string; icon: string }[] = [
  { route: 'home', label: 'Головна', icon: 'sports_tennis' },
  { route: 'stats', label: 'Статистика', icon: 'bar_chart' },
  { route: 'players', label: 'Гравці', icon: 'group' },
  { route: 'settings', label: 'Налаштування', icon: 'settings' },
];

export function renderShell(
  root: HTMLElement,
  active: Route,
  buildContent: (content: HTMLElement) => void | (() => void),
): () => void {
  root.innerHTML = `
    <div class="screen screen-with-nav">
      <div id="offline-banner"></div>
      <div id="shell-content" class="col fill"></div>
    </div>
    <md-navigation-bar class="bottom-nav" active-index="${TABS.findIndex((t) => t.route === active)}">
      ${TABS.map(
        (t) => `<md-navigation-tab label="${t.label}"><md-icon slot="active-icon">${t.icon}</md-icon><md-icon slot="inactive-icon">${t.icon}</md-icon></md-navigation-tab>`,
      ).join('')}
    </md-navigation-bar>
  `;

  const nav = root.querySelector('md-navigation-bar')!;
  nav.querySelectorAll('md-navigation-tab').forEach((tab, i) => {
    tab.addEventListener('click', () => navigate(TABS[i].route));
  });

  const content = root.querySelector('#shell-content') as HTMLElement;
  const contentCleanup = buildContent(content);

  const banner = root.querySelector('#offline-banner') as HTMLElement;
  const renderBanner = (count: number) => {
    banner.innerHTML =
      count > 0
        ? `<div class="offline-banner md-typescale-body-medium"><md-icon>cloud_off</md-icon>${count} ${pluralMatches(count)} очікують відправки</div>`
        : '';
  };
  const unsubscribe = onPendingCountChange(renderBanner);

  return () => {
    unsubscribe();
    if (typeof contentCleanup === 'function') contentCleanup();
  };
}

function pluralMatches(n: number): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return 'незбережений матч';
  if ([2, 3, 4].includes(mod10) && ![12, 13, 14].includes(mod100)) return 'незбережені матчі';
  return 'незбережених матчів';
}
