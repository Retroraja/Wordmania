package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameTest {
    private val dict = TestDict.dict

    private val level = Level(
        number = 99, cols = 5, rows = 4, moves = 10, targets = listOf(Objective.Words(1)),
        start = listOf("CTAQJ", "QJQZQ", "JZQJZ", "QJZQJ"), seed = 42,
    )

    @Test fun swapMakesWordAndClears() {
        val g = Game(level, dict)
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertEquals("CAT", r.effects.words.first().word)
        assertEquals(GameStatus.WON, r.status)
        assertTrue(r.leftoverBonus > 0)
    }

    @Test fun swapWithoutWordStillCostsAMove() {
        val g = Game(level.copy(targets = listOf(Objective.Words(5))), dict)
        g.swap(Pos(3, 0), Pos(3, 1))
        assertEquals(9, g.movesLeft)
        assertEquals(GameStatus.PLAYING, g.status)
    }

    @Test fun wordsAwayFromTheMoveDontCount() {
        // DOG sits on the board already; moving tiles elsewhere doesn't score it.
        val l = level.copy(start = listOf("DOGQJ", "QJQZQ", "JZQJZ", "QJZQJ"), targets = listOf(Objective.Words(5)))
        val g = Game(l, dict)
        val r = g.swap(Pos(3, 3), Pos(3, 4))
        assertTrue(r.effects.words.isEmpty())
    }

    @Test fun crateNextToWordBreaksAndGivesPrize() {
        val l = level.copy(
            start = listOf("CTAQJ", "Q#QZQ", "JZQJZ", "QJZQJ"),
            targets = listOf(Objective.Crates(1)),
            cratePrizes = listOf(Prize(PrizeKind.COINS, 25)),
        )
        val g = Game(l, dict)
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertTrue(r.effects.crateHits.single().broken)
        assertTrue(r.effects.coins >= 25)
        assertEquals(GameStatus.WON, r.status)
    }

    @Test fun goldIceAndChains() {
        val l = level.copy(
            start = listOf("CTAQJ", "QJQZQ", "JZQJZ", "QJZQJ"),
            marks = listOf("g....", ".c...", ".....", "....."),
            targets = listOf(Objective.Gold(1), Objective.Chains(1)),
        )
        val g = Game(l, dict)
        assertTrue(!g.canSwap(Pos(1, 1), Pos(1, 2)), "chained tile can't move")
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertEquals(1, r.effects.gold.size)
        assertEquals(1, r.effects.chainsBroken.size)
        assertEquals(GameStatus.WON, r.status)
    }

    @Test fun boostersWork() {
        val g = Game(level.copy(targets = listOf(Objective.Words(3)), moves = 20), dict)
        assertNotNull(g.useBooster(Booster.HAMMER, Pos(1, 1)))
        val before = g.board.toString()
        assertNotNull(g.useBooster(Booster.MAGIC_SWAP, Pos(0, 0), Pos(3, 4)))
        assertNotNull(g.useBooster(Booster.UNDO))
        assertEquals(before, g.board.toString())
        assertNull(g.useBooster(Booster.UNDO), "only one undo")
        val moves = g.movesLeft
        g.useBooster(Booster.PLUS_MOVES)
        assertEquals(moves + 3, g.movesLeft)
        val paint = g.useBooster(Booster.PAINT, Pos(3, 0), letter = 'A')
        assertNotNull(paint)
        assertNotNull(g.useBooster(Booster.SPIN, Pos(1, 1)))
        assertNotNull(g.useBooster(Booster.ROW_SLIDE, Pos(2, 0), dir = 1))
        assertNotNull(g.useBooster(Booster.SHUFFLE))
        assertNull(g.useBooster(Booster.VOWEL_FLIP, Pos(0, 0), letter = 'B'), "vowel flip needs a vowel")
    }

    @Test fun jokerMakesWord() {
        val l = level.copy(start = listOf("CQTQJ", "QJQZQ", "JZQJZ", "QJZQJ"))
        val g = Game(l, dict)
        val r = g.useBooster(Booster.JOKER, Pos(0, 1))!!
        assertEquals(3, r.effects.words.single().length)
        assertEquals(GameStatus.WON, r.status)
    }

    @Test fun sameMovesGiveSameBoard() {
        fun run(): String {
            val g = Game(level.copy(targets = listOf(Objective.Words(50)), moves = 30), dict)
            val rng = Rng(3)
            repeat(15) { if (g.status == GameStatus.PLAYING) { val (a, b) = Solver.chooseSwap(g, dict, 0.8, rng); g.swap(a, b) } }
            return g.board.toString()
        }
        assertEquals(run(), run())
    }
}
