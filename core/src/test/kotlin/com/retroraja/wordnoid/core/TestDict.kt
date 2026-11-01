package com.retroraja.wordnoid.core

import java.io.File

object TestDict {
    val dict: Dictionary by lazy {
        val dir = File(System.getProperty("wordnoid.assets") ?: "app/src/main/assets")
        Dictionary.fromLines(File(dir, "words.txt").readLines().asSequence(), File(dir, "common_words.txt").readLines().asSequence())
    }
}
