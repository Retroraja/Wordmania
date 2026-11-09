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
    /** Tiko friends to rescue (placed in the upper middle rows). */
    val buddies: Int = 0,
    val balloons: Int = 0,
    /** Treasure chests (2 sand cells each, near the bottom). */
    val chests: Int = 0,
    /** x2 tiles and mystery tiles (new in Update 1). */
    val twins: Int = 0,
    val mysteries: Int = 0,
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
data class Placed(val crates: Int, val ice: Int, val chains: Int, val gold: Int, val buddies: Int = 0, val balloons: Int = 0, val chests: Int = 0, val ironCrates: Int = 0, val twins: Int = 0, val mysteries: Int = 0)

private fun words(n: Int): (Placed) -> List<Objective> = { listOf(Objective.Words(n)) }

const val CASUAL_SKILL = 0.6
const val RUNS = 40

// How often a casual player wins on the first try. From level 6 about 1 in 3 tries fail, from level 15 about 1 in 2.
val TUTORIAL = 0.9..1.0
val EASY = 0.75..0.95
val GENTLE = 0.58..0.78
val MEDIUM = 0.45..0.65
val HARD = 0.38..0.58
val BOSS = 0.22..0.42
val BREATHER = 0.65..0.85
val SPELL = 0.2..0.55

// Update 1.1 (user 2026-11-08): "not about failing, it's about using coins, boosters, video ads for moves".
// A thinking player without boosters often ends 1-3 moves short; with one booster or +5 moves they almost always win.
// Levels 1-10 stay easy. Every 5th level (that isn't a boss) is a breather.
val U11 = 0.40..0.52
val U21 = 0.30..0.42
val U_BREATHER = 0.62..0.80
val U_BOSS = 0.25..0.40
val U_SPELL = 0.25..0.45
/** With +5 moves the casual player should win at least this often (boss levels a bit less). */
const val PLUS5 = 0.85
const val PLUS5_BOSS = 0.75

/** The casual win-rate range a recipe aims for (Update 1.1 targets from Level 11). */
fun tunedRange(r: Recipe): ClosedFloatingPointRange<Double> = when {
    r.number <= 10 -> r.casual
    r.boss -> U_BOSS
    r.number % 5 == 0 -> U_BREATHER
    r.plant.isNotEmpty() -> U_SPELL
    // A level that brings in a new goal is a little kinder.
    r.feature != null || r.number <= 20 -> U11
    else -> U21
}

private fun G(p: Placed) = Objective.Gold(p.gold)
private fun C(p: Placed) = Objective.Crates(p.crates)
/** Iron Crates (2 hits) are their own goal. */
private fun IC(p: Placed) = Objective.IronCrates(p.ironCrates)
private fun I(p: Placed) = Objective.Ice(p.ice)
private fun CH(p: Placed) = Objective.Chains(p.chains)
private fun R(p: Placed) = Objective.Rescue(p.buddies)
private fun B(p: Placed) = Objective.Balloons(p.balloons)
private fun T(p: Placed) = Objective.Treasure(p.chests)
private fun TW(p: Placed) = Objective.Twins(p.twins)
private fun M(p: Placed) = Objective.Mystery(p.mysteries)

/**
 * 50 levels in 5 worlds of 10. A boss closes every world. A theme level comes every 4 or 5 levels.
 * From level 26 most levels mix 2 or 3 goals, using the new goals (Tiko friends, balloons, treasure) often.
 */
