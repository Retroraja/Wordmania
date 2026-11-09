package com.retroraja.wordnoid.data

/**
 * Update 1.1 Mini Games (menu button in the town, opens after Level 5).
 * Every game: 1 free play a day, plus ONE extra play a day for a video (on any one game the player picks).
 * Coins go to the normal coin counter. Every play is a fresh round; its size follows the player's main-game level.
 * Nothing is ever forced inside levels.
 */
object MiniGames {
    const val UNLOCK_AFTER = 5

    enum class Game(val id: String, val title: String, val emoji: String, val about: String) {
        LETTER_WHEEL("wheel", "Letter Wheel", "🎡", "Swipe the letters to fill the little crossword. Extra words give bonus coins."),
        PICTURE_HUNT("hunt", "Picture Hunt", "🔍", "Tap a shadow, then spell it. It comes to life!"),
        CRACKER_BLAST("crack", "Cracker Blast", "🎆", "Match 3 crackers for 30 seconds. Every 3 crackers = 1 coin."),
    }

    /** The games in this update. The newest one has the NEW badge until it is played. */
    val LAUNCHED = listOf(Game.LETTER_WHEEL, Game.PICTURE_HUNT, Game.CRACKER_BLAST)

    /** Coming in the weekly updates (shown as "coming soon" in the menu). */
    val COMING = listOf("Coin Rain 🪙", "Balloon Pop 🎈", "Treasure Dig ⛏️", "Chest Pick 🧰", "Spin Slots 🎰")

    /**
     * Size step from the player's main-game level (user 2026-11-09): L1-8, L9-20, L21-35, L36-50, L51+.
     * 0 = smallest. Letter Wheel 4/5/6/6/7 letters, Picture Hunt 4/5/6/6/7 shadows, Cracker Blast faster drops.
     */
    fun tier(level: Int): Int = when {
        level <= 8 -> 0
        level <= 20 -> 1
        level <= 35 -> 2
        level <= 50 -> 3
        else -> 4
    }

    fun tierName(t: Int): String = listOf("Levels 1-8", "Levels 9-20", "Levels 21-35", "Levels 36-50", "Levels 51+")[t.coerceIn(0, 4)]

    fun anyFreeToday(p: Progress): Boolean = LAUNCHED.any { !p.miniFreeUsed(it.id) }

    fun newestIsNew(p: Progress): Boolean = !p.seen("mg_played_${LAUNCHED.first().id}")

    /** Can [g] be played now, and how: "free", "ad" (a video gives the play) or null (come back tomorrow). */
    fun howToPlay(p: Progress, g: Game): String? = when {
        !p.miniFreeUsed(g.id) -> "free"
        !p.miniAdUsed -> "ad"
        else -> null
    }

    /** Free pencil hints in one round. More come from short videos. */
    const val FREE_HINTS = 3
}
