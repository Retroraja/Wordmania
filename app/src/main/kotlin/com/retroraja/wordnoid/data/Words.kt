package com.retroraja.wordnoid.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.retroraja.wordnoid.core.Dictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the word lists from the app once and keeps them in memory. */
object Words {
    @Volatile private var dict: Dictionary? = null

    suspend fun load(context: Context): Dictionary {
        dict?.let { return it }
        return withContext(Dispatchers.IO) {
            dict ?: run {
                val start = SystemClock.elapsedRealtime()
                val assets = context.applicationContext.assets
                val words = assets.open("words.txt").use { it.readBytes() }
                val common = assets.open("common_words.txt").use { it.readBytes() }
                Dictionary.fromSortedBytes(words, common).also {
                    dict = it
                    Log.i("Wordnoid", "Loaded ${it.size} words in ${SystemClock.elapsedRealtime() - start} ms")
                }
            }
        }
    }
}
