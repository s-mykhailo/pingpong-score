import { BACKEND_URL } from '../config';
import type { Match, Player, Preset, RoomSummary, Rules, Session, User } from '../types';

const SESSION_KEY = 'pp_session';

export function getSession(): Session | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as Session;
  } catch {
    return null;
  }
}

export function setSession(session: Session) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function updateSessionUser(user: User) {
  const session = getSession();
  if (!session) return;
  setSession({ ...session, user });
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
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

// --- акаунт ---

export async function register(
  nickname: string,
  avatar = '',
  question?: string,
  answer?: string,
): Promise<{ session: Session; recoveryCode: string }> {
  const res = await call<{ token: string; recoveryCode: string; user: User }>('register', { nickname, avatar, question, answer });
  const session: Session = { token: res.token, user: res.user };
  setSession(session);
  return { session, recoveryCode: res.recoveryCode };
}

export async function recoverAccount(code: string): Promise<Session> {
  const res = await call<{ token: string; user: User }>('recoverAccount', { code });
  const session: Session = { token: res.token, user: res.user };
  setSession(session);
  return session;
}

export async function getRecoveryQuestion(nickname: string): Promise<string> {
  const res = await call<{ question: string }>('getRecoveryQuestion', { nickname });
  return res.question;
}

export async function loginByQuestion(nickname: string, answer: string): Promise<Session> {
  const res = await call<{ token: string; user: User }>('loginByQuestion', { nickname, answer });
  const session: Session = { token: res.token, user: res.user };
  setSession(session);
  return session;
}

export async function setRecoveryQuestion(question: string, answer: string): Promise<User> {
  const res = await call<{ user: User }>('setRecoveryQuestion', { question, answer });
  updateSessionUser(res.user);
  return res.user;
}

export async function me(): Promise<User> {
  const res = await call<{ user: User }>('me');
  updateSessionUser(res.user);
  return res.user;
}

export async function updateProfile(nickname: string, avatar: string): Promise<User> {
  const res = await call<{ user: User }>('updateProfile', { nickname, avatar });
  updateSessionUser(res.user);
  return res.user;
}

export async function claimMasterkey(masterKey: string): Promise<User> {
  const res = await call<{ user: User }>('claimMasterkey', { masterKey });
  updateSessionUser(res.user);
  return res.user;
}

export async function createAdminInvite(): Promise<string> {
  const res = await call<{ code: string }>('createAdminInvite');
  return res.code;
}

export async function claimAdmin(code: string): Promise<User> {
  const res = await call<{ user: User }>('claimAdmin', { code });
  updateSessionUser(res.user);
  return res.user;
}

export async function logout(): Promise<void> {
  try {
    await call('logout');
  } finally {
    clearSession();
  }
}

// --- кімнати ---

export async function myRooms(): Promise<RoomSummary[]> {
  const res = await call<{ rooms: RoomSummary[] }>('myRooms');
  return res.rooms;
}

export async function createRoom(name: string): Promise<{ room: RoomSummary; joinCode: string }> {
  const res = await call<{ room: { id: string; name: string; ownerId: string }; joinCode: string }>('createRoom', { name });
  return { room: { id: res.room.id, name: res.room.name, isAdmin: true }, joinCode: res.joinCode };
}

export async function createRoomInvite(roomId: string): Promise<string> {
  const res = await call<{ code: string }>('createRoomInvite', { roomId });
  return res.code;
}

export async function joinRoom(code: string): Promise<RoomSummary> {
  const res = await call<{ room: { id: string; name: string } }>('joinRoom', { code });
  return { id: res.room.id, name: res.room.name, isAdmin: false };
}

// --- дані кімнати (гравці/матчі/пресети) ---

export interface AllData {
  players: Player[];
  matches: Match[];
  presets: Preset[];
}

export async function getAll(roomId: string): Promise<AllData> {
  return call<AllData>('getAll', { roomId });
}

export async function addPlayer(roomId: string, name: string, id?: string): Promise<Player> {
  const res = await call<{ player: Player }>('addPlayer', { roomId, name, id });
  return res.player;
}

export async function deletePlayer(roomId: string, id: string): Promise<void> {
  await call('deletePlayer', { roomId, id });
}

export async function addMatch(roomId: string, match: Match): Promise<{ id: string; winner?: string; duplicate?: boolean }> {
  return call('addMatch', { roomId, match });
}

export async function deleteMatch(roomId: string, id: string): Promise<void> {
  await call('deleteMatch', { roomId, id });
}

export async function savePreset(roomId: string, preset: { id?: string; name: string; rules: Rules }): Promise<Preset> {
  const res = await call<{ preset: Preset }>('savePreset', { roomId, preset });
  return res.preset;
}

export async function deletePreset(roomId: string, id: string): Promise<void> {
  await call('deletePreset', { roomId, id });
}
