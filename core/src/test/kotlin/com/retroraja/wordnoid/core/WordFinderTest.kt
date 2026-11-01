package com.retroraja.wordnoid.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WordFinderTest {
    private val dict = TestDict.dict
    private fun board(vararg rows: String) = Board.fromRows(rows.toList(), IdCounter())

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
