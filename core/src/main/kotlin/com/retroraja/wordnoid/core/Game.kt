package com.retroraja.wordnoid.core

enum class GameStatus { PLAYING, WON, LOST }

/** One round of clearing: the words found, and the board after the tiles fell and new ones dropped in. */
data class ClearStep(
    val words: List<FoundWord>,
    val boardAfter: Board,
    /** 1 for the words made by the swap itself, 2+ for combos (words made by falling tiles). */
    val combo: Int,
    val bonusMoves: Int,
    val coins: Int,
    val points: Int,
)

data class MoveResult(
    val swappedBoard: Board,
    val steps: List<ClearStep>,
    /** Set if the board had no possible words left and was reshuffled for free. */
    val shuffledBoard: Board?,
    val status: GameStatus,
    /** Extra points for moves left over when the level is won. */
    val leftoverBonus: Int,
)

/** The rules of one level being played. Has no screen code, so it can be tested and used by the level maker. */
class Game private constructor(
    val level: Level,
    private val dict: Dictionary,
    board: Board,
    movesLeft: Int,
    private val rng: Rng,
    private val ids: IdCounter,
    found: List<FoundWord>,
    score: Int,
    coins: Int,
    status: GameStatus,
    bestCombo: Int,
) {
    var board: Board = board; private set
    var movesLeft: Int = movesLeft; private set
    private val _found = found.toMutableList()
    val found: List<FoundWord> get() = _found
    var score: Int = score; private set
    var coins: Int = coins; private set
    var status: GameStatus = status; private set
    var bestCombo: Int = bestCombo; private set
    var movesUsed: Int = 0; private set

    constructor(level: Level, dict: Dictionary, bonusMoves: Int = 0) : this(
        level, dict,
        Board.fromRows(level.start, IdCounter()).let { b -> b },
        level.moves + bonusMoves,
        Rng(level.seed),
        IdCounter(1_000L),
        emptyList(), 0, 0, GameStatus.PLAYING, 0,
    )

    fun copy(): Game = Game(level, dict, board, movesLeft, rng.copy(), ids.copy(), _found.toList(), score, coins, status, bestCombo)
        .also { it.movesUsed = movesUsed }

    val goalMet: Boolean get() = level.goal.isMet(_found)

    fun canSwap(a: Pos, b: Pos): Boolean =
        status == GameStatus.PLAYING && movesLeft > 0 && board.contains(a) && board.contains(b) && a.isNeighbour(b)

    fun swap(a: Pos, b: Pos): MoveResult {
        require(canSwap(a, b)) { "Swap not allowed" }
        movesLeft--
        movesUsed++
        board = board.swap(a, b)
        val swapped = board
        val steps = ArrayList<ClearStep>()
        var combo = 0
        while (combo < MAX_CASCADES) {
            val words = WordFinder.findAll(board, dict)
            if (words.isEmpty()) break
            combo++
            var bonus = 0
            var coinsGained = 0
            var points = 0
            for (w in words) {
                coinsGained += w.length
                points += w.length * 10 * combo
                if (w.length >= 4) bonus += LONG_WORD_BONUS_MOVES
            }
            if (combo >= 2) bonus += COMBO_BONUS_MOVES
            _found += words
            movesLeft += bonus
            coins += coinsGained
            score += points
            val cleared = words.flatMap { it.cells }.toSet()
            board = Refill.collapse(board, cleared, rng, ids, dict)
            steps += ClearStep(words, board, combo, bonus, coinsGained, points)
        }
        bestCombo = maxOf(bestCombo, combo)

        var leftoverBonus = 0
        var shuffled: Board? = null
        status = when {
            goalMet -> GameStatus.WON
            movesLeft <= 0 -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }
        if (status == GameStatus.WON) {
            leftoverBonus = movesLeft * LEFTOVER_MOVE_POINTS
            score += leftoverBonus
        } else if (status == GameStatus.PLAYING && !hasWordMove(board)) {
            board = shuffle(board)
            shuffled = board
        }
        return MoveResult(swapped, steps, shuffled, status, leftoverBonus)
    }

    /** "+3 moves" from the out-of-moves screen. */
    fun addMoves(n: Int) {
        movesLeft += n
        if (status == GameStatus.LOST && movesLeft > 0) status = GameStatus.PLAYING
    }

    fun stars(): Int = if (status == GameStatus.WON) level.starsFor(movesLeft) else 0

    /** Words that one swap of [a] and [b] would make right now (without changing the game). */
    fun wordsForSwap(a: Pos, b: Pos): List<FoundWord> = wordsForSwap(board, a, b, dict)

    /** A swap that makes an everyday word (longest first), or any word if there is none. Null if no swap makes a word. */
    fun hint(): Pair<Pos, Pos>? {
        var best: Pair<Pos, Pos>? = null
        var bestScore = 0
        for ((a, b) in board.neighbourPairs()) {
            val words = wordsForSwap(a, b)
            if (words.isEmpty()) continue
            val s = words.sumOf { (if (it.common) 100 else 1) * it.length }
            if (s > bestScore) { bestScore = s; best = a to b }
        }
        return best
    }

    private fun hasWordMove(b: Board): Boolean = b.neighbourPairs().any { (x, y) -> wordsForSwap(b, x, y, dict).isNotEmpty() }

    /** Mixes the tiles until the board has no words but at least one swap makes a word. */
    private fun shuffle(b: Board): Board {
        var tiles = b.tiles()
        repeat(200) {
            val list = tiles.toMutableList()
            for (i in list.indices.reversed()) {
                val j = rng.nextInt(i + 1)
                val t = list[i]; list[i] = list[j]; list[j] = t
            }
            val candidate = Board(b.cols, b.rows, list.map { it.copy(dropFrom = 0) })
            if (WordFinder.findAll(candidate, dict).isEmpty() && hasWordMove(candidate)) return candidate
            tiles = list
        }
        return b
    }

    companion object {
        const val LONG_WORD_BONUS_MOVES = 3
        const val COMBO_BONUS_MOVES = 1
        const val LEFTOVER_MOVE_POINTS = 50
        private const val MAX_CASCADES = 20

        fun wordsForSwap(board: Board, a: Pos, b: Pos, dict: Dictionary): List<FoundWord> {
            val swapped = board.swap(a, b)
            return WordFinder.findInLines(swapped, WordFinder.linesThrough(swapped, a, b), dict)
                .filter { w -> w.cells.contains(a) || w.cells.contains(b) }
        }
    }
}
