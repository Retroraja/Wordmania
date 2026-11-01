package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameTest {
    private val dict = TestDict.dict

    private val level = Level(
        number = 99, cols = 5, rows = 4, moves = 10, goal = Goal.Words(1),
        start = listOf("CTAQJ", "QJQZQ", "JZQJZ", "QJZQJ"), seed = 42,
    )

    @Test fun swapMakesWordAndClears() {
        val g = Game(level, dict)
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertEquals("CAT", r.steps.first().words.first().word)
        assertEquals(GameStatus.WON, r.status)
        assertTrue(r.leftoverBonus > 0)
    }

    @Test fun swapWithoutWordStillCostsAMove() {
        val g = Game(level.copy(goal = Goal.Words(5)), dict)
        g.swap(Pos(3, 0), Pos(3, 1))
        assertEquals(9, g.movesLeft)
        assertEquals(GameStatus.PLAYING, g.status)
    }

    @Test fun refillNeverMakesWordsOnItsOwnForNewTiles() {
        val g = Game(level.copy(goal = Goal.Words(50), moves = 50), dict)
        val rng = Rng(7)
        repeat(40) {
            if (g.status != GameStatus.PLAYING) return
            val (a, b) = Solver.chooseSwap(g, dict, 1.0, rng)
            g.swap(a, b)
            assertTrue(WordFinder.findAll(g.board, dict).isEmpty(), "Board should be settled after a move: ${g.board}")
        }
    }

    @Test fun sameMovesGiveSameBoard() {
        fun run(): String {
            val g = Game(level.copy(goal = Goal.Words(50), moves = 30), dict)
            val rng = Rng(3)
            repeat(15) { if (g.status == GameStatus.PLAYING) { val (a, b) = Solver.chooseSwap(g, dict, 1.0, rng); g.swap(a, b) } }
            return g.board.toString()
        }
        assertEquals(run(), run())
    }
}
