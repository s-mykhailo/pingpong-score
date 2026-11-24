import { getSession } from './api/client';
import { getCurrentRoomId } from './room';

export type Route =
  | 'login'
  | 'rooms'
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
  rooms: () => import('./screens/rooms'),
  home: () => import('./screens/home'),
  'match-setup': () => import('./screens/matchSetup'),
  scoreboard: () => import('./screens/scoreboard'),
  stats: () => import('./screens/stats'),
  players: () => import('./screens/players'),
  settings: () => import('./screens/settings'),
};

const PUBLIC_ROUTES: Route[] = ['login'];
const ROOM_ROUTES: Route[] = ['home', 'match-setup', 'scoreboard', 'stats', 'players', 'settings'];

let currentCleanup: (() => void) | void;
let root: HTMLElement;

export function navigate(route: Route): void {
  location.hash = `#/${route}`;
}

function parseRoute(): Route {
  const hash = location.hash.replace(/^#\/?/, '');
  const route = hash.split('?')[0] as Route;
  if (route in screens) return route;
  return getSession() ? 'rooms' : 'login';
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
    route = getCurrentRoomId() ? 'home' : 'rooms';
    location.hash = `#/${route}`;
    return;
  }
  if (session && ROOM_ROUTES.includes(route) && !getCurrentRoomId()) {
    route = 'rooms';
    location.hash = '#/rooms';
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
