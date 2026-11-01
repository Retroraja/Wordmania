package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertTrue

/** Every shipped level must start with no words, have a word one swap away, and be winnable by the computer player. */
class LevelsTest {
    private val dict = TestDict.dict

    @Test fun levelsAreFairAndWinnable() {
        assertTrue(Levels.all.isNotEmpty(), "No levels")
        for (level in Levels.all) {
            val game = Game(level, dict)
            assertTrue(WordFinder.findAll(game.board, dict).isEmpty(), "Level ${level.number} starts with a word")
            assertTrue(game.hint() != null, "Level ${level.number} has no word one swap away")
            val strong = Solver.play(level, dict, 1.0, Rng(1))
            assertTrue(strong.won, "Level ${level.number} not won by the computer player")
            level.tutorialSwap?.let { (a, b) ->
                assertTrue(game.wordsForSwap(a, b).any { it.common }, "Level ${level.number} tutorial swap makes no everyday word")
            }
        }
    }
}
