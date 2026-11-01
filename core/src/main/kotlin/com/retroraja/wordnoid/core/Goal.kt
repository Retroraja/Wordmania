package com.retroraja.wordnoid.core

/** What the player has done so far in a level. */
data class Stats(
    val words: List<FoundWord> = emptyList(),
    val gold: Int = 0,
    val crates: Int = 0,
    val ice: Int = 0,
    val chains: Int = 0,
    val letters: Map<Char, Int> = emptyMap(),
    val themeWords: Int = 0,
) {
    fun letter(c: Char): Int = letters[c] ?: 0
}

/** One part of a level's goal. A level is won when every target is reached. */
sealed class Objective {
    abstract val count: Int
    abstract fun progress(s: Stats): Int
    /** Short label for the goal panel. */
    abstract val label: String
    abstract val icon: String

    fun done(s: Stats): Boolean = progress(s) >= count
    fun left(s: Stats): Int = maxOf(0, count - progress(s))

    data class Words(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.words.size
        override val label get() = "Words"
        override val icon get() = "🔤"
    }

    data class LongWords(override val count: Int, val minLength: Int = 4) : Objective() {
        override fun progress(s: Stats) = s.words.count { it.length >= minLength }
        override val label get() = "$minLength+ letter words"
        override val icon get() = "📏"
    }

    data class Gold(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.gold
        override val label get() = "Gold tiles"
        override val icon get() = "🪙"
    }

    data class Crates(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.crates
        override val label get() = "Crates"
        override val icon get() = "📦"
    }

    data class Ice(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.ice
        override val label get() = "Ice tiles"
        override val icon get() = "🧊"
    }

    data class Chains(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.chains
        override val label get() = "Chains"
        override val icon get() = "⛓️"
    }

    data class Letter(val letter: Char, override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.letter(letter)
        override val label get() = "Use the letter $letter"
        override val icon get() = letter.toString()
    }

    data class Theme(val theme: String, override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.themeWords
        override val label get() = theme
        override val icon get() = Themes.byName(theme)?.emoji ?: "⭐"
    }

    data class Spell(val word: String) : Objective() {
        override val count get() = 1
        override fun progress(s: Stats) = if (s.words.any { it.word == word }) 1 else 0
        override val label get() = "Spell $word"
        override val icon get() = "✍️"
    }
}

/** Something new a level teaches (shown on its intro card). */
enum class Feature(val title: String, val text: String) {
    GOLD("Gold tiles", "Use gold-framed letters in your words to collect them."),
    CRATES("Wooden crates", "Make a word right next to a crate to smash it. Every crate hides a prize!"),
    LETTERS("Collect letters", "Use the shown letter in your words. Every time it's in a word, it counts."),
    BONUS_WORD("Bonus word", "Spell the bonus word to win a mystery card. It's optional, so go for it if you can!"),
    PRE_BOOSTERS("Start boosters", "Before a level, you can start with a booster. Try +3 moves, free this time!"),
    GIFTS("Gift tiles", "Tiles with a ribbon sometimes fall in. Use one in a word to get a free booster."),
    THEME("Theme words", "Find words from the theme. Theme words give double coins!"),
    LONG("Long words", "Make words with 4 or more letters. Long words give extra moves too."),
    DOUBLE_CRATES("Strong crates", "These crates need two hits. The first hit cracks them."),
    ICE("Ice tiles", "Frozen letters can't be moved, but you can use them in a word to melt them."),
    SPELL("Spell the word", "Spell the target word anywhere on the board, in any direction."),
    CHAINS("Chains", "Chained tiles can't be moved. Make a word next to them (or with them) to snap the chain."),
    BOSS("Boss level", "A tough one! Use your boosters wisely. Big prize if you win."),
}

/** One level. The starting letters and the seed are fixed, so every player gets the same level. */
data class Level(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val moves: Int,
    val targets: List<Objective>,
    val start: List<String>,
    /** Same shape as [start]: '.' plain, 'g' gold, 'i' ice, 'c' chain, 'p' gift. */
    val marks: List<String>? = null,
    val seed: Long,
    /** Show the swap tutorial with these two cells (level 1). */
    val tutorialSwap: Pair<Pos, Pos>? = null,
    val feature: Feature? = null,
    val bonusWord: String? = null,
    val boss: Boolean = false,
    /** Chance (0..1) that a new tile is a gift tile. */
    val giftRate: Double = 0.0,
    /** Prizes for the crates, in board order (row by row). */
    val cratePrizes: List<Prize> = emptyList(),
) {
    init {
        require(start.size == rows && start.all { it.length == cols }) { "Level $number: start letters don't match ${cols}x$rows" }
        require(marks == null || (marks.size == rows && marks.all { it.length == cols })) { "Level $number: marks don't match" }
    }

    val introBoosters: List<Booster> get() = Booster.introducedAt(number)

    fun isMet(s: Stats): Boolean = targets.all { it.done(s) }

    /** 3 stars with 30%+ of moves left, 2 stars with 10%+, otherwise 1. */
    fun starsFor(movesLeft: Int): Int = when {
        movesLeft * 100 >= moves * 30 -> 3
        movesLeft * 100 >= moves * 10 -> 2
        else -> 1
    }

    fun themeName(): String? = targets.filterIsInstance<Objective.Theme>().firstOrNull()?.theme
}
