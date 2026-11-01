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
    HINT("Hint", "💡", 60, 1, Aim.NONE, true, "Shows a swap that makes a word"),
    HAMMER("Hammer", "🔨", 40, 1, Aim.TILE, false, "Smash one tile. A new letter drops in"),
    MAGIC_SWAP("Magic Swap", "✨", 100, 2, Aim.TWO_TILES, true, "Swap any two tiles, even far apart"),
    JOKER("Joker", "🃏", 80, 3, Aim.MOVABLE, false, "Turns a tile into a star that can be any letter"),
    UNDO("Undo", "↩️", 40, 5, Aim.NONE, false, "Take back your last move and get it back"),
    SHUFFLE("Shuffle", "🔀", 30, 7, Aim.NONE, false, "Mix up the board for free"),
    PAINT("Paintbrush", "🖌️", 90, 9, Aim.MOVABLE, false, "Change a tile to any letter", needsLetter = true),
    FREE_MOVE("Free Move", "⏳", 50, 11, Aim.NONE, false, "Your next swap costs no move"),
    PLUS_MOVES("+3 Moves", "➕", 60, 13, Aim.NONE, true, "Three extra moves"),
    SPIN("Spin", "🌀", 50, 15, Aim.SQUARE, false, "Turn a 2x2 group of tiles around"),
    ROW_SLIDE("Row Slide", "↔️", 60, 17, Aim.ROW, false, "Slide a whole row one step"),
    COLUMN_SLIDE("Column Slide", "↕️", 60, 17, Aim.COLUMN, false, "Slide a whole column one step"),
    VOWEL_FLIP("Vowel Flip", "🅰️", 50, 19, Aim.MOVABLE, false, "Turn a tile into the vowel you pick", needsLetter = true),
    DICE("Dice", "🎲", 40, 21, Aim.MOVABLE, false, "Re-roll a tile and pick 1 of 3 new letters", needsLetter = true),
    SCANNER("Word Scanner", "🔍", 90, 23, Aim.NONE, true, "Lights up every tile that can make a word"),
    AXE("Axe", "🪓", 50, 25, Aim.OBSTACLE, false, "Breaks a crate, chain or ice in one chop"),
    LOCK("Lock", "🔒", 30, 27, Aim.MOVABLE, false, "Keeps a tile on the board when its word bursts"),
    DOUBLE("Double Points", "🌟", 40, 29, Aim.NONE, false, "Your next word gives double coins and points"),
    MAGNET("Magnet", "🧲", 60, 31, Aim.GOLD, false, "Pulls one gold tile off the board for you"),
    LETTER_RAIN("Letter Rain", "🌧️", 70, 33, Aim.NONE, false, "Rains 3 useful letters onto the board");

    companion object {
        fun unlockedAt(level: Int): List<Booster> = entries.filter { it.introLevel <= level }
        fun introducedAt(level: Int): List<Booster> = entries.filter { it.introLevel == level }
    }
}
