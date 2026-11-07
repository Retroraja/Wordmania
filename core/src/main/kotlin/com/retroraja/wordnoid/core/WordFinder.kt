package com.retroraja.wordnoid.core

/**
 * A word found on the board. [cells] are in reading order (first letter first).
 * [word] is uppercase, with any Joker replaced by the letter it stands for.
 */
data class FoundWord(val word: String, val cells: List<Pos>, val common: Boolean = true) {
    val length: Int get() = word.length
}

/**
 * Finds words on the board.
 * Words are read in all four directions: left to right, right to left, top to bottom and bottom to top.
 * Crates and locked letters break a line. A Joker (*) stands for any letter.
 * In each row or column the longest words win, and words in the same line never share a tile.
 */
object WordFinder {

    fun rowLine(board: Board, row: Int): List<Pos> = (0 until board.cols).map { Pos(row, it) }
    fun colLine(board: Board, col: Int): List<Pos> = (0 until board.rows).map { Pos(it, col) }

    fun allLines(board: Board): List<List<Pos>> =
        (0 until board.rows).map { rowLine(board, it) } + (0 until board.cols).map { colLine(board, it) }

    /** The rows and columns that go through any of [cells]. */
    fun linesThrough(board: Board, cells: Collection<Pos>): List<List<Pos>> {
        val rows = cells.map { it.row }.toSortedSet().map { rowLine(board, it) }
        val cols = cells.map { it.col }.toSortedSet().map { colLine(board, it) }
        return rows + cols
    }

    fun linesThrough(board: Board, a: Pos, b: Pos): List<List<Pos>> = linesThrough(board, listOf(a, b))

    /** Every word on the board (used to check a start board is clean). */
    fun findAll(board: Board, dict: Dictionary): List<FoundWord> = allLines(board).flatMap { findInLine(board, it, dict, null) }

    /**
     * Words that go through at least one of [changed]. This is how the game scores a move:
     * only words made with the tile(s) the player moved or changed count.
     */
    fun wordsThrough(board: Board, changed: Collection<Pos>, dict: Dictionary, rank: Rank? = null): List<FoundWord> {
        val must = changed.toSet()
        val out = ArrayList<FoundWord>()
        for (line in linesThrough(board, changed)) out += findInLine(board, line, dict, must, rank)
        // A word can be found twice only if it is the same cells; keep one.
        return out.distinctBy { it.cells.toSet() }
    }

    private class Candidate(val start: Int, val len: Int, val word: String, val reversed: Boolean, val hasMust: Boolean, val rank: Int)

    /**
     * Word priority: when one line could make more than one word, higher ranks win first
     * (theme word, then a word with a gold tile, then a word that helps the goal), then longer words.
     * Gets the word (uppercase) and its cells.
     */
    fun interface Rank { fun of(word: String, cells: List<Pos>): Int }

    /** Resolves a pattern (lowercase, '*' = any letter, at most 2) to a word, or null. */
    fun resolve(pattern: String, dict: Dictionary): String? {
        val stars = pattern.count { it == JOKER_CHAR }
        if (stars == 0) return if (dict.isWord(pattern)) pattern else null
        if (stars > 2) return null
        val chars = pattern.toCharArray()
        val i = pattern.indexOf(JOKER_CHAR)
        // Try common letters first so the Joker picks a friendly word.
        for (c in "eaoitnsrlhdcumpgbyfwkvjxzq") {
            chars[i] = c
            val w = resolve(String(chars), dict)
            if (w != null) return w
        }
        return null
    }

    fun findInLine(board: Board, line: List<Pos>, dict: Dictionary, must: Set<Pos>?, rank: Rank? = null): List<FoundWord> {
        val n = line.size
        if (n < Dictionary.MIN_LENGTH) return emptyList()
        val letters = CharArray(n) { val t = board[line[it]]; if (t.hasLetter && !t.locked) t.letter.lowercaseChar() else '#' }
        val mustIdx = BooleanArray(n) { must == null || line[it] in must }
        val candidates = ArrayList<Candidate>()
        for (len in minOf(n, 10) downTo Dictionary.MIN_LENGTH) {
            for (start in 0..n - len) {
                var hasMust = false
                var blocked = false
                for (k in start until start + len) {
                    if (letters[k] == '#') { blocked = true; break }
                    if (mustIdx[k]) hasMust = true
                }
                if (blocked || !hasMust) continue
                val forward = String(letters, start, len)
                val f = resolve(forward, dict)
                val b = if (f == null) resolve(forward.reversed(), dict) else null
                val word = f ?: b ?: continue
                val r = rank?.of(word.uppercase(), line.subList(start, start + len)) ?: 0
                candidates += Candidate(start, len, word, f == null, true, r)
            }
        }
        if (candidates.isEmpty()) return emptyList()
        candidates.sortWith(compareByDescending<Candidate> { it.rank }.thenByDescending { it.len }.thenBy { it.start })
        val used = BooleanArray(n)
        val out = ArrayList<FoundWord>()
        for (c in candidates) {
            if ((c.start until c.start + c.len).any { used[it] }) continue
            for (i in c.start until c.start + c.len) used[i] = true
            val cells = line.subList(c.start, c.start + c.len).let { if (c.reversed) it.reversed() else it.toList() }
            out += FoundWord(c.word.uppercase(), cells)
        }
        return out
    }

    /** True if any word passes through [pos], looking only at filled cells (nulls and '#' break a line). Used when refilling. */
    fun anyWordThrough(grid: Array<Array<Char?>>, pos: Pos, dict: Dictionary): Boolean {
        val rows = grid.size
        val cols = grid[0].size
        fun ok(r: Int, c: Int) = grid[r][c] != null && grid[r][c] != CRATE_CHAR
        var left = pos.col
        while (left - 1 >= 0 && ok(pos.row, left - 1)) left--
        var right = pos.col
        while (right + 1 < cols && ok(pos.row, right + 1)) right++
        val rowSeg = (left..right).map { grid[pos.row][it]!!.lowercaseChar() }
        if (segmentHasWordThrough(rowSeg, pos.col - left, dict)) return true
        var top = pos.row
        while (top - 1 >= 0 && ok(top - 1, pos.col)) top--
        var bottom = pos.row
        while (bottom + 1 < rows && ok(bottom + 1, pos.col)) bottom++
        val colSeg = (top..bottom).map { grid[it][pos.col]!!.lowercaseChar() }
        return segmentHasWordThrough(colSeg, pos.row - top, dict)
    }

    private fun segmentHasWordThrough(seg: List<Char>, index: Int, dict: Dictionary): Boolean {
        val n = seg.size
        if (n < Dictionary.MIN_LENGTH) return false
        val s = String(seg.toCharArray())
        for (len in Dictionary.MIN_LENGTH..n) {
            for (start in maxOf(0, index - len + 1)..minOf(index, n - len)) {
                val w = s.substring(start, start + len)
                if (resolve(w, dict) != null || resolve(w.reversed(), dict) != null) return true
            }
        }
        return false
    }
}
