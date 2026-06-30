package com.simplenotes.data

import com.simplenotes.domain.model.Difficulty
import com.simplenotes.domain.model.GameStatus
import com.simplenotes.engine.SimpleNotesEngine
import com.simplenotes.engine.SimpleNotesGenerator
import com.simplenotes.util.ProgressionCalculator
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionCalculatorTest {

    @Test
    fun xpForCompletedGame_isPositive() {
        val level = SimpleNotesGenerator.generate(1L, 1, Difficulty.EASY)
        val game = SimpleNotesEngine.createInitialGame(level).copy(status = GameStatus.COMPLETED)
        assertTrue(ProgressionCalculator.xpForGame(game) > 0)
    }

    @Test
    fun xpForGame_withHints_isLowerThanWithoutHints() {
        val level = SimpleNotesGenerator.generate(1L, 1, Difficulty.EASY)
        val withHints = SimpleNotesEngine.createInitialGame(level).copy(hintsUsed = 2, status = GameStatus.COMPLETED)
        val noHints = SimpleNotesEngine.createInitialGame(level).copy(hintsUsed = 0, status = GameStatus.COMPLETED)
        assertTrue(ProgressionCalculator.xpForGame(noHints) >= ProgressionCalculator.xpForGame(withHints))
    }
}
