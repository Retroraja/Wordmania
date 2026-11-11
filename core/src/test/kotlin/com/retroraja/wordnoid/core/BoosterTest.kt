package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Every booster, on every level, at the start and after some moves: it works where the game says it works. */
class BoosterTest {
    private val dict = TestDict.dict

    /** The game after [n] random swaps (null if the level ended). */
    private fun played(level: Level, n: Int): Game? {
        val g = Game(level, dict)
        val r = java.util.Random(level.number * 101L + n)
        repeat(n) {
            if (g.status != GameStatus.PLAYING) return null
            val pairs = g.board.swappablePairs()
            if (pairs.isEmpty()) return null
            val (a, b) = pairs[r.nextInt(pairs.size)]
            g.swap(a, b)
        }
        return g.takeIf { it.status == GameStatus.PLAYING }
    }

    private fun use(g: Game, b: Booster, p: Pos?): MoveResult? {
        val other = if (b.aim == Aim.TWO_TILES && p != null) g.board.positions().firstOrNull { it != p && g.board[it].movable } else null
        val letter = if (b.needsLetter && p != null) (if (g.board[p].letter == 'E') 'A' else 'E') else null
        return g.useBooster(b, p, other, letter)
    }

    @Test fun everyBoosterWorksOnItsTeachingLevel() {
        for (b in Booster.entries) {
            val level = Levels.get(b.introLevel)!!
            val g = Game(level, dict)
            if (b == Booster.UNDO) g.swap(g.board.swappablePairs().first().first, g.board.swappablePairs().first().second)
            assertTrue(g.canUseNow(b), "${b.title} can't be used on level ${b.introLevel}, the level that teaches it")
        }
    }

    @Test fun targetsAlwaysWorkAndOtherTilesNeverDo() {
        val tileBoosters = Booster.entries.filter { it.aim != Aim.NONE }
        var checked = 0
        for (level in Levels.all) for (n in listOf(0, 2, 5)) {
            val g = played(level, n) ?: continue
            for (b in tileBoosters) {
                val targets = g.targets(b)
                for (p in g.board.positions()) {
                    val copy = g.copy()
                    val before = copy.board
                    val r = use(copy, b, p)
                    if (p in targets) {
                        assertNotNull(r, "L${level.number} after $n moves: ${b.title} at $p is lit but did nothing")
                        assertTrue(r.changedBoard != before || copy.board != before, "L${level.number}: ${b.title} at $p changed nothing")
                        checked++
                    } else if (b.aim != Aim.TWO_TILES) {
                        assertNull(r, "L${level.number} after $n moves: ${b.title} works at $p but is not lit")
                    }
                }
            }
        }
        assertTrue(checked > 1000)
    }

    @Test fun boostersWithoutATileDoSomething() {
        for (level in Levels.all.filter { it.number % 5 == 0 }) {
            val g = played(level, 2) ?: continue
            fun fresh() = g.copy()
            fresh().let { c -> c.useBooster(Booster.PLUS_MOVES); assertEquals(g.movesLeft + 3, c.movesLeft) }
            fresh().let { c -> c.useBooster(Booster.FREE_MOVE); assertEquals(1, c.freeMoves) }
            fresh().let { c -> c.useBooster(Booster.DOUBLE); assertTrue(c.doubleNext) }
            fresh().let { c -> assertNotNull(c.useBooster(Booster.SHUFFLE)) }
            fresh().let { c -> val b = c.board; assertNotNull(c.useBooster(Booster.VOWEL_FLIP)); assertTrue(c.board != b) }
            fresh().let { c -> val b = c.board; assertNotNull(c.useBooster(Booster.LETTER_RAIN)); assertTrue(c.board != b) }
            fresh().let { c -> assertTrue(c.canUndo); assertNotNull(c.useBooster(Booster.UNDO)); assertTrue(!c.canUndo) }
            assertTrue(g.hintWords(3).isNotEmpty() || g.scan().isEmpty())
        }
    }

    @Test fun axePopsABalloonOnLevel37() {
        val g = Game(Levels.get(37)!!, dict)
        val balloon = g.board.positions().first { g.board[it].isBalloon }
        assertTrue(balloon in g.targets(Booster.AXE))
        val r = assertNotNull(g.useBooster(Booster.AXE, balloon))
        assertEquals(listOf(balloon), r.effects.balloons)
        assertTrue(!g.board[balloon].isBalloon)
    }

    @Test fun rowAndColumnSlideWorkOnLevel38() {
        val g = Game(Levels.get(38)!!, dict)
        val rows = g.targets(Booster.ROW_SLIDE).map { it.row }.toSet()
        val cols = g.targets(Booster.COLUMN_SLIDE).map { it.col }.toSet()
        assertTrue(rows.isNotEmpty() && cols.isNotEmpty())
        val r = assertNotNull(g.copy().useBooster(Booster.ROW_SLIDE, Pos(rows.first(), 0)))
        assertTrue(r.effects.words.isNotEmpty(), "Row Slide makes a word")
        val c = assertNotNull(g.copy().useBooster(Booster.COLUMN_SLIDE, Pos(0, cols.first())))
        assertTrue(c.effects.words.isNotEmpty(), "Column Slide makes a word")
    }
}
