package com.retroraja.wordnoid.levelmaker

import com.retroraja.wordnoid.core.*
import java.io.File

/**
 * Level maker. Run from the repo root:  ./gradlew :levelmaker:run   (or :levelmaker:run --args="12" for one level)
 *
 * For each recipe it makes random boards with the recipe's obstacles, then lets computer players try them:
 *  - a strong player (plans ahead) must win, so every level can be won without boosters;
 *  - the number of moves is set so a casual player (misses words, doesn't plan) wins about as often as the
 *    recipe says: almost always on tutorials, about half the time later on (that's where boosters come in);
 *  - a player who swaps at random must almost always lose (from level 6), so the player has to think.
 * The result is written to core/src/main/kotlin/.../GeneratedLevels.kt.
 */

data class Recipe(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val targets: (Placed) -> List<Objective>,
    val crates: Int = 0,
    val strongCrates: Int = 0,
    val ice: Int = 0,
    val chains: Int = 0,
    val gold: Int = 0,
    val feature: Feature? = null,
    val boss: Boolean = false,
    val bonus: Boolean = false,
    val giftRate: Double = 0.0,
    /** Letters to plant on the board (for Spell levels). */
    val plant: String = "",
    /** Casual player's win rate should be in this range. */
    val casual: ClosedFloatingPointRange<Double>,
    val tutorial: Boolean = false,
)

/** What was placed on a board, so targets can count it. */
data class Placed(val crates: Int, val ice: Int, val chains: Int, val gold: Int)

private fun words(n: Int): (Placed) -> List<Objective> = { listOf(Objective.Words(n)) }

const val CASUAL_SKILL = 0.6
const val RUNS = 40

val TUTORIAL = 0.9..1.0
val EASY = 0.75..1.0
val GENTLE = 0.6..0.85
val MEDIUM = 0.45..0.7
val HARD = 0.3..0.55
val TOUGH = 0.22..0.45
val BOSS = 0.12..0.35
val BREATHER = 0.65..0.9
val SPELL = 0.15..0.5

