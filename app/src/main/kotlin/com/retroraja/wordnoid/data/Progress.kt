package com.retroraja.wordnoid.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Game
import java.time.LocalDate

/**
 * Everything saved on the phone: levels, Letter Bricks, coins, boosters, Power Meter, Wordbook, town, daily gifts, settings.
 * Same save name as Build 1, so progress carries over (Build 1 stars are now Letter Bricks).
 * Android backs it up automatically (see backup_rules.xml).
 */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("wordnoid_v2", Context.MODE_PRIVATE)

    var unlocked by mutableIntStateOf(prefs.getInt("unlocked", 1)); private set
    var coins by mutableIntStateOf(prefs.getInt("coins", STARTING_COINS)); private set
    var meter by mutableIntStateOf(prefs.getInt("meter", 0)); private set
    var music by mutableStateOf(prefs.getBoolean("music", true)); private set
    var sound by mutableStateOf(prefs.getBoolean("sound", true)); private set
    var vibration by mutableStateOf(prefs.getBoolean("vibration", true)); private set
    var notifications by mutableStateOf(prefs.getBoolean("notifications", true)); private set
    var noAds by mutableStateOf(prefs.getBoolean("no_ads", false)); private set
    /** Bricks spent on town tasks. Bricks to spend = earned - spent. */
    var bricksSpent by mutableIntStateOf(prefs.getInt("bricks_spent", 0)); private set
    /** Changes whenever something in the town or the daily gifts changes, so screens redraw. */
    var version by mutableIntStateOf(0); private set
    val boosters = mutableStateMapOf<Booster, Int>().apply {
        for (b in Booster.entries) put(b, prefs.getInt("b_${b.name}", 0))
    }
    private val words: MutableSet<String> = HashSet(prefs.getStringSet("wordbook", emptySet()) ?: emptySet())
    var wordbookSize by mutableIntStateOf(words.size); private set

    init {
        if (!prefs.contains("first_day")) prefs.edit().putLong("first_day", today()).apply()
    }

    private fun bump() { version++ }
    private fun today(): Long = LocalDate.now().toEpochDay()

    // ---------------------------------------------------------------- levels and bricks

    /** Letter Bricks won on a level (1 to 3). Build 1 stars are kept as bricks. */
    fun bricks(level: Int): Int = prefs.getInt("stars_$level", 0)
    val bricksEarned: Int get() = (1..unlocked).sumOf { bricks(it) } + prefs.getInt("bricks_bonus", 0)
    val bricksToSpend: Int get() = (bricksEarned - bricksSpent).coerceAtLeast(0)
    fun addBonusBricks(n: Int) { prefs.edit().putInt("bricks_bonus", prefs.getInt("bricks_bonus", 0) + n).apply(); bump() }
    fun worldBricks(world: Int): Int = (world * 10 + 1..world * 10 + 10).sumOf { bricks(it) }

    fun fails(level: Int): Int = prefs.getInt("fails_$level", 0)
    fun seen(key: String): Boolean = prefs.getBoolean("seen_$key", false)
    fun markSeen(key: String) = prefs.edit().putBoolean("seen_$key", true).apply()

    fun levelWon(level: Int, bricks: Int) {
        val e = prefs.edit()
        if (bricks > bricks(level)) e.putInt("stars_$level", bricks)
        if (level + 1 > unlocked) { unlocked = level + 1; e.putInt("unlocked", unlocked) }
        e.putInt("fails_$level", 0)
        e.putInt("levels_since_ad", prefs.getInt("levels_since_ad", 0) + 1)
        e.apply()
        bump()
    }

    fun levelLost(level: Int) {
        prefs.edit().putInt("fails_$level", fails(level) + 1).apply()
    }

    // ---------------------------------------------------------------- boosters and coins

    /** Free first use of a booster during its tutorial. */
    fun hasFreeUse(b: Booster): Boolean = prefs.getBoolean("free_${b.name}", false)
    fun setFreeUse(b: Booster, on: Boolean) = prefs.edit().putBoolean("free_${b.name}", on).apply()

    fun count(b: Booster): Int = boosters[b] ?: 0

    fun addBooster(b: Booster, n: Int = 1) {
        boosters[b] = count(b) + n
        prefs.edit().putInt("b_${b.name}", count(b)).apply()
    }

    /** Uses one booster (free use first, then owned). False if none. */
    fun useBooster(b: Booster): Boolean {
        if (hasFreeUse(b)) { setFreeUse(b, false); return true }
        if (count(b) <= 0) return false
        boosters[b] = count(b) - 1
        prefs.edit().putInt("b_${b.name}", count(b)).apply()
        return true
    }

    fun addCoins(n: Int) {
        coins = (coins + n).coerceAtLeast(0)
        prefs.edit().putInt("coins", coins).apply()
    }

    fun spend(n: Int): Boolean {
        if (coins < n) return false
        addCoins(-n)
        return true
    }

    /** Adds Power Meter segments. Returns how many times it filled up (each fill = one mystery card pick). */
    fun addMeter(n: Int): Int {
        val total = meter + n
        val fills = total / Game.METER_SIZE
        meter = total % Game.METER_SIZE
        prefs.edit().putInt("meter", meter).apply()
        return fills
    }

    // ---------------------------------------------------------------- wordbook

    fun isNewWord(w: String): Boolean = w.uppercase() !in words

    fun addWord(w: String): Boolean {
        val added = words.add(w.uppercase())
        if (added) {
            wordbookSize = words.size
            prefs.edit().putStringSet("wordbook", HashSet(words)).apply()
        }
        return added
    }

    fun wordbook(): List<String> = words.sorted()

    // ---------------------------------------------------------------- settings

    fun toggleMusic() { music = !music; prefs.edit().putBoolean("music", music).apply() }
    fun toggleSound() { sound = !sound; prefs.edit().putBoolean("sound", sound).apply() }
    fun toggleVibration() { vibration = !vibration; prefs.edit().putBoolean("vibration", vibration).apply() }
    fun toggleNotifications() { notifications = !notifications; prefs.edit().putBoolean("notifications", notifications).apply() }

    /** Birth year from the first-time question (0 = not asked yet). Under 13 = child-safe ads, no sign-in. */
    var birthYear: Int
        get() = prefs.getInt("birth_year", 0)
        set(v) { prefs.edit().putInt("birth_year", v).apply(); bump() }
    val isChild: Boolean get() = birthYear != 0 && LocalDate.now().year - birthYear < 13

    fun setNoAds() { noAds = true; prefs.edit().putBoolean("no_ads", true).apply() }

    // ---------------------------------------------------------------- town

    fun taskDone(district: Int, task: Int): Boolean = prefs.getBoolean("task_${district}_$task", false)
    fun tasksDone(district: Int): Int = (0 until 20).count { taskDone(district, it) }

    /** Pays bricks for a town task. False if not enough bricks. */
    fun doTask(district: Int, task: Int, cost: Int): Boolean {
        if (taskDone(district, task) || bricksToSpend < cost) return false
        bricksSpent += cost
        prefs.edit().putBoolean("task_${district}_$task", true).putInt("bricks_spent", bricksSpent).apply()
        bump()
        return true
    }

    /** A district's big building opens when its boss is beaten (and gives a gift every day after). */
    fun districtOpen(district: Int): Boolean = unlocked > (district + 1) * 10
    fun buildingGiftReady(district: Int): Boolean = districtOpen(district) && prefs.getLong("bgift_$district", -1) != today()
    fun claimBuildingGift(district: Int) { prefs.edit().putLong("bgift_$district", today()).apply(); bump() }

    // ---------------------------------------------------------------- quizzes (after the 4th and 8th level of each world)

    fun quizScore(world: Int, slot: Int): Int = prefs.getInt("quiz_${world}_$slot", -1)
    fun setQuizScore(world: Int, slot: Int, score: Int) {
        if (score > quizScore(world, slot)) prefs.edit().putInt("quiz_${world}_$slot", score).apply()
        bump()
    }

    // ---------------------------------------------------------------- daily gifts: rewards calendar

    /** Day 1..7 of the daily reward if it can be claimed today, else 0. Missing a day doesn't reset it. */
    fun dailyGiftDay(): Int {
        if (prefs.getLong("daily_last", -1) == today()) return 0
        return prefs.getInt("daily_count", 0) % 7 + 1
    }
    val dailyStreakDay: Int get() = prefs.getInt("daily_count", 0) % 7

    fun claimDaily() {
        prefs.edit().putLong("daily_last", today()).putInt("daily_count", prefs.getInt("daily_count", 0) + 1).apply()
        bump()
    }

    // ---------------------------------------------------------------- daily gifts: spin

    private fun dayInt(key: String): Int = if (prefs.getLong("${key}_day", -1) == today()) prefs.getInt(key, 0) else 0
    private fun setDayInt(key: String, v: Int) { prefs.edit().putLong("${key}_day", today()).putInt(key, v).apply(); bump() }

    val freeSpinReady: Boolean get() = dayInt("spins_free") == 0
    val adSpinReady: Boolean get() = dayInt("spins_ad") == 0
    val spinsBought: Int get() = dayInt("spins_bought")
    fun useFreeSpin() = setDayInt("spins_free", 1)
    fun useAdSpin() = setDayInt("spins_ad", 1)
    fun buySpin(): Boolean {
        val n = spinsBought
        if (n >= SPIN_PRICES.size || !spend(SPIN_PRICES[n])) return false
        setDayInt("spins_bought", n + 1)
        return true
    }

    // ---------------------------------------------------------------- daily gifts: challenges

    fun challengeProgress(i: Int): Int = dayInt("ch_$i")
    fun addChallenge(i: Int, n: Int = 1) = setDayInt("ch_$i", challengeProgress(i) + n)
    fun challengeClaimed(i: Int): Boolean = dayInt("chc_$i") == 1
    fun claimChallenge(i: Int) = setDayInt("chc_$i", 1)
    val challengeChestClaimed: Boolean get() = dayInt("ch_chest") == 1
    fun claimChallengeChest() = setDayInt("ch_chest", 1)

    // ---------------------------------------------------------------- ads

    val adsToday: Int get() = dayInt("ads")
    fun adWatched() = setDayInt("ads", adsToday + 1)
    fun spotUses(spot: String): Int = dayInt("ad_$spot")
    fun spotUsed(spot: String) = setDayInt("ad_$spot", spotUses(spot) + 1)
    val levelsSinceAd: Int get() = prefs.getInt("levels_since_ad", 0)
    fun resetLevelsSinceAd() = prefs.edit().putInt("levels_since_ad", 0).apply()
    val daysPlayed: Long get() = today() - prefs.getLong("first_day", today())
    var lastClose: Long
        get() = prefs.getLong("last_close", 0)
        set(v) = prefs.edit().putLong("last_close", v).apply()
    val appOpenAdsToday: Int get() = dayInt("app_open_ads")
    fun appOpenAdShown() = setDayInt("app_open_ads", appOpenAdsToday + 1)

    // ---------------------------------------------------------------- shop purchases (test mode until Play Billing is set up)

    fun bought(item: String): Boolean = prefs.getBoolean("iap_$item", false)
    fun markBought(item: String) { prefs.edit().putBoolean("iap_$item", true).apply(); bump() }

    // ---------------------------------------------------------------- rate us

    var lastRateAsk: Long
        get() = prefs.getLong("rate_ask", -100)
        set(v) = prefs.edit().putLong("rate_ask", v).apply()
    fun canAskRating(): Boolean = today() - lastRateAsk >= 30

    companion object {
        const val STARTING_COINS = 200
        /** Continue after running out of moves. */
        const val CONTINUE_PRICE = 300
        const val NEW_WORD_COINS = 2
        val SPIN_PRICES = listOf(50, 100, 150)
        const val DAILY_AD_CAP = 18

        /** Moves the coin continue gives, growing with fails on this level. */
        fun continueMoves(fails: Int): Int = when {
            fails >= 8 -> 30
            fails >= 5 -> 15
            fails >= 3 -> 10
            else -> 5
        }
    }
}
