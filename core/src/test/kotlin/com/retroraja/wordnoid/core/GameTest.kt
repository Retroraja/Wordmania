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
        assertNotNull(g.useBooster(Booster.SHUFFLE))
        assertNull(g.useBooster(Booster.VOWEL_FLIP, Pos(0, 0), letter = 'B'), "vowel flip needs a vowel")
    }

    @Test fun lockedLetterMakesNoWordUntilUnlocked() {
        val g = Game(level.copy(targets = listOf(Objective.Words(2))), dict)
        // Lock the A, move it next to C and T: no word while locked, the swap still costs a move.
        assertNotNull(g.useBooster(Booster.LOCK, Pos(0, 2)))
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertTrue(r.effects.words.isEmpty())
        assertEquals(9, g.movesLeft)
        assertEquals(listOf(Pos(0, 1)), g.lockedCells)
        // Unlocking is free and checks words through the letter right away.
        val u = g.unlock(Pos(0, 1))
        assertNotNull(u)
        assertEquals("CAT", u.effects.words.first().word)
        assertEquals(9, g.movesLeft)
        assertTrue(g.lockedCells.isEmpty())
    }

    @Test fun everyThemeWordIsInTheWordList() {
        for (t in Themes.all) {
            if (t.isNames) continue
            for (w in t.words) assertTrue(dict.isWord(w.lowercase()), "${t.name}: $w is not in the word list")
            for (w in t.examples) assertTrue(w in t.words, "${t.name}: example $w")
        }
        val body = Themes.byName("Body")!!.words
        for (w in listOf("EAR", "EARS", "NOSE", "NOSES", "TEETH", "FEET")) assertTrue(w in body, w)
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

    @Test fun spinAndSlideOnlyWhenTheyMakeAWord() {
        // Row 1 holds T, A, C in the wrong order: the slide spells CAT. Spin on Q/J tiles can't make a word.
        val l = level.copy(start = listOf("QJZQJ", "TQACJ", "JZQJZ", "QJZQJ"), targets = listOf(Objective.Words(5)))
        val g = Game(l, dict)
        assertNull(g.useBooster(Booster.SPIN, Pos(2, 0)), "no word: booster refunded")
        val r = g.useBooster(Booster.ROW_SLIDE, Pos(1, 0))
        assertNotNull(r)
        assertTrue(r.effects.words.isNotEmpty())
        val g2 = Game(level.copy(start = listOf("QJZQJ", "AQTJZ", "CJQZQ", "QJZQJ"), targets = listOf(Objective.Words(5))), dict)
        val r2 = g2.useBooster(Booster.SPIN, Pos(1, 0))
        assertNotNull(r2)
        assertTrue(r2.effects.words.isNotEmpty())
    }

    @Test fun hammerCracksIceAndSkipsGold() {
        val l = level.copy(marks = listOf("g....", ".i...", ".....", "....."), targets = listOf(Objective.Words(5)))
        val g = Game(l, dict)
        assertNull(g.useBooster(Booster.HAMMER, Pos(0, 0)), "gold can't be smashed")
        val r = g.useBooster(Booster.HAMMER, Pos(1, 1))!!
        assertEquals(1, r.effects.iceCracked.size)
        assertTrue(g.board[Pos(1, 1)].isIce)
        g.useBooster(Booster.HAMMER, Pos(1, 1))
        assertEquals(1, g.stats.ice)
    }

    @Test fun wordNextToBalloonPopsIt() {
        val l = level.copy(marks = listOf(".....", ".b...", ".....", "....."), targets = listOf(Objective.Balloons(1)))
        val g = Game(l, dict)
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertEquals(1, r.effects.balloons.size)
        assertEquals(GameStatus.WON, r.status)
    }

    @Test fun friendEscapesAtTheBottom() {
        // The friend sits on C, A, T's row? No: it sits above a column that clears twice.
        val l = level.copy(
            rows = 2, start = listOf("Q@JZQ", "CTAQJ"), targets = listOf(Objective.Rescue(1)),
        )
        val g = Game(l, dict)
        val r = g.swap(Pos(1, 1), Pos(1, 2))
        assertEquals(1, r.rescued)
        assertEquals(GameStatus.WON, r.status)
    }

    @Test fun treasureIsDugOut() {
        val l = level.copy(treasure = listOf(".tt..", ".....", ".....", "....."), targets = listOf(Objective.Treasure(1)))
        val g = Game(l, dict)
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertEquals(listOf(0), r.chestsFound)
        assertEquals(GameStatus.WON, r.status)
        val g2 = Game(l, dict)
        assertNotNull(g2.useBooster(Booster.TREASURE_FINDER))
        assertEquals(GameStatus.WON, g2.status)
    }

    @Test fun bossAttacksEveryFourSwaps() {
        val l = level.copy(boss = true, number = 10, targets = listOf(Objective.Words(9)), moves = 20)
        val g = Game(l, dict)
        var attack: BossAttack? = null
        val pairs = listOf(Pos(3, 0) to Pos(3, 1), Pos(3, 0) to Pos(3, 1), Pos(3, 0) to Pos(3, 1), Pos(3, 0) to Pos(3, 1))
        for ((a, b) in pairs) attack = g.swap(a, b).bossAttack ?: attack
        assertNotNull(attack)
    }

    @Test fun wordsGiveNoCoinsButLongWordsGiveMoves() {
        val g = Game(level.copy(targets = listOf(Objective.Words(5))), dict)
        val r = g.swap(Pos(0, 1), Pos(0, 2))
        assertEquals(0, r.effects.coins)
        assertEquals(0, r.effects.bonusMoves)
        assertEquals(1, level.movesForWord(5)); assertEquals(2, level.movesForWord(6)); assertEquals(0, level.movesForWord(4))
    }
}
