package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Update 1.3: words must not "match on their own". Plays every level and checks:
 * - no level starts with a ready-made word on the board,
 * - one move bursts the player's word plus at most 1 free word, and a free word has 4+ letters,
 * - on average a level gives at most 1 free word.
 */
class FreeWordsTest {
    private val dict = TestDict.dict

    @Test fun startBoardsHaveNoReadyWords() {
        for (level in Levels.all) {
            val ready = WordFinder.findAll(Game.startBoard(level), dict)
            assertTrue(ready.isEmpty(), "Level ${level.number} starts with ready words ${ready.map { it.word }}")
        }
    }

    @Test fun freeWordsAreRare() {
        var free = 0
        var games = 0
        for (level in Levels.all) for (seed in 1L..3L) {
            val rng = Rng(seed * 31 + level.number)
            val g = Game(level, dict)
            var guard = 0
            games++
            while (g.status == GameStatus.PLAYING && guard++ < 200) {
                if (g.stuck) { g.useBooster(Booster.SHUFFLE); continue }
                val (a, b) = Solver.chooseSwap(g, dict, 0.7, rng)
                if (!g.canSwap(a, b)) break
                val words = g.swap(a, b).effects.words
                assertTrue(words.size <= 1 + Game.MAX_FREE_WORDS, "Level ${level.number}: ${words.size} words in one move")
                words.drop(1).forEach { assertTrue(it.length >= Game.FREE_WORD_MIN, "Level ${level.number}: short free word ${it.word}") }
                free += maxOf(0, words.size - 1)
            }
        }
        val avg = free.toDouble() / games
        println("Free words per level: %.2f".format(avg))
        assertTrue(avg <= 1.0, "Too many free words: %.2f per level".format(avg))
    }
}
