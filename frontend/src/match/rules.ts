import type { LiveMatchState, Rules, SetScore } from '../types';

export function isDeuceZone(a: number, b: number, rules: Rules): boolean {
  return rules.winBy === 2 && a >= rules.pointsToWin - 1 && b >= rules.pointsToWin - 1;
}

export function setWinner(a: number, b: number, rules: Rules): 0 | 1 | null {
  const hi = Math.max(a, b);
  const lo = Math.min(a, b);
  if (hi < rules.pointsToWin) return null;
  if (rules.winBy === 1) {
    if (hi !== rules.pointsToWin) return null;
    return a > b ? 0 : 1;
  }
  const won = hi === rules.pointsToWin ? lo <= rules.pointsToWin - 2 : hi - lo === 2;
  if (!won) return null;
  return a > b ? 0 : 1;
}

export function neededSets(rules: Rules): number {
  return Math.ceil(rules.bestOf / 2);
}

export function matchWinner(finishedSets: SetScore[], rules: Rules): 0 | 1 | null {
  const need = neededSets(rules);
  let w0 = 0;
  let w1 = 0;
  for (const [a, b] of finishedSets) {
    if (a > b) w0++;
    else w1++;
  }
  if (w0 >= need) return 0;
  if (w1 >= need) return 1;
  return null;
}

export function createLiveMatch(
  player1: string,
  player2: string,
  rules: Rules,
  presetName: string,
  firstServer: 0 | 1,
): LiveMatchState {
  return {
    player1,
    player2,
    rules,
    presetName,
    finishedSets: [],
    currentSet: [0, 0],
    server: firstServer,
    pointsSinceSwitch: 0,
    firstServerOfMatch: firstServer,
    startedAt: new Date().toISOString(),
  };
}

/** Mutates nothing; returns the next state after a point for `scorer` (0 or 1). */
export function addPoint(state: LiveMatchState, scorer: 0 | 1): LiveMatchState {
  const [a, b] = state.currentSet;
  const currentSet: SetScore = scorer === 0 ? [a + 1, b] : [a, b + 1];

  const deuce = isDeuceZone(currentSet[0], currentSet[1], state.rules);
  const every = deuce ? state.rules.deuceServeEvery : state.rules.serveEvery;
  const pointsSinceSwitch = state.pointsSinceSwitch + 1;
  const shouldSwitch = pointsSinceSwitch >= every;
  const server: 0 | 1 = shouldSwitch ? (state.server === 0 ? 1 : 0) : state.server;

  const winner = setWinner(currentSet[0], currentSet[1], state.rules);
  if (winner !== null) {
    const finishedSets = [...state.finishedSets, currentSet];
    const setIndex = finishedSets.length;
    const nextFirstServer: 0 | 1 = setIndex % 2 === 0 ? state.firstServerOfMatch : flip(state.firstServerOfMatch);
    return {
      ...state,
      finishedSets,
      currentSet: [0, 0],
      server: nextFirstServer,
      pointsSinceSwitch: 0,
    };
  }

  return {
    ...state,
    currentSet,
    server,
    pointsSinceSwitch: shouldSwitch ? 0 : pointsSinceSwitch,
  };
}

function flip(x: 0 | 1): 0 | 1 {
  return x === 0 ? 1 : 0;
}

export function isMatchOver(state: LiveMatchState): boolean {
  return matchWinner(state.finishedSets, state.rules) !== null;
}

export function sideSwitchDue(state: LiveMatchState): boolean {
  // Classic rule: switch ends in the deciding set once either player reaches half the points to win.
  const need = neededSets(state.rules);
  const isDecidingSet = state.finishedSets.length === need * 2 - 2;
  if (!isDecidingSet) return false;
  const threshold = Math.ceil(state.rules.pointsToWin / 2);
  const [a, b] = state.currentSet;
  return a === threshold || b === threshold;
}
