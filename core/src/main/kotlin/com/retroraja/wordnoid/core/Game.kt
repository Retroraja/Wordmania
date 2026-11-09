package com.retroraja.wordnoid.core

enum class GameStatus { PLAYING, WON, LOST }

data class CrateHit(val pos: Pos, val broken: Boolean, val prize: Prize?, val iron: Boolean = false)

/** What the boss did after a move: froze a tile (now ice) or dropped a crate. */
data class BossAttack(val pos: Pos, val freeze: Boolean)

/** Everything that happens when words burst (or a booster smashes something). */
data class Effects(
    val words: List<FoundWord> = emptyList(),
    /** Cells that burst (word tiles that aren't locked, broken crates, smashed tiles). */
    val cleared: Set<Pos> = emptySet(),
    val crateHits: List<CrateHit> = emptyList(),
    val chainsBroken: List<Pos> = emptyList(),
    val iceMelted: List<Pos> = emptyList(),
    /** Ice hit once by the Hammer (cracked, not melted yet). */
    val iceCracked: List<Pos> = emptyList(),
    val balloons: List<Pos> = emptyList(),
    val gold: List<Pos> = emptyList(),
    val gifts: List<Pos> = emptyList(),
    val themeWords: List<String> = emptyList(),
    val bonusWord: Boolean = false,
    val bonusMoves: Int = 0,
    val coins: Int = 0,
    val points: Int = 0,
    val meter: Int = 0,
    /** Boosters won (crate prizes and gift tiles). The app picks which. */
    val boosters: Int = 0,
    val doubled: Boolean = false,
    /** New power tiles left by long words: cell -> 1 (clears its row) or 2 (row and column). */
    val powerMade: Map<Pos, Int> = emptyMap(),
    /** Power tiles that fired in this move. */
    val powerFired: List<Pos> = emptyList(),
    /** Cells cleared by power tiles (for the blast animation). */
    val powerCleared: Set<Pos> = emptySet(),
    /** x2 tiles used in words this move. */
    val twins: List<Pos> = emptyList(),
    /** Mystery tiles revealed this move. */
    val revealed: List<Pos> = emptyList(),
) {
    val isEmpty: Boolean get() = words.isEmpty() && cleared.isEmpty() && chainsBroken.isEmpty() && iceMelted.isEmpty() && iceCracked.isEmpty() && balloons.isEmpty() && revealed.isEmpty()
}

data class MoveResult(
    /** The board right after the swap or booster change, before anything bursts. */
    val changedBoard: Board,
    val effects: Effects,
    /** After bursting and refilling. */
    val boardAfter: Board,
    /** Always null since Update 1.1: the board is never shuffled by itself (see [stuck]). */
    val shuffled: Board?,
    val status: GameStatus,
    /** Extra points for moves left over when the level is won. */
    val leftoverBonus: Int,
    val leftoverCoins: Int,
    /** Tiko friends that reached the bottom and escaped in this move. */
    val rescued: Int = 0,
    /** Treasure chests dug out in this move (index into [Level.chests]). */
    val chestsFound: List<Int> = emptyList(),
    val bossAttack: BossAttack? = null,
    /** No swap makes a word any more. Nothing moves by itself: the player gets a free Shuffle to tap. */
    val stuck: Boolean = false,
)

/** The rules of one level being played. Has no screen code, so it can be tested and used by the level maker. */
class Game private constructor(val level: Level, private val baseDict: Dictionary, private var s: State) {

    private data class State(
        val board: Board,
        val movesLeft: Int,
        val rng: Rng,
        val ids: IdCounter,
        val stats: Stats,
        val score: Int,
        val coins: Int,
        val status: GameStatus,
        val movesUsed: Int,
        val freeMoves: Int,
        val doubleNext: Boolean,
        val bonusFound: Boolean,
        val dug: Set<Pos> = emptySet(),
        val chests: Set<Int> = emptySet(),
    ) {
        fun deep(): State = copy(rng = rng.copy(), ids = ids.copy())
    }

    private var undo: State? = null
    private val themeInfo: Themes.Theme? = level.themeName()?.let { Themes.byName(it) }
    private val theme: Set<String> = themeInfo?.let { Themes.cleaned(it, baseDict) } ?: emptySet()
    /** During a Names goal, names count as words too. */
    private val dict: Dictionary = if (themeInfo?.isNames == true) baseDict.withExtra(theme.map { it.lowercase() }.toSet()) else baseDict
    private val treasureCells: Set<Pos> = level.chests.flatten().toSet()

    val board: Board get() = s.board
    val movesLeft: Int get() = s.movesLeft
    val stats: Stats get() = s.stats
    val found: List<FoundWord> get() = s.stats.words
    val score: Int get() = s.score
    val coins: Int get() = s.coins
    val status: GameStatus get() = s.status
    val movesUsed: Int get() = s.movesUsed
    val freeMoves: Int get() = s.freeMoves
    val doubleNext: Boolean get() = s.doubleNext
    val bonusFound: Boolean get() = s.bonusFound
    val canUndo: Boolean get() = undo != null
    val themeWords: Set<String> get() = theme
    /** Sand cells already dug (their tile was cleared at least once). */
    val dug: Set<Pos> get() = s.dug
    val chestsFound: Set<Int> get() = s.chests

