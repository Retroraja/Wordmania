package com.retroraja.wordnoid.core

import kotlin.math.abs

/** A cell on the board. Row 0 is the top row, column 0 is the left column. */
data class Pos(val row: Int, val col: Int) {
    fun isNeighbour(other: Pos): Boolean = abs(row - other.row) + abs(col - other.col) == 1
}

/**
 * One letter tile.
 * [id] stays the same while the tile moves around, so the screen can animate it.
 * [dropFrom] is how many rows above the board a brand-new tile falls from (0 for tiles already on the board).
 */
data class Tile(val id: Long, val letter: Char, val dropFrom: Int = 0)

/** Hands out unique tile ids. */
class IdCounter(private var next: Long = 1) {
    fun next(): Long = next++
    fun copy(): IdCounter = IdCounter(next)
}

/** The grid of tiles. Boards never change: every change returns a new board. */
class Board(val cols: Int, val rows: Int, private val cells: List<Tile>) {
    init {
        require(cols > 0 && rows > 0) { "Board must have at least one row and column" }
        require(cells.size == cols * rows) { "Expected ${cols * rows} tiles, got ${cells.size}" }
    }

    operator fun get(p: Pos): Tile = cells[p.row * cols + p.col]
    operator fun get(row: Int, col: Int): Tile = cells[row * cols + col]

    fun contains(p: Pos): Boolean = p.row in 0 until rows && p.col in 0 until cols

    fun tiles(): List<Tile> = cells

    fun positions(): List<Pos> = (0 until rows).flatMap { r -> (0 until cols).map { c -> Pos(r, c) } }

    fun swap(a: Pos, b: Pos): Board {
        val list = cells.toMutableList()
        val i = a.row * cols + a.col
        val j = b.row * cols + b.col
        list[i] = cells[j]
        list[j] = cells[i]
        return Board(cols, rows, list)
    }

    /** All pairs of neighbouring cells (each pair once). */
    fun neighbourPairs(): List<Pair<Pos, Pos>> {
        val out = ArrayList<Pair<Pos, Pos>>()
        for (r in 0 until rows) for (c in 0 until cols) {
            if (c + 1 < cols) out += Pos(r, c) to Pos(r, c + 1)
            if (r + 1 < rows) out += Pos(r, c) to Pos(r + 1, c)
        }
        return out
    }

    /** Letters row by row, e.g. ["CAT", "DOG"]. */
    fun rowStrings(): List<String> =
        (0 until rows).map { r -> (0 until cols).map { c -> this[r, c].letter }.joinToString("") }

    override fun toString(): String = rowStrings().joinToString("/")

    companion object {
        fun fromRows(rows: List<String>, ids: IdCounter): Board {
            require(rows.isNotEmpty())
            val cols = rows[0].length
            require(rows.all { it.length == cols }) { "All rows must be the same length" }
            val tiles = rows.flatMap { row -> row.uppercase().map { Tile(ids.next(), it) } }
            return Board(cols, rows.size, tiles)
        }
    }
}
