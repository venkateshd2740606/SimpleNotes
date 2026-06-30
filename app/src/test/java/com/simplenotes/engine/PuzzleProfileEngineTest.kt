package com.simplenotes.engine

import com.simplenotes.domain.model.Difficulty
import com.simplenotes.domain.model.GameStatus
import com.simplenotes.domain.model.SimpleNotesGame
import com.simplenotes.domain.model.PuzzleProfileMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleProfileEngineTest {

    @Test
    fun buildProfile_afterCompletedGame_updatesMetrics() {
        val level = SimpleNotesGenerator.generate(42L, 1, Difficulty.EASY)
        val completed = SimpleNotesEngine.createInitialGame(level).copy(
            status = GameStatus.COMPLETED,
            moves = 10,
            hintsUsed = 0,
            elapsedSeconds = 30
        )
        val metrics = PuzzleProfileEngine.updateMetrics(PuzzleProfileMetrics(), completed)
        assertEquals(1, metrics.gamesAnalyzed)
        val profile = PuzzleProfileEngine.buildProfile(metrics)
        assertTrue(profile.archetype.name.isNotEmpty())
    }

    @Test
    fun adaptiveGenerationProfile_returnsModifierInRange() {
        val profile = PuzzleProfileEngine.buildProfile(
            PuzzleProfileMetrics(gamesAnalyzed = 5, perfectCompletions = 2)
        )
        val generation = PuzzleProfileEngine.adaptiveGenerationProfile(profile)
        assertTrue(generation.colorCountModifier in -1..2)
    }
}
