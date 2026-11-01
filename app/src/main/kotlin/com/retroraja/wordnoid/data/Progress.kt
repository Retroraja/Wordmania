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
 * Everything saved on the phone: levels, stars, coins, boosters, Power Meter, Wordbook, settings.
 * Uses a new save name, so progress from the first test version starts fresh.
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
    val boosters = mutableStateMapOf<Booster, Int>().apply {
        for (b in Booster.entries) put(b, prefs.getInt("b_${b.name}", 0))
    }
    private val words: MutableSet<String> = HashSet(prefs.getStringSet("wordbook", emptySet()) ?: emptySet())
    var wordbookSize by mutableIntStateOf(words.size); private set

    fun stars(level: Int): Int = prefs.getInt("stars_$level", 0)
    val totalStars: Int get() = (1..unlocked).sumOf { stars(it) }
    fun fails(level: Int): Int = prefs.getInt("fails_$level", 0)
    fun seen(key: String): Boolean = prefs.getBoolean("seen_$key", false)
    fun markSeen(key: String) = prefs.edit().putBoolean("seen_$key", true).apply()

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

    fun levelWon(level: Int, stars: Int) {
        val e = prefs.edit()
        if (stars > stars(level)) e.putInt("stars_$level", stars)
        if (level + 1 > unlocked) { unlocked = level + 1; e.putInt("unlocked", unlocked) }
        e.putInt("fails_$level", 0)
        e.apply()
    }

    fun levelLost(level: Int) {
        prefs.edit().putInt("fails_$level", fails(level) + 1).apply()
    }

    fun toggleMusic() { music = !music; prefs.edit().putBoolean("music", music).apply() }
    fun toggleSound() { sound = !sound; prefs.edit().putBoolean("sound", sound).apply() }
    fun toggleVibration() { vibration = !vibration; prefs.edit().putBoolean("vibration", vibration).apply() }

    // ---------------------------------------------------------------- daily gift and star chest

    /** Day 1..7 of the daily gift if it can be claimed today, else 0. Missing a day doesn't reset it. */
    fun dailyGiftDay(): Int {
        val today = LocalDate.now().toEpochDay()
        if (prefs.getLong("daily_last", -1) == today) return 0
        return prefs.getInt("daily_count", 0) % 7 + 1
    }

    fun claimDaily() {
        prefs.edit()
            .putLong("daily_last", LocalDate.now().toEpochDay())
            .putInt("daily_count", prefs.getInt("daily_count", 0) + 1)
            .apply()
    }

    /** Star chests opened so far. A chest is ready for every 10 stars. */
    val chestsOpened: Int get() = prefs.getInt("chests", 0)
    val chestReady: Boolean get() = totalStars / STARS_PER_CHEST > chestsOpened
    fun openChest() = prefs.edit().putInt("chests", chestsOpened + 1).apply()
    fun starsToNextChest(): Int = STARS_PER_CHEST - totalStars % STARS_PER_CHEST

    companion object {
        const val STARTING_COINS = 200
        const val CONTINUE_PRICE = 150
        const val STARS_PER_CHEST = 10
        const val NEW_WORD_COINS = 5
        val MILESTONES = setOf(10, 20, 30, 40, 50)

        /** Moves the 150-coin continue gives, growing with fails on this level (Gardenscapes style). */
        fun continueMoves(fails: Int): Int = when {
            fails >= 8 -> 30
            fails >= 5 -> 15
            fails >= 3 -> 10
            else -> 5
        }
    }
}
