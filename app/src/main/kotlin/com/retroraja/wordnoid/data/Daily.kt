package com.retroraja.wordnoid.data

import com.retroraja.wordnoid.core.Booster
import java.time.LocalDate
import kotlin.random.Random

/** Daily Gifts: the 7-day rewards calendar, the Daily Spin wheel and the 3 Daily Challenges. */
object Daily {
    // ---------------------------------------------------------------- 7-day rewards calendar

    data class DayReward(val coins: Int, val booster: Booster?, val bricks: Int = 0) {
        val text: String get() = listOfNotNull(
            if (coins > 0) "🪙$coins" else null,
            booster?.let { "${it.emoji} ${it.title}" },
            if (bricks > 0) "🧱$bricks" else null,
        ).joinToString(" + ")
    }

    val CALENDAR = listOf(
        DayReward(20, null), DayReward(30, Booster.HAMMER), DayReward(40, null), DayReward(50, Booster.JOKER),
        DayReward(60, null, 1), DayReward(80, Booster.HAMMER), DayReward(150, Booster.PLUS_MOVES, 2),
    )

    // ---------------------------------------------------------------- spin wheel

    enum class SlotKind { COINS, BOOSTER, BRICK }
    data class Slot(val label: String, val kind: SlotKind, val amount: Int, val booster: Booster?, val weight: Int)

    /** Odds agreed in the plan: 10c 25%, 20c 20%, 40c 15%, Hammer 12%, brick 12%, +3 Moves 6%, Joker 6%, 300c 4%. */
    val WHEEL = listOf(
        Slot("10", SlotKind.COINS, 10, null, 25),
        Slot("🔨", SlotKind.BOOSTER, 1, Booster.HAMMER, 12),
        Slot("20", SlotKind.COINS, 20, null, 20),
        Slot("➕", SlotKind.BOOSTER, 1, Booster.PLUS_MOVES, 6),
        Slot("40", SlotKind.COINS, 40, null, 15),
        Slot("🧱", SlotKind.BRICK, 1, null, 12),
        Slot("🃏", SlotKind.BOOSTER, 1, Booster.JOKER, 6),
        Slot("300", SlotKind.COINS, 300, null, 4),
    )

    fun spin(random: Random = Random): Int {
        var r = random.nextInt(WHEEL.sumOf { it.weight })
        for ((i, s) in WHEEL.withIndex()) { if (r < s.weight) return i; r -= s.weight }
        return 0
    }

    fun give(progress: Progress, s: Slot) {
        when (s.kind) {
            SlotKind.COINS -> progress.addCoins(s.amount)
            SlotKind.BOOSTER -> s.booster?.let { progress.addBooster(it, s.amount) }
            SlotKind.BRICK -> progress.addBonusBricks(s.amount)
        }
    }

    // ---------------------------------------------------------------- challenges (always called "challenges")

    enum class Kind { WORDS, LONG_WORD, WIN_LEVELS, BOOSTERS, GOLD, CRATES, QUIZ, NEW_WORDS, THREE_BRICKS }

    data class Challenge(val kind: Kind, val target: Int, val text: String, val coins: Int)

    private val POOL = listOf(
        Challenge(Kind.WORDS, 15, "Make 15 words", 40),
        Challenge(Kind.WORDS, 25, "Make 25 words", 60),
        Challenge(Kind.LONG_WORD, 1, "Make a 5-letter word", 40),
        Challenge(Kind.LONG_WORD, 3, "Make three 5-letter words", 70),
        Challenge(Kind.WIN_LEVELS, 2, "Win 2 levels", 50),
        Challenge(Kind.WIN_LEVELS, 3, "Win 3 levels", 70),
        Challenge(Kind.BOOSTERS, 2, "Use 2 boosters", 40),
        Challenge(Kind.GOLD, 5, "Collect 5 gold letters", 50),
        Challenge(Kind.CRATES, 4, "Break 4 boxes", 50),
        Challenge(Kind.NEW_WORDS, 5, "Find 5 new words for your Wordbook", 60),
        Challenge(Kind.THREE_BRICKS, 1, "Win a level with 3 bricks", 60),
    )

    /** Mini quiz is always one of the 3 challenges. */
    val QUIZ_CHALLENGE = Challenge(Kind.QUIZ, 1, "Answer today's mini quiz", 50)

    fun today(): List<Challenge> {
        val r = Random(LocalDate.now().toEpochDay())
        val two = POOL.shuffled(r).distinctBy { it.kind }.take(2)
        return two + QUIZ_CHALLENGE
    }

    /** Counts something the player did toward today's challenges. */
    fun report(progress: Progress, kind: Kind, n: Int = 1) {
        if (n <= 0) return
        today().forEachIndexed { i, c -> if (c.kind == kind && progress.challengeProgress(i) < c.target) progress.addChallenge(i, n) }
    }

    fun done(progress: Progress, i: Int): Boolean = progress.challengeProgress(i) >= today()[i].target
    val CHEST_COINS = 100
}
