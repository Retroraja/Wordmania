package com.retroraja.wordnoid.levelmaker

import com.retroraja.wordnoid.core.*
import java.io.File

/**
 * Level maker. Run from the repo root:  ./gradlew :levelmaker:run
 *
 * For each level recipe it makes many random boards, throws away boards that already contain a word,
 * then lets computer players try each board. A board is kept only if:
 *  - enough swaps make an everyday word straight away (so the start never feels stuck), and
 *  - a casual computer player (who misses words and doesn't plan) still wins often enough.
 * The result is written to core/src/main/kotlin/.../GeneratedLevels.kt.
 */

data class Recipe(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val moves: Int,
    val goal: Goal,
    /** At least this many swaps must make an everyday word at the start. */
    val minEasySwaps: Int,
    /** The casual player must win at least this share of games. */
    val minCasualWinRate: Double,
    /** ...and at most this share (so later levels aren't too easy). */
    val maxCasualWinRate: Double = 1.0,
)

val RECIPES = listOf(
    Recipe(1, 6, 4, 10, Goal.Words(2), minEasySwaps = 7, minCasualWinRate = 0.98),
    Recipe(2, 6, 4, 10, Goal.Words(3), minEasySwaps = 6, minCasualWinRate = 0.93),
    Recipe(3, 7, 5, 12, Goal.Words(4), minEasySwaps = 6, minCasualWinRate = 0.88),
    Recipe(4, 7, 5, 12, Goal.WordsWithLong(3), minEasySwaps = 5, minCasualWinRate = 0.80),
    Recipe(5, 8, 5, 15, Goal.Words(5), minEasySwaps = 5, minCasualWinRate = 0.78),
)

const val CASUAL_SKILL = 0.6
const val RUNS = 60

fun main(args: Array<String>) {
    val assets = File("app/src/main/assets")
    val dict = Dictionary.fromLines(File(assets, "words.txt").readLines().asSequence(), File(assets, "common_words.txt").readLines().asSequence())
    println("Loaded ${dict.size} words")
    val only = args.firstOrNull()?.toIntOrNull()
    val levels = RECIPES.map { recipe ->
        if (only != null && recipe.number != only) Levels.get(recipe.number)!! else make(recipe, dict)
    }
    write(levels)
}

fun randomBoard(recipe: Recipe, rng: Rng, dict: Dictionary): List<String>? {
    val cells = (0 until recipe.rows).flatMap { r -> (0 until recipe.cols).map { c -> r to c } }
    // About 38% vowels, placed at random.
    val vowelCount = Math.round(cells.size * 0.38).toInt()
    val order = cells.toMutableList()
    for (i in order.indices.reversed()) { val j = rng.nextInt(i + 1); val t = order[i]; order[i] = order[j]; order[j] = t }
    val vowelCells = order.take(vowelCount).toSet()
    val grid = Array(recipe.rows) { CharArray(recipe.cols) }
    for ((r, c) in cells) grid[r][c] = if ((r to c) in vowelCells) Letters.pick(rng, vowelOnly = true) else Letters.pickConsonant(rng)
    // Repair: change letters (vowel for vowel, consonant for consonant) until there are no words on the board.
    repeat(300) {
        val rows = grid.map { String(it) }
        val words = WordFinder.findAll(Board.fromRows(rows, IdCounter()), dict)
        if (words.isEmpty()) return rows
        val p = words.first().cells.let { it[rng.nextInt(it.size)] }
        grid[p.row][p.col] = if (Letters.isVowel(grid[p.row][p.col])) Letters.pick(rng, vowelOnly = true) else Letters.pickConsonant(rng)
    }
    return null
}

fun make(recipe: Recipe, dict: Dictionary): Level {
    val rng = Rng(recipe.number * 7919L)
    var tried = 0
    var evaluated = 0
    while (true) {
        tried++
        val rows = randomBoard(recipe, rng, dict) ?: continue
        // Keep hard letters rare, so the board looks friendly.
        val hard = rows.sumOf { r -> r.count { it in "JKVWXYZ" } }
        if (hard > recipe.cols * recipe.rows / 12) continue
        val seed = recipe.number * 100_000L + tried
        var level = Level(recipe.number, recipe.cols, recipe.rows, recipe.moves, recipe.goal, rows, seed)
        val game = Game(level, dict)
        val easySwaps = game.board.neighbourPairs().count { (a, b) -> game.wordsForSwap(a, b).any { it.common } }
        if (easySwaps < recipe.minEasySwaps) continue
        evaluated++
        val strong = Solver.play(level, dict, 1.0, Rng(1))
        if (!strong.won) continue
        val casual = Solver.winRate(level, dict, CASUAL_SKILL, RUNS, seed = 99)
        print("  level ${recipe.number}: board ${rows.joinToString("/")} easySwaps=$easySwaps casualWin=${"%.2f".format(casual)} strongMovesLeft=${strong.movesLeft}")
        if (casual < recipe.minCasualWinRate || casual > recipe.maxCasualWinRate) { println("  -> no"); continue }
        println("  -> KEEP")
        if (recipe.number == 1) {
            // Tutorial: show the swap that makes the longest everyday word.
            val tut = game.board.neighbourPairs()
                .map { it to game.wordsForSwap(it.first, it.second).filter { w -> w.common } }
                .filter { it.second.isNotEmpty() }
                .maxBy { it.second.sumOf { w -> w.length } }.first
            level = level.copy(tutorialSwap = tut)
        }
        println("Level ${recipe.number} done after $tried boards ($evaluated played)")
        return level
    }
}

fun goalCode(g: Goal): String = when (g) {
    is Goal.Words -> "Goal.Words(${g.count})"
    is Goal.WordsWithLong -> "Goal.WordsWithLong(count = ${g.count}, minLength = ${g.minLength}, longCount = ${g.longCount})"
}

fun write(levels: List<Level>) {
    val sb = StringBuilder()
    sb.appendLine("package com.retroraja.wordnoid.core")
    sb.appendLine()
    sb.appendLine("// Made by the level maker (levelmaker/). Run ./gradlew :levelmaker:run to remake. Small hand edits are fine;")
    sb.appendLine("// LevelsTest checks every level still starts with no words and can be won.")
    sb.appendLine("object GeneratedLevels {")
    sb.appendLine("    val all: List<Level> = listOf(")
    for (l in levels) {
        sb.appendLine("        Level(")
        sb.appendLine("            number = ${l.number}, cols = ${l.cols}, rows = ${l.rows}, moves = ${l.moves},")
        sb.appendLine("            goal = ${goalCode(l.goal)},")
        sb.appendLine("            start = listOf(${l.start.joinToString(", ") { "\"$it\"" }}),")
        sb.append("            seed = ${l.seed}L")
        l.tutorialSwap?.let { (a, b) -> sb.append(",\n            tutorialSwap = Pos(${a.row}, ${a.col}) to Pos(${b.row}, ${b.col})") }
        sb.appendLine(",")
        sb.appendLine("        ),")
    }
    sb.appendLine("    )")
    sb.appendLine("}")
    val out = File("core/src/main/kotlin/com/retroraja/wordnoid/core/GeneratedLevels.kt")
    out.writeText(sb.toString())
    println("Wrote ${levels.size} levels to $out")
}
