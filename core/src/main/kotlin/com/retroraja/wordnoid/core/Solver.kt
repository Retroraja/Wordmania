package com.retroraja.wordnoid.core

/**
 * Computer players, used by the level maker to check that levels are fair and how hard they are.
 * They only swap (no boosters), so a level that a casual player often loses is one where boosters help.
 *
 * [skill] 1.0 = always plays the swap that helps the goal most, and plans 2 swaps ahead.
 * 0.0 = random swaps. In between: sometimes misses words, and picks any helpful word rather than the best.
 */
object Solver {

    data class Result(val won: Boolean, val movesLeft: Int, val wordsFound: Int, val movesUsed: Int)

    fun play(level: Level, dict: Dictionary, skill: Double, rng: Rng, bonusMoves: Int = 0): Result {
        val game = Game(level, dict, bonusMoves)
        var guard = 0
        while (game.status == GameStatus.PLAYING && guard++ < 400) {
            val (a, b) = chooseSwap(game, dict, skill, rng)
            if (!game.canSwap(a, b)) break
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

    fun chooseSwap(game: Game, dict: Dictionary, skill: Double, rng: Rng): Pair<Pos, Pos> {
        val board = game.board
        // Only swaps the player can pay for (an ice swap costs 2 moves).
        val pairs = board.swappablePairs().filter { (a, b) -> game.canSwap(a, b) }.ifEmpty { board.swappablePairs() }
        if (rng.nextDouble() >= skill) return pairs[rng.nextInt(pairs.size)]
        val scored = pairs.map { it to game.previewGain(it.first, it.second) }.filter { it.second > 0 }
        if (skill >= 1.0) {
            val helpful = scored.filter { it.second >= 5.0 }
            if (helpful.isNotEmpty()) return helpful.maxBy { it.second }.first
            // Nothing helps the goal right now: look two swaps ahead.
            var best: Pair<Pos, Pos>? = null
            var bestValue = 0.0
            for (first in pairs) {
                val g = game.copy()
                g.swap(first.first, first.second)
                if (g.status != GameStatus.PLAYING) { if (g.status == GameStatus.WON) return first else continue }
                val now = g.previewGainNow()
                val v = now + game.previewGain(first.first, first.second) * 0.5
                if (v > bestValue) { bestValue = v; best = first }
            }
            if (best != null) return best
            if (scored.isNotEmpty()) return scored.maxBy { it.second }.first
            return pairs[rng.nextInt(pairs.size)]
        }
        if (scored.isNotEmpty()) {
            // A casual player sees a few words and takes one, preferring ones that help.
            val helpful = scored.filter { it.second >= 5.0 }
            val pool = if (helpful.isNotEmpty() && rng.nextDouble() < skill) helpful else scored
            return pool[rng.nextInt(pool.size)].first
        }
        return pairs[rng.nextInt(pairs.size)]
    }

    private fun Game.previewGainNow(): Double {
        var best = 0.0
        for ((a, b) in board.swappablePairs()) best = maxOf(best, previewGain(a, b))
        return best
    }
}
