package com.sm.smtts.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sm.smtts.data.ApiException
import com.sm.smtts.data.LiveMatchState
import com.sm.smtts.data.Match
import com.sm.smtts.data.MatchEngine
import com.sm.smtts.data.MatchRepository
import com.sm.smtts.data.NetworkException
import com.sm.smtts.data.PendingQueue
import com.sm.smtts.data.Player
import com.sm.smtts.data.Preset
import com.sm.smtts.data.Rules
import com.sm.smtts.data.RoomData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class RoomViewModel(
    private val token: String,
    private val roomId: String,
    private val matchRepository: MatchRepository,
    private val pendingQueue: PendingQueue,
) : ViewModel() {

    private val _data = MutableStateFlow<RoomData?>(null)
    val data: StateFlow<RoomData?> = _data.asStateFlow()

    private val _liveMatch = MutableStateFlow<LiveMatchState?>(null)
    val liveMatch: StateFlow<LiveMatchState?> = _liveMatch.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val undoHistory = mutableListOf<Pair<LiveMatchState, Boolean>>()
    private var swapped = false

    init {
        load()
    }

    fun load() {
        _loading.value = true
        viewModelScope.launch {
            try {
                _data.value = matchRepository.getAll(token, roomId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
            refreshPendingCount()
            syncPending()
        }
    }

    fun refresh(): Job {
        return viewModelScope.launch {
            try {
                _data.value = matchRepository.getAll(token, roomId)
            } catch (_: Exception) {
                // keep showing stale data
            }
        }
    }

    fun addPlayer(name: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val player = matchRepository.addPlayer(token, roomId, name)
                mergePlayer(player)
            } catch (e: NetworkException) {
                onError("Немає інтернету.")
            } catch (e: Exception) {
                onError("Не вдалося додати гравця (можливо, таке ім'я вже є).")
            }
        }
    }

    fun deletePlayer(id: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                matchRepository.deletePlayer(token, roomId, id)
                _data.value = _data.value?.copy(players = _data.value!!.players.filter { it.id != id })
            } catch (e: NetworkException) {
                onError("Немає інтернету.")
            } catch (e: Exception) {
                onError("Не вдалося видалити гравця.")
            }
        }
    }

    fun savePreset(name: String, rules: Rules, onSaved: (Preset) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val preset = matchRepository.savePreset(token, roomId, null, name, rules)
                mergePreset(preset)
                onSaved(preset)
            } catch (e: NetworkException) {
                onError("Немає інтернету.")
            } catch (e: Exception) {
                onError("Не вдалося зберегти пресет.")
            }
        }
    }

    private fun mergePlayer(player: Player) {
        val current = _data.value ?: return
        val idx = current.players.indexOfFirst { it.id == player.id }
        val updated = if (idx >= 0) current.players.toMutableList().also { it[idx] = player } else current.players + player
        _data.value = current.copy(players = updated)
    }

    private fun mergePreset(preset: Preset) {
        val current = _data.value ?: return
        val idx = current.presets.indexOfFirst { it.id == preset.id }
        val updated = if (idx >= 0) current.presets.toMutableList().also { it[idx] = preset } else current.presets + preset
        _data.value = current.copy(presets = updated)
    }

    fun startMatch(player1: String, player2: String, rules: Rules, presetName: String, firstServer: Int) {
        undoHistory.clear()
        swapped = false
        _liveMatch.value = MatchEngine.createLiveMatch(player1, player2, rules, presetName, firstServer)
    }

    fun resumeMatch() {
        // liveMatch already holds state; scoreboard just needs to be shown again.
    }

    fun isSideSwapped(): Boolean = swapped

    fun canUndo(): Boolean = undoHistory.isNotEmpty()

    fun addPoint(scorer: Int, onFinished: (Match) -> Unit) {
        val prev = _liveMatch.value ?: return
        undoHistory.add(prev to swapped)
        val next = MatchEngine.addPoint(prev, scorer)
        if (MatchEngine.sideSwitchDue(next)) swapped = !swapped
        _liveMatch.value = next

        if (MatchEngine.isMatchOver(next)) {
            viewModelScope.launch { onFinished(finishMatch(next)) }
        }
    }

    fun undo() {
        val prev = undoHistory.removeLastOrNull() ?: return
        _liveMatch.value = prev.first
        swapped = prev.second
    }

    /** Clears the live match — either the user abandoned it, or already saw the result screen. */
    fun clearLiveMatch() {
        _liveMatch.value = null
    }

    private suspend fun finishMatch(state: LiveMatchState): Match {
        val winnerIdx = MatchEngine.matchWinner(state.finishedSets, state.rules) ?: 0
        val ids = listOf(state.player1, state.player2)
        val match = Match(
            id = UUID.randomUUID().toString(),
            playedAt = state.startedAt,
            player1 = state.player1,
            player2 = state.player2,
            sets = state.finishedSets,
            winner = ids[winnerIdx],
            rules = state.rules,
            presetName = state.presetName,
        )
        pendingQueue.add(roomId, match)
        val current = _data.value
        if (current != null) _data.value = current.copy(matches = current.matches + match)
        refreshPendingCount()
        syncPending()
        return match
    }

    private suspend fun refreshPendingCount() {
        _pendingCount.value = pendingQueue.getAll(roomId).size
    }

    private var syncing = false

    fun syncPending() {
        if (syncing) return
        syncing = true
        viewModelScope.launch {
            try {
                val pending = pendingQueue.getAll(roomId)
                for (match in pending) {
                    try {
                        matchRepository.addMatch(token, roomId, match)
                        pendingQueue.remove(roomId, match.id)
                    } catch (e: NetworkException) {
                        break // offline or unreachable - stop, try again later
                    } catch (e: ApiException) {
                        // server rejected the match permanently (bad data) - drop it so the queue doesn't jam
                        pendingQueue.remove(roomId, match.id)
                    }
                }
            } finally {
                syncing = false
                refreshPendingCount()
            }
        }
    }

    companion object {
        fun factory(
            token: String,
            roomId: String,
            matchRepository: MatchRepository,
            pendingQueue: PendingQueue,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RoomViewModel(token, roomId, matchRepository, pendingQueue) as T
            }
        }
    }
}