    constructor(level: Level, dict: Dictionary, bonusMoves: Int = 0) : this(
        level, dict,
        State(
            board = startBoard(level),
            movesLeft = level.moves + bonusMoves,
            rng = Rng(level.seed),
            ids = IdCounter(1_000L),
            stats = Stats(),
            score = 0, coins = 0, status = GameStatus.PLAYING, movesUsed = 0,
            freeMoves = 0, doubleNext = false, bonusFound = false,
        ),
    )

    fun copy(): Game = Game(level, baseDict, s.deep()).also { it.undo = undo?.deep() }

    val goalMet: Boolean get() = level.isMet(s.stats)

    /** Moves this swap would cost (0 with a Free Move; 2 when an ice tile moves). */
    fun swapCost(a: Pos, b: Pos): Int = if (s.freeMoves > 0) 0 else board.swapCost(a, b)

    fun canSwap(a: Pos, b: Pos): Boolean =
        s.status == GameStatus.PLAYING && board.contains(a) && board.contains(b) && a.isNeighbour(b) &&
            board[a].swappable && board[b].swappable && (s.freeMoves > 0 || s.movesLeft >= board.swapCost(a, b))

    fun swap(a: Pos, b: Pos): MoveResult {
        require(canSwap(a, b)) { "Swap not allowed" }
        undo = s.deep()
        s = if (s.freeMoves > 0) s.copy(freeMoves = s.freeMoves - 1) else s.copy(movesLeft = s.movesLeft - board.swapCost(a, b))
        s = s.copy(movesUsed = s.movesUsed + 1, board = board.swap(a, b))
        val r = act(listOf(a, b), fromSwap = true)
        return if (level.boss && r.status == GameStatus.PLAYING && s.movesUsed % BOSS_EVERY == 0) bossAttack(r) else r
    }

    /** The boss fights back: freezes a tile, or drops a crate near the top. Never on the move that wins. */
    private fun bossAttack(r: MoveResult): MoveResult {
        val freeze = (s.movesUsed / BOSS_EVERY) % 2 == 1
        val b = board
        val spots = b.positions().filter { p ->
            val t = b[p]
            t.kind == TileKind.NORMAL && !t.locked && (freeze || p.row <= 2)
        }
        if (spots.isEmpty()) return r
        val p = spots[s.rng.nextInt(spots.size)]
        var nb = if (freeze) b.with(p, b[p].copy(kind = TileKind.ICE, hp = ICE_HP))
        else b.with(p, b[p].copy(letter = CRATE_CHAR, kind = TileKind.CRATE, hp = 1, prize = Prize(PrizeKind.COINS, 5)))
        s = s.copy(board = nb)
        return r.copy(boardAfter = board, stuck = !hasWordMove(nb), bossAttack = BossAttack(p, freeze))
    }

    /** Locked letters on the board (Lock booster). */
    val lockedCells: List<Pos> get() = board.positions().filter { board[it].locked }

    /** Unlocks a locked letter (free, no move) and checks for words through it right away. */
    fun unlock(p: Pos): MoveResult? {
        if (s.status != GameStatus.PLAYING || !board.contains(p) || !board[p].locked) return null
        undo = s.deep()
        s = s.copy(board = board.with(p, board[p].copy(locked = false)))
        return act(listOf(p))
    }

    /** "+N moves" from the out-of-moves screen. Keeps the board as it is. */
    fun continueWith(moves: Int) {
        s = s.copy(movesLeft = s.movesLeft + moves, status = if (s.status == GameStatus.LOST) GameStatus.PLAYING else s.status)
    }

    fun stars(): Int = if (s.status == GameStatus.WON) level.starsFor(s.movesLeft) else 0

    // ---------------------------------------------------------------- words and effects

    /** Words that one swap of [a] and [b] would make right now (without changing the game). */
    fun wordsForSwap(a: Pos, b: Pos): List<FoundWord> { val sw = board.swap(a, b); return WordFinder.wordsThrough(sw, listOf(a, b), dict, rankFor(sw)) }

    /**
     * Word priority (when one swap could make different words): the theme word first, then a word with a gold tile,
     * then a word that helps the goal, then any other word (longest first).
     */
    private fun rankFor(b: Board): WordFinder.Rank {
        val letterGoal = level.targets.filterIsInstance<Objective.Letter>().filter { !it.done(s.stats) }.map { it.letter }.toSet()
        val spell = level.targets.filterIsInstance<Objective.Spell>().map { it.word }.toSet()
        val wantCrates = level.targets.any { (it is Objective.Crates || it is Objective.IronCrates) && !it.done(s.stats) }
        val wantChains = level.targets.any { it is Objective.Chains && !it.done(s.stats) }
        val wantBalloons = level.targets.any { it is Objective.Balloons && !it.done(s.stats) }
        val wantIce = level.targets.any { it is Objective.Ice && !it.done(s.stats) }
        return WordFinder.Rank { word, cells ->
            when {
                word in theme || word in spell -> 3
                cells.any { b[it].isGold } -> 2
                word.any { it in letterGoal } -> 1
                wantIce && cells.any { b[it].isIce } -> 1
                cells.any { b[it].isPower } -> 1
                cells.any { p -> b.neighbours(p).any { n -> val t = b[n]; (wantCrates && t.isCrate) || (wantChains && t.isChain) || (wantBalloons && t.isBalloon) } } -> 1
                else -> 0
            }
        }
    }

