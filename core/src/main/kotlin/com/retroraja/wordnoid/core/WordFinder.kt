package com.retroraja.wordnoid.core

/** A word found on the board. [cells] are in reading order (first letter first). */
data class FoundWord(val word: String, val cells: List<Pos>, val common: Boolean) {
    val length: Int get() = word.length
}

/**
 * Finds words on the board.
 * Words are read in all four directions: left to right, right to left, top to bottom and bottom to top.
 * In each row or column the longest words win, and words in the same line never share a tile.
 * If the same tiles spell a word both ways (TAB / BAT) it counts once, preferring an everyday word.
 */
object WordFinder {

    fun rowLine(board: Board, row: Int): List<Pos> = (0 until board.cols).map { Pos(row, it) }
    fun colLine(board: Board, col: Int): List<Pos> = (0 until board.rows).map { Pos(it, col) }

    fun allLines(board: Board): List<List<Pos>> =
        (0 until board.rows).map { rowLine(board, it) } + (0 until board.cols).map { colLine(board, it) }

    /** The rows and columns that change when [a] and [b] swap. */
    fun linesThrough(board: Board, a: Pos, b: Pos): List<List<Pos>> {
        val rows = setOf(a.row, b.row).map { rowLine(board, it) }
        val cols = setOf(a.col, b.col).map { colLine(board, it) }
        return rows + cols
    }

    fun findAll(board: Board, dict: Dictionary): List<FoundWord> = findInLines(board, allLines(board), dict)

    fun findInLines(board: Board, lines: List<List<Pos>>, dict: Dictionary): List<FoundWord> =
        lines.flatMap { findInLine(board, it, dict) }

    private class Candidate(val start: Int, val len: Int, val word: String, val reversed: Boolean, val common: Boolean)

    fun findInLine(board: Board, line: List<Pos>, dict: Dictionary): List<FoundWord> {
        val n = line.size
        if (n < Dictionary.MIN_LENGTH) return emptyList()
        val letters = CharArray(n) { board[line[it]].letter.lowercaseChar() }
        val candidates = ArrayList<Candidate>()
        for (len in n downTo Dictionary.MIN_LENGTH) {
            for (start in 0..n - len) {
                val forward = String(letters, start, len)
                val backward = forward.reversed()
                val fOk = dict.isWord(forward)
                val bOk = dict.isWord(backward)
                if (!fOk && !bOk) continue
                val useBackward = when {
                    fOk && bOk -> !dict.isCommon(forward) && dict.isCommon(backward)
                    else -> bOk
                }
                val word = if (useBackward) backward else forward
                candidates += Candidate(start, len, word, useBackward, dict.isCommon(word))
            }
        }
        if (candidates.isEmpty()) return emptyList()
        candidates.sortWith(compareByDescending<Candidate> { it.len }.thenByDescending { it.common }.thenBy { it.start })
        val used = BooleanArray(n)
        val out = ArrayList<FoundWord>()
        for (c in candidates) {
            if ((c.start until c.start + c.len).any { used[it] }) continue
            for (i in c.start until c.start + c.len) used[i] = true
            val cells = line.subList(c.start, c.start + c.len).let { if (c.reversed) it.reversed() else it.toList() }
            out += FoundWord(c.word.uppercase(), cells, c.common)
        }
        return out
    }

    /** True if any word passes through [pos], looking only at filled cells (nulls break a line). Used when refilling. */
    fun anyWordThrough(grid: Array<Array<Char?>>, pos: Pos, dict: Dictionary): Boolean {
        val rows = grid.size
        val cols = grid[0].size
        // Row
        var left = pos.col
        while (left - 1 >= 0 && grid[pos.row][left - 1] != null) left--
        var right = pos.col
        while (right + 1 < cols && grid[pos.row][right + 1] != null) right++
        val rowSeg = (left..right).map { grid[pos.row][it]!!.lowercaseChar() }
        if (segmentHasWordThrough(rowSeg, pos.col - left, dict)) return true
        // Column
        var top = pos.row
        while (top - 1 >= 0 && grid[top - 1][pos.col] != null) top--
        var bottom = pos.row
        while (bottom + 1 < rows && grid[bottom + 1][pos.col] != null) bottom++
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
                if (dict.isWord(w) || dict.isWord(w.reversed())) return true
            }
        }
        return false
    }
}