val RECIPES: List<Recipe> = listOf(
    // World 1: Candy Land
    Recipe(1, 6, 4, words(3), casual = TUTORIAL, tutorial = true),
    Recipe(2, 6, 4, words(4), casual = TUTORIAL, tutorial = true),
    Recipe(3, 7, 4, words(4), casual = TUTORIAL, tutorial = true),
    Recipe(4, 7, 5, { p -> listOf(G(p)) }, gold = 3, feature = Feature.GOLD, casual = TUTORIAL, tutorial = true),
    Recipe(5, 7, 5, { listOf(Objective.Theme("Animals", 2), Objective.Words(4)) }, feature = Feature.THEME, casual = EASY),
    Recipe(6, 7, 5, { p -> listOf(C(p)) }, crates = 4, feature = Feature.CRATES, casual = GENTLE),
    Recipe(7, 7, 5, { p -> listOf(C(p), Objective.Words(5)) }, crates = 4, feature = Feature.BONUS_WORD, bonus = true, casual = GENTLE),
    Recipe(8, 7, 5, { listOf(Objective.Letter('E', 5)) }, feature = Feature.LETTERS, bonus = true, casual = GENTLE),
    Recipe(9, 7, 5, { p -> listOf(Objective.Theme("Food", 2), G(p)) }, gold = 3, feature = Feature.GIFTS, giftRate = 0.05, bonus = true, casual = GENTLE),
    Recipe(10, 8, 5, { p -> listOf(C(p), G(p)) }, crates = 4, gold = 3, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
    // World 2: Under the Sea
    Recipe(11, 7, 5, { listOf(Objective.LongWords(3)) }, feature = Feature.LONG, giftRate = 0.04, bonus = true, casual = GENTLE),
    Recipe(12, 7, 5, { p -> listOf(C(p), IC(p)) }, crates = 3, strongCrates = 2, feature = Feature.DOUBLE_CRATES, giftRate = 0.04, bonus = true, casual = GENTLE),
    Recipe(13, 8, 5, { p -> listOf(Objective.Letter('A', 6), G(p)) }, gold = 3, giftRate = 0.04, bonus = true, casual = GENTLE),
    Recipe(14, 8, 5, { listOf(Objective.Theme("Fruits", 1), Objective.Words(5)) }, giftRate = 0.04, bonus = true, casual = GENTLE),
    Recipe(15, 8, 5, { p -> listOf(I(p)) }, ice = 5, feature = Feature.ICE, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(16, 8, 5, { p -> listOf(G(p), C(p)) }, gold = 4, crates = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(17, 8, 5, { listOf(Objective.Spell("RAIN"), Objective.Words(4)) }, plant = "RAIN", feature = Feature.SPELL, giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(18, 8, 5, { p -> listOf(Objective.Theme("Colours", 2), I(p)) }, ice = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(19, 8, 5, { listOf(Objective.Letter('S', 6), Objective.LongWords(2)) }, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(20, 8, 6, { p -> listOf(I(p), C(p), IC(p), G(p)) }, ice = 4, crates = 3, strongCrates = 2, gold = 3, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
    // World 3: Fruit Jungle
    Recipe(21, 8, 5, { p -> listOf(CH(p)) }, chains = 5, feature = Feature.CHAINS, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(22, 7, 5, words(8), giftRate = 0.04, bonus = true, casual = BREATHER),
    Recipe(23, 8, 5, { p -> listOf(Objective.Theme("Body", 2), CH(p)) }, chains = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(24, 8, 5, { p -> listOf(C(p), IC(p), Objective.Letter('R', 5)) }, crates = 3, strongCrates = 3, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(25, 8, 5, { p -> listOf(I(p), Objective.LongWords(3)) }, ice = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(26, 8, 5, { p -> listOf(R(p), C(p)) }, buddies = 2, crates = 3, feature = Feature.RESCUE, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(27, 8, 5, { p -> listOf(Objective.Theme("Home", 2), R(p)) }, buddies = 2, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(28, 8, 5, { p -> listOf(G(p), I(p), R(p)) }, gold = 3, ice = 3, buddies = 2, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(29, 8, 5, { p -> listOf(B(p), CH(p)) }, balloons = 5, chains = 3, feature = Feature.BALLOONS, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(30, 8, 6, { p -> listOf(B(p), R(p), C(p)) }, balloons = 4, buddies = 2, crates = 4, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
    // World 4: Cloud Kingdom
    Recipe(31, 8, 5, { p -> listOf(Objective.Theme("Nature", 2), B(p)) }, balloons = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(32, 8, 5, { p -> listOf(T(p), G(p)) }, chests = 1, gold = 3, feature = Feature.TREASURE, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(33, 8, 5, { p -> listOf(R(p), I(p), Objective.Letter('O', 4)) }, buddies = 2, ice = 4, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(34, 8, 5, { p -> listOf(Objective.Spell("GOLD"), T(p)) }, plant = "GOLD", chests = 1, giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(35, 8, 5, { p -> listOf(Objective.Theme("Family", 1), B(p), TW(p)) }, balloons = 4, twins = 3, feature = Feature.TWIN, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(36, 8, 5, { p -> listOf(T(p), CH(p), R(p)) }, chests = 2, chains = 3, buddies = 2, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(37, 8, 5, { p -> listOf(B(p), Objective.LongWords(3)) }, balloons = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(38, 8, 5, { p -> listOf(R(p), C(p), IC(p)) }, buddies = 3, crates = 2, strongCrates = 3, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(39, 8, 5, { p -> listOf(Objective.Theme("Numbers", 1), T(p)) }, chests = 2, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(40, 8, 6, { p -> listOf(T(p), B(p), I(p)) }, chests = 2, balloons = 4, ice = 4, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
    // World 5: Sunny Galaxy
    Recipe(41, 8, 5, { p -> listOf(R(p), G(p), CH(p)) }, buddies = 2, gold = 3, chains = 3, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(42, 8, 5, { p -> listOf(B(p), Objective.Spell("TREE")) }, balloons = 4, plant = "TREE", giftRate = 0.04, bonus = true, casual = SPELL),
    Recipe(43, 8, 5, { p -> listOf(Objective.Theme("Names", 2), T(p)) }, chests = 2, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(44, 8, 5, { p -> listOf(R(p), B(p), C(p)) }, buddies = 2, balloons = 4, crates = 3, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(45, 8, 5, { p -> listOf(T(p), M(p), Objective.Letter('L', 4)) }, chests = 2, mysteries = 4, feature = Feature.MYSTERY, giftRate = 0.04, bonus = true, casual = MEDIUM),
    Recipe(46, 8, 5, { p -> listOf(Objective.Theme("Food", 2), R(p)) }, buddies = 3, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(47, 8, 5, { p -> listOf(B(p), CH(p), G(p)) }, balloons = 4, chains = 3, gold = 3, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(48, 8, 5, { p -> listOf(T(p), R(p), Objective.LongWords(2)) }, chests = 2, buddies = 2, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(49, 8, 5, { p -> listOf(Objective.Theme("Fruits", 1), B(p)) }, balloons = 5, giftRate = 0.04, bonus = true, casual = HARD),
    Recipe(50, 8, 6, { p -> listOf(R(p), T(p), C(p), IC(p)) }, buddies = 3, chests = 2, crates = 3, strongCrates = 2, feature = Feature.BOSS, boss = true, giftRate = 0.04, bonus = true, casual = BOSS),
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

class Layout(val start: List<String>, val marks: List<String>, val placed: Placed, val treasure: List<String>? = null)

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
            val candidates = free.filter { p -> grid[p.row][p.col] != BUDDY_CHAR }
            if (candidates.isEmpty()) return
            val p = candidates[rng.nextInt(candidates.size)]
            marks[p.row][p.col] = m
            free.remove(p)
        }
    }
    // Tiko friends: in the upper middle rows, one per column, never on top of crates.
    var buddies = 0
    if (recipe.buddies > 0) {
        val spots = free.filter { it.row in 1..2 }.groupBy { it.col }.values.map { it[rng.nextInt(it.size)] }.toMutableList()
        rng.shuffle(spots)
        for (p in spots.take(recipe.buddies)) { grid[p.row][p.col] = BUDDY_CHAR; free.remove(p); buddies++ }
    }
    // Treasure: chests of 2 side-by-side sand cells in the bottom two rows.
    val sand = Array(rows) { CharArray(cols) { '.' } }
    var chests = 0
    repeat(recipe.chests) {
        val options = (rows - 2 until rows).flatMap { r -> (0 until cols - 1).map { c -> Pos(r, c) } }.filter { p ->
            val q = Pos(p.row, p.col + 1)
            listOf(p, q).all { grid[it.row][it.col] != CRATE_CHAR && grid[it.row][it.col] != CRATE2_CHAR && sand[it.row][it.col] == '.' } &&
                listOf(p, q).none { c -> (-1..1).any { dc -> sand.getOrNull(c.row - 1)?.getOrNull(c.col + dc) == 't' || sand.getOrNull(c.row + 1)?.getOrNull(c.col + dc) == 't' || sand[c.row].getOrNull(c.col + dc) == 't' } }
        }
        if (options.isNotEmpty()) {
            val p = options[rng.nextInt(options.size)]
            sand[p.row][p.col] = 't'; sand[p.row][p.col + 1] = 't'; chests++
        }
    }
    mark(recipe.ice, 'i')
    mark(recipe.chains, 'c')
    mark(recipe.gold, 'g')
    mark(recipe.balloons, 'b')
    mark(recipe.twins, 'x')
    mark(recipe.mysteries, 'm')
    // Hard letters stay rare.
    for (p in cells) if (p !in planted && grid[p.row][p.col] in "JKVWXYZ" && rng.nextDouble() < 0.6) grid[p.row][p.col] = Letters.pickConsonant(rng)
    // Repair: change letters until there are no words already on the board (so the start looks calm).
    repeat(300) {
        val rowsNow = grid.map { String(it) }
        val words = WordFinder.findAll(Board.fromRows(rowsNow, IdCounter(), marks.map { String(it) }), dict)
        if (words.isEmpty()) {
            return Layout(
                rowsNow, marks.map { String(it) }, Placed(recipe.crates, recipe.ice, recipe.chains, recipe.gold, buddies, recipe.balloons, chests, recipe.strongCrates, recipe.twins, recipe.mysteries),
                if (chests > 0) sand.map { String(it) } else null,
            )
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
        r < 0.55 -> Prize(PrizeKind.COINS, listOf(5, 5, 10, 10)[rng.nextInt(4)])
        r < 0.8 -> Prize(PrizeKind.MOVES, 1)
        else -> Prize(PrizeKind.BOOSTER, 1)
    }
}

fun make(recipe: Recipe, dict: Dictionary): Level {
    val rng = Rng(recipe.number * 7919L + 13)
    var tried = 0
    val started = System.currentTimeMillis()
    var range = tunedRange(recipe)
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
            start = layout.start, marks = layout.marks.takeIf { m -> m.any { it.any { c -> c != '.' } } }, treasure = layout.treasure, seed = seed,
            feature = recipe.feature, bonusWord = bonus, boss = recipe.boss, giftRate = recipe.giftRate,
            cratePrizes = cratePrizes(recipe.crates + recipe.strongCrates, rng),
        )
        val game = Game(level, dict)
        val wordSwaps = game.board.swappablePairs().count { (a, b) -> game.wordsForSwap(a, b).isNotEmpty() }
        // Levels 1-4 are easy (tester feedback): lots of words to find from the very first move.
        if (wordSwaps < (if (recipe.tutorial) 8 else 3)) continue
        val strong = Solver.play(level, dict, 1.0, Rng(1))
        if (!strong.won) continue
        val used = strong.movesUsed
        // A level that takes even a planner 25+ moves feels like a grind: try another board.
        if (used > (if (recipe.boss) 18 else 15)) continue
        val moves: Int
        val casualRate: Double
        if (recipe.tutorial) {
            moves = maxOf(used * 2 + 6, 12)
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
        // With one booster's worth of help (+5 moves) the casual player should almost always win.
        val plus5 = if (recipe.number > 10) Solver.winRate(level.copy(moves = moves + 5), dict, CASUAL_SKILL, RUNS, 99) else 1.0
        // Spell levels depend on finding one word, so extra moves help less there.
        // A hard recipe lowers the bar a little every 40 boards (never under 0.5), so the maker never gets stuck.
        val need = ((if (recipe.plant.isNotEmpty()) 0.6 else if (recipe.boss) PLUS5_BOSS else PLUS5) - 0.1 * (tried / 40)).coerceAtLeast(0.5)
        if (plus5 < need) {
            println("  level ${recipe.number}: +5 moves only wins ${"%.2f".format(plus5)}, next")
            continue
        }
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
                "casual=${"%.2f".format(casualRate)} plus5=${"%.2f".format(plus5)} random=${"%.2f".format(random)} boards=$tried " +
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
    is Objective.IronCrates -> "Objective.IronCrates(${t.count})"
    is Objective.Ice -> "Objective.Ice(${t.count})"
    is Objective.Chains -> "Objective.Chains(${t.count})"
    is Objective.Letter -> "Objective.Letter('${t.letter}', ${t.count})"
    is Objective.Theme -> "Objective.Theme(\"${t.theme}\", ${t.count})"
    is Objective.Spell -> "Objective.Spell(\"${t.word}\")"
    is Objective.Rescue -> "Objective.Rescue(${t.count})"
    is Objective.Balloons -> "Objective.Balloons(${t.count})"
    is Objective.Treasure -> "Objective.Treasure(${t.count})"
    is Objective.Twins -> "Objective.Twins(${t.count})"
    is Objective.Mystery -> "Objective.Mystery(${t.count})"
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
        l.treasure?.let { sb.appendLine("            treasure = listOf(${it.joinToString(", ") { m -> "\"$m\"" }}),") }
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
