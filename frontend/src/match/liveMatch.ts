import { getCurrentMatch, saveCurrentMatch } from '../offline/db';
import { getCurrentRoomId } from '../room';
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
  const roomId = getCurrentRoomId();
  current = roomId ? ((await getCurrentMatch(roomId)) ?? null) : null;
  return current;
}

export function getLiveMatch(): LiveMatchState | null {
  return current;
}

export function setLiveMatch(state: LiveMatchState | null): void {
  current = state;
  const roomId = getCurrentRoomId();
  if (roomId) void saveCurrentMatch(roomId, state);
  notify();
}
