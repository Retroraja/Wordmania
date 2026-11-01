package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordFinderTest {
    private val dict = TestDict.dict
    private fun board(vararg rows: String) = Board.fromRows(rows.toList(), IdCounter())

    @Test fun wordFileIsSortedAndEveryday() {
        val dir = java.io.File(System.getProperty("wordnoid.assets") ?: "app/src/main/assets")
        val bytes = java.io.File(dir, "play_words.txt").readBytes()
        assertTrue(SortedWordSet(bytes).isSorted(), "play_words.txt must be sorted A-Z (LC_ALL=C)")
        val fast = Dictionary.fromSortedBytes(bytes)
        for (w in listOf("cat", "colour", "color", "zebra", "house", "rain")) assertTrue(fast.isWord(w), w)
        // Odd words that made version 1 feel random are gone.
        for (w in listOf("alw", "heo", "grs", "cer", "iff", "xqz", "catt")) assertTrue(!fast.isWord(w), w)
    }

    @Test fun onlyWordsThroughChangedTilesCount() {
        val b = board("CATQQ", "QQQQQ", "DOGQQ")
        assertEquals(listOf("CAT"), WordFinder.wordsThrough(b, listOf(Pos(0, 1)), dict).map { it.word })
        assertTrue(WordFinder.wordsThrough(b, listOf(Pos(1, 4)), dict).isEmpty())
    }

    @Test fun jokerStandsForAnyLetter() {
        val w = WordFinder.wordsThrough(board("C*TQ", "QQQQ", "QQQQ"), listOf(Pos(0, 1)), dict).single()
        assertEquals(3, w.length)
        assertTrue(w.word.startsWith("C") && w.word.endsWith("T"))
    }

    @Test fun cratesBreakWords() {
        assertTrue(WordFinder.findAll(board("CA#T", "QQQQ", "QQQQ"), dict).isEmpty())
    }

    @Test fun findsLeftToRight() {
        val words = WordFinder.findAll(board("XCATX", "QQQQQ", "QQQQQ"), dict).map { it.word }
        assertEquals(listOf("CAT"), words)
    }

    @Test fun findsRightToLeft() {
        val found = WordFinder.findAll(board("XGODX", "QQQQQ", "QQQQQ"), dict)
        assertEquals(1, found.size)
        assertTrue(found[0].word == "DOG" || found[0].word == "GOD")
    }

    @Test fun findsTopToBottomAndBottomToTop() {
        val down = WordFinder.findAll(board("CQQ", "AQQ", "TQQ"), dict).map { it.word }
        assertEquals(listOf("CAT"), down)
        val up = WordFinder.findAll(board("QQT", "QQA", "QQC"), dict).map { it.word }
        assertEquals(listOf("CAT"), up)
    }

    @Test fun longestWordWins() {
        val words = WordFinder.findAll(board("CATSQ", "QQQQQ", "QQQQQ"), dict).map { it.word }
        assertEquals(listOf("CATS"), words)
    }

    @Test fun cellsAreInReadingOrder() {
        val w = WordFinder.findAll(board("QTACQ", "QQQQQ", "QQQQQ"), dict).single()
        assertEquals("CAT", w.word)
        assertEquals(listOf(Pos(0, 3), Pos(0, 2), Pos(0, 1)), w.cells)
    }
}
