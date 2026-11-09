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
    var voice by mutableStateOf(prefs.getBoolean("voice", true)); private set
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

    /** Wins in a row (Update 1). Gives free boosters at the next level start. */
    var streak by mutableIntStateOf(prefs.getInt("streak", 0)); private set
    /** Level chest: 0..4 slots filled (one per win). The 5th win opens it. */
    var chest by mutableIntStateOf(prefs.getInt("chest", 0)); private set

    init {
        if (!prefs.contains("first_day")) prefs.edit().putLong("first_day", today()).apply()
        upgradeSave()
    }

    /**
     * Update 1 save upgrade (runs once). Some boosters now arrive earlier (Double Points L7, Free Move L13, Letter Rain L49).
     * A player who is already past the new level gets that booster plus one to try. Nothing owned is ever removed.
     * Streak and chest start at 0 (new keys).
     */
    private fun upgradeSave() {
        val from = prefs.getInt("save_version", 1)
        if (from >= SAVE_VERSION) return
        val e = prefs.edit()
        if (from < 3) upgradeTo11(e)
        if (from >= 2) { e.putInt("save_version", SAVE_VERSION).apply(); return }
        val oldLevels = mapOf(Booster.FREE_MOVE to 49, Booster.DOUBLE to 52, Booster.LETTER_RAIN to 55)
        if (prefs.contains("unlocked")) for ((b, old) in oldLevels) {
            if (unlocked > b.introLevel && unlocked <= old) {
                boosters[b] = count(b) + 1
                e.putInt("b_${b.name}", count(b))
                e.putBoolean("seen_intro_b_${b.name}", true)
            }
        }
        e.putInt("save_version", SAVE_VERSION).apply()
    }

    /**
     * Update 1.1 save upgrade (runs once, for every player wherever they are).
     * The Spin wheel and Daily Challenges are gone: any of today's challenge coins that were earned but not claimed
     * (and the bonus chest, if all 3 were done) are paid now, so nothing is lost. Levels, coins, boosters, town and
     * streak stay as they are. The 7-day calendar keeps its day.
     */
    private fun upgradeTo11(e: android.content.SharedPreferences.Editor) {
        if (!prefs.contains("unlocked")) return
        var gift = 0
        val list = Daily.today()
        list.forEachIndexed { i, c -> if (challengeProgress(i) >= c.target && !challengeClaimed(i)) gift += c.coins }
        if (list.indices.all { challengeProgress(it) >= list[it].target } && !challengeChestClaimed) { gift += Daily.CHEST_COINS; e.putInt("bricks_bonus", prefs.getInt("bricks_bonus", 0) + 1) }
        if (gift > 0) { coins += gift; e.putInt("coins", coins); e.putInt("upgrade11_gift", gift) }
    }

    /** Coins paid by the Update 1.1 save upgrade (shown once on the welcome card), 0 if none. */
    val upgradeGift: Int get() = prefs.getInt("upgrade11_gift", 0)

    private fun bump() { version++ }
    private fun today(): Long = LocalDate.now().toEpochDay()

    // ---------------------------------------------------------------- Update 1.1: mini games and word ideas

    /** Today's free play of a mini game was used. */
    fun miniFreeUsed(id: String): Boolean = dayInt("mg_free_$id") == 1
    fun useMiniFree(id: String) = setDayInt("mg_free_$id", 1)
    /** The one extra play a day for a video (any one game). */
    val miniAdUsed: Boolean get() = dayInt("mg_ad") == 1
    fun useMiniAd() = setDayInt("mg_ad", 1)
    fun miniBest(id: String): Int { @Suppress("UNUSED_VARIABLE") val v = version; return prefs.getInt("mg_best_$id", 0) }
    fun setMiniBest(id: String, coins: Int) { if (coins > miniBest(id)) { prefs.edit().putInt("mg_best_$id", coins).apply(); bump() } }
    fun miniGamesFreeReady(): Boolean = MiniGames.LAUNCHED.any { !miniFreeUsed(it.id) }
    /** Extra pencil hints bought with videos today, per game. */
    fun miniHintsUsed(id: String): Int = dayInt("mg_hint_$id")
    fun useMiniHint(id: String) = setDayInt("mg_hint_$id", miniHintsUsed(id) + 1)

    /** Word ideas sent today (3 a day per phone). */
    val suggestionsToday: Int get() = dayInt("suggest")
    fun suggestionSent() = setDayInt("suggest", suggestionsToday + 1)
    /** Word ideas waiting to be sent (no internet when the player sent them). */
    var pendingSuggestions: Set<String>
        get() = prefs.getStringSet("suggest_queue", emptySet()) ?: emptySet()
        set(v) { prefs.edit().putStringSet("suggest_queue", v).apply() }

    // ---------------------------------------------------------------- levels and bricks

    /** Letter Bricks won on a level (1 to 3). Build 1 stars are kept as bricks. */
    fun bricks(level: Int): Int = prefs.getInt("stars_$level", 0)
    val bricksEarned: Int get() = (1..unlocked).sumOf { bricks(it) } + prefs.getInt("bricks_bonus", 0)
    val bricksToSpend: Int get() = (bricksEarned - bricksSpent).coerceAtLeast(0)
    fun addBonusBricks(n: Int) { prefs.edit().putInt("bricks_bonus", prefs.getInt("bricks_bonus", 0) + n).apply(); bump() }
    fun worldBricks(world: Int): Int = (world * 10 + 1..world * 10 + 10).sumOf { bricks(it) }

    fun fails(level: Int): Int = prefs.getInt("fails_$level", 0)
    fun seen(key: String): Boolean { @Suppress("UNUSED_VARIABLE") val v = version; return prefs.getBoolean("seen_$key", false) }
    fun markSeen(key: String) { prefs.edit().putBoolean("seen_$key", true).apply(); bump() }

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

    // ---------------------------------------------------------------- win streak and level chest (Update 1)

    fun streakWin() { streak++; prefs.edit().putInt("streak", streak).apply(); bump() }
    /** A loss (or quitting a level) breaks the streak. The level chest is never lost. */
    fun streakLost() { if (streak != 0) { streak = 0; prefs.edit().putInt("streak", 0).apply(); bump() } }
    /** Once a day a video can keep the streak after a loss. */
    val streakSaveReady: Boolean get() = dayInt("streak_save") == 0
    fun useStreakSave() = setDayInt("streak_save", 1)

    /** Fills one chest slot. True when this win filled the 5th slot (the chest opens and starts again). */
    fun chestWin(): Boolean {
        val full = chest + 1 >= CHEST_SLOTS
        chest = if (full) 0 else chest + 1
        prefs.edit().putInt("chest", chest).apply()
        bump()
        return full
    }

    /** Win screens where a town task was ready but the player went on (gentle gate after 3). */
    var buildSkips: Int
        get() = prefs.getInt("build_skips", 0)
        set(v) { prefs.edit().putInt("build_skips", v).apply() }

    // ---------------------------------------------------------------- boosters and coins

    /** Free first use of a booster during its tutorial. */
    fun hasFreeUse(b: Booster): Boolean { @Suppress("UNUSED_VARIABLE") val v = version; return prefs.getBoolean("free_${b.name}", false) }
    fun setFreeUse(b: Booster, on: Boolean) { prefs.edit().putBoolean("free_${b.name}", on).apply(); bump() }

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
    /** Level 3 teaches the meter: it starts almost full so the first word fills it. */
    fun primeMeter(n: Int) { if (meter < n) { meter = n; prefs.edit().putInt("meter", meter).apply() } }

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
    fun toggleVoice() { voice = !voice; prefs.edit().putBoolean("voice", voice).apply() }
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

    /** Reads [version] so any screen showing daily values redraws when they change. */
    private fun dayInt(key: String): Int { @Suppress("UNUSED_VARIABLE") val v = version; return if (prefs.getLong("${key}_day", -1) == today()) prefs.getInt(key, 0) else 0 }
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
        const val SAVE_VERSION = 3
        const val CHEST_SLOTS = 5
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
