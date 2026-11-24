import type { RoomSummary } from './types';

const ROOM_KEY = 'pp_room';

export function getCurrentRoom(): RoomSummary | null {
  const raw = localStorage.getItem(ROOM_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as RoomSummary;
  } catch {
    return null;
  }
}

export function getCurrentRoomId(): string | null {
  return getCurrentRoom()?.id ?? null;
}

export function setCurrentRoom(room: RoomSummary): void {
  localStorage.setItem(ROOM_KEY, JSON.stringify(room));
}

export function clearCurrentRoom(): void {
  localStorage.removeItem(ROOM_KEY);
}
