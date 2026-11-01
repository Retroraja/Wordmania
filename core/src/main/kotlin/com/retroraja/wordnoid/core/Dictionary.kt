package com.retroraja.wordnoid.core

/**
 * The word lists.
 * [words]: every word the game accepts (lowercase).
 * [common]: everyday words. Used when the game suggests words (hints) and by the level maker's computer player.
 */
class Dictionary(private val words: Set<String>, private val common: Set<String>) {

    fun isWord(word: String): Boolean = word.length >= MIN_LENGTH && words.contains(word)

    fun isCommon(word: String): Boolean = common.contains(word)

    val size: Int get() = words.size

    companion object {
        const val MIN_LENGTH = 3

        /** Builds a dictionary from the lines of words.txt and common_words.txt. */
        fun fromLines(wordLines: Sequence<String>, commonLines: Sequence<String>): Dictionary {
            val words = HashSet<String>(400_000)
            wordLines.forEach { line -> clean(line)?.let { words.add(it) } }
            val common = HashSet<String>(80_000)
            commonLines.forEach { line -> clean(line)?.let { if (it in words) common.add(it) } }
            return Dictionary(words, common)
        }

        private fun clean(line: String): String? {
            val w = line.trim().lowercase()
            return if (w.length >= MIN_LENGTH && w.all { it in 'a'..'z' }) w else null
        }
    }
}
