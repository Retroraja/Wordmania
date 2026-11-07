package com.retroraja.wordnoid.core

/** What the player has done so far in a level. */
data class Stats(
    val words: List<FoundWord> = emptyList(),
    val gold: Int = 0,
    val crates: Int = 0,
    val ironCrates: Int = 0,
    val ice: Int = 0,
    val chains: Int = 0,
    val letters: Map<Char, Int> = emptyMap(),
    val themeWords: Int = 0,
    val rescued: Int = 0,
    val balloons: Int = 0,
    val treasures: Int = 0,
    val twins: Int = 0,
    val mysteries: Int = 0,
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
    /** The goal in plain words, e.g. "Use 5 gold letters in words". */
    abstract val text: String

    fun done(s: Stats): Boolean = progress(s) >= count
    fun left(s: Stats): Int = maxOf(0, count - progress(s))

    data class Words(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.words.size
        override val label get() = "Words"
        override val icon get() = "🔤"
        override val text get() = "Make $count words"
    }

    data class LongWords(override val count: Int, val minLength: Int = 4) : Objective() {
        override fun progress(s: Stats) = s.words.count { it.length >= minLength }
        override val label get() = "$minLength+ letter words"
        override val icon get() = "📏"
        override val text get() = if (count == 1) "Make a $minLength-letter word" else "Make $count words with $minLength+ letters"
    }

    data class Gold(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.gold
        override val label get() = "Gold tiles"
        override val icon get() = "🟨"
        override val text get() = "Use $count gold letters in words"
    }

    data class Crates(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.crates
        override val label get() = "Crates"
        override val icon get() = "📦"
        override val text get() = "Break $count crates (make a word next to them)"
    }

    data class IronCrates(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.ironCrates
        override val label get() = "Iron Crates"
        override val icon get() = "🔩"
        override val text get() = "Break $count iron crates (2 hits each: make words next to them)"
    }

    data class Ice(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.ice
        override val label get() = "Ice tiles"
        override val icon get() = "🧊"
        override val text get() = "Melt $count ice tiles (use them in words)"
    }

    data class Chains(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.chains
        override val label get() = "Chains"
        override val icon get() = "⛓️"
        override val text get() = "Free $count chained tiles (make a word next to them)"
    }

    data class Letter(val letter: Char, override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.letter(letter)
        override val label get() = "Use the letter $letter"
        override val icon get() = letter.toString()
        override val text get() = "Use the letter $letter $count times"
    }

    data class Theme(val theme: String, override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.themeWords
        override val label get() = theme
        override val icon get() = Themes.byName(theme)?.emoji ?: "⭐"
        override val text get() = Themes.byName(theme)?.goalNoun.let { n -> if (count == 1) "Make 1 ${(n ?: "theme words").removeSuffix("s")}" else "Make $count $n" }
    }

    data class Spell(val word: String) : Objective() {
        override val count get() = 1
        override fun progress(s: Stats) = if (s.words.any { it.word == word }) 1 else 0
        override val label get() = "Spell $word"
        override val icon get() = "✍️"
        override val text get() = "Spell the word $word"
    }

    data class Rescue(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.rescued
        override val label get() = "Tiko friends"
        override val icon get() = "💖"
        override val text get() = "Rescue $count Tiko friends (bring them to the bottom)"
    }

    data class Balloons(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.balloons
        override val label get() = "Balloons"
        override val icon get() = "🎈"
        override val text get() = "Pop $count balloons (make a word next to them)"
    }

    data class Twins(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.twins
        override val label get() = "x2 tiles"
        override val icon get() = "✖️"
        override val text get() = "Use $count x2 tiles in words (each word counts twice)"
    }

    data class Mystery(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.mysteries
        override val label get() = "Mystery tiles"
        override val icon get() = "❓"
        override val text get() = "Reveal $count mystery tiles (make a word beside them)"
    }

    data class Treasure(override val count: Int) : Objective() {
        override fun progress(s: Stats) = s.treasures
        override val label get() = "Treasure"
        override val icon get() = "💎"
        override val text get() = if (count == 1) "Find the treasure (clear the tiles on the sand)" else "Find $count treasures (clear the tiles on the sand)"
    }
}

