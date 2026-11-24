package com.sm.smtts.data

import java.time.Instant
import kotlin.math.ceil

data class Rules(
    val pointsToWin: Int = 11,
    val winBy: Int = 2, // 1 or 2
    val bestOf: Int = 3, // odd
    val serveEvery: Int = 2,
    val deuceServeEvery: Int = 1,
)

data class SetScore(val a: Int, val b: Int) {
    fun forIdx(idx: Int) = if (idx == 0) a else b
}

data class LiveMatchState(
    val player1: String,
    val player2: String,
    val rules: Rules,
    val presetName: String,
    val finishedSets: List<SetScore> = emptyList(),
    val currentSet: SetScore = SetScore(0, 0),
    // Singles: 0/1 (which side serves). Doubles: 0..3, a slot in rotationOrder()
    // ([player1, player2, partner1, partner2] = A1,B1,A2,B2, the ITTF doubles serve order).
    // side-for-slot is always `server % 2` - singles collapses to the old 0/1 meaning.
    val server: Int,
    val pointsSinceSwitch: Int = 0,
    val firstServerOfMatch: Int,
    val startedAt: String,
    val mode: String = MODE_SINGLES,
    val partner1: String? = null,
    val partner2: String? = null,
)

/** Direct port of frontend/src/match/rules.ts — keep the two in sync. */
object MatchEngine {

    fun isDeuceZone(a: Int, b: Int, rules: Rules): Boolean =
        rules.winBy == 2 && a >= rules.pointsToWin - 1 && b >= rules.pointsToWin - 1

    fun setWinner(a: Int, b: Int, rules: Rules): Int? {
        val hi = maxOf(a, b)
        val lo = minOf(a, b)
        if (hi < rules.pointsToWin) return null
        if (rules.winBy == 1) {
            if (hi != rules.pointsToWin) return null
            return if (a > b) 0 else 1
        }
        val won = if (hi == rules.pointsToWin) lo <= rules.pointsToWin - 2 else hi - lo == 2
        if (!won) return null
        return if (a > b) 0 else 1
    }

    fun neededSets(rules: Rules): Int = ceil(rules.bestOf / 2.0).toInt()

    fun matchWinner(finishedSets: List<SetScore>, rules: Rules): Int? {
        val need = neededSets(rules)
        var w0 = 0
        var w1 = 0
        for (s in finishedSets) {
            if (s.a > s.b) w0++ else w1++
        }
        if (w0 >= need) return 0
        if (w1 >= need) return 1
        return null
    }

    fun createLiveMatch(
        player1: String,
        player2: String,
        rules: Rules,
        presetName: String,
        firstServer: Int,
        mode: String = MODE_SINGLES,
        partner1: String? = null,
        partner2: String? = null,
    ) = LiveMatchState(
        player1 = player1,
        player2 = player2,
        rules = rules,
        presetName = presetName,
        server = firstServer,
        firstServerOfMatch = firstServer,
        startedAt = Instant.now().toString(),
        mode = mode,
        partner1 = partner1,
        partner2 = partner2,
    )

    /** [A1, B1, A2, B2] - the fixed ITTF doubles serve/receive rotation order. Singles: [A, B]. */
    fun rotationOrder(state: LiveMatchState): List<String> =
        if (state.mode == MODE_DOUBLES) listOf(state.player1, state.player2, state.partner1!!, state.partner2!!)
        else listOf(state.player1, state.player2)

    private fun slotCount(state: LiveMatchState) = if (state.mode == MODE_DOUBLES) 4 else 2

    fun addPoint(state: LiveMatchState, scorer: Int): LiveMatchState {
        val currentSet = if (scorer == 0) {
            SetScore(state.currentSet.a + 1, state.currentSet.b)
        } else {
            SetScore(state.currentSet.a, state.currentSet.b + 1)
        }

        val deuce = isDeuceZone(currentSet.a, currentSet.b, state.rules)
        val every = if (deuce) state.rules.deuceServeEvery else state.rules.serveEvery
        val pointsSinceSwitch = state.pointsSinceSwitch + 1
        val shouldSwitch = pointsSinceSwitch >= every
        val server = if (shouldSwitch) (state.server + 1) % slotCount(state) else state.server

        val winner = setWinner(currentSet.a, currentSet.b, state.rules)
        if (winner != null) {
            val finishedSets = state.finishedSets + currentSet
            val setIndex = finishedSets.size
            val nextFirstServer = if (setIndex % 2 == 0) state.firstServerOfMatch else flip(state.firstServerOfMatch)
            return state.copy(
                finishedSets = finishedSets,
                currentSet = SetScore(0, 0),
                server = nextFirstServer,
                pointsSinceSwitch = 0,
            )
        }

        return state.copy(
            currentSet = currentSet,
            server = server,
            pointsSinceSwitch = if (shouldSwitch) 0 else pointsSinceSwitch,
        )
    }

    // x xor 1 flips 0<->1 and 2<->3 - "same rotation position, other team" - correct for both
    // singles (binary sides) and doubles (4-slot rotation) with no mode branch needed.
    private fun flip(x: Int) = x xor 1

    fun isMatchOver(state: LiveMatchState): Boolean = matchWinner(state.finishedSets, state.rules) != null

    /** Classic rule: switch ends in the deciding set once either player reaches half the points to win. */
    fun sideSwitchDue(state: LiveMatchState): Boolean {
        val need = neededSets(state.rules)
        val isDecidingSet = state.finishedSets.size == need * 2 - 2
        if (!isDecidingSet) return false
        val threshold = ceil(state.rules.pointsToWin / 2.0).toInt()
        return state.currentSet.a == threshold || state.currentSet.b == threshold
    }
}
