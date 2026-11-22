import { BACKEND_URL } from '../config';
import type { Group, Match, Player, Preset, Role, Rules, Session } from '../types';

const TOKEN_KEY = 'pp_token';
const ROLE_KEY = 'pp_role';
const GROUP_KEY = 'pp_group';

export function getSession(): Session | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const role = localStorage.getItem(ROLE_KEY) as Role | null;
  const groupRaw = localStorage.getItem(GROUP_KEY);
  if (!token || !role || !groupRaw) return null;
  return { token, role, group: JSON.parse(groupRaw) as Group };
}

export function setSession(session: Session) {
  localStorage.setItem(TOKEN_KEY, session.token);
  localStorage.setItem(ROLE_KEY, session.role);
  localStorage.setItem(GROUP_KEY, JSON.stringify(session.group));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(GROUP_KEY);
}

/** The server understood the request and rejected it (bad data, auth, etc). Not recoverable by retrying. */
export class ApiError extends Error {}

/** The request never reached the server (offline, DNS, timeout). Safe to retry later. */
export class NetworkError extends Error {}

async function call<T>(action: string, payload: Record<string, unknown> = {}): Promise<T> {
  const session = getSession();
  const body = JSON.stringify({ action, token: session?.token, ...payload });
  let res: Response;
  try {
    res = await fetch(BACKEND_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body,
    });
  } catch {
    throw new NetworkError('network');
  }
  const json = (await res.json()) as { ok: boolean; error?: string } & T;
  if (!json.ok) throw new ApiError(json.error ?? 'unknown error');
  return json;
}

export async function join(code: string): Promise<Session> {
  const res = await call<{ token: string; role: Role; group: Group }>('join', { code });
  const session: Session = { token: res.token, role: res.role, group: res.group };
  setSession(session);
  return session;
}

export async function logout(): Promise<void> {
  try {
    await call('logout');
  } finally {
    clearSession();
  }
}

export interface AllData {
  group: Group;
  role: Role;
  players: Player[];
  matches: Match[];
  presets: Preset[];
}

export async function getAll(): Promise<AllData> {
  return call<AllData>('getAll');
}

export async function addPlayer(name: string, id?: string): Promise<Player> {
  const res = await call<{ player: Player }>('addPlayer', { name, id });
  return res.player;
}

export async function deletePlayer(id: string): Promise<void> {
  await call('deletePlayer', { id });
}

export async function addMatch(match: Match): Promise<{ id: string; winner?: string; duplicate?: boolean }> {
  return call('addMatch', { match });
}

export async function deleteMatch(id: string): Promise<void> {
  await call('deleteMatch', { id });
}

export async function savePreset(preset: { id?: string; name: string; rules: Rules }): Promise<Preset> {
  const res = await call<{ preset: Preset }>('savePreset', { preset });
  return res.preset;
}

export async function deletePreset(id: string): Promise<void> {
  await call('deletePreset', { id });
}
