import { argbFromHex, themeFromSourceColor, applyTheme } from '@material/material-color-utilities';

export type ThemeMode = 'light' | 'dark' | 'system';

const MODE_KEY = 'pp_theme_mode';
const COLOR_KEY = 'pp_theme_color';
const DEFAULT_COLOR = '#6750A4';

export const ACCENT_PRESETS = [
  { name: 'Фіолетовий', hex: '#6750A4' },
  { name: 'Зелений', hex: '#2E7D32' },
  { name: 'Синій', hex: '#1565C0' },
  { name: 'Помаранчевий', hex: '#E65100' },
  { name: 'Червоний', hex: '#C62828' },
  { name: 'Бірюзовий', hex: '#00796B' },
];

export function getThemeMode(): ThemeMode {
  return (localStorage.getItem(MODE_KEY) as ThemeMode | null) ?? 'system';
}

export function getAccentColor(): string {
  return localStorage.getItem(COLOR_KEY) ?? DEFAULT_COLOR;
}

function resolveIsDark(mode: ThemeMode): boolean {
  if (mode === 'system') return window.matchMedia('(prefers-color-scheme: dark)').matches;
  return mode === 'dark';
}

let mediaListenerAttached = false;

export function applyCurrentTheme(): void {
  const mode = getThemeMode();
  const color = getAccentColor();
  const theme = themeFromSourceColor(argbFromHex(color));
  applyTheme(theme, { target: document.documentElement, dark: resolveIsDark(mode) });
  document.documentElement.style.colorScheme = resolveIsDark(mode) ? 'dark' : 'light';

  if (!mediaListenerAttached) {
    mediaListenerAttached = true;
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (getThemeMode() === 'system') applyCurrentTheme();
    });
  }
}

export function setThemeMode(mode: ThemeMode): void {
  localStorage.setItem(MODE_KEY, mode);
  applyCurrentTheme();
}

export function setAccentColor(hex: string): void {
  localStorage.setItem(COLOR_KEY, hex);
  applyCurrentTheme();
}
