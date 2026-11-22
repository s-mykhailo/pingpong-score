import { getCurrentMatch, saveCurrentMatch } from '../offline/db';
import type { LiveMatchState } from '../types';

let current: LiveMatchState | null = null;
const listeners = new Set<() => void>();

export function subscribeLiveMatch(fn: () => void): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

function notify() {
  for (const fn of listeners) fn();
}

export async function loadLiveMatch(): Promise<LiveMatchState | null> {
  current = (await getCurrentMatch()) ?? null;
  return current;
}

export function getLiveMatch(): LiveMatchState | null {
  return current;
}

export function setLiveMatch(state: LiveMatchState | null): void {
  current = state;
  void saveCurrentMatch(state);
  notify();
}
