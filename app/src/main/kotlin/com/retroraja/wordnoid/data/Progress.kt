package com.retroraja.wordnoid.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Everything saved on the phone. Android's backup copies it to the player's Google account. */
class Progress(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wordnoid", Context.MODE_PRIVATE)

    var unlocked by mutableIntStateOf(prefs.getInt("unlocked", 1)); private set
    var coins by mutableIntStateOf(prefs.getInt("coins", STARTING_COINS)); private set
    var sound by mutableStateOf(prefs.getBoolean("sound", true)); private set
    var vibration by mutableStateOf(prefs.getBoolean("vibration", true)); private set
    private var starsVersion by mutableIntStateOf(0)

    fun stars(level: Int): Int { starsVersion; return prefs.getInt("stars_$level", 0) }
    fun fails(level: Int): Int = prefs.getInt("fails_$level", 0)
    fun tutorialSeen(name: String): Boolean = prefs.getBoolean("tutorial_$name", false)

    fun levelWon(level: Int, stars: Int, coinsEarned: Int, lastLevel: Int) {
        val e = prefs.edit()
        if (stars > stars(level)) e.putInt("stars_$level", stars)
        if (level + 1 > unlocked && level + 1 <= lastLevel) { unlocked = level + 1; e.putInt("unlocked", unlocked) }
        if (level == lastLevel && unlocked < lastLevel) { unlocked = lastLevel; e.putInt("unlocked", unlocked) }
        e.putInt("fails_$level", 0)
        coins += coinsEarned
        e.putInt("coins", coins)
        e.apply()
        starsVersion++
    }

    fun levelLost(level: Int, coinsEarned: Int) {
        coins += coinsEarned
        prefs.edit().putInt("fails_$level", fails(level) + 1).putInt("coins", coins).apply()
    }

    fun spend(amount: Int): Boolean {
        if (coins < amount) return false
        coins -= amount
        prefs.edit().putInt("coins", coins).apply()
        return true
    }

    fun markTutorialSeen(name: String) = prefs.edit().putBoolean("tutorial_$name", true).apply()
    fun toggleSound() { sound = !sound; prefs.edit().putBoolean("sound", sound).apply() }
    fun toggleVibration() { vibration = !vibration; prefs.edit().putBoolean("vibration", vibration).apply() }

    companion object {
        const val STARTING_COINS = 60
        const val EXTRA_MOVES_PRICE = 30
        const val EXTRA_MOVES = 3
        /** After this many losses in a row on a level, the next try starts with bonus moves. */
        const val HELP_AFTER_FAILS = 3
    }
}
