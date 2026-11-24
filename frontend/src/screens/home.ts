import { renderShell } from './shell';
import { navigate } from '../router';
import { getData, subscribe, refresh } from '../store';
import { getCurrentRoom } from '../room';
import { getLiveMatch, subscribeLiveMatch } from '../match/liveMatch';
import { sortedMatchHistory, playerName } from '../stats/compute';
import type { Match, Player } from '../types';

export function render(root: HTMLElement): () => void {
  return renderShell(root, 'home', (content) => {
    const draw = () => {
      content.innerHTML = buildContent();
      wireUp();
    };

    const buildContent = (): string => {
      const data = getData();
      const live = getLiveMatch();
      const matches = data ? sortedMatchHistory(data.matches).slice(0, 10) : [];

      return `
        <div class="topbar">
          <md-icon-button id="rooms-btn"><md-icon>arrow_back</md-icon></md-icon-button>
          <div class="topbar-title md-typescale-headline-small">${escapeHtml(getCurrentRoom()?.name ?? '')}</div>
          <md-icon-button id="refresh-btn"><md-icon>refresh</md-icon></md-icon-button>
        </div>

        ${
          live
            ? `<div class="card" id="resume-card">
                <div class="md-typescale-title-medium">Матч триває</div>
                <div class="muted md-typescale-body-medium">${sideLabel(data?.players ?? [], live.player1, live.partner1)} проти ${sideLabel(data?.players ?? [], live.player2, live.partner2)}</div>
                <md-filled-button id="resume-btn"><md-icon slot="icon">play_arrow</md-icon>Продовжити</md-filled-button>
              </div>`
            : `<md-filled-button id="new-match-btn" style="align-self:stretch;">
                <md-icon slot="icon">add</md-icon>
                Новий матч
              </md-filled-button>`
        }

        <div class="md-typescale-title-medium">Останні матчі</div>
        <div class="col" id="match-list">
          ${
            matches.length === 0
              ? `<div class="muted md-typescale-body-medium">Матчів ще немає</div>`
              : matches.map((m) => matchRow(m, data?.players ?? [])).join('')
          }
        </div>
      `;
    };

    function sideLabel(players: Player[], primary: string, partner: string | null): string {
      return partner !== null ? `${playerName(players, primary)} / ${playerName(players, partner)}` : playerName(players, primary);
    }

    function matchRow(m: Match, players: Player[]): string {
      const p1 = sideLabel(players, m.player1, m.partner1);
      const p2 = sideLabel(players, m.player2, m.partner2);
      const setsWon1 = m.sets.filter(([a, b]) => a > b).length;
      const setsWon2 = m.sets.length - setsWon1;
      const winnerIsP1 = m.winner === m.player1;
      const date = new Date(m.playedAt).toLocaleDateString('uk-UA', { day: '2-digit', month: '2-digit' });
      return `
        <div class="card">
          <div class="row-between">
            <div class="md-typescale-body-large">
              <strong>${winnerIsP1 ? p1 : p2}</strong> переміг ${winnerIsP1 ? p2 : p1}
            </div>
            <div class="muted md-typescale-body-small">${date}</div>
          </div>
          <div class="muted md-typescale-body-medium">${setsWon1}:${setsWon2} (${m.sets.map(([a, b]) => `${a}:${b}`).join(', ')})</div>
        </div>
      `;
    }

    function wireUp() {
      content.querySelector('#new-match-btn')?.addEventListener('click', () => navigate('match-setup'));
      content.querySelector('#resume-btn')?.addEventListener('click', () => navigate('scoreboard'));
      content.querySelector('#refresh-btn')?.addEventListener('click', () => void refresh());
      content.querySelector('#rooms-btn')?.addEventListener('click', () => navigate('rooms'));
    }

    draw();
    const unsubData = subscribe(draw);
    const unsubLive = subscribeLiveMatch(draw);

    return () => {
      unsubData();
      unsubLive();
    };
  });
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}
