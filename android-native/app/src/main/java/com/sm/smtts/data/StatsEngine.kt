package com.sm.smtts.data

data class PlayerStats(
    val playerId: String,
    val wins: Int,
    val losses: Int,
    val matchesPlayed: Int,
    val winRate: Double,
    val setsWon: Int,
    val setsLost: Int,
    val currentStreak: Int, // positive = win streak, negative = loss streak
)

/** Direct port of frontend/src/stats/compute.ts — keep the two in sync. */
object StatsEngine {

    fun computePlayerStats(matches: List<Match>, playerId: String): PlayerStats {
        val own = matches.filter { it.player1 == playerId || it.player2 == playerId }
            .sortedBy { it.playedAt }

        var wins = 0
        var losses = 0
        var setsWon = 0
        var setsLost = 0
        val results = mutableListOf<Boolean>()

        for (m in own) {
            val won = m.winner == playerId
            if (won) wins++ else losses++
            results.add(won)

            val isP1 = m.player1 == playerId
            for (s in m.sets) {
                val mine = if (isP1) s.a else s.b
                val theirs = if (isP1) s.b else s.a
                if (mine > theirs) setsWon++ else setsLost++
            }
        }

        var currentStreak = 0
        for (i in results.indices.reversed()) {
            if (i == results.size - 1) {
                currentStreak = if (results[i]) 1 else -1
                continue
            }
            val sameAsLast = results[i] == results[results.size - 1]
            if (!sameAsLast) break
            currentStreak += if (results[i]) 1 else -1
        }

        val matchesPlayed = own.size
        return PlayerStats(
            playerId = playerId,
            wins = wins,
            losses = losses,
            matchesPlayed = matchesPlayed,
            winRate = if (matchesPlayed > 0) wins.toDouble() / matchesPlayed else 0.0,
            setsWon = setsWon,
            setsLost = setsLost,
            currentStreak = currentStreak,
        )
    }

    fun playerName(players: List<Player>, id: String): String = players.find { it.id == id }?.name ?: "?"

    fun sortedMatchHistory(matches: List<Match>): List<Match> = matches.sortedByDescending { it.playedAt }

    fun computeMvp(matches: List<Match>, sinceMs: Long?): MvpEntry? {
        val winsByPlayer = mutableMapOf<String, Int>()
        val playedByPlayer = mutableMapOf<String, Int>()

        for (m in matches) {
            if (sinceMs != null) {
                val playedAtMs = try {
                    java.time.Instant.parse(m.playedAt).toEpochMilli()
                } catch (e: Exception) {
                    null
                }
                if (playedAtMs == null || playedAtMs < sinceMs) continue
            }
            playedByPlayer[m.player1] = (playedByPlayer[m.player1] ?: 0) + 1
            playedByPlayer[m.player2] = (playedByPlayer[m.player2] ?: 0) + 1
            winsByPlayer[m.winner] = (winsByPlayer[m.winner] ?: 0) + 1
        }

        var best: MvpEntry? = null
        for ((playerId, wins) in winsByPlayer) {
            if (best == null || wins > best.wins) {
                best = MvpEntry(playerId = playerId, wins = wins, matchesPlayed = playedByPlayer[playerId] ?: 0)
            }
        }
        return best
    }
}

data class MvpEntry(
    val playerId: String,
    val wins: Int,
    val matchesPlayed: Int,
)
