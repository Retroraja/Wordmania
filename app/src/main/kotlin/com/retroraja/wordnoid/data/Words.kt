package com.retroraja.wordnoid.data

import android.content.Context
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
                val assets = context.applicationContext.assets
                val words = assets.open("words.txt").bufferedReader().use { r -> r.readLines() }
                val common = assets.open("common_words.txt").bufferedReader().use { r -> r.readLines() }
                Dictionary.fromLines(words.asSequence(), common.asSequence()).also { dict = it }
            }
        }
    }
}