    private fun act(changed: Collection<Pos>, fromSwap: Boolean = false): MoveResult {
        val changedBoard = board
        val moved = changed.filter { changedBoard[it].hasLetter }
        val words = WordFinder.wordsThrough(changedBoard, moved, dict, rankFor(changedBoard))
        val fx = if (words.isEmpty()) Effects() else effectsOf(changedBoard, words, emptySet(), power = if (fromSwap) powerSpots(changedBoard, words, moved) else emptyMap())
        return apply(changedBoard, fx)
    }

    /**
     * Power tiles: a 5+ letter word made by a swap leaves its moved tile behind as a rainbow power tile
     * (6+ letters: one that clears row and column). Not in the tutorials, and at most [MAX_POWER_TILES] on the board.
     */
    private fun powerSpots(b: Board, words: List<FoundWord>, moved: List<Pos>): Map<Pos, Int> {
        if (level.number <= POWER_FROM_LEVEL - 1) return emptyMap()
        var room = MAX_POWER_TILES - b.tiles().count { it.isPower }
        val out = HashMap<Pos, Int>()
        for (w in words.sortedByDescending { it.length }) {
            if (room <= 0 || w.length < 5) continue
            val p = moved.firstOrNull { it in w.cells && !b[it].isPower && it !in out } ?: continue
            out[p] = if (w.length >= 6) 2 else 1
            room--
        }
        return out
    }

    /**
     * Works out what bursting [words] (plus [smashed] cells, from boosters) does, without changing anything.
     */
    private fun effectsOf(b: Board, words: List<FoundWord>, smashed0: Set<Pos>, crateFullBreak: Boolean = false, power: Map<Pos, Int> = emptyMap()): Effects {
        val wordCells = words.flatMap { it.cells }.toSet()
        // Power tiles used in a word fire: they clear their row (and column). A power tile caught in a blast fires too.
        val fired = ArrayList<Pos>()
        val blast = LinkedHashSet<Pos>()
        val queue = ArrayDeque(wordCells.filter { b[it].isPower && it !in power })
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            if (p in fired) continue
            fired += p
            val line = WordFinder.rowLine(b, p.row) + if (b[p].hp >= 2) WordFinder.colLine(b, p.col) else emptyList()
            for (q in line) {
                if (q == p || q in wordCells || b[q].isBuddy || b[q].locked) continue
                blast += q
                if (b[q].isPower && q !in fired) queue += q
            }
        }
        val smashed = smashed0 + blast
        val cleared = HashSet<Pos>()
        val crateHits = ArrayList<CrateHit>()
        val chains = ArrayList<Pos>()
        val ice = ArrayList<Pos>()
        val gold = ArrayList<Pos>()
        val gifts = ArrayList<Pos>()
        val balloons = LinkedHashSet<Pos>()
        var boosters = 0
        var bonusMoves = 0
        var coins = 0
        var points = 0
        var meter = 0
        val themeHits = ArrayList<String>()
        val doubled = s.doubleNext && words.isNotEmpty()

        for (p in wordCells + smashed) {
            val t = b[p]
            when {
                t.isCrate -> {} // only smashed crates get here; handled below
                else -> cleared += p
            }
            if (t.isGold) gold += p
            if (t.isGift) { gifts += p; boosters++ }
            if (t.isIce) ice += p
            if (t.isChain) chains += p
            if (t.isBalloon) balloons += p
        }
        // Crates next to a word take one hit each. Smashed crates take one hit (or break fully with the Axe).
        val hitCrates = LinkedHashSet<Pos>()
        for (p in wordCells) for (n in b.neighbours(p)) if (b[n].isCrate) hitCrates += n
        for (p in smashed) if (b[p].isCrate) hitCrates += p
        for (p in hitCrates) {
            val t = b[p]
            val broken = crateFullBreak && p in smashed0 || t.hp <= 1
            val prize = if (broken) (t.prize ?: Prize(PrizeKind.COINS, 5)) else null
            crateHits += CrateHit(p, broken, prize, t.iron)
            if (broken) {
                cleared += p
                when (prize!!.kind) {
                    PrizeKind.COINS -> coins += prize.amount
                    PrizeKind.MOVES -> bonusMoves += prize.amount
                    PrizeKind.BOOSTER -> boosters += prize.amount
                }
            }
        }
        // Chains next to a word snap (the tile stays, now free to move). Balloons next to a word pop. Mystery tiles show their letter.
        val revealed = LinkedHashSet<Pos>()
        for (p in wordCells) for (n in b.neighbours(p)) {
            if (b[n].isChain && n !in chains) chains += n
            if (b[n].isBalloon) balloons += n
            if (b[n].isMystery) revealed += n
        }
        for (p in smashed) if (b[p].isMystery) revealed += p
        val twins = wordCells.filter { b[it].isTwin }