val RECIPES: List<Recipe> = listOf(
    Recipe(1, 6, 4, words(3), casual = TUTORIAL, tutorial = true),
    Recipe(2, 6, 4, words(4), casual = TUTORIAL, tutorial = true),
    Recipe(3, 7, 4, words(4), casual = TUTORIAL, tutorial = true),
    Recipe(4, 7, 5, { p -> listOf(Objective.Gold(p.gold)) }, gold = 3, feature = Feature.GOLD, casual = EASY),
    Recipe(5, 7, 5, { p -> listOf(Objective.Gold(p.gold), Objective.Words(5)) }, gold = 3, feature = Feature.BONUS_WORD, bonus = true, casual = EASY),
    Recipe(6, 7, 5, { p -> listOf(Objective.Crates(p.crates)) }, crates = 4, feature = Feature.CRATES, bonus = true, casual = GENTLE),
    Recipe(7, 7, 5, { p -> listOf(Objective.Crates(p.crates), Objective.Words(5)) }, crates = 5, bonus = true, casual = MEDIUM),
    Recipe(8, 7, 5, { listOf(Objective.Letter('E', 6)) }, feature = Feature.LETTERS, bonus = true, casual = MEDIUM),
    Recipe(9, 7, 5, { p -> listOf(Objective.Gold(p.gold), Objective.Crates(p.crates)) }, gold = 3, crates = 3, feature = Feature.GIFTS, giftRate = 0.05, bonus = true, casual = MEDIUM),
    Recipe(10, 8, 5, { listOf(Objective.Theme("Animals", 2), Objective.Words(5)) }, feature = Feature.THEME, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(11, 7, 5, { p -> listOf(Objective.Words(6), Objective.Crates(p.crates)) }, crates = 4, giftRate = 0.04, bonus = true, casual = GENTLE),
    Recipe(12, 7, 5, { listOf(Objective.LongWords(3)) }, feature = Feature.LONG, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(13, 8, 5, { p -> listOf(Objective.Letter('A', 6), Objective.Gold(p.gold)) }, gold = 3, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(14, 8, 5, { p -> listOf(Objective.Crates(p.crates)) }, crates = 3, strongCrates = 2, feature = Feature.DOUBLE_CRATES, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(15, 8, 5, { listOf(Objective.Theme("Food", 2), Objective.Words(6)) }, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(16, 8, 5, { p -> listOf(Objective.Gold(p.gold), Objective.Crates(p.crates)) }, gold = 4, crates = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(17, 8, 5, { listOf(Objective.Letter('S', 6), Objective.LongWords(2)) }, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(18, 8, 5, { p -> listOf(Objective.Ice(p.ice)) }, ice = 5, feature = Feature.ICE, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(19, 8, 5, { p -> listOf(Objective.Ice(p.ice), Objective.Gold(p.gold)) }, ice = 4, gold = 3, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(20, 8, 6, { p -> listOf(Objective.Crates(p.crates), Objective.Gold(p.gold)) }, crates = 3, strongCrates = 3, gold = 4, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
    Recipe(21, 7, 5, words(8), giftRate = 0.04, bonus = true, casual = BREATHER),
    Recipe(22, 8, 5, { listOf(Objective.Spell("RAIN"), Objective.Words(4)) }, plant = "RAIN", feature = Feature.SPELL, giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(23, 8, 5, { p -> listOf(Objective.Theme("Nature", 2), Objective.Ice(p.ice)) }, ice = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(24, 8, 5, { p -> listOf(Objective.Crates(p.crates), Objective.Letter('R', 5)) }, crates = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(25, 8, 5, { p -> listOf(Objective.Crates(p.crates)) }, crates = 4, strongCrates = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(26, 8, 5, { p -> listOf(Objective.Theme("Colours", 2), Objective.Gold(p.gold)) }, gold = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(27, 8, 5, { p -> listOf(Objective.Ice(p.ice), Objective.LongWords(3)) }, ice = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(28, 8, 5, { p -> listOf(Objective.Spell("STAR"), Objective.Crates(p.crates)) }, plant = "STAR", crates = 4, giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(29, 8, 6, { p -> listOf(Objective.Letter('T', 6), Objective.Gold(p.gold), Objective.Ice(p.ice)) }, gold = 4, ice = 4, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
    Recipe(30, 8, 5, { p -> listOf(Objective.Chains(p.chains)) }, chains = 5, feature = Feature.CHAINS, giftRate = 0.04, bonus = true, casual = BREATHER),
    Recipe(31, 8, 5, { p -> listOf(Objective.Gold(p.gold), Objective.Words(6)) }, gold = 6, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(32, 8, 5, { p -> listOf(Objective.Chains(p.chains), Objective.Theme("Home", 2)) }, chains = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(33, 8, 5, { p -> listOf(Objective.Letter('O', 6), Objective.Ice(p.ice)) }, ice = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(34, 8, 5, { p -> listOf(Objective.Spell("GOLD"), Objective.Gold(p.gold), Objective.Chains(p.chains)) }, plant = "GOLD", gold = 4, chains = 3, giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(35, 8, 5, { p -> listOf(Objective.Crates(p.crates), Objective.Ice(p.ice)) }, crates = 5, strongCrates = 2, ice = 4, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(36, 8, 5, { p -> listOf(Objective.Theme("Body", 2), Objective.Chains(p.chains)) }, chains = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(37, 8, 5, { p -> listOf(Objective.LongWords(4), Objective.Gold(p.gold)) }, gold = 5, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(38, 8, 5, { p -> listOf(Objective.Letter('N', 6), Objective.Crates(p.crates)) }, crates = 6, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(39, 8, 6, { p -> listOf(Objective.Ice(p.ice), Objective.Chains(p.chains), Objective.Crates(p.crates)) }, ice = 5, chains = 4, crates = 4, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
    Recipe(40, 7, 5, words(10), giftRate = 0.05, bonus = true, casual = BREATHER),
    Recipe(41, 8, 5, { p -> listOf(Objective.Theme("Sports", 2), Objective.Crates(p.crates)) }, crates = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(42, 8, 5, { p -> listOf(Objective.Spell("TREE"), Objective.Ice(p.ice)) }, plant = "TREE", ice = 5, giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(43, 8, 5, { p -> listOf(Objective.Gold(p.gold), Objective.Chains(p.chains)) }, gold = 6, chains = 5, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(44, 8, 5, { listOf(Objective.Letter('L', 6), Objective.LongWords(3)) }, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(45, 8, 6, { p -> listOf(Objective.Crates(p.crates)) }, crates = 5, strongCrates = 5, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(46, 8, 5, { p -> listOf(Objective.Theme("Jobs", 2), Objective.Gold(p.gold)) }, gold = 5, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(47, 8, 5, { p -> listOf(Objective.Ice(p.ice), Objective.Chains(p.chains)) }, ice = 6, chains = 5, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(48, 8, 5, { listOf(Objective.Spell("MOON"), Objective.Letter('E', 7)) }, plant = "MOON", giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(49, 8, 5, { p -> listOf(Objective.Theme("Weather", 2), Objective.Crates(p.crates), Objective.Ice(p.ice)) }, crates = 5, ice = 4, giftRate = 0.04, bonus = true, casual = TOUGH),
    Recipe(50, 8, 6, { p -> listOf(Objective.Gold(p.gold), Objective.Crates(p.crates), Objective.Chains(p.chains), Objective.Ice(p.ice)) }, gold = 5, crates = 4, strongCrates = 3, chains = 4, ice = 4, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
)

val BONUS_WORDS = listOf(
    "SUN", "CAT", "DOG", "TEA", "SEA", "RAIN", "STAR", "MOON", "FISH", "TREE", "BIRD", "CAKE", "LAMP", "SNOW",
    "WIND", "ROSE", "GOLD", "SAND", "BOAT", "KITE", "MILK", "NEST", "RING", "SONG", "WAVE", "LEAF", "HOME", "BELL",
)

fun main(args: Array<String>) {
    val dict = Dictionary.fromLines(File("app/src/main/assets/play_words.txt").readLines().asSequence())
    println("Loaded ${dict.size} words")
    for (t in Themes.all) {
        val clean = Themes.cleaned(t, dict)
        val missing = t.words - clean
        if (missing.isNotEmpty()) println("  theme ${t.name}: not in word list: $missing")
    }
    // Args: nothing = remake all; "12" = remake level 12; "from 20" = keep 1-19, remake the rest.
    val from = if (args.firstOrNull() == "from") args.getOrNull(1)?.toIntOrNull() else null
    val onlySet = if (from == null) args.mapNotNull { it.toIntOrNull() }.toSet() else emptySet()
    val only = onlySet.firstOrNull()
    val done = Levels.all.associateBy { it.number }.toMutableMap()
    for (recipe in RECIPES) {
        val keep = (onlySet.isNotEmpty() && recipe.number !in onlySet) || (from != null && recipe.number < from)
        if (keep && done[recipe.number] != null) continue
        done[recipe.number] = make(recipe, dict)
        // Save after every level, so a long run can be stopped and continued.
        write(RECIPES.mapNotNull { done[it.number] }.takeWhile { true })
    }
    write(RECIPES.mapNotNull { done[it.number] })
}

private fun Rng.shuffle(list: MutableList<Pos>) {
    for (i in list.indices.reversed()) { val j = nextInt(i + 1); val t = list[i]; list[i] = list[j]; list[j] = t }
}

class Layout(val start: List<String>, val marks: List<String>, val placed: Placed)

fun randomLayout(recipe: Recipe, rng: Rng, dict: Dictionary): Layout? {
    val cols = recipe.cols
    val rows = recipe.rows
    val cells = (0 until rows).flatMap { r -> (0 until cols).map { c -> Pos(r, c) } }.toMutableList()
    val grid = Array(rows) { CharArray(cols) }
    val marks = Array(rows) { CharArray(cols) { '.' } }
    // Letters: about 38% vowels.
    rng.shuffle(cells)
    val vowelCount = Math.round(cells.size * 0.38).toInt()
    cells.forEachIndexed { i, p -> grid[p.row][p.col] = if (i < vowelCount) Letters.pick(rng, vowelOnly = true) else Letters.pickConsonant(rng) }
    // Plant letters (Spell levels): the word sits in a middle row with two letters swapped,
    // so the player has to spot it and fix it (before other moves scramble it).
    val free = cells.toMutableList()
    val planted = HashSet<Pos>()
    if (recipe.plant.isNotEmpty()) {
        val w = recipe.plant
        val r = 1 + rng.nextInt(maxOf(1, rows - 2))
        val c0 = rng.nextInt(cols - w.length + 1)
        val k = 1 + rng.nextInt(w.length - 1)
        val scrambled = w.toCharArray().also { val t = it[k]; it[k] = it[k - 1]; it[k - 1] = t }
        for (i in w.indices) {
            grid[r][c0 + i] = scrambled[i]
            free.remove(Pos(r, c0 + i))
            planted += Pos(r, c0 + i)
        }
    }
    // Crates: in a cluster near the bottom or the middle, so the player works around them.
    val crateTotal = recipe.crates + recipe.strongCrates
    if (crateTotal > 0) {
        val anchor = Pos(rows - 1 - rng.nextInt(2), rng.nextInt(cols))
        val near = free.sortedBy { Math.abs(it.row - anchor.row) * 2 + Math.abs(it.col - anchor.col) + rng.nextDouble() * 2.5 }
        val chosen = near.take(crateTotal)
        chosen.forEachIndexed { i, p -> grid[p.row][p.col] = if (i < recipe.strongCrates) CRATE2_CHAR else CRATE_CHAR }
        free.removeAll(chosen.toSet())
    }
    fun mark(n: Int, m: Char) {
        repeat(n) {
            val candidates = free.filter { p -> recipe.plant.isEmpty() || true }
            if (candidates.isEmpty()) return
            val p = candidates[rng.nextInt(candidates.size)]
            marks[p.row][p.col] = m
            free.remove(p)
        }
    }
    mark(recipe.ice, 'i')
    mark(recipe.chains, 'c')
    mark(recipe.gold, 'g')
    // Hard letters stay rare.
    for (p in cells) if (p !in planted && grid[p.row][p.col] in "JKVWXYZ" && rng.nextDouble() < 0.6) grid[p.row][p.col] = Letters.pickConsonant(rng)
    // Repair: change letters until there are no words already on the board (so the start looks calm).
    repeat(300) {
        val rowsNow = grid.map { String(it) }
        val words = WordFinder.findAll(Board.fromRows(rowsNow, IdCounter(), marks.map { String(it) }), dict)
        if (words.isEmpty()) {
            return Layout(rowsNow, marks.map { String(it) }, Placed(crateTotal, recipe.ice, recipe.chains, recipe.gold))
        }
        val p = words.first().cells.filter { it !in planted }.ifEmpty { return null }.let { it[rng.nextInt(it.size)] }
        val c = grid[p.row][p.col]
        grid[p.row][p.col] = if (Letters.isVowel(c)) Letters.pick(rng, vowelOnly = true) else Letters.pickConsonant(rng)
    }
    return null
}

fun cratePrizes(n: Int, rng: Rng): List<Prize> = List(n) {
    val r = rng.nextDouble()
    when {
        r < 0.55 -> Prize(PrizeKind.COINS, listOf(10, 15, 20, 25)[rng.nextInt(4)])
        r < 0.8 -> Prize(PrizeKind.MOVES, 1)
        else -> Prize(PrizeKind.BOOSTER, 1)
    }
}

fun make(recipe: Recipe, dict: Dictionary): Level {
    val rng = Rng(recipe.number * 7919L + 13)
    var tried = 0
    val started = System.currentTimeMillis()
    var range = recipe.casual
    while (true) {
        tried++
        // If a recipe is hard to hit, widen the range a little every 40 boards.
        if (tried % 40 == 0) {
            range = (range.start - 0.05).coerceAtLeast(0.05)..(range.endInclusive + 0.05).coerceAtMost(1.0)
            println("  level ${recipe.number}: widening casual range to ${"%.2f".format(range.start)}..${"%.2f".format(range.endInclusive)}")
        }
        val layout = randomLayout(recipe, rng, dict) ?: continue
        val seed = recipe.number * 100_000L + tried
        val targets = recipe.targets(layout.placed)
        val bonus = if (recipe.bonus) BONUS_WORDS[rng.nextInt(BONUS_WORDS.size)] else null
        var level = Level(
            number = recipe.number, cols = recipe.cols, rows = recipe.rows, moves = 60, targets = targets,
            start = layout.start, marks = layout.marks.takeIf { m -> m.any { it.any { c -> c != '.' } } }, seed = seed,
            feature = recipe.feature, bonusWord = bonus, boss = recipe.boss, giftRate = recipe.giftRate,
            cratePrizes = cratePrizes(recipe.crates + recipe.strongCrates, rng),
        )
        val game = Game(level, dict)
        val wordSwaps = game.board.swappablePairs().count { (a, b) -> game.wordsForSwap(a, b).isNotEmpty() }
        if (wordSwaps < (if (recipe.tutorial) 5 else 3)) continue
        val strong = Solver.play(level, dict, 1.0, Rng(1))
        if (!strong.won) continue
        val used = strong.movesUsed
        // A level that takes even a planner 25+ moves feels like a grind: try another board.
        if (used > 25) continue
        val moves: Int
        val casualRate: Double
        if (recipe.tutorial) {
            moves = maxOf(used * 2 + 4, 10)
            casualRate = Solver.winRate(level.copy(moves = moves), dict, CASUAL_SKILL, RUNS, 99)
            if (casualRate < range.start) continue
        } else {
            // Fewest moves that give the casual player at least the low end of the range.
            // At least 8 moves (10 on boss levels), so a level never feels over before it starts.
            val floor = maxOf(used, if (recipe.boss) 10 else 8)
            var lo = floor
            var hi = maxOf(floor, used * 2 + 6)
            if (Solver.winRate(level.copy(moves = hi), dict, CASUAL_SKILL, RUNS, 99) < range.start) {
                println("  level ${recipe.number}: board too hard for casual players, next")
                continue
            }
            while (lo < hi) {
                val mid = (lo + hi) / 2
                if (Solver.winRate(level.copy(moves = mid), dict, CASUAL_SKILL, RUNS, 99) >= range.start) hi = mid else lo = mid + 1
            }
            moves = lo
            casualRate = Solver.winRate(level.copy(moves = moves), dict, CASUAL_SKILL, RUNS, 99)
            if (casualRate > range.endInclusive) {
                println("  level ${recipe.number}: casual ${"%.2f".format(casualRate)} even with the fewest moves, next")
                continue
            }
        }
        level = level.copy(moves = moves)
        if (!Solver.play(level, dict, 1.0, Rng(1)).won) continue
        val random = Solver.winRate(level, dict, 0.0, RUNS, 7)
        if (recipe.number >= 6 && random > 0.15) {
            println("  level ${recipe.number}: random swaps win ${"%.2f".format(random)}, next")
            continue
        }
        if (recipe.number == 1) {
            val tut = game.board.swappablePairs()
                .map { it to game.wordsForSwap(it.first, it.second) }
                .filter { it.second.isNotEmpty() }
                .maxBy { it.second.sumOf { w -> w.length } }.first
            level = level.copy(tutorialSwap = tut)
        }
        println(
            "Level ${recipe.number}: ${level.start.joinToString("/")} moves=$moves strongUsed=$used " +
                "casual=${"%.2f".format(casualRate)} random=${"%.2f".format(random)} boards=$tried " +
                "time=${(System.currentTimeMillis() - started) / 1000}s"
        )
        return level
    }
}

fun targetCode(t: Objective): String = when (t) {
    is Objective.Words -> "Objective.Words(${t.count})"
    is Objective.LongWords -> "Objective.LongWords(${t.count}, ${t.minLength})"
    is Objective.Gold -> "Objective.Gold(${t.count})"
    is Objective.Crates -> "Objective.Crates(${t.count})"
    is Objective.Ice -> "Objective.Ice(${t.count})"
    is Objective.Chains -> "Objective.Chains(${t.count})"
    is Objective.Letter -> "Objective.Letter('${t.letter}', ${t.count})"
    is Objective.Theme -> "Objective.Theme(\"${t.theme}\", ${t.count})"
    is Objective.Spell -> "Objective.Spell(\"${t.word}\")"
}

fun write(levels: List<Level>) {
    val sb = StringBuilder()
    sb.appendLine("package com.retroraja.wordnoid.core")
    sb.appendLine()
    sb.appendLine("// Made by the level maker (levelmaker/). Run ./gradlew :levelmaker:run to remake.")
    sb.appendLine("// LevelsTest checks every level can still be won.")
    sb.appendLine("object GeneratedLevels {")
    sb.appendLine("    val all: List<Level> = listOf(")
    for (l in levels) {
        sb.appendLine("        Level(")
        sb.appendLine("            number = ${l.number}, cols = ${l.cols}, rows = ${l.rows}, moves = ${l.moves},")
        sb.appendLine("            targets = listOf(${l.targets.joinToString(", ") { targetCode(it) }}),")
        sb.appendLine("            start = listOf(${l.start.joinToString(", ") { "\"$it\"" }}),")
        l.marks?.let { sb.appendLine("            marks = listOf(${it.joinToString(", ") { m -> "\"$m\"" }}),") }
        sb.appendLine("            seed = ${l.seed}L,")
        l.tutorialSwap?.let { (a, b) -> sb.appendLine("            tutorialSwap = Pos(${a.row}, ${a.col}) to Pos(${b.row}, ${b.col}),") }
        l.feature?.let { sb.appendLine("            feature = Feature.${it.name},") }
        l.bonusWord?.let { sb.appendLine("            bonusWord = \"$it\",") }
        if (l.boss) sb.appendLine("            boss = true,")
        if (l.giftRate > 0) sb.appendLine("            giftRate = ${l.giftRate},")
        if (l.cratePrizes.isNotEmpty()) {
            sb.appendLine("            cratePrizes = listOf(${l.cratePrizes.joinToString(", ") { "Prize(PrizeKind.${it.kind.name}, ${it.amount})" }}),")
        }
        sb.appendLine("        ),")
    }
    sb.appendLine("    )")
    sb.appendLine("}")
    val out = File("core/src/main/kotlin/com/retroraja/wordnoid/core/GeneratedLevels.kt")
    out.writeText(sb.toString())
    println("Wrote ${levels.size} levels to $out")
}
