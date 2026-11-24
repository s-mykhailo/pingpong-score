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

export interface Group {
  id: string;
  name: string;
}

export type Role = 'admin' | 'member';

export interface Session {
  token: string;
  role: Role;
  group: Group;
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
