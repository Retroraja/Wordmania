package com.retroraja.wordnoid.core

/** The game's levels. The starting letters are made by the level maker (see levelmaker/). */
object Levels {
    /** First live release: levels from here on are harder (fewer moves, bigger goals). */
    const val HARD_FROM = 21

    val all: List<Level> = GeneratedLevels.all.map { if (it.number >= HARD_FROM) harder(it) else it }
    fun get(number: Int): Level? = all.getOrNull(number - 1)

    /**
     * Level 21+: about 10% fewer moves (1 fewer from 10 moves, 2 fewer from 15), and goals that count words or
     * letters need 1 more (when the moves leave room). Board goals (crates, ice, chains...) stay as the board has them.
     */
    fun harder(l: Level): Level {
        val cut = when { l.moves >= 15 -> 2; l.moves >= 10 -> 1; else -> 0 }
        val moves = l.moves - cut
        // A goal only grows when there is room for it (a word needs a move).
        val targets = l.targets.map { t ->
            when (t) {
                is Objective.Words -> if (t.count + 1 < moves) t.copy(count = t.count + 1) else t
                is Objective.LongWords -> if (t.count + 1 < moves / 2) t.copy(count = t.count + 1) else t
                is Objective.Letter -> t.copy(count = t.count + 1)
                else -> t
            }
        }
        return l.copy(moves = moves, targets = targets)
    }
}
