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

export interface Match {
  id: string;
  playedAt: string;
  player1: string;
  player2: string;
  sets: SetScore[];
  winner: string;
  rules: Rules;
  presetName: string;
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
  server: 0 | 1;
  pointsSinceSwitch: number;
  firstServerOfMatch: 0 | 1;
  startedAt: string;
}
