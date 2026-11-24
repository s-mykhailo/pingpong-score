import { createStore, get, set, del, keys } from 'idb-keyval';
import type { AllData } from '../api/client';
import type { LiveMatchState, Match } from '../types';

const store = createStore('pingpong-score', 'kv');

const CACHE_PREFIX = 'cache-all-data-';
const CURRENT_MATCH_PREFIX = 'current-match-';
const PENDING_PREFIX = 'pending-match-';

// Усі ключі нижче намспейсяться по roomId - кожен акаунт може мати кілька кімнат
// з незалежними кешем/живим матчем/черги на відправку.

export async function cacheAllData(roomId: string, data: AllData): Promise<void> {
  await set(CACHE_PREFIX + roomId, data, store);
}

export async function getCachedAllData(roomId: string): Promise<AllData | undefined> {
  return get(CACHE_PREFIX + roomId, store);
}

export async function queueMatch(roomId: string, match: Match): Promise<void> {
  await set(PENDING_PREFIX + roomId + '-' + match.id, match, store);
}

export async function getPendingMatches(roomId: string): Promise<Match[]> {
  const prefix = PENDING_PREFIX + roomId + '-';
  const allKeys = await keys(store);
  const pendingKeys = allKeys.filter((k) => typeof k === 'string' && k.startsWith(prefix));
  const matches = await Promise.all(pendingKeys.map((k) => get<Match>(k, store)));
  return matches.filter((m): m is Match => !!m);
}

export async function removePendingMatch(roomId: string, id: string): Promise<void> {
  await del(PENDING_PREFIX + roomId + '-' + id, store);
}

export async function saveCurrentMatch(roomId: string, state: LiveMatchState | null): Promise<void> {
  if (state === null) {
    await del(CURRENT_MATCH_PREFIX + roomId, store);
  } else {
    await set(CURRENT_MATCH_PREFIX + roomId, state, store);
  }
}

export async function getCurrentMatch(roomId: string): Promise<LiveMatchState | undefined> {
  return get(CURRENT_MATCH_PREFIX + roomId, store);
}
