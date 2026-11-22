import { addMatch, NetworkError } from '../api/client';
import { getPendingMatches, removePendingMatch } from './db';

type Listener = (pendingCount: number) => void;

const listeners = new Set<Listener>();
let syncing = false;

export function onPendingCountChange(fn: Listener): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

async function notify() {
  const pending = await getPendingMatches();
  for (const fn of listeners) fn(pending.length);
}

export async function syncPendingMatches(): Promise<void> {
  if (syncing || !navigator.onLine) return;
  syncing = true;
  try {
    const pending = await getPendingMatches();
    for (const match of pending) {
      try {
        await addMatch(match);
        await removePendingMatch(match.id);
      } catch (e) {
        if (e instanceof NetworkError) {
          break; // offline or unreachable - stop, try again later
        }
        // Server rejected the match permanently (bad data) - drop it so the queue doesn't jam forever.
        await removePendingMatch(match.id);
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
