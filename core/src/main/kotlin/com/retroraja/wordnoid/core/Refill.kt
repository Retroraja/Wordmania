package com.retroraja.wordnoid.core

/** Drops tiles down into the gaps and fills the top with new letters. */
object Refill {
    private const val TRIES = 14

    fun collapse(board: Board, cleared: Set<Pos>, rng: Rng, ids: IdCounter, dict: Dictionary): Board {
        val rows = board.rows
        val cols = board.cols
        val tiles = Array(rows) { arrayOfNulls<Tile>(cols) }
        val newCount = IntArray(cols)
        for (c in 0 until cols) {
            val survivors = (rows - 1 downTo 0).map { Pos(it, c) }.filter { it !in cleared }.map { board[it] }
            survivors.forEachIndexed { i, t -> tiles[rows - 1 - i][c] = t.copy(dropFrom = 0) }
            newCount[c] = rows - survivors.size
        }
        val letters = Array(rows) { r -> Array<Char?>(cols) { c -> tiles[r][c]?.letter } }
        // Fill each column from the lowest gap upwards.
        for (c in 0 until cols) {
            for (r in newCount[c] - 1 downTo 0) {
                val placed = letters.sumOf { row -> row.count { it != null } }
                val vowels = letters.sumOf { row -> row.count { it != null && Letters.isVowel(it) } }
                val needVowel = placed > 0 && vowels.toDouble() / (placed + 1) < Letters.MIN_VOWEL_SHARE
                var chosen = Letters.pick(rng, vowelOnly = needVowel)
                for (attempt in 0 until TRIES) {
                    val letter = if (attempt == 0) chosen else Letters.pick(rng, vowelOnly = needVowel && attempt < TRIES / 2)
                    letters[r][c] = letter
                    chosen = letter
                    if (!WordFinder.anyWordThrough(letters, Pos(r, c), dict)) break
                }
                letters[r][c] = chosen
                tiles[r][c] = Tile(ids.next(), chosen, dropFrom = newCount[c])
            }
        }
        return Board(cols, rows, (0 until rows).flatMap { r -> (0 until cols).map { c -> tiles[r][c]!! } })
    }
}
