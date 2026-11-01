package com.retroraja.wordnoid.data

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.retroraja.wordnoid.core.Dictionary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the everyday word list once and keeps it in memory. */
object Words {
    @Volatile private var dict: Dictionary? = null

    suspend fun load(context: Context): Dictionary {
        dict?.let { return it }
        return withContext(Dispatchers.IO) {
            dict ?: run {
                val start = SystemClock.elapsedRealtime()
                val bytes = context.applicationContext.assets.open("play_words.txt").use { it.readBytes() }
                Dictionary.fromSortedBytes(bytes).also {
                    dict = it
                    Log.i("Wordnoid", "Loaded ${it.size} words in ${SystemClock.elapsedRealtime() - start} ms")
                }
            }
        }
    }
}
