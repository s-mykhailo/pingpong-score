export interface Rules {
  pointsToWin: number;
  winBy: 1 | 2;
  bestOf: number;
  serveEvery: number;
  deuceServeEvery: number;
}

export interface Player {
  id: string;
  name: string;
  createdAt: string;
}

export interface Preset {
  id: string;
  name: string;
  rules: Rules;
}

export type SetScore = [number, number];

export type MatchMode = 'singles' | 'doubles';

export interface Match {
  id: string;
  playedAt: string;
  player1: string;
  player2: string;
  sets: SetScore[];
  winner: string;
  rules: Rules;
  presetName: string;
  mode: MatchMode;
  partner1: string | null;
  partner2: string | null;
}

export type UserRole = 'user' | 'admin' | 'masterkey';

export interface User {
  id: string;
  nickname: string;
  avatar: string;
  role: UserRole;
  hasRecoveryQuestion: boolean;
  recoveryQuestion: string;
}

export interface Session {
  token: string;
  user: User;
}

export interface RoomSummary {
  id: string;
  name: string;
  isAdmin: boolean;
}

export interface LiveMatchState {
  player1: string;
  player2: string;
  rules: Rules;
  presetName: string;
  finishedSets: SetScore[];
  currentSet: SetScore;
  // Singles: 0/1 (which side serves). Doubles: 0..3, a slot in rotationOrder()
  // ([player1, player2, partner1, partner2] = A1,B1,A2,B2, the ITTF doubles serve order).
  // side-for-slot is always `server % 2` - singles collapses to the old 0/1 meaning.
  server: number;
  pointsSinceSwitch: number;
  firstServerOfMatch: number;
  startedAt: string;
  mode: MatchMode;
  partner1: string | null;
  partner2: string | null;
}
