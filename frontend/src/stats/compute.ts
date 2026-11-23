import type { Match, Player } from '../types';

export interface PlayerStats {
  playerId: string;
  wins: number;
  losses: number;
  matchesPlayed: number;
  winRate: number;
  setsWon: number;
  setsLost: number;
  currentStreak: number; // positive = win streak, negative = loss streak
}

function matchesForPlayer(matches: Match[], playerId: string): Match[] {
  return matches
    .filter((m) => m.player1 === playerId || m.player2 === playerId)
    .sort((a, b) => a.playedAt.localeCompare(b.playedAt));
}

export function computePlayerStats(matches: Match[], playerId: string): PlayerStats {
  const own = matchesForPlayer(matches, playerId);
  let wins = 0;
  let losses = 0;
  let setsWon = 0;
  let setsLost = 0;
  const results: boolean[] = [];

  for (const m of own) {
    const won = m.winner === playerId;
    if (won) wins++;
    else losses++;
    results.push(won);

    const isP1 = m.player1 === playerId;
    for (const [s1, s2] of m.sets) {
      const mine = isP1 ? s1 : s2;
      const theirs = isP1 ? s2 : s1;
      if (mine > theirs) setsWon++;
      else setsLost++;
    }
  }

  let currentStreak = 0;
  for (let i = results.length - 1; i >= 0; i--) {
    if (i === results.length - 1) {
      currentStreak = results[i] ? 1 : -1;
      continue;
    }
    const sameAsLast = results[i] === results[results.length - 1];
    if (!sameAsLast) break;
    currentStreak += results[i] ? 1 : -1;
  }

  const matchesPlayed = own.length;
  return {
    playerId,
    wins,
    losses,
    matchesPlayed,
    winRate: matchesPlayed > 0 ? wins / matchesPlayed : 0,
    setsWon,
    setsLost,
    currentStreak,
  };
}

export interface HeadToHead {
  aWins: number;
  bWins: number;
  matches: Match[];
}

export function computeHeadToHead(matches: Match[], playerA: string, playerB: string): HeadToHead {
  const between = matches
    .filter(
      (m) =>
        (m.player1 === playerA && m.player2 === playerB) || (m.player1 === playerB && m.player2 === playerA),
    )
    .sort((a, b) => b.playedAt.localeCompare(a.playedAt));

  let aWins = 0;
  let bWins = 0;
  for (const m of between) {
    if (m.winner === playerA) aWins++;
    else bWins++;
  }
  return { aWins, bWins, matches: between };
}

export interface MvpEntry {
  playerId: string;
  wins: number;
  matchesPlayed: number;
}

export function computeMvp(matches: Match[], sinceMs: number | null): MvpEntry | null {
  const winsByPlayer = new Map<string, number>();
  const playedByPlayer = new Map<string, number>();

  for (const m of matches) {
    if (sinceMs !== null && new Date(m.playedAt).getTime() < sinceMs) continue;
    playedByPlayer.set(m.player1, (playedByPlayer.get(m.player1) ?? 0) + 1);
    playedByPlayer.set(m.player2, (playedByPlayer.get(m.player2) ?? 0) + 1);
    winsByPlayer.set(m.winner, (winsByPlayer.get(m.winner) ?? 0) + 1);
  }

  let best: MvpEntry | null = null;
  for (const [playerId, wins] of winsByPlayer) {
    if (!best || wins > best.wins) {
      best = { playerId, wins, matchesPlayed: playedByPlayer.get(playerId) ?? 0 };
    }
  }
  return best;
}

export function playerName(players: Player[], id: string): string {
  return players.find((p) => p.id === id)?.name ?? '?';
}

export function sortedMatchHistory(matches: Match[]): Match[] {
  return [...matches].sort((a, b) => b.playedAt.localeCompare(a.playedAt));
}
