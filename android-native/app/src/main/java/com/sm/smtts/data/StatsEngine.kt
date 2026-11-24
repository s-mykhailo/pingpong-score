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

    /** 0 if playerId is on the player1/partner1 side, 1 if on the player2/partner2 side, null if not in this match. */
    fun playerSide(m: Match, playerId: String): Int? = when {
        playerId == m.player1 || playerId == m.partner1 -> 0
        playerId == m.player2 || playerId == m.partner2 -> 1
        else -> null
    }

    fun computePlayerStats(matches: List<Match>, playerId: String): PlayerStats {
        val own = matches.filter { playerSide(it, playerId) != null }
            .sortedBy { it.playedAt }

        var wins = 0
        var losses = 0
        var setsWon = 0
        var setsLost = 0
        val results = mutableListOf<Boolean>()

        for (m in own) {
            val isP1 = playerSide(m, playerId) == 0
            val won = m.winner == (if (isP1) m.player1 else m.player2)
            if (won) wins++ else losses++
            results.add(won)

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
            val winningSideIds = if (m.winner == m.player1) listOfNotNull(m.player1, m.partner1) else listOfNotNull(m.player2, m.partner2)
            for (id in listOfNotNull(m.player1, m.partner1, m.player2, m.partner2)) {
                playedByPlayer[id] = (playedByPlayer[id] ?: 0) + 1
            }
            for (id in winningSideIds) {
                winsByPlayer[id] = (winsByPlayer[id] ?: 0) + 1
            }
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
