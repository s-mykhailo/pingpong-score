import { renderShell } from './shell';
import { getData, subscribe } from '../store';
import { computePlayerStats, computeHeadToHead, playerName, sortedMatchHistory } from '../stats/compute';
import type { Match, Player } from '../types';

export function render(root: HTMLElement): () => void {
  let selectedPlayer = '';
  let h2hA = '';
  let h2hB = '';

  return renderShell(root, 'stats', (content) => {
    const draw = () => {
      const data = getData();
      const players = data?.players ?? [];
      const matches = data?.matches ?? [];
      if (!selectedPlayer && players.length > 0) selectedPlayer = players[0].id;
      if (!h2hA && players[0]) h2hA = players[0].id;
      if (!h2hB && players[1]) h2hB = players[1].id;

      content.innerHTML = `
        <div class="md-typescale-headline-small">Статистика</div>

        ${
          players.length === 0
            ? `<div class="empty-state"><md-icon>bar_chart</md-icon><div>Додайте гравців, щоб побачити статистику</div></div>`
            : `
          <md-outlined-select id="player-select" label="Гравець">
            ${players.map((p) => option(p, selectedPlayer)).join('')}
          </md-outlined-select>
          <div id="player-stats"></div>

          <div class="md-typescale-title-medium" style="margin-top:8px;">Особисті зустрічі</div>
          <div class="row">
            <md-outlined-select class="fill" id="h2h-a" label="Гравець A">
              ${players.map((p) => option(p, h2hA)).join('')}
            </md-outlined-select>
            <md-outlined-select class="fill" id="h2h-b" label="Гравець B">
              ${players.map((p) => option(p, h2hB)).join('')}
            </md-outlined-select>
          </div>
          <div id="h2h-result"></div>
        `
        }
      `;

      if (players.length > 0) {
        renderPlayerStats(players, matches);
        renderH2H(players, matches);
        wire(players);
      }
    };

    function option(p: Player, selected: string): string {
      return `<md-select-option value="${p.id}" ${p.id === selected ? 'selected' : ''}><div slot="headline">${escapeHtml(p.name)}</div></md-select-option>`;
    }

    function renderPlayerStats(players: Player[], matches: Match[]) {
      const el = content.querySelector('#player-stats');
      if (!el || !selectedPlayer) return;
      const s = computePlayerStats(matches, selectedPlayer);
      const history = sortedMatchHistory(matches.filter((m) => m.player1 === selectedPlayer || m.player2 === selectedPlayer)).slice(0, 10);
      el.innerHTML = `
        <div class="card">
          <div class="row-between">
            <div class="col"><div class="muted md-typescale-label-medium">Перемоги</div><div class="md-typescale-headline-small">${s.wins}</div></div>
            <div class="col"><div class="muted md-typescale-label-medium">Поразки</div><div class="md-typescale-headline-small">${s.losses}</div></div>
            <div class="col"><div class="muted md-typescale-label-medium">Вінрейт</div><div class="md-typescale-headline-small">${Math.round(s.winRate * 100)}%</div></div>
            <div class="col"><div class="muted md-typescale-label-medium">Серія</div><div class="md-typescale-headline-small">${s.currentStreak > 0 ? '+' : ''}${s.currentStreak}</div></div>
          </div>
          <div class="muted md-typescale-body-small">Сети: ${s.setsWon}:${s.setsLost}</div>
        </div>
        <div class="col">
          ${history
            .map((m) => {
              const opponentId = m.player1 === selectedPlayer ? m.player2 : m.player1;
              const won = m.winner === selectedPlayer;
              const date = new Date(m.playedAt).toLocaleDateString('uk-UA', { day: '2-digit', month: '2-digit' });
              return `<div class="card row-between">
                <div class="md-typescale-body-medium">${won ? '✓' : '✗'} проти ${playerName(players, opponentId)}</div>
                <div class="muted md-typescale-body-small">${date}</div>
              </div>`;
            })
            .join('')}
        </div>
      `;
    }

    function renderH2H(players: Player[], matches: Match[]) {
      const el = content.querySelector('#h2h-result');
      if (!el || !h2hA || !h2hB || h2hA === h2hB) {
        if (el) el.innerHTML = h2hA === h2hB ? `<div class="muted md-typescale-body-medium">Оберіть двох різних гравців</div>` : '';
        return;
      }
      const h2h = computeHeadToHead(matches, h2hA, h2hB);
      el.innerHTML = `
        <div class="card">
          <div class="row-between md-typescale-title-medium">
            <span>${playerName(players, h2hA)}</span>
            <span>${h2h.aWins} : ${h2h.bWins}</span>
            <span>${playerName(players, h2hB)}</span>
          </div>
        </div>
        <div class="col">
          ${h2h.matches
            .slice(0, 10)
            .map((m) => {
              const date = new Date(m.playedAt).toLocaleDateString('uk-UA', { day: '2-digit', month: '2-digit' });
              return `<div class="card row-between"><div class="md-typescale-body-medium">${m.sets.map(([a, b]) => `${a}:${b}`).join(', ')}</div><div class="muted md-typescale-body-small">${date}</div></div>`;
            })
            .join('')}
        </div>
      `;
    }

    function wire(players: Player[]) {
      const ps = content.querySelector('#player-select') as HTMLElement & { value: string };
      ps?.addEventListener('change', () => {
        selectedPlayer = ps.value;
        renderPlayerStats(players, getData()?.matches ?? []);
      });
      const a = content.querySelector('#h2h-a') as HTMLElement & { value: string };
      const b = content.querySelector('#h2h-b') as HTMLElement & { value: string };
      a?.addEventListener('change', () => {
        h2hA = a.value;
        renderH2H(players, getData()?.matches ?? []);
      });
      b?.addEventListener('change', () => {
        h2hB = b.value;
        renderH2H(players, getData()?.matches ?? []);
      });
    }

    draw();
    return subscribe(draw);
  });
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}
