package com.simplenotes.engine

import androidx.compose.ui.graphics.Color
import com.simplenotes.domain.model.SimpleNotesGame
import com.simplenotes.domain.model.SimpleNotesLevel
import com.simplenotes.domain.model.GameStatus

object SimpleNotesPalette {
    val liquidColors: List<Color> = listOf(
        Color(0xFFEF5350),
        Color(0xFF42A5F5),
        Color(0xFF66BB6A),
        Color(0xFFFFA726),
        Color(0xFFAB47BC),
        Color(0xFF26A69A),
        Color(0xFF5C6BC0),
        Color(0xFFEC407A),
        Color(0xFF8D6E63),
        Color(0xFF78909C)
    )

    fun colorForIndex(index: Int): Color = liquidColors[index % liquidColors.size]
}

object SimpleNotesEngine {

    fun createInitialGame(level: SimpleNotesLevel): SimpleNotesGame = SimpleNotesGame(
        level = level,
        tubes = level.initialTubes.map { it.toList() }
    )

    fun topColor(tube: List<Int>): Int? = tube.lastOrNull()

    fun pourCount(game: SimpleNotesGame, from: Int, to: Int): Int {
        val fromTube = game.tubes.getOrNull(from) ?: return 0
        val toTube = game.tubes.getOrNull(to) ?: return 0
        if (fromTube.isEmpty() || from == to) return 0
        if (toTube.size >= game.level.tubeCapacity) return 0
        val top = fromTube.last()
        if (toTube.isNotEmpty() && toTube.last() != top) return 0
        var count = 0
        var idx = fromTube.lastIndex
        while (idx >= 0 &&
            fromTube[idx] == top &&
            toTube.size + count < game.level.tubeCapacity
        ) {
            count++
            idx--
        }
        return count
    }

    fun canPour(game: SimpleNotesGame, from: Int, to: Int): Boolean = pourCount(game, from, to) > 0

    fun pour(game: SimpleNotesGame, from: Int, to: Int): SimpleNotesGame {
        val count = pourCount(game, from, to)
        if (count <= 0) return game

        val tubes = game.tubes.map { it.toMutableList() }.toMutableList()
        repeat(count) {
            val ball = tubes[from].removeAt(tubes[from].lastIndex)
            tubes[to].add(ball)
        }
        val won = isWon(tubes, game.level)
        return game.copy(
            tubes = tubes.map { it.toList() },
            selectedTubeId = null,
            moves = game.moves + 1,
            status = if (won) GameStatus.COMPLETED else game.status,
            completedAt = if (won) System.currentTimeMillis() else game.completedAt,
            lastPlayedAt = System.currentTimeMillis()
        )
    }

    fun onTubeSelected(game: SimpleNotesGame, tubeId: Int): SimpleNotesGame {
        if (tubeId !in game.tubes.indices) return game
        val selected = game.selectedTubeId
        if (selected == null) {
            if (game.tubes[tubeId].isEmpty()) return game
            return game.copy(selectedTubeId = tubeId, lastPlayedAt = System.currentTimeMillis())
        }
        if (selected == tubeId) {
            return game.copy(selectedTubeId = null, lastPlayedAt = System.currentTimeMillis())
        }
        if (canPour(game, selected, tubeId)) {
            return pour(game, selected, tubeId)
        }
        return if (game.tubes[tubeId].isEmpty()) {
            game.copy(selectedTubeId = null, lastPlayedAt = System.currentTimeMillis())
        } else {
            game.copy(selectedTubeId = tubeId, lastPlayedAt = System.currentTimeMillis())
        }
    }

    fun isWon(tubes: List<List<Int>>, level: SimpleNotesLevel): Boolean =
        tubes.all { tube -> tube.isEmpty() || tube.distinct().size == 1 }

    fun isWon(game: SimpleNotesGame): Boolean = isWon(game.tubes, game.level)

    fun getHintMove(game: SimpleNotesGame): Pair<Int, Int>? {
        if (isWon(game)) return null
        for (from in game.tubes.indices) {
            if (game.tubes[from].isEmpty()) continue
            for (to in game.tubes.indices) {
                if (from == to) continue
                if (canPour(game, from, to)) return from to to
            }
        }
        return null
    }

    fun applyHint(game: SimpleNotesGame): SimpleNotesGame {
        val hint = getHintMove(game) ?: return game
        return pour(game, hint.first, hint.second).copy(hintsUsed = game.hintsUsed + 1)
    }

    fun validateLevel(level: SimpleNotesLevel): Boolean {
        if (level.initialTubes.isEmpty() || level.colorCount <= 0) return false
        val game = createInitialGame(level)
        if (isWon(game)) return false
        return solve(game) != null
    }

    fun solve(game: SimpleNotesGame): List<Pair<Int, Int>>? {
        if (isWon(game)) return emptyList()

        data class Node(val state: SimpleNotesGame, val path: List<Pair<Int, Int>>)

        val visited = mutableSetOf(tubeStateKey(game.tubes))
        val queue = ArrayDeque<Node>()
        queue.add(Node(game, emptyList()))

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            if (isWon(current)) return path

            for (from in current.tubes.indices) {
                for (to in current.tubes.indices) {
                    if (from == to || !canPour(current, from, to)) continue
                    val next = pour(current, from, to)
                    val key = tubeStateKey(next.tubes)
                    if (key in visited) continue
                    visited.add(key)
                    queue.add(Node(next, path + (from to to)))
                }
            }
            if (visited.size > 8000) return null
        }
        return null
    }

    private fun tubeStateKey(tubes: List<List<Int>>): String =
        tubes.joinToString("|") { it.joinToString(",") }

    fun optimalMoveCount(game: SimpleNotesGame): Int =
        solve(createInitialGame(game.level))?.size ?: game.moves.coerceAtLeast(1)
}