/** Something new a level teaches (shown on its intro card). */
enum class Feature(val title: String, val text: String) {
    GOLD("Gold tiles", "Use gold-framed letters in your words to collect them."),
    CRATES("Wooden crates", "Make a word right beside a crate (above, below, left or right) to smash it. Every crate hides a prize!"),
    LETTERS("Collect letters", "Use the shown letter in your words. Every time it's in a word, it counts."),
    BONUS_WORD("Bonus word", "Spell the bonus word to win a mystery card. It's optional, so go for it if you can!"),
    PRE_BOOSTERS("Start boosters", "Before a level, you can start with a booster. Try +3 moves, free this time!"),
    GIFTS("Gift tiles", "Tiles with a ribbon sometimes fall in. Use one in a word to get a free booster."),
    THEME("Theme words", "Find words from the theme. Theme words give double coins!"),
    LONG("Long words", "Make words with 4 or more letters. Long words give extra moves too."),
    DOUBLE_CRATES("Iron Crates", "Iron Crates need 2 hits. The first word next to one snaps its iron bands, the second breaks it."),
    ICE("Ice tiles", "Ice tiles cost 2 moves to swap. Use them in a word to melt them."),
    SPELL("Spell the word", "Spell the target word anywhere on the board, in any direction."),
    CHAINS("Chains", "Chained tiles can't be moved. Make a word next to them (or with them) to snap the chain."),
    BOSS("Boss level", "The villain fights back! Every few moves it freezes a tile or drops a crate. Each word you make hits it."),
    RESCUE("Tiko friends", "Little Tiko friends are stuck! Use the letters under them in words so they drop down. When they reach the bottom, they're free."),
    BALLOONS("Balloons", "Make a word right next to a balloon (above, below or beside) to pop it."),
    TWIN("x2 tiles", "A word that uses a x2 tile counts twice for your goals. Look for the shiny x2 badge!"),
    MYSTERY("Mystery tiles", "A ? tile hides a letter. Make a word right beside it to reveal the letter, then use it!"),
    TREASURE("Hidden treasure", "A treasure chest is hidden under the sandy tiles. Use those tiles in words to dig it out. Treasure Finder digs it out at once!"),
}

/** One level. The starting letters and the seed are fixed, so every player gets the same level. */
data class Level(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val moves: Int,
    val targets: List<Objective>,
    val start: List<String>,
    /** Same shape as [start]: '.' plain, 'g' gold, 'i' ice, 'c' chain, 'p' gift, 'b' balloon. */
    val marks: List<String>? = null,
    /** Same shape as [start]: 't' marks sand cells hiding treasure (each connected group is one chest). */
    val treasure: List<String>? = null,
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
        require(treasure == null || (treasure.size == rows && treasure.all { it.length == cols })) { "Level $number: treasure doesn't match" }
    }

    /** Which world (0-based, 10 levels each) this level is in. */
    val world: Int get() = (number - 1) / 10

    /** Extra moves a word of [length] letters gives. Bosses are stricter. */
    fun movesForWord(length: Int): Int = when {
        boss && number <= 20 -> if (length >= 5) 1 else 0
        boss && number <= 50 -> if (length >= 6) 1 else 0
        boss -> 0
        length >= 6 -> 2
        length == 5 -> 1
        else -> 0
    }

    /** The treasure chests: each is a connected group of sand cells. */
    val chests: List<Set<Pos>> by lazy {
        val cells = treasure?.flatMapIndexed { r, row -> row.mapIndexedNotNull { c, ch -> if (ch == 't') Pos(r, c) else null } }?.toMutableSet() ?: mutableSetOf()
        val out = ArrayList<Set<Pos>>()
        while (cells.isNotEmpty()) {
            val group = HashSet<Pos>()
            val stack = ArrayDeque(listOf(cells.first()))
            while (stack.isNotEmpty()) {
                val p = stack.removeLast()
                if (!cells.remove(p)) continue
                group += p
                stack += listOf(Pos(p.row - 1, p.col), Pos(p.row + 1, p.col), Pos(p.row, p.col - 1), Pos(p.row, p.col + 1))
            }
            out += group
        }
        out
    }

    val introBoosters: List<Booster> get() = Booster.introducedAt(number)

    fun isMet(s: Stats): Boolean = targets.all { it.done(s) }

    /** Letter Bricks earned: 3 with 30%+ of moves left, 2 with 10%+, otherwise 1. */
    fun starsFor(movesLeft: Int): Int = when {
        movesLeft * 100 >= moves * 30 -> 3
        movesLeft * 100 >= moves * 10 -> 2
        else -> 1
    }

    fun themeName(): String? = targets.filterIsInstance<Objective.Theme>().firstOrNull()?.theme
}
