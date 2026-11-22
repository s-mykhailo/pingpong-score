import { getSession } from './api/client';

export type Route =
  | 'login'
  | 'home'
  | 'match-setup'
  | 'scoreboard'
  | 'stats'
  | 'players'
  | 'settings';

type ScreenModule = {
  render(root: HTMLElement): void | (() => void);
};

const screens: Partial<Record<Route, () => Promise<ScreenModule>>> = {
  login: () => import('./screens/login'),
  home: () => import('./screens/home'),
  'match-setup': () => import('./screens/matchSetup'),
  scoreboard: () => import('./screens/scoreboard'),
  stats: () => import('./screens/stats'),
  players: () => import('./screens/players'),
  settings: () => import('./screens/settings'),
};

const PUBLIC_ROUTES: Route[] = ['login'];

let currentCleanup: (() => void) | void;
let root: HTMLElement;

export function navigate(route: Route): void {
  location.hash = `#/${route}`;
}

function parseRoute(): Route {
  const hash = location.hash.replace(/^#\/?/, '');
  const route = hash.split('?')[0] as Route;
  if (route in screens) return route;
  return getSession() ? 'home' : 'login';
}

async function renderRoute(): Promise<void> {
  let route = parseRoute();
  const session = getSession();

  if (!session && !PUBLIC_ROUTES.includes(route)) {
    route = 'login';
    location.hash = '#/login';
    return; // hashchange will re-trigger renderRoute
  }
  if (session && route === 'login') {
    route = 'home';
    location.hash = '#/home';
    return;
  }

  if (typeof currentCleanup === 'function') currentCleanup();
  root.innerHTML = '';

  const mod = await screens[route]!();
  currentCleanup = mod.render(root);
}

export function initRouter(appRoot: HTMLElement): void {
  root = appRoot;
  window.addEventListener('hashchange', () => void renderRoute());
  void renderRoute();
}
