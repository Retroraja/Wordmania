package com.retroraja.wordnoid.core

/** Drops tiles down into the gaps and fills the top with new letters. */
object Refill {
    private const val TRIES = 10

    fun collapse(board: Board, cleared: Set<Pos>, rng: Rng, ids: IdCounter, dict: Dictionary, giftRate: Double = 0.0, lucky: Boolean = false): Board {
        val rows = board.rows
        val cols = board.cols
        val tiles = Array(rows) { arrayOfNulls<Tile>(cols) }
        val newCount = IntArray(cols)
        for (c in 0 until cols) {
            val survivors = (rows - 1 downTo 0).map { Pos(it, c) }.filter { it !in cleared }.map { board[it] }
            survivors.forEachIndexed { i, t -> tiles[rows - 1 - i][c] = t.copy(dropFrom = 0) }
            newCount[c] = rows - survivors.size
        }
        val letters = Array(rows) { r -> Array<Char?>(cols) { c -> tiles[r][c]?.let { if (!it.hasLetter) CRATE_CHAR else it.letter } } }
        for (c in 0 until cols) {
            for (r in newCount[c] - 1 downTo 0) {
                var placed = 0
                var vowels = 0
                for (row in letters) for (ch in row) if (ch != null && ch != CRATE_CHAR && ch != JOKER_CHAR) {
                    placed++
                    if (Letters.isVowel(ch)) vowels++
                }
                val needVowel = placed > 0 && vowels.toDouble() / (placed + 1) < Letters.MIN_VOWEL_SHARE
                var chosen = Letters.pick(rng, vowelOnly = needVowel)
                                // Lucky drop (Update 1.3): only a long word (4+ letters) may form; short ones are re-rolled too.
                val minOk = if (lucky) 4 else Int.MAX_VALUE
                for (attempt in 0 until TRIES) {
                    val letter = if (attempt == 0) chosen else Letters.pick(rng, vowelOnly = needVowel && attempt < TRIES / 2)
                    letters[r][c] = letter
                    chosen = letter
                    if (!WordFinder.anyWordThrough(letters, Pos(r, c), dict, maxLen = minOk - 1)) break
                }
                letters[r][c] = chosen
                val kind = if (giftRate > 0 && rng.nextDouble() < giftRate) TileKind.GIFT else TileKind.NORMAL
                tiles[r][c] = Tile(ids.next(), chosen, dropFrom = newCount[c], kind = kind)
            }
        }
        return Board(cols, rows, (0 until rows).flatMap { r -> (0 until cols).map { c -> tiles[r][c]!! } })
    }
}
