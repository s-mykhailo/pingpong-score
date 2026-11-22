import { renderShell } from './shell';
import { getSession, logout } from '../api/client';
import { navigate } from '../router';
import { ACCENT_PRESETS, getAccentColor, getThemeMode, setAccentColor, setThemeMode, type ThemeMode } from '../theme/theme';
import { clearData } from '../store';

export function render(root: HTMLElement): () => void {
  return renderShell(root, 'settings', (content) => {
    const draw = () => {
      const session = getSession();
      const mode = getThemeMode();
      const accent = getAccentColor();

      content.innerHTML = `
        <div class="md-typescale-headline-small">Налаштування</div>

        <div class="card">
          <div class="muted md-typescale-label-medium">Група</div>
          <div class="md-typescale-title-medium">${session?.group.name ?? ''}</div>
          <div class="muted md-typescale-body-small">Роль: ${session?.role === 'admin' ? 'адміністратор' : 'учасник'}</div>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Тема</div>
          <div class="row">
            ${themeOption('light', 'Світла', mode)}
            ${themeOption('dark', 'Темна', mode)}
            ${themeOption('system', 'Системна', mode)}
          </div>
        </div>

        <div class="col">
          <div class="md-typescale-title-medium">Акцентний колір</div>
          <div class="row" style="flex-wrap:wrap;">
            ${ACCENT_PRESETS.map(
              (c) =>
                `<div class="color-swatch ${c.hex === accent ? 'selected' : ''}" style="background:${c.hex}" data-hex="${c.hex}" title="${c.name}"></div>`,
            ).join('')}
          </div>
        </div>

        <md-outlined-button id="logout-btn" style="align-self:flex-start;">
          <md-icon slot="icon">logout</md-icon>
          Вийти
        </md-outlined-button>
      `;

      content.querySelectorAll('[data-mode]').forEach((btn) => {
        btn.addEventListener('click', () => {
          setThemeMode((btn as HTMLElement).dataset.mode as ThemeMode);
          draw();
        });
      });
      content.querySelectorAll('.color-swatch').forEach((el) => {
        el.addEventListener('click', () => {
          setAccentColor((el as HTMLElement).dataset.hex!);
          draw();
        });
      });
      content.querySelector('#logout-btn')?.addEventListener('click', () => void onLogout());
    };

    function themeOption(value: ThemeMode, label: string, current: ThemeMode): string {
      const selected = value === current;
      return `<md-filled-tonal-button data-mode="${value}" style="${selected ? '' : 'opacity:0.6;'}">${label}</md-filled-tonal-button>`;
    }

    async function onLogout() {
      await logout();
      clearData();
      navigate('login');
    }

    draw();
    return () => {};
  });
}
