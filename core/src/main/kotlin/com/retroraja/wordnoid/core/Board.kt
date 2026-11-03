package com.retroraja.wordnoid.core

import kotlin.math.abs

/** A cell on the board. Row 0 is the top row, column 0 is the left column. */
data class Pos(val row: Int, val col: Int) {
    fun isNeighbour(other: Pos): Boolean = abs(row - other.row) + abs(col - other.col) == 1
}

/**
 * NORMAL: a letter tile.
 * GOLD: a letter tile with a gold frame. Collected when used in a word.
 * CRATE: a wooden crate with no letter. Breaks (after [Tile.hp] hits) when a word is made next to it, and gives its prize.
 * ICE: a frozen letter. Can't be swapped, but can be used in a word, which melts it.
 * CHAIN: a chained letter. Can't be swapped until a word is made next to it (or with it).
 * JOKER: a star tile that stands for any letter.
 * GIFT: a letter tile with a ribbon. Gives a booster when used in a word.
 * BALLOON: a letter tile with a balloon tied on. Pops (and becomes NORMAL) when a word is made next to it or with it.
 * BUDDY: a little Tiko friend with no letter. Falls when tiles below it clear, and escapes at the bottom row.
 */
enum class TileKind { NORMAL, GOLD, CRATE, ICE, CHAIN, JOKER, GIFT, BALLOON, BUDDY }

enum class PrizeKind { COINS, MOVES, BOOSTER }

/** What a crate gives when it breaks. For BOOSTER the app picks which booster. */
data class Prize(val kind: PrizeKind, val amount: Int)

/**
 * One tile.
 * [id] stays the same while the tile moves around, so the screen can animate it.
 * [dropFrom] is how many rows above the board a brand-new tile falls from (0 for tiles already on the board).
 * [locked] tiles (Lock booster) can't be part of any word until the player unlocks them.
 */
data class Tile(
    val id: Long,
    val letter: Char,
    val dropFrom: Int = 0,
    val kind: TileKind = TileKind.NORMAL,
    val hp: Int = 0,
    val prize: Prize? = null,
    val locked: Boolean = false,
) {
    val isCrate: Boolean get() = kind == TileKind.CRATE
    val isJoker: Boolean get() = kind == TileKind.JOKER
    val isGold: Boolean get() = kind == TileKind.GOLD
    val isIce: Boolean get() = kind == TileKind.ICE
    val isChain: Boolean get() = kind == TileKind.CHAIN
    val isGift: Boolean get() = kind == TileKind.GIFT
    val isBalloon: Boolean get() = kind == TileKind.BALLOON
    val isBuddy: Boolean get() = kind == TileKind.BUDDY

    /** Can the player swap this tile? */
    val movable: Boolean get() = kind != TileKind.CRATE && kind != TileKind.ICE && kind != TileKind.CHAIN && kind != TileKind.BUDDY

    /** Can this tile be part of a word? */
    val hasLetter: Boolean get() = kind != TileKind.CRATE && kind != TileKind.BUDDY
}

const val CRATE_CHAR = '#'
const val CRATE2_CHAR = '%'
const val JOKER_CHAR = '*'
const val BUDDY_CHAR = '@'

/** Ice needs two Hammer hits (the first cracks it). A word melts it at once. */
const val ICE_HP = 2

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

    fun find(id: Long): Pos? = cells.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { Pos(it / cols, it % cols) }

    fun with(p: Pos, tile: Tile): Board {
        val list = cells.toMutableList()
        list[p.row * cols + p.col] = tile
        return Board(cols, rows, list)
    }

    fun neighbours(p: Pos): List<Pos> =
        listOf(Pos(p.row - 1, p.col), Pos(p.row + 1, p.col), Pos(p.row, p.col - 1), Pos(p.row, p.col + 1)).filter { contains(it) }

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

    /** Neighbouring pairs the player may swap (both tiles movable). */
    fun swappablePairs(): List<Pair<Pos, Pos>> = neighbourPairs().filter { (a, b) -> this[a].movable && this[b].movable }

    /** Letters row by row. Crates are '#' (or '%' with 2 hits left), jokers '*'. */
    fun rowStrings(): List<String> =
        (0 until rows).map { r -> (0 until cols).map { c -> code(this[r, c]) }.joinToString("") }

    /** Marks row by row: '.' plain, 'g' gold, 'i' ice, 'c' chain, 'p' gift, 'b' balloon. */
    fun markStrings(): List<String> =
        (0 until rows).map { r -> (0 until cols).map { c -> mark(this[r, c]) }.joinToString("") }

    override fun toString(): String = rowStrings().joinToString("/")

    companion object {
        fun fromRows(rows: List<String>, ids: IdCounter, marks: List<String>? = null): Board {
            require(rows.isNotEmpty())
            val cols = rows[0].length
            require(rows.all { it.length == cols }) { "All rows must be the same length" }
            val tiles = rows.flatMapIndexed { r, row ->
                row.mapIndexed { c, ch -> tileFor(ch, marks?.getOrNull(r)?.getOrNull(c) ?: '.', ids) }
            }
            return Board(cols, rows.size, tiles)
        }

        fun tileFor(ch: Char, mark: Char, ids: IdCounter): Tile = when (ch) {
            CRATE_CHAR -> Tile(ids.next(), CRATE_CHAR, kind = TileKind.CRATE, hp = 1)
            CRATE2_CHAR -> Tile(ids.next(), CRATE_CHAR, kind = TileKind.CRATE, hp = 2)
            JOKER_CHAR -> Tile(ids.next(), JOKER_CHAR, kind = TileKind.JOKER)
            BUDDY_CHAR -> Tile(ids.next(), BUDDY_CHAR, kind = TileKind.BUDDY)
            else -> Tile(
                ids.next(), ch.uppercaseChar(),
                kind = when (mark) {
                    'g' -> TileKind.GOLD
                    'i' -> TileKind.ICE
                    'c' -> TileKind.CHAIN
                    'p' -> TileKind.GIFT
                    'b' -> TileKind.BALLOON
                    else -> TileKind.NORMAL
                },
                hp = if (mark == 'i') ICE_HP else 0,
            )
        }

        fun code(t: Tile): Char = when {
            t.isCrate -> if (t.hp >= 2) CRATE2_CHAR else CRATE_CHAR
            t.isBuddy -> BUDDY_CHAR
            else -> t.letter
        }

        fun mark(t: Tile): Char = when (t.kind) {
            TileKind.GOLD -> 'g'
            TileKind.ICE -> 'i'
            TileKind.CHAIN -> 'c'
            TileKind.GIFT -> 'p'
            TileKind.BALLOON -> 'b'
            else -> '.'
        }
    }
}
