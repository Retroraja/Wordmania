package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordFinderTest {
    private val dict = TestDict.dict
    private fun board(vararg rows: String) = Board.fromRows(rows.toList(), IdCounter())

    @Test fun wordFilesAreSortedForFastSearch() {
        val dir = java.io.File(System.getProperty("wordnoid.assets") ?: "app/src/main/assets")
        for (name in listOf("words.txt", "common_words.txt")) {
            val set = SortedWordSet(java.io.File(dir, name).readBytes())
            assertTrue(set.isSorted(), "$name must be sorted A-Z (LC_ALL=C)")
        }
        val fast = Dictionary.fromSortedBytes(java.io.File(dir, "words.txt").readBytes(), java.io.File(dir, "common_words.txt").readBytes())
        for (w in listOf("cat", "colour", "color", "zebra", "aardvark", "lakh")) assertTrue(fast.isWord(w), w)
        for (w in listOf("xqz", "catt", "aaaa")) assertTrue(!fast.isWord(w), w)
        assertTrue(fast.isCommon("cat"))
    }

    @Test fun findsLeftToRight() {
        val words = WordFinder.findAll(board("XCATX", "QQQQQ", "QQQQQ"), dict).map { it.word }
        assertEquals(listOf("CAT"), words)
    }

    @Test fun findsRightToLeft() {
        val found = WordFinder.findAll(board("XGODX", "QQQQQ", "QQQQQ"), dict)
        // GOD and DOG are both words; the everyday one is shown.
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
