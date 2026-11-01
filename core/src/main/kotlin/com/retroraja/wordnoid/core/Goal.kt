package com.retroraja.wordnoid.core

/** What the player must do to win a level. */
sealed class Goal {
    abstract fun isMet(found: List<FoundWord>): Boolean

    /** Short text for the goal panel, e.g. "Find 2 words". */
    abstract val title: String

    /** e.g. "1/2". */
    abstract fun progressText(found: List<FoundWord>): String

    /** Friendly "how close were you" text for the out-of-moves screen. */
    abstract fun remainingText(found: List<FoundWord>): String

    /** Find [count] words of any length. */
    data class Words(val count: Int) : Goal() {
        override fun isMet(found: List<FoundWord>) = found.size >= count
        override val title get() = "Find $count words"
        override fun progressText(found: List<FoundWord>) = "${minOf(found.size, count)}/$count"
        override fun remainingText(found: List<FoundWord>): String {
            val left = count - found.size
            return if (left == 1) "Just 1 word to go!" else "$left words to go!"
        }
    }

    /** Find [count] words, and at least [longCount] of them must have [minLength]+ letters. */
    data class WordsWithLong(val count: Int, val minLength: Int = 4, val longCount: Int = 1) : Goal() {
        private fun longs(found: List<FoundWord>) = found.count { it.length >= minLength }
        override fun isMet(found: List<FoundWord>) = found.size >= count && longs(found) >= longCount
        override val title get() = "Find $count words, $longCount with $minLength+ letters"
        override fun progressText(found: List<FoundWord>) =
            "${minOf(found.size, count)}/$count  ·  $minLength+: ${minOf(longs(found), longCount)}/$longCount"
        override fun remainingText(found: List<FoundWord>): String {
            val wordsLeft = maxOf(0, count - found.size)
            val longLeft = maxOf(0, longCount - longs(found))
            return when {
                wordsLeft == 0 && longLeft > 0 -> "Just one $minLength-letter word to go!"
                longLeft == 0 -> if (wordsLeft == 1) "Just 1 word to go!" else "$wordsLeft words to go!"
                else -> "$wordsLeft words to go, including a $minLength-letter word!"
            }
        }
    }
}

/** One level. The starting letters and the seed are fixed, so every player gets the same level. */
data class Level(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val moves: Int,
    val goal: Goal,
    val start: List<String>,
    val seed: Long,
    /** Show the swap tutorial with these two cells (level 1). */
    val tutorialSwap: Pair<Pos, Pos>? = null,
    /** Show hints after a pause on this level. */
    val hints: Boolean = true,
) {
    init {
        require(start.size == rows && start.all { it.length == cols }) { "Level $number: start letters don't match ${cols}x$rows" }
    }

    /** 3 stars with 40%+ of moves left, 2 stars with 20%+, otherwise 1. */
    fun starsFor(movesLeft: Int): Int = when {
        movesLeft * 100 >= moves * 40 -> 3
        movesLeft * 100 >= moves * 20 -> 2
        else -> 1
    }
}
