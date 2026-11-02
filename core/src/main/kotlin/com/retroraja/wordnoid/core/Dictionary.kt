package com.retroraja.wordnoid.core

/** A set of lowercase words. */
interface WordSet {
    fun contains(word: String): Boolean
    val size: Int
}

class HashWordSet(private val set: Set<String>) : WordSet {
    override fun contains(word: String) = set.contains(word)
    override val size get() = set.size
}

/**
 * Words kept as one block of bytes (one word per line, sorted A to Z), searched by halving.
 * Much faster to load on a phone than building a set of 280,000 strings.
 */
class SortedWordSet(private val bytes: ByteArray) : WordSet {
    private val starts: IntArray
    private val ends: IntArray

    init {
        var count = 0
        var lineStart = 0
        for (i in 0..bytes.size) {
            if (i == bytes.size || bytes[i] == '\n'.code.toByte()) {
                if (i - lineStart > 0) count++
                lineStart = i + 1
            }
        }
        starts = IntArray(count)
        ends = IntArray(count)
        var n = 0
        lineStart = 0
        for (i in 0..bytes.size) {
            if (i == bytes.size || bytes[i] == '\n'.code.toByte()) {
                var end = i
                if (end > lineStart && bytes[end - 1] == '\r'.code.toByte()) end--
                if (end - lineStart > 0) { starts[n] = lineStart; ends[n] = end; n++ }
                lineStart = i + 1
            }
        }
    }

    override val size: Int get() = starts.size

    /** True if the words are in A to Z byte order (needed for searching). */
    fun isSorted(): Boolean = (1 until size).all { compareAt(it - 1, it) < 0 }

    private fun compareAt(a: Int, b: Int): Int {
        val la = ends[a] - starts[a]
        val lb = ends[b] - starts[b]
        for (k in 0 until minOf(la, lb)) {
            val d = bytes[starts[a] + k] - bytes[starts[b] + k]
            if (d != 0) return d
        }
        return la - lb
    }

    override fun contains(word: String): Boolean {
        var lo = 0
        var hi = size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val c = compareWord(mid, word)
            when {
                c < 0 -> lo = mid + 1
                c > 0 -> hi = mid - 1
                else -> return true
            }
        }
        return false
    }

    private fun compareWord(index: Int, word: String): Int {
        val start = starts[index]
        val len = ends[index] - start
        for (k in 0 until minOf(len, word.length)) {
            val d = bytes[start + k].toInt() - word[k].code
            if (d != 0) return d
        }
        return len - word.length
    }
}

/**
 * The word list. The game accepts only everyday words (play_words.txt), so [common] is normally the same list.
 */
class Dictionary(private val words: WordSet, private val common: WordSet) {

    fun isWord(word: String): Boolean = word.length >= MIN_LENGTH && words.contains(word)

    fun isCommon(word: String): Boolean = common.contains(word)

    val size: Int get() = words.size

    /** The same dictionary plus [extra] words (lowercase), e.g. names during a Names goal. */
    fun withExtra(extra: Set<String>): Dictionary {
        if (extra.isEmpty()) return this
        val plus = object : WordSet {
            override fun contains(word: String) = word in extra || words.contains(word)
            override val size get() = words.size + extra.size
        }
        return Dictionary(plus, plus)
    }

    companion object {
        const val MIN_LENGTH = 3

        /** Builds a dictionary from the lines of words.txt and common_words.txt. */
        fun fromLines(wordLines: Sequence<String>, commonLines: Sequence<String>): Dictionary {
            val words = HashSet<String>(400_000)
            wordLines.forEach { line -> clean(line)?.let { words.add(it) } }
            val common = HashSet<String>(80_000)
            commonLines.forEach { line -> clean(line)?.let { if (it in words) common.add(it) } }
            return Dictionary(HashWordSet(words), HashWordSet(common))
        }

        /** One everyday list (play_words.txt) used for everything. */
        fun fromLines(lines: Sequence<String>): Dictionary {
            val words = HashSet<String>(60_000)
            lines.forEach { line -> clean(line)?.let { words.add(it) } }
            val set = HashWordSet(words)
            return Dictionary(set, set)
        }

        fun fromSortedBytes(bytes: ByteArray): Dictionary {
            val set = SortedWordSet(bytes)
            return Dictionary(set, set)
        }

        /** Fast loading from the raw bytes of the sorted word files. */
        fun fromSortedBytes(words: ByteArray, common: ByteArray): Dictionary =
            Dictionary(SortedWordSet(words), SortedWordSet(common))

        private fun clean(line: String): String? {
            val w = line.trim().lowercase()
            return if (w.length >= MIN_LENGTH && w.all { it in 'a'..'z' }) w else null
        }
    }
}
