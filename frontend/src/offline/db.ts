import { createStore, get, set, del, keys } from 'idb-keyval';
import type { AllData } from '../api/client';
import type { LiveMatchState, Match } from '../types';

const store = createStore('pingpong-score', 'kv');

const CACHE_KEY = 'cache-all-data';
const CURRENT_MATCH_KEY = 'current-match';
const PENDING_PREFIX = 'pending-match-';

export async function cacheAllData(data: AllData): Promise<void> {
  await set(CACHE_KEY, data, store);
}

export async function getCachedAllData(): Promise<AllData | undefined> {
  return get(CACHE_KEY, store);
}

export async function queueMatch(match: Match): Promise<void> {
  await set(PENDING_PREFIX + match.id, match, store);
}

export async function getPendingMatches(): Promise<Match[]> {
  const allKeys = await keys(store);
  const pendingKeys = allKeys.filter((k) => typeof k === 'string' && k.startsWith(PENDING_PREFIX));
  const matches = await Promise.all(pendingKeys.map((k) => get<Match>(k, store)));
  return matches.filter((m): m is Match => !!m);
}

export async function removePendingMatch(id: string): Promise<void> {
  await del(PENDING_PREFIX + id, store);
}

export async function saveCurrentMatch(state: LiveMatchState | null): Promise<void> {
  if (state === null) {
    await del(CURRENT_MATCH_KEY, store);
  } else {
    await set(CURRENT_MATCH_KEY, state, store);
  }
}

export async function getCurrentMatch(): Promise<LiveMatchState | undefined> {
  return get(CURRENT_MATCH_KEY, store);
}
