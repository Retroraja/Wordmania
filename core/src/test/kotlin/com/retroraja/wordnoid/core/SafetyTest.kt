package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Update 1.1 safety checks (tester feedback "the game plays by itself"):
 * words only come from the tiles the player moved, the board never shuffles by itself,
 * and refills almost always leave at least a few word swaps to find.
 */
class SafetyTest {
    private val dict = TestDict.dict

    @Test fun nothingHappensWithoutThePlayer() {
        var moves = 0
        var rich = 0
        for (level in Levels.all.filter { it.number >= 6 }) {
            val game = Game(level, dict)
            val rng = Rng(level.number.toLong())
            repeat(12) {
                if (game.status != GameStatus.PLAYING) return@repeat
                if (game.stuck) { game.useBooster(Booster.SHUFFLE); return@repeat }
                val (a, b) = Solver.chooseSwap(game, dict, 0.6, rng)
                val r = game.swap(a, b)
                assertNull(r.shuffled, "Level ${level.number}: the board shuffled by itself")
                r.effects.words.forEach { w ->
                    assertTrue(a in w.cells || b in w.cells, "Level ${level.number}: ${w.word} was not made by the moved tiles")
                }
                if (r.status == GameStatus.PLAYING) {
                    moves++
                    if (game.wordSwaps(cap = 3) >= 3) rich++
                }
            }
        }
        assertTrue(moves > 100, "Too few moves checked ($moves)")
        // Smart refill: at least 3 word swaps after most moves.
        assertTrue(rich >= moves * 0.75, "Only $rich of $moves moves left 3+ word swaps")
    }
}