        // Words give no coins (coins come from wins, crates and gifts), only points, Power Meter and long-word moves.
        for (w in words) {
            val isTheme = w.word in theme
            if (isTheme) themeHits += w.word
            var pts = w.length * 10 * maxOf(1, w.length - 2)
            var m = when { w.length >= 5 -> 3; w.length == 4 -> 2; else -> 1 }
            if (isTheme) pts *= 2
            if (doubled) { pts *= 2; m *= 2 }
            points += pts
            meter += m
            bonusMoves += level.movesForWord(w.length)
        }
        points += 20 * (crateHits.size + chains.size + ice.size + gold.size + balloons.size)
        val bonusWord = !s.bonusFound && level.bonusWord != null && words.any { it.word == level.bonusWord }
        if (bonusWord) boosters++
        // New power tiles stay on the board (they don't burst).
        cleared.removeAll(power.keys)
        points += 50 * fired.size
        return Effects(
            words = words, cleared = cleared, crateHits = crateHits, chainsBroken = chains.distinct(), iceMelted = ice,
            balloons = balloons.toList(), gold = gold, gifts = gifts, themeWords = themeHits, bonusWord = bonusWord,
            bonusMoves = bonusMoves, coins = coins, points = points, meter = meter, boosters = boosters, doubled = doubled,
            powerMade = power, powerFired = fired, powerCleared = blast, twins = twins, revealed = revealed.toList(),
        )
    }

    private fun statsAfter(st: Stats, fx: Effects): Stats {
        val letters = st.letters.toMutableMap()
        // A word with a x2 tile counts twice (words, letters and theme goals).
        val counted = fx.words.flatMap { w -> if (w.cells.any { it in fx.twins }) listOf(w, w) else listOf(w) }
        for (w in counted) for (ch in w.word) letters[ch] = (letters[ch] ?: 0) + 1
        val themeExtra = fx.words.count { w -> w.word in fx.themeWords && w.cells.any { it in fx.twins } }
        return st.copy(
            words = st.words + counted,
            twins = st.twins + fx.twins.size,
            mysteries = st.mysteries + fx.revealed.size,
            gold = st.gold + fx.gold.size,
            crates = st.crates + fx.crateHits.count { it.broken && !it.iron },
            ironCrates = st.ironCrates + fx.crateHits.count { it.broken && it.iron },
            ice = st.ice + fx.iceMelted.size,
            chains = st.chains + fx.chainsBroken.size,
            letters = letters,
            themeWords = st.themeWords + fx.themeWords.size + themeExtra,
            balloons = st.balloons + fx.balloons.size,
        )
    }

    private fun apply(changedBoard: Board, fx: Effects): MoveResult {
        var b = changedBoard
        // Tiles that stay but change: hit crates, snapped chains, melted ice.
        for (h in fx.crateHits) if (!h.broken) b = b.with(h.pos, b[h.pos].copy(hp = b[h.pos].hp - 1))
        for (p in fx.chainsBroken) if (p !in fx.cleared) b = b.with(p, b[p].copy(kind = TileKind.NORMAL))
        for (p in fx.iceMelted) if (p !in fx.cleared) b = b.with(p, b[p].copy(kind = TileKind.NORMAL, hp = 0))
        for (p in fx.iceCracked) b = b.with(p, b[p].copy(hp = b[p].hp - 1))
        for (p in fx.balloons) if (p !in fx.cleared) b = b.with(p, b[p].copy(kind = TileKind.NORMAL))
        for ((p, hp) in fx.powerMade) b = b.with(p, b[p].copy(kind = TileKind.POWER, hp = hp, locked = false))
        for (p in fx.revealed) if (p !in fx.cleared) b = b.with(p, b[p].copy(kind = TileKind.NORMAL))
        var after = if (fx.cleared.isEmpty()) b else smartRefill(b, fx.cleared)
        // Tiko friends that reach the bottom row escape (and the tiles above drop again).
        var rescued = 0
        while (true) {
            val out = (0 until after.cols).map { Pos(after.rows - 1, it) }.filter { after[it].isBuddy }.toSet()
            if (out.isEmpty()) break
            rescued += out.size
            after = smartRefill(after, out)
        }
        // Digging: every cleared sand cell is dug. A chest is found when all of its cells are dug.
        val dug = s.dug + fx.cleared.filter { it in treasureCells }
        val newChests = level.chests.indices.filter { it !in s.chests && level.chests[it].all { c -> c in dug } }
        val st = statsAfter(s.stats, fx)
        s = s.copy(
            board = after,
            dug = dug,
            chests = s.chests + newChests,
            stats = st.copy(rescued = st.rescued + rescued, treasures = st.treasures + newChests.size),
            movesLeft = s.movesLeft + fx.bonusMoves,
            coins = s.coins + fx.coins,
            score = s.score + fx.points,
            doubleNext = s.doubleNext && !fx.doubled,
            bonusFound = s.bonusFound || fx.bonusWord,
        )
        return finish(changedBoard, fx).copy(rescued = rescued, chestsFound = newChests)
    }

    /**
     * Smart refill: new letters are picked so the board keeps at least [MIN_WORD_SWAPS] swaps that make a word
     * (new tiles never make a word by themselves). Tries a few letter sets and keeps the best one.
     */
    private fun smartRefill(b: Board, cleared: Set<Pos>): Board {
        var best: Board? = null
        var bestCount = -1
        repeat(REFILL_TRIES) {
            val nb = Refill.collapse(b, cleared, s.rng, s.ids, dict, level.giftRate)
            val n = wordSwaps(nb, MIN_WORD_SWAPS)
            if (n > bestCount) { best = nb; bestCount = n }
            if (n >= MIN_WORD_SWAPS) return nb
        }
        return best!!
    }

    /** How many swaps make a word on [b] (counting stops at [cap]). */
    fun wordSwaps(b: Board = board, cap: Int = Int.MAX_VALUE): Int {
        var n = 0
        for ((x, y) in b.swappablePairs()) if (wordsForSwap(b, x, y, dict).isNotEmpty()) { if (++n >= cap) return n }
        return n
    }

    /** True when no swap makes a word: the player should tap Shuffle (given free). */
    val stuck: Boolean get() = s.status == GameStatus.PLAYING && !hasWordMove(board)

    private fun finish(changedBoard: Board, fx: Effects): MoveResult {
        var leftover = 0
        var leftoverCoins = 0
        var stuck = false
        val status = when {
            goalMet -> GameStatus.WON
            s.movesLeft <= 0 && s.freeMoves <= 0 -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }
        s = s.copy(status = status)
        if (status == GameStatus.WON) {
            leftover = s.movesLeft * LEFTOVER_MOVE_POINTS
            leftoverCoins = minOf(s.movesLeft * LEFTOVER_MOVE_COINS, LEFTOVER_COINS_MAX)
            s = s.copy(score = s.score + leftover, coins = s.coins + leftoverCoins)
            undo = null
        } else if (status == GameStatus.PLAYING && !hasWordMove(board)) {
            // Never shuffled by itself (Update 1.1): the app gives a free Shuffle for the player to tap.
            stuck = true
        }
        return MoveResult(changedBoard, fx, board, null, status, leftover, leftoverCoins, stuck = stuck)
    }

    // ---------------------------------------------------------------- boosters

    /**
     * Uses a booster. Returns null if it can't be used there (nothing is changed).
     * [a]/[b] are the tiles picked, [letter] for Paintbrush / Vowel Flip / Dice, [dir] (+1/-1) for slides.
     * Hint and Word Scanner don't change the board: use [hint] and [scan].
     */
    fun useBooster(booster: Booster, a: Pos? = null, b: Pos? = null, letter: Char? = null, dir: Int = 1): MoveResult? {
        if (s.status != GameStatus.PLAYING) return null
        val bd = board
        fun ok(p: Pos?) = p != null && bd.contains(p)
        val before = s.deep()
        val result: MoveResult? = when (booster) {
            Booster.HINT, Booster.SCANNER -> null
            Booster.HAMMER -> when {
                !ok(a) -> null
                bd[a!!].isGold || bd[a].isBuddy -> null // gold is collected with words (or the Magnet), friends can't be smashed
                bd[a].isIce && bd[a].hp >= 2 -> apply(bd, Effects(iceCracked = listOf(a)))
                else -> smash(setOf(a), fullBreak = false)
            }
            Booster.TREASURE_FINDER -> {
                val i = level.chests.indices.firstOrNull { it !in s.chests }
                if (i == null) null else {
                    val cells = level.chests[i]
                    s = s.copy(dug = s.dug + cells, chests = s.chests + i, stats = s.stats.copy(treasures = s.stats.treasures + 1))
                    finish(bd, Effects()).copy(chestsFound = listOf(i))
                }
            }
            Booster.AXE -> {
                if (!ok(a)) null else {
                    val t = bd[a!!]
                    when {
                        t.isCrate -> smash(setOf(a), fullBreak = true)
                        t.isIce || t.isChain -> apply(bd, Effects(iceMelted = if (t.isIce) listOf(a) else emptyList(), chainsBroken = if (t.isChain) listOf(a) else emptyList(), points = 20))
                        else -> null
                    }
                }
            }
            Booster.MAGNET -> if (!ok(a) || !bd[a!!].isGold) null else smash(setOf(a), fullBreak = false)
            Booster.MAGIC_SWAP -> {
                if (!ok(a) || !ok(b) || a == b || !bd[a!!].movable || !bd[b!!].movable) null
                else { s = s.copy(board = bd.swap(a, b)); act(listOf(a, b)) }
            }
            Booster.JOKER -> {
                if (!ok(a) || !bd[a!!].movable || bd[a].isJoker) null
                else { s = s.copy(board = bd.with(a, bd[a].copy(letter = JOKER_CHAR, kind = TileKind.JOKER))); act(listOf(a)) }
            }
            Booster.VOWEL_FLIP -> rain(bd, 4 + s.rng.nextInt(2), "AEIOU")
            Booster.PAINT, Booster.DICE -> {
                val l = letter?.uppercaseChar()
                if (!ok(a) || !bd[a!!].movable || l == null || l !in 'A'..'Z') null
                else {
                    val t = bd[a]
                    val kind = if (t.isJoker) TileKind.NORMAL else t.kind
                    s = s.copy(board = bd.with(a, t.copy(letter = l, kind = kind)))
                    act(listOf(a))
                }
            }
            Booster.SPIN -> {
                if (!ok(a)) null else {
                    val tl = a!!; val cells = listOf(tl, Pos(tl.row, tl.col + 1), Pos(tl.row + 1, tl.col + 1), Pos(tl.row + 1, tl.col))
                    if (!cells.all { bd.contains(it) && bd[it].movable }) null else {
                        val nb = bestSpin(bd, cells)
                        if (nb == null) null else { s = s.copy(board = nb); act(cells) }
                    }
                }
            }
            Booster.ROW_SLIDE, Booster.COLUMN_SLIDE -> {
                if (!ok(a)) null else {
                    val line = if (booster == Booster.ROW_SLIDE) WordFinder.rowLine(bd, a!!.row) else WordFinder.colLine(bd, a!!.col)
                    val nb = arrangeLine(bd, line)
                    if (nb == null) null else { s = s.copy(board = nb); act(line.filter { nb[it].movable }) }
                }
            }
            Booster.UNDO -> {
                val u = undo
                if (u == null) null else {
                    s = u
                    undo = null
                    MoveResult(board, Effects(), board, null, s.status, 0, 0)
                }
            }
            Booster.SHUFFLE -> {
                val nb = shuffled(bd)
                s = s.copy(board = nb)
                MoveResult(nb, Effects(), nb, null, s.status, 0, 0)
            }
            Booster.FREE_MOVE -> { s = s.copy(freeMoves = s.freeMoves + 1); MoveResult(bd, Effects(), bd, null, s.status, 0, 0) }
            Booster.PLUS_MOVES -> { s = s.copy(movesLeft = s.movesLeft + 3); MoveResult(bd, Effects(), bd, null, s.status, 0, 0) }
            Booster.DOUBLE -> { s = s.copy(doubleNext = true); MoveResult(bd, Effects(), bd, null, s.status, 0, 0) }
            Booster.LOCK -> {
                if (!ok(a) || !bd[a!!].movable || bd[a].locked) null
                else { val nb = bd.with(a, bd[a].copy(locked = true)); s = s.copy(board = nb); MoveResult(nb, Effects(), nb, null, s.status, 0, 0) }
            }
            Booster.LETTER_RAIN -> {
                val target = level.targets.filterIsInstance<Objective.Letter>().firstOrNull { !it.done(s.stats) }?.letter
                rain(bd, 3, target?.toString() ?: "BDLMNRST")
            }
        }
        if (result == null) {
            s = before
            return null
        }
        if (booster != Booster.UNDO) undo = before
        return result
    }

    /**
     * Spin: tries every way to arrange the 4 tiles (turns first) and keeps the one that makes the longest words.
     * Null if no arrangement makes a word (the booster is not used).
     */
    private fun bestSpin(bd: Board, cells: List<Pos>): Board? {
        val tiles = cells.map { bd[it] }
        val orders = ArrayList<List<Int>>()
        for (k in 1..3) orders += (0..3).map { (it - k + 4) % 4 } // the 3 turns
        fun perms(rest: List<Int>, acc: List<Int>) {
            if (rest.isEmpty()) { if (acc !in orders && acc != listOf(0, 1, 2, 3)) orders += acc; return }
            for (x in rest) perms(rest - x, acc + x)
        }
        perms(listOf(0, 1, 2, 3), emptyList())
        var best: Board? = null
        var bestLen = 0
        for (o in orders) {
            var nb = bd
            cells.forEachIndexed { i, p -> nb = nb.with(p, tiles[o[i]]) }
            val len = WordFinder.wordsThrough(nb, cells, dict).sumOf { it.length }
            if (len > bestLen) { bestLen = len; best = nb }
        }
        return best
    }

    /**
     * Row / Column Slide: rearranges the movable letters of [line] so that the longest possible word is spelled in order,
     * moving as few letters as possible. Crates, ice and chains stay where they are. Null if no word can be made.
     */
    private fun arrangeLine(bd: Board, line: List<Pos>): Board? {
        val movable = line.filter { bd[it].movable }
        if (movable.size < 3) return null
        val pool = movable.map { bd[it] }
        // Runs of neighbouring movable cells: a word must fit inside one run.
        val runs = ArrayList<List<Pos>>()
        var cur = ArrayList<Pos>()
        for (p in line) if (bd[p].movable) cur.add(p) else { if (cur.isNotEmpty()) runs += cur; cur = ArrayList() }
        if (cur.isNotEmpty()) runs += cur
        val maxRun = runs.maxOf { it.size }
        for (k in minOf(maxRun, 8) downTo 3) {
            val words = wordsFromPool(pool.map { it.letter }, k)
            if (words.isEmpty()) continue
            var best: Pair<List<Pos>, String>? = null
            var bestCost = Int.MAX_VALUE
            for (run in runs) for (start in 0..run.size - k) {
                val window = run.subList(start, start + k)
                for (w in words) {
                    val cost = window.indices.count { bd[window[it]].letter != w[it] }
                    if (cost < bestCost) { bestCost = cost; best = window to w }
                }
            }
            val (window, w) = best ?: continue
            if (bestCost == 0) continue // already spelled (it would have burst): look for another
            val left = pool.toMutableList()
            val place = HashMap<Pos, Tile>()
            // Keep tiles that already sit on the right letter, then fill the rest of the window.
            for (i in window.indices) { val t = bd[window[i]]; if (t.letter == w[i] && left.remove(t)) place[window[i]] = t }
            for (i in window.indices) if (window[i] !in place) {
                val t = left.first { it.letter == w[i] }
                left.remove(t); place[window[i]] = t
            }
            for (p in movable) if (p !in place) { place[p] = left.removeAt(0) }
            var nb = bd
            for ((p, t) in place) nb = nb.with(p, t)
            if (WordFinder.wordsThrough(nb, window, dict).isEmpty()) continue
            return nb
        }
        return null
    }

    /** Words of length [k] that can be spelled with [letters] (a Joker '*' can be any letter). Uppercase, with '*' kept. */
    private fun wordsFromPool(letters: List<Char>, k: Int): List<String> {
        val out = LinkedHashSet<String>()
        val used = BooleanArray(letters.size)
        val sb = StringBuilder()
        fun dfs() {
            if (out.size > 200) return
            if (sb.length == k) {
                if (WordFinder.resolve(sb.toString().lowercase(), dict) != null) out += sb.toString()
                return
            }
            val seen = HashSet<Char>()
            for (i in letters.indices) if (!used[i] && seen.add(letters[i])) {
                used[i] = true; sb.append(letters[i]); dfs(); sb.deleteCharAt(sb.length - 1); used[i] = false
            }
        }
        dfs()
        return out.toList()
    }

    /**
     * Vowel Rain / Letter Rain: [n] letters from [pool] rain onto plain letter tiles (never gold, crates, ice, chains,
     * locked or power tiles). They never make a word by themselves: the player still swaps to make words.
     */
    private fun rain(bd: Board, n: Int, pool: String): MoveResult? {
        val spots = bd.positions().filter { bd[it].kind == TileKind.NORMAL && !bd[it].locked }
        if (spots.isEmpty()) return null
        var first: Board? = null
        var chosen: Board? = null
        for (attempt in 0 until 80) {
            val left = spots.toMutableList()
            var nb = bd
            val changed = ArrayList<Pos>()
            repeat(minOf(n, left.size)) {
                val p = left.removeAt(s.rng.nextInt(left.size))
                val options = pool.filter { it != nb[p].letter }.ifEmpty { pool }
                nb = nb.with(p, nb[p].copy(letter = options[s.rng.nextInt(options.length)]))
                changed += p
            }
            if (first == null) first = nb
            if (WordFinder.wordsThrough(nb, changed, dict).isEmpty()) { chosen = nb; break }
        }
        val nb = chosen ?: first!!
        s = s.copy(board = nb)
        return finish(nb, Effects())
    }

    private fun smash(cells: Set<Pos>, fullBreak: Boolean): MoveResult {
        val fx = effectsOf(board, emptyList(), cells, crateFullBreak = fullBreak)
        return apply(board, fx)
    }

    /** A swap that helps the goal most (longest word if nothing helps). Null if no swap makes a word. */
    fun hint(): Pair<Pos, Pos>? {
        var best: Pair<Pos, Pos>? = null
        var bestScore = 0.0
        for ((a, b) in board.swappablePairs()) {
            val g = previewGain(a, b)
            if (g > bestScore) { bestScore = g; best = a to b }
        }
        return best
    }

    /** Up to [max] different words the player can make with one swap now, best first (for the Hint strip). */
    fun hintWords(max: Int = 3): List<Pair<Pair<Pos, Pos>, String>> {
        val out = ArrayList<Triple<Pair<Pos, Pos>, String, Double>>()
        for ((a, b) in board.swappablePairs()) {
            val words = wordsForSwap(a, b)
            if (words.isEmpty()) continue
            val g = previewGain(a, b)
            for (w in words) if (out.none { it.second == w.word }) out += Triple(a to b, w.word, g + w.length)
        }
        return out.sortedByDescending { it.third }.take(max).map { it.first to it.second }
    }

    /** Every tile that is part of a word one swap away (Word Scanner). */
    fun scan(): Set<Pos> {
        val out = HashSet<Pos>()
        for ((a, b) in board.swappablePairs()) {
            val words = wordsForSwap(a, b)
            if (words.isEmpty()) continue
            // Show where the tiles are now: the swapped pair plus the word's other tiles.
            for (w in words) for (p in w.cells) out += when (p) { a -> b; b -> a; else -> p }
        }
        return out
    }

    /** Three letters for the Dice: letters that make a word first, then random ones. */
    fun diceOptions(p: Pos): List<Char> {
        val t = board[p]
        val good = ('A'..'Z').filter { it != t.letter && it != 'Q' }.filter { l ->
            WordFinder.wordsThrough(board.with(p, t.copy(letter = l, kind = TileKind.NORMAL)), listOf(p), dict).isNotEmpty()
        }
        val r = s.rng.copy()
        val out = LinkedHashSet<Char>()
        val shuffledGood = good.toMutableList()
        while (shuffledGood.isNotEmpty() && out.size < 2) out += shuffledGood.removeAt(r.nextInt(shuffledGood.size))
        while (out.size < 3) { val l = Letters.pick(r); if (l != t.letter) out += l }
        return out.toList().shuffledWith(r)
    }

    private fun <T> List<T>.shuffledWith(r: Rng): List<T> {
        val l = toMutableList()
        for (i in l.indices.reversed()) { val j = r.nextInt(i + 1); val x = l[i]; l[i] = l[j]; l[j] = x }
        return l
    }

    /** Best tile for a booster tutorial (where it helps most), or null for "any tile". */
    fun suggestTarget(booster: Booster): Pos? = when (booster) {
        Booster.HAMMER -> board.positions().filter { board[it].isCrate }.firstOrNull()
            ?: board.positions().filter { board[it].hasLetter && !board[it].isGold }.maxByOrNull { "JQXZVKWYFB".indexOf(board[it].letter).let { i -> if (i < 0) -1 else 10 - i } }
        Booster.JOKER -> board.positions().filter { board[it].movable }.maxByOrNull { p ->
            WordFinder.wordsThrough(board.with(p, board[p].copy(letter = JOKER_CHAR, kind = TileKind.JOKER)), listOf(p), dict).sumOf { it.length }
        }
        Booster.AXE -> board.positions().firstOrNull { board[it].isCrate || board[it].isIce || board[it].isChain }
        Booster.MAGNET -> board.positions().firstOrNull { board[it].isGold }
        else -> null
    }

    /** Best Magic Swap for the tutorial: the far-apart swap that makes the longest word. */
    fun suggestMagicSwap(): Pair<Pos, Pos>? {
        val cells = board.positions().filter { board[it].movable }
        var best: Pair<Pos, Pos>? = null
        var bestLen = 0
        for (i in cells.indices) for (j in i + 1 until cells.size) {
            val a = cells[i]; val b = cells[j]
            if (a.isNeighbour(b)) continue
            val len = WordFinder.wordsThrough(board.swap(a, b), listOf(a, b), dict).sumOf { it.length }
            if (len > bestLen) { bestLen = len; best = a to b }
        }
        return best
    }

    // ---------------------------------------------------------------- helpers for computer players

    /**
     * How much one swap helps: goal progress counts most, then word length. 0 if no word.
     * Used by the hint and by the level maker's computer players.
     */
    fun previewGain(a: Pos, b: Pos): Double {
        val swapped = board.swap(a, b)
        val words = WordFinder.wordsThrough(swapped, listOf(a, b), dict, rankFor(swapped))
        if (words.isEmpty()) return 0.0
        val fx = effectsOf(swapped, words, emptySet(), power = powerSpots(swapped, words, listOf(a, b)))
        val after = statsAfter(s.stats, fx)
        var gain = 0.0
        for (t in level.targets) gain += (minOf(t.count, t.progress(after)) - minOf(t.count, t.progress(s.stats))) * 10.0
        gain += fx.bonusMoves * 4.0
        gain += words.sumOf { it.length } * 0.5
        // An ice swap costs an extra move.
        if (board.swapCost(a, b) > 1) gain -= 3.0
        // Goals that only count after the board settles: digging sand and bringing friends down.
        if (level.targets.any { it is Objective.Treasure }) gain += fx.cleared.count { it in treasureCells && it !in s.dug } * 3.0
        if (level.targets.any { it is Objective.Rescue }) {
            for (p in fx.cleared) for (r in 0 until p.row) if (swapped[Pos(r, p.col)].isBuddy) gain += 3.0 + p.row
        }
        return gain
    }

    private fun hasWordMove(b: Board): Boolean = b.swappablePairs().any { (x, y) -> wordsForSwap(b, x, y, dict).isNotEmpty() }

    /** Mixes the movable tiles until at least one swap makes a word. Crates, ice and chains stay put. */
    private fun shuffled(b: Board): Board {
        val spots = b.positions().filter { b[it].movable && !b[it].locked }
        var best = b
        repeat(200) {
            val tiles = spots.map { b[it] }.shuffledWith(s.rng)
            var nb = b
            spots.forEachIndexed { i, p -> nb = nb.with(p, tiles[i].copy(dropFrom = 0)) }
            best = nb
            if (hasWordMove(nb)) return nb
        }
        return best
    }

    companion object {
        const val LEFTOVER_MOVE_POINTS = 50
        const val LEFTOVER_MOVE_COINS = 1
        const val LEFTOVER_COINS_MAX = 10
        /** The boss attacks after every this many swaps. */
        const val BOSS_EVERY = 4
        const val METER_SIZE = 8
        /** Power tiles start here (never in the first tutorials). */
        const val POWER_FROM_LEVEL = 4
        /** Smart refill keeps at least this many word-making swaps on the board. */
        const val MIN_WORD_SWAPS = 3
        const val REFILL_TRIES = 12

        fun startBoard(level: Level): Board {
            val ids = IdCounter()
            var b = Board.fromRows(level.start, ids, level.marks)
            // Give each crate its prize.
            var i = 0
            for (p in b.positions()) if (b[p].isCrate) {
                val prize = level.cratePrizes.getOrNull(i++) ?: Prize(PrizeKind.COINS, 5)
                b = b.with(p, b[p].copy(prize = prize))
            }
            return b
        }

        fun wordsForSwap(board: Board, a: Pos, b: Pos, dict: Dictionary): List<FoundWord> =
            WordFinder.wordsThrough(board.swap(a, b), listOf(a, b), dict)
    }
}
