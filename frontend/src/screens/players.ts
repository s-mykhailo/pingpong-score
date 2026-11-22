import { renderShell } from './shell';
import { getData, subscribe, mergePlayer, removePlayerLocal } from '../store';
import { addPlayer, deletePlayer, getSession, NetworkError } from '../api/client';
import { computePlayerStats } from '../stats/compute';

export function render(root: HTMLElement): () => void {
  let newName = '';
  let error = '';

  return renderShell(root, 'players', (content) => {
    const draw = () => {
      const data = getData();
      const players = data?.players ?? [];
      const matches = data?.matches ?? [];
      const isAdmin = getSession()?.role === 'admin';

      content.innerHTML = `
        <div class="md-typescale-headline-small">Гравці</div>

        <div class="row">
          <md-outlined-text-field class="fill" id="new-name" label="Ім'я гравця" value="${escapeAttr(newName)}"></md-outlined-text-field>
          <md-filled-button id="add-btn"><md-icon slot="icon">add</md-icon>Додати</md-filled-button>
        </div>
        ${error ? `<div class="md-typescale-body-medium" style="color:var(--md-sys-color-error)">${error}</div>` : ''}

        <div class="col">
          ${
            players.length === 0
              ? `<div class="empty-state"><md-icon>group</md-icon><div>Поки немає гравців</div></div>`
              : players
                  .map((p) => {
                    const s = computePlayerStats(matches, p.id);
                    return `
                <div class="card row-between">
                  <div class="col">
                    <div class="md-typescale-title-medium">${escapeHtml(p.name)}</div>
                    <div class="muted md-typescale-body-small">${s.wins}П / ${s.losses}П · ${Math.round(s.winRate * 100)}%</div>
                  </div>
                  ${isAdmin ? `<md-icon-button data-id="${p.id}" class="delete-btn"><md-icon>delete</md-icon></md-icon-button>` : ''}
                </div>`;
                  })
                  .join('')
          }
        </div>
      `;

      const nameField = content.querySelector('#new-name') as HTMLInputElement & { value: string };
      nameField?.addEventListener('input', () => {
        newName = nameField.value;
      });
      content.querySelector('#add-btn')?.addEventListener('click', () => void onAdd());
      content.querySelectorAll('.delete-btn').forEach((btn) => {
        btn.addEventListener('click', () => void onDelete((btn as HTMLElement).dataset.id!));
      });
    };

    async function onAdd() {
      const name = newName.trim();
      if (!name) return;
      try {
        const player = await addPlayer(name);
        mergePlayer(player);
        newName = '';
        error = '';
        draw();
      } catch (e) {
        error = e instanceof NetworkError ? 'Немає інтернету.' : 'Не вдалося додати гравця (можливо, таке ім’я вже є).';
        draw();
      }
    }

    async function onDelete(id: string) {
      try {
        await deletePlayer(id);
        removePlayerLocal(id);
      } catch (e) {
        error = e instanceof NetworkError ? 'Немає інтернету.' : 'Не вдалося видалити гравця.';
        draw();
      }
    }

    draw();
    return subscribe(draw);
  });
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}

function escapeAttr(s: string): string {
  return escapeHtml(s);
}
