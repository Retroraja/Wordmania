package com.retroraja.wordnoid.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Firebase Analytics + Crashlytics (first live release). Only game events, no names or emails:
 * which levels players reach, win and lose, boosters used, ads shown, daily gifts and mini games.
 * Everything is wrapped so a Firebase problem can never stop the game.
 */
object Analytics {
    private var fa: FirebaseAnalytics? = null

    fun start(context: Context) {
        runCatching {
            fa = FirebaseAnalytics.getInstance(context)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
    }

    /** The highest level the player has opened (so crash reports and analytics show where players are). */
    fun setLevel(level: Int) {
        runCatching {
            fa?.setUserProperty("max_level", level.toString())
            FirebaseCrashlytics.getInstance().setCustomKey("max_level", level)
        }
    }

    private fun log(name: String, vararg params: Pair<String, Any>) {
        runCatching {
            val b = Bundle()
            for ((k, v) in params) when (v) {
                is Int -> b.putLong(k, v.toLong())
                is Long -> b.putLong(k, v)
                is Double -> b.putDouble(k, v)
                else -> b.putString(k, v.toString())
            }
            fa?.logEvent(name, b)
            FirebaseCrashlytics.getInstance().log("$name ${params.joinToString { "${it.first}=${it.second}" }}")
        }
    }

    fun levelStart(level: Int, boss: Boolean) = log(FirebaseAnalytics.Event.LEVEL_START, FirebaseAnalytics.Param.LEVEL_NAME to "L$level", "level" to level, "boss" to if (boss) 1 else 0)
    fun levelWin(level: Int, stars: Int, movesLeft: Int) = log(FirebaseAnalytics.Event.LEVEL_END, FirebaseAnalytics.Param.LEVEL_NAME to "L$level", "level" to level, FirebaseAnalytics.Param.SUCCESS to 1, "stars" to stars, "moves_left" to movesLeft)
    fun levelLose(level: Int, fails: Int) = log("level_fail", "level" to level, "fails" to fails)
    fun levelQuit(level: Int) = log("level_quit", "level" to level)
    fun continued(level: Int, how: String) = log("level_continue", "level" to level, "how" to how)
    fun booster(name: String, level: Int) = log("booster_used", "booster" to name, "level" to level)
    fun ad(kind: String, where: String) = log("ad_show", "kind" to kind, "where" to where)
    fun dailyGift(day: Int) = log("daily_gift", "day" to day)
    fun miniGame(id: String, coins: Int) = log("mini_game", "game" to id, "coins" to coins)
}
