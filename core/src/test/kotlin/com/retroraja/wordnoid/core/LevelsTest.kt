package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every shipped level must be numbered in order, have a word one swap away, and be winnable by the computer player. */
class LevelsTest {
    private val dict = TestDict.dict

    @Test fun levelsAreFairAndWinnable() {
        assertTrue(Levels.all.isNotEmpty(), "No levels")
        Levels.all.forEachIndexed { i, level ->
            assertEquals(i + 1, level.number)
            val game = Game(level, dict)
            assertTrue(game.hint() != null, "Level ${level.number} has no word one swap away")
            val strong = Solver.play(level, dict, 1.0, Rng(1))
            assertTrue(strong.won, "Level ${level.number} not won by the computer player")
            level.tutorialSwap?.let { (a, b) ->
                assertTrue(game.wordsForSwap(a, b).isNotEmpty(), "Level ${level.number} tutorial swap makes no word")
            }
            level.themeName()?.let { assertTrue(Themes.byName(it) != null, "Unknown theme $it") }
        }
    }
}
