import { addMatch, NetworkError } from '../api/client';
import { getPendingMatches, removePendingMatch } from './db';
import { getCurrentRoomId } from '../room';

type Listener = (pendingCount: number) => void;

const listeners = new Set<Listener>();
let syncing = false;

export function onPendingCountChange(fn: Listener): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

async function notify() {
  const roomId = getCurrentRoomId();
  const pending = roomId ? await getPendingMatches(roomId) : [];
  for (const fn of listeners) fn(pending.length);
}

export async function syncPendingMatches(): Promise<void> {
  const roomId = getCurrentRoomId();
  if (syncing || !navigator.onLine || !roomId) return;
  syncing = true;
  try {
    const pending = await getPendingMatches(roomId);
    for (const match of pending) {
      try {
        await addMatch(roomId, match);
        await removePendingMatch(roomId, match.id);
      } catch (e) {
        if (e instanceof NetworkError) {
          break; // offline or unreachable - stop, try again later
        }
        // Server rejected the match permanently (bad data) - drop it so the queue doesn't jam forever.
        await removePendingMatch(roomId, match.id);
      }
    }
  } finally {
    syncing = false;
    await notify();
  }
}

export function initSync(): void {
  void notify();
  void syncPendingMatches();
  window.addEventListener('online', () => void syncPendingMatches());
  setInterval(() => void syncPendingMatches(), 30_000);
}

export async function refreshPendingCount(): Promise<void> {
  await notify();
}
