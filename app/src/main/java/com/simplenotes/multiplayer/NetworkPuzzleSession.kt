package com.simplenotes.multiplayer

import com.simplenotes.domain.model.SimpleNotesGame
import com.simplenotes.domain.model.Difficulty
import com.simplenotes.domain.model.MultiplayerMode
import com.simplenotes.domain.model.MultiplayerSession
import com.simplenotes.domain.model.P2PRole
import com.simplenotes.engine.SimpleNotesEngine
import com.simplenotes.engine.SimpleNotesGenerator
import com.simplenotes.network.P2PMessage
import com.simplenotes.network.P2PSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkPuzzleSession @Inject constructor(
    private val p2pSessionManager: P2PSessionManager
) {
    private val _session = MutableStateFlow<MultiplayerSession?>(null)
    val session: StateFlow<MultiplayerSession?> = _session.asStateFlow()

    private var sharedGame: SimpleNotesGame? = null
    private var localName = "Player 1"
    private var remoteName = "Player 2"
    private var isMyTurn = false

    val isLocalTurn: Boolean get() = isMyTurn

    suspend fun startAsHost(localPlayer: String, remotePlayer: String, difficulty: Difficulty) {
        localName = localPlayer
        remoteName = remotePlayer
        val seed = System.currentTimeMillis()
        val level = SimpleNotesGenerator.generate(seed, 1, difficulty)
        val game = SimpleNotesEngine.createInitialGame(level)
        sharedGame = game
        isMyTurn = true
        publishSession(difficulty, seed, isActive = true)
        p2pSessionManager.send(
            P2PMessage.gameStart(
                levelSeed = seed,
                levelNumber = 1,
                hostName = localName,
                difficulty = difficulty.name
            )
        )
    }

    fun getGame(): SimpleNotesGame? = sharedGame

    suspend fun applyLocalTubeClick(tubeId: Int): SimpleNotesGame? {
        if (!isMyTurn) return null
        val game = sharedGame ?: return null
        val previousMoves = game.moves
        val updated = SimpleNotesEngine.onTubeSelected(game, tubeId)
        if (updated == game) return updated
        sharedGame = updated
        if (updated.moves > previousMoves) {
            isMyTurn = false
            publishSession(
                difficulty = updated.level.difficulty,
                seed = updated.level.seed,
                isActive = true
            )
        }
        p2pSessionManager.send(P2PMessage.move("tube:$tubeId"))
        return updated
    }

    suspend fun onRemoteMessage(message: P2PMessage): SimpleNotesGame? {
        return when (message.type) {
            P2PMessage.TYPE_GAME_START -> {
                val seed = message.levelSeed ?: return null
                val levelNumber = message.levelNumber ?: 1
                val difficulty = message.difficulty?.let {
                    runCatching { Difficulty.valueOf(it) }.getOrNull()
                } ?: Difficulty.MEDIUM
                val level = SimpleNotesGenerator.generate(seed, levelNumber, difficulty)
                val game = SimpleNotesEngine.createInitialGame(level)
                sharedGame = game
                isMyTurn = false
                remoteName = message.playerName ?: remoteName
                publishSession(difficulty, seed, isActive = true)
                game
            }
            P2PMessage.TYPE_MOVE -> {
                if (isMyTurn) return sharedGame
                val tubeId = message.movePayload?.removePrefix("tube:")?.toIntOrNull() ?: return null
                val game = sharedGame ?: return null
                val previousMoves = game.moves
                val updated = SimpleNotesEngine.onTubeSelected(game, tubeId)
                sharedGame = updated
                if (updated.moves > previousMoves) {
                    isMyTurn = true
                    publishSession(updated.level.difficulty, updated.level.seed, isActive = true)
                }
                updated
            }
            P2PMessage.TYPE_RESIGN -> {
                isMyTurn = false
                sharedGame
            }
            else -> sharedGame
        }
    }

    suspend fun resign() {
        p2pSessionManager.send(P2PMessage.resign())
    }

    fun onRoundWon(localWon: Boolean) {
        val session = _session.value ?: return
        val newLocal = session.localScore + if (localWon) 1 else 0
        val newRemote = session.remoteScore + if (localWon) 0 else 1
        val newLevel = SimpleNotesGenerator.generate(
            session.seed + newLocal + newRemote,
            newLocal + newRemote + 1,
            session.difficulty
        )
        val newGame = SimpleNotesEngine.createInitialGame(newLevel)
        sharedGame = newGame
        isMyTurn = if (localWon) {
            p2pSessionManager.role.value != P2PRole.HOST
        } else {
            p2pSessionManager.role.value == P2PRole.HOST
        }
        _session.value = session.copy(
            localScore = newLocal,
            remoteScore = newRemote,
            activePlayerName = if (isMyTurn) localName else remoteName
        )
    }

    fun end() {
        _session.value = null
        sharedGame = null
        isMyTurn = false
    }

    private fun publishSession(difficulty: Difficulty, seed: Long, isActive: Boolean) {
        _session.value = MultiplayerSession(
            mode = MultiplayerMode.LOCAL_P2P,
            localPlayerName = localName,
            remotePlayerName = remoteName,
            activePlayerName = if (isMyTurn) localName else remoteName,
            localScore = _session.value?.localScore ?: 0,
            remoteScore = _session.value?.remoteScore ?: 0,
            isActive = isActive,
            seed = seed,
            difficulty = difficulty
        )
    }
}
