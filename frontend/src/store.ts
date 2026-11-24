import { getAll, type AllData } from './api/client';
import { cacheAllData, getCachedAllData } from './offline/db';
import { getCurrentRoomId } from './room';
import type { Match, Preset, Player } from './types';

type Listener = () => void;

let data: AllData | null = null;
const listeners = new Set<Listener>();

export function subscribe(fn: Listener): () => void {
  listeners.add(fn);
  return () => listeners.delete(fn);
}

function notify() {
  for (const fn of listeners) fn();
}

export function getData(): AllData | null {
  return data;
}

export async function loadInitial(): Promise<void> {
  const roomId = getCurrentRoomId();
  if (!roomId) return;
  const cached = await getCachedAllData(roomId);
  if (cached) {
    data = cached;
    notify();
  }
  if (navigator.onLine) {
    try {
      const fresh = await getAll(roomId);
      data = fresh;
      await cacheAllData(roomId, fresh);
      notify();
    } catch {
      // stay on cached data
    }
  }
}

export async function refresh(): Promise<void> {
  const roomId = getCurrentRoomId();
  if (!roomId || !navigator.onLine) return;
  try {
    const fresh = await getAll(roomId);
    data = fresh;
    await cacheAllData(roomId, fresh);
    notify();
  } catch {
    // ignore, keep showing stale data
  }
}

export function mergePlayer(player: Player): void {
  if (!data) return;
  const existing = data.players.findIndex((p) => p.id === player.id);
  if (existing >= 0) data.players[existing] = player;
  else data.players.push(player);
  notify();
}

export function removePlayerLocal(id: string): void {
  if (!data) return;
  data.players = data.players.filter((p) => p.id !== id);
  notify();
}

export function mergeMatchLocal(match: Match): void {
  if (!data) return;
  const existing = data.matches.findIndex((m) => m.id === match.id);
  if (existing >= 0) data.matches[existing] = match;
  else data.matches.push(match);
  notify();
}

export function removeMatchLocal(id: string): void {
  if (!data) return;
  data.matches = data.matches.filter((m) => m.id !== id);
  notify();
}

export function mergePresetLocal(preset: Preset): void {
  if (!data) return;
  const existing = data.presets.findIndex((p) => p.id === preset.id);
  if (existing >= 0) data.presets[existing] = preset;
  else data.presets.push(preset);
  notify();
}

export function removePresetLocal(id: string): void {
  if (!data) return;
  data.presets = data.presets.filter((p) => p.id !== id);
  notify();
}

export function clearData(): void {
  data = null;
  notify();
}
