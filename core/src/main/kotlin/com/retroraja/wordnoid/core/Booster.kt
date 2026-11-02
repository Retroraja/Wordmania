package com.retroraja.wordnoid.core

/** What the player taps after choosing a booster. */
enum class Aim {
    /** Works straight away. */
    NONE,
    /** Any tile with a letter. */
    TILE,
    /** A tile the player can move. */
    MOVABLE,
    /** Two tiles the player can move, anywhere. */
    TWO_TILES,
    /** Any tile in the row to slide (then a direction). */
    ROW,
    /** Any tile in the column to slide (then a direction). */
    COLUMN,
    /** The top-left tile of a 2x2 group. */
    SQUARE,
    /** A crate, ice or chain. */
    OBSTACLE,
    /** A gold tile. */
    GOLD,
}

/**
 * All boosters. Every booster only changes the tiles the player picks, so it never ruins a planned word.
 * [introLevel] is the level that teaches it. [strong] boosters are rare in mystery cards.
 * [needsLetter]: the player also picks a letter (Paintbrush, Vowel Flip, Dice).
 */
enum class Booster(
    val title: String,
    val emoji: String,
    val price: Int,
    val introLevel: Int,
    val aim: Aim,
    val strong: Boolean,
    val description: String,
    val needsLetter: Boolean = false,
) {
    HAMMER("Hammer", "🔨", 80, 1, Aim.TILE, false, "Smash one tile or crate. Ice needs two hits"),
    JOKER("Joker", "🃏", 160, 4, Aim.MOVABLE, false, "Turns a tile into a star that can be any letter"),
    MAGIC_SWAP("Magic Swap", "✨", 200, 7, Aim.TWO_TILES, true, "Swap any two tiles, even far apart"),
    HINT("Hint", "💡", 120, 11, Aim.NONE, true, "Shows 2 or 3 words you can make right now"),
    UNDO("Undo", "↩️", 80, 13, Aim.NONE, false, "Take back your last move and get it back"),
    SHUFFLE("Shuffle", "🔀", 60, 16, Aim.NONE, false, "Mix up the board for free"),
    PLUS_MOVES("+3 Moves", "➕", 150, 19, Aim.NONE, true, "Three extra moves"),
    PAINT("Paintbrush", "🖌️", 180, 22, Aim.MOVABLE, false, "Change a tile to any letter", needsLetter = true),
    FREE_MOVE("Free Move", "⏳", 120, 25, Aim.NONE, false, "Your next swap costs no move"),
    ROW_SLIDE("Row Slide", "↔️", 120, 28, Aim.ROW, false, "Rearranges a row's letters to spell a word"),
    COLUMN_SLIDE("Column Slide", "↕️", 120, 28, Aim.COLUMN, false, "Rearranges a column's letters to spell a word"),
    SPIN("Spin", "🌀", 100, 31, Aim.SQUARE, false, "Turns 4 tiles until they make a word"),
    TREASURE_FINDER("Treasure Finder", "🗺️", 150, 32, Aim.NONE, true, "Finds the hidden treasure and digs it out at once"),
    VOWEL_FLIP("Vowel Flip", "🅰️", 100, 34, Aim.MOVABLE, false, "Turn a tile into the vowel you pick", needsLetter = true),
    AXE("Axe", "🪓", 100, 37, Aim.OBSTACLE, false, "Breaks a crate, chain or ice in one chop"),
    SCANNER("Word Scanner", "🔍", 180, 41, Aim.NONE, true, "Lights up every tile that can make a word"),
    DICE("Dice", "🎲", 80, 43, Aim.MOVABLE, false, "Re-roll a tile and pick 1 of 3 new letters", needsLetter = true),
    MAGNET("Magnet", "🧲", 120, 46, Aim.GOLD, false, "Pulls one gold tile off the board for you"),
    LOCK("Lock", "🔒", 60, 49, Aim.MOVABLE, false, "Keeps a tile on the board when its word bursts"),
    DOUBLE("Double Points", "🌟", 80, 52, Aim.NONE, false, "Your next word fills the Power Meter twice"),
    LETTER_RAIN("Letter Rain", "🌧️", 140, 55, Aim.NONE, false, "Rains 3 useful letters onto the board");

    companion object {
        fun unlockedAt(level: Int): List<Booster> = entries.filter { it.introLevel <= level }
        fun introducedAt(level: Int): List<Booster> = entries.filter { it.introLevel == level }
    }
}
