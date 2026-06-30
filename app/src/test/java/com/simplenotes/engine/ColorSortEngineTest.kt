package com.simplenotes.engine

import com.simplenotes.domain.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleNotesEngineTest {

    @Test
    fun tutorialLevel_isValidAndSolvable() {
        val level = TutorialLevels.getTutorialLevel(0)!!
        assertTrue(SimpleNotesEngine.validateLevel(level))
    }

    @Test
    fun pour_updatesTubeState() {
        val level = TutorialLevels.getTutorialLevel(0)!!
        var game = SimpleNotesEngine.createInitialGame(level)
        assertTrue(SimpleNotesEngine.canPour(game, 0, 2))
        game = SimpleNotesEngine.pour(game, 0, 2)
        assertEquals(1, game.moves)
        assertTrue(game.tubes[2].isNotEmpty())
    }

    @Test
    fun solveTutorial_completesGame() {
        val level = TutorialLevels.getTutorialLevel(0)!!
        var game = SimpleNotesEngine.createInitialGame(level)
        val solution = SimpleNotesEngine.solve(game)!!
        solution.forEach { (from, to) ->
            game = SimpleNotesEngine.pour(game, from, to)
        }
        assertTrue(SimpleNotesEngine.isWon(game))
    }

    @Test
    fun generatedLevel_isValid() {
        val level = SimpleNotesGenerator.generate(12345L, 1, Difficulty.EASY)
        assertTrue(SimpleNotesEngine.validateLevel(level))
    }

    @Test
    fun tubeSelection_poursWhenSecondTubeSelected() {
        val level = TutorialLevels.getTutorialLevel(0)!!
        var game = SimpleNotesEngine.createInitialGame(level)
        game = SimpleNotesEngine.onTubeSelected(game, 0)
        assertEquals(0, game.selectedTubeId)
        game = SimpleNotesEngine.onTubeSelected(game, 2)
        assertEquals(1, game.moves)
    }

    @Test
    fun generator_sameSeed_producesSameLevel() {
        val a = SimpleNotesGenerator.generate(999L, 5, Difficulty.MEDIUM)
        val b = SimpleNotesGenerator.generate(999L, 5, Difficulty.MEDIUM)
        assertEquals(a.initialTubes, b.initialTubes)
    }
}
