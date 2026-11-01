package com.retroraja.wordnoid.core

// Made by the level maker (levelmaker/). Run ./gradlew :levelmaker:run to remake. Small hand edits are fine;
// LevelsTest checks every level still starts with no words and can be won.
object GeneratedLevels {
    val all: List<Level> = listOf(
        Level(
            number = 1, cols = 6, rows = 4, moves = 10,
            goal = Goal.Words(2),
            start = listOf("SFCICI", "TEIYCC", "RJRIIC", "GEHLOI"),
            seed = 100011L,
            tutorialSwap = Pos(2, 1) to Pos(3, 1),
        ),
        Level(
            number = 2, cols = 6, rows = 4, moves = 10,
            goal = Goal.Words(3),
            start = listOf("OYAUIS", "AJHORU", "BNLHUF", "HSBHAC"),
            seed = 200004L,
        ),
        Level(
            number = 3, cols = 7, rows = 5, moves = 12,
            goal = Goal.Words(4),
            start = listOf("IUUHGIA", "EBZLILD", "THRRIUN", "WPNFALG", "UBUIDNS"),
            seed = 300596L,
        ),
        Level(
            number = 4, cols = 7, rows = 5, moves = 12,
            goal = Goal.WordsWithLong(count = 3, minLength = 4, longCount = 1),
            start = listOf("EOAEHAC", "PTCGDLA", "FSMNRGA", "IGNMDOK", "AHBUAKI"),
            seed = 400028L,
        ),
        Level(
            number = 5, cols = 8, rows = 5, moves = 15,
            goal = Goal.Words(5),
            start = listOf("AMFIIMEO", "ICCGRNCG", "GIICCHLU", "HUZODFUW", "GCICOKSO"),
            seed = 500056L,
        ),
    )
}
