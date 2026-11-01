package com.retroraja.wordnoid.core

import java.io.File

object TestDict {
    val dict: Dictionary by lazy {
        val dir = File(System.getProperty("wordnoid.assets") ?: "app/src/main/assets")
        Dictionary.fromLines(File(dir, "play_words.txt").readLines().asSequence())
    }
}
