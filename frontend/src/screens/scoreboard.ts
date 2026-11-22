import { navigate } from '../router';
import { getLiveMatch, setLiveMatch } from '../match/liveMatch';
import { addPoint, isMatchOver, matchWinner, sideSwitchDue } from '../match/rules';
import { queueMatch } from '../offline/db';
import { syncPendingMatches, refreshPendingCount } from '../offline/sync';
import { mergeMatchLocal } from '../store';
import { getData } from '../store';
import type { LiveMatchState, Match } from '../types';

export function render(root: HTMLElement): () => void {
  const live = getLiveMatch();
  if (!live) {
    navigate('home');
    return () => {};
  }

  const history: { state: LiveMatchState; swapped: boolean }[] = [];
  let swapped = false;
  let wakeLock: WakeLockSentinel | null = null;

  void requestWakeLock();
  document.addEventListener('visibilitychange', onVisibilityChange);

  const players = getData()?.players ?? [];
  const nameOf = (id: string) => players.find((p) => p.id === id)?.name ?? '?';

  function draw() {
    const state = getLiveMatch();
    if (!state) return;
    const leftIdx: 0 | 1 = swapped ? 1 : 0;
    const rightIdx: 0 | 1 = swapped ? 0 : 1;
    const ids = [state.player1, state.player2] as const;
    const serverOnLeft = state.server === leftIdx;

    root.innerHTML = `
      <div class="scoreboard">
        <div class="scoreboard-topbar">
          <md-icon-button id="exit-btn"><md-icon>close</md-icon></md-icon-button>
          <md-filled-tonal-button id="undo-btn" ${history.length === 0 ? 'disabled' : ''}>
            <md-icon slot="icon">undo</md-icon>Undo
          </md-filled-tonal-button>
        </div>
        <div class="scoreboard-half left" data-side="${leftIdx}">
          <div class="scoreboard-name">${serverOnLeft ? '<span class="serve-dot"></span>' : ''}${nameOf(ids[leftIdx])}</div>
          <div class="scoreboard-score">${state.currentSet[leftIdx]}</div>
          <div class="scoreboard-sets">${setsSummary(state, leftIdx)}</div>
        </div>
        <div class="scoreboard-half right" data-side="${rightIdx}">
          <div class="scoreboard-name">${nameOf(ids[rightIdx])}${!serverOnLeft ? '<span class="serve-dot"></span>' : ''}</div>
          <div class="scoreboard-score">${state.currentSet[rightIdx]}</div>
          <div class="scoreboard-sets">${setsSummary(state, rightIdx)}</div>
        </div>
      </div>
    `;

    root.querySelector('.scoreboard-half.left')?.addEventListener('click', () => onTap(leftIdx));
    root.querySelector('.scoreboard-half.right')?.addEventListener('click', () => onTap(rightIdx));
    root.querySelector('#undo-btn')?.addEventListener('click', onUndo);
    root.querySelector('#exit-btn')?.addEventListener('click', () => navigate('home'));
  }

  function setsSummary(state: LiveMatchState, idx: 0 | 1): string {
    return state.finishedSets.map((s) => s[idx]).join(' · ');
  }

  function onTap(scorer: 0 | 1) {
    const prev = getLiveMatch();
    if (!prev) return;
    history.push({ state: prev, swapped });
    const next = addPoint(prev, scorer);
    if (sideSwitchDue(next)) swapped = !swapped;
    setLiveMatch(next);

    if (isMatchOver(next)) {
      void finishMatch(next);
      return;
    }
    draw();
  }

  function onUndo() {
    const prev = history.pop();
    if (!prev) return;
    swapped = prev.swapped;
    setLiveMatch(prev.state);
    draw();
  }

  async function finishMatch(state: LiveMatchState) {
    const winnerIdx = matchWinner(state.finishedSets, state.rules);
    const ids = [state.player1, state.player2] as const;
    const match: Match = {
      id: crypto.randomUUID(),
      playedAt: state.startedAt,
      player1: state.player1,
      player2: state.player2,
      sets: state.finishedSets,
      winner: ids[winnerIdx ?? 0],
      rules: state.rules,
      presetName: state.presetName,
    };
    await queueMatch(match);
    mergeMatchLocal(match);
    await refreshPendingCount();
    void syncPendingMatches();
    setLiveMatch(null);
    showResult(match, winnerIdx ?? 0);
  }

  function showResult(match: Match, winnerIdx: 0 | 1) {
    const ids = [match.player1, match.player2] as const;
    root.innerHTML = `
      <div class="scoreboard" style="flex-direction:column; align-items:center; justify-content:center; gap:16px;">
        <md-icon style="font-size:64px; width:64px; height:64px; color:var(--md-sys-color-primary);">emoji_events</md-icon>
        <div class="md-typescale-headline-medium">${nameOf(ids[winnerIdx])} переміг!</div>
        <div class="muted md-typescale-title-medium">${match.sets.map(([a, b]) => `${a}:${b}`).join(', ')}</div>
        <md-filled-button id="done-btn">Готово</md-filled-button>
      </div>
    `;
    root.querySelector('#done-btn')?.addEventListener('click', () => navigate('home'));
  }

  async function requestWakeLock() {
    try {
      wakeLock = await navigator.wakeLock?.request('screen');
    } catch {
      // not supported or denied - not fatal
    }
  }

  function onVisibilityChange() {
    if (document.visibilityState === 'visible' && !wakeLock) void requestWakeLock();
  }

  draw();

  return () => {
    document.removeEventListener('visibilitychange', onVisibilityChange);
    void wakeLock?.release();
  };
}
