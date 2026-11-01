package com.retroraja.wordnoid.core

/**
 * A computer player, used to check that levels can be won and how hard they are.
 * It looks for everyday words, like a real player would. Unusual words only happen by accident.
 *
 * [skill] 1.0 = always plays the best word it can see (and plans 2 swaps ahead).
 * Lower skill = sometimes misses words and makes a random swap, and picks any word rather than the best.
 */
object Solver {

    data class Result(val won: Boolean, val movesLeft: Int, val wordsFound: Int, val movesUsed: Int)

    fun play(level: Level, dict: Dictionary, skill: Double, rng: Rng, bonusMoves: Int = 0): Result {
        val game = Game(level, dict, bonusMoves)
        while (game.status == GameStatus.PLAYING) {
            val (a, b) = chooseSwap(game, dict, skill, rng)
            game.swap(a, b)
        }
        return Result(game.status == GameStatus.WON, game.movesLeft, game.found.size, game.movesUsed)
    }

    /** Win rate over [runs] games. */
    fun winRate(level: Level, dict: Dictionary, skill: Double, runs: Int, seed: Long): Double {
        val rng = Rng(seed)
        var wins = 0
        repeat(runs) { if (play(level, dict, skill, rng).won) wins++ }
        return wins.toDouble() / runs
    }

    private fun value(words: List<FoundWord>): Int = words.sumOf { if (it.common) 10 * it.length else 1 }

    fun chooseSwap(game: Game, dict: Dictionary, skill: Double, rng: Rng): Pair<Pos, Pos> {
        val board = game.board
        val pairs = board.neighbourPairs()
        val misses = rng.nextDouble() > skill
        if (!misses) {
            val scored = pairs.map { it to value(game.wordsForSwap(it.first, it.second)) }.filter { it.second >= 10 }
            if (scored.isNotEmpty()) {
                return if (skill >= 1.0) scored.maxBy { it.second }.first
                else scored[rng.nextInt(scored.size)].first
            }
            // No word in one swap: look two swaps ahead for an everyday word.
            if (rng.nextDouble() < skill) {
                var best: Pair<Pos, Pos>? = null
                var bestValue = 0
                for (first in pairs) {
                    val after = board.swap(first.first, first.second)
                    for (second in after.neighbourPairs()) {
                        if (second == first) continue
                        val v = value(Game.wordsForSwap(after, second.first, second.second, dict))
                        if (v > bestValue) { bestValue = v; best = first }
                    }
                }
                if (best != null && bestValue >= 10) return best
            }
        }
        return pairs[rng.nextInt(pairs.size)]
    }
}
