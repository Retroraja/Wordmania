package com.retroraja.wordnoid.core

/** How often each letter appears on the board. Common letters appear more; Q is left out for now. */
object Letters {
    val VOWELS = setOf('A', 'E', 'I', 'O', 'U')

    private val WEIGHTS: List<Pair<Char, Int>> = listOf(
        'A' to 18, 'B' to 4, 'C' to 6, 'D' to 8, 'E' to 24, 'F' to 4, 'G' to 6, 'H' to 6, 'I' to 16,
        'J' to 1, 'K' to 2, 'L' to 8, 'M' to 5, 'N' to 12, 'O' to 15, 'P' to 5, 'R' to 12, 'S' to 12,
        'T' to 14, 'U' to 7, 'V' to 2, 'W' to 3, 'X' to 1, 'Y' to 4, 'Z' to 1,
    )
    private val TOTAL = WEIGHTS.sumOf { it.second }
    private val VOWEL_WEIGHTS = WEIGHTS.filter { it.first in VOWELS }
    private val VOWEL_TOTAL = VOWEL_WEIGHTS.sumOf { it.second }
    private val CONSONANT_WEIGHTS = WEIGHTS.filter { it.first !in VOWELS }
    private val CONSONANT_TOTAL = CONSONANT_WEIGHTS.sumOf { it.second }

    /** Fewer vowels than this share of the board triggers "vowel rescue". */
    const val MIN_VOWEL_SHARE = 0.34

    fun isVowel(c: Char): Boolean = c in VOWELS

    fun pick(rng: Rng, vowelOnly: Boolean = false): Char =
        if (vowelOnly) pickFrom(rng, VOWEL_WEIGHTS, VOWEL_TOTAL) else pickFrom(rng, WEIGHTS, TOTAL)

    fun pickConsonant(rng: Rng): Char = pickFrom(rng, CONSONANT_WEIGHTS, CONSONANT_TOTAL)

    private fun pickFrom(rng: Rng, list: List<Pair<Char, Int>>, total: Int): Char {
        var r = rng.nextInt(total)
        for ((c, w) in list) {
            if (r < w) return c
            r -= w
        }
        return list.last().first
    }
}
