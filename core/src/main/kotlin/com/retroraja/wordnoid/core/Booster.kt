package com.retroraja.wordnoid.core

/** Intro level of removed boosters (Double Points, Spin, Lock, Dice, Word Scanner). */
const val RETIRED = 1000
/** Intro level of boosters saved for a later update (Free Move, Vowel Rain, Letter Rain): not in Levels 1-50. */
const val LATER = 900

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
    /** A crate, ice, chain or balloon. */
    OBSTACLE,
    /** A gold tile. */
    GOLD,
}

/**
 * All boosters. Every booster only changes the tiles the player picks, so it never ruins a planned word.
 * [introLevel] is the level that teaches it (Select + Match update: Joker 2, Hammer 4, Magic Swap 6, Hint 9, Shuffle 12,
 * +3 Moves 15, Magnet 20, Paintbrush 26, Treasure Finder 32, Axe 37, Row/Column Slide 42, Undo 47). [strong] boosters are rare in mystery cards.
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
    HAMMER("Hammer", "🔨", 80, 4, Aim.TILE, false, "Smash one tile or crate. Ice needs two hits"),
    JOKER("Joker", "🃏", 160, 2, Aim.MOVABLE, false, "Turns a tile into a star that can be any letter. Use it in a word and MATCH"),
    MAGIC_SWAP("Magic Swap", "✨", 200, 6, Aim.TWO_TILES, true, "Swap any two tiles, even far apart. Free, no move"),
    HINT("Hint", "💡", 120, 9, Aim.NONE, true, "Shows 2 or 3 words you can make right now"),
    UNDO("Undo", "↩️", 80, 47, Aim.NONE, false, "Take back your last swap or word"),
    SHUFFLE("Shuffle", "🔀", 60, 12, Aim.NONE, false, "Mix up the board for free"),
    PLUS_MOVES("+3 Moves", "➕", 150, 15, Aim.NONE, true, "Three extra moves"),
    PAINT("Paintbrush", "🖌️", 180, 26, Aim.MOVABLE, false, "Change a tile to any letter", needsLetter = true),
    FREE_MOVE("Free Move", "⏳", 60, LATER, Aim.NONE, false, "Your next swap costs no move"),
    ROW_SLIDE("Row Slide", "↔️", 120, 42, Aim.ROW, false, "Rearranges a row's letters to spell a word, ready to MATCH"),
    COLUMN_SLIDE("Column Slide", "↕️", 120, 42, Aim.COLUMN, false, "Rearranges a column's letters to spell a word, ready to MATCH"),
    SPIN("Spin", "🌀", 100, RETIRED, Aim.SQUARE, false, "Turns 4 tiles until they make a word"),
    TREASURE_FINDER("Treasure Finder", "🗺️", 150, 32, Aim.NONE, true, "Shows where the next treasure is buried. Clear those glowing tiles with words to dig it out"),
    // Saved as VOWEL_FLIP (old name) so old saves keep their boosters.
    VOWEL_FLIP("Vowel Rain", "💧", 100, LATER, Aim.NONE, false, "Rains 4-5 vowels onto the board"),
    AXE("Axe", "🪓", 100, 37, Aim.OBSTACLE, false, "Breaks a crate, chain or ice, or pops a balloon, in one chop"),
    SCANNER("Word Scanner", "🔍", 180, RETIRED, Aim.NONE, true, "Lights up every tile that can make a word"),
    DICE("Dice", "🎲", 80, RETIRED, Aim.MOVABLE, false, "Re-roll a tile and pick 1 of 3 new letters", needsLetter = true),
    MAGNET("Magnet", "🧲", 120, 20, Aim.GOLD, false, "Pulls one gold tile off the board for you"),
    LOCK("Lock", "🔒", 60, RETIRED, Aim.MOVABLE, false, "Locks a letter so no word uses it. Move it anywhere, then tap it to unlock"),
    DOUBLE("Double Points", "🌟", 80, RETIRED, Aim.NONE, false, "Your next word fills the Power Meter twice"),
    LETTER_RAIN("Letter Rain", "🌧️", 140, LATER, Aim.NONE, false, "Rains 3 useful letters (B, D, L, M, N, R, S, T) onto the board");

    /** Taken out of the game (Select + Match update). Old saves turn these into coins. */
    val retired: Boolean get() = introLevel == RETIRED

    companion object {
        fun unlockedAt(level: Int): List<Booster> = entries.filter { it.introLevel <= level }
        fun introducedAt(level: Int): List<Booster> = entries.filter { it.introLevel == level }
    }
}
