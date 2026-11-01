package com.retroraja.wordnoid.core

enum class GameStatus { PLAYING, WON, LOST }

data class CrateHit(val pos: Pos, val broken: Boolean, val prize: Prize?)

/** Everything that happens when words burst (or a booster smashes something). */
data class Effects(
    val words: List<FoundWord> = emptyList(),
    /** Cells that burst (word tiles that aren't locked, broken crates, smashed tiles). */
    val cleared: Set<Pos> = emptySet(),
    val crateHits: List<CrateHit> = emptyList(),
    val chainsBroken: List<Pos> = emptyList(),
    val iceMelted: List<Pos> = emptyList(),
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
) {
    val isEmpty: Boolean get() = words.isEmpty() && cleared.isEmpty() && chainsBroken.isEmpty() && iceMelted.isEmpty()
}

data class MoveResult(
    /** The board right after the swap or booster change, before anything bursts. */
    val changedBoard: Board,
    val effects: Effects,
    /** After bursting and refilling. */
    val boardAfter: Board,
    /** Set if the board had no possible words left and was reshuffled for free. */
    val shuffled: Board?,
    val status: GameStatus,
    /** Extra points for moves left over when the level is won. */
    val leftoverBonus: Int,
    val leftoverCoins: Int,
)

/** The rules of one level being played. Has no screen code, so it can be tested and used by the level maker. */
class Game private constructor(val level: Level, private val dict: Dictionary, private var s: State) {

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
    ) {
        fun deep(): State = copy(rng = rng.copy(), ids = ids.copy())
    }

    private var undo: State? = null
    private val theme: Set<String> = level.themeName()?.let { Themes.byName(it) }?.let { Themes.cleaned(it, dict) } ?: emptySet()

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

    fun copy(): Game = Game(level, dict, s.deep()).also { it.undo = undo?.deep() }

    val goalMet: Boolean get() = level.isMet(s.stats)

    fun canSwap(a: Pos, b: Pos): Boolean =
        s.status == GameStatus.PLAYING && s.movesLeft > 0 && board.contains(a) && board.contains(b) && a.isNeighbour(b) &&
            board[a].movable && board[b].movable

    fun swap(a: Pos, b: Pos): MoveResult {
        require(canSwap(a, b)) { "Swap not allowed" }
        undo = s.deep()
        s = if (s.freeMoves > 0) s.copy(freeMoves = s.freeMoves - 1) else s.copy(movesLeft = s.movesLeft - 1)
        s = s.copy(movesUsed = s.movesUsed + 1, board = board.swap(a, b))
        return act(listOf(a, b))
    }

    /** "+N moves" from the out-of-moves screen. Keeps the board as it is. */
    fun continueWith(moves: Int) {
        s = s.copy(movesLeft = s.movesLeft + moves, status = if (s.status == GameStatus.LOST) GameStatus.PLAYING else s.status)
    }

    fun stars(): Int = if (s.status == GameStatus.WON) level.starsFor(s.movesLeft) else 0

    // ---------------------------------------------------------------- words and effects

    /** Words that one swap of [a] and [b] would make right now (without changing the game). */
    fun wordsForSwap(a: Pos, b: Pos): List<FoundWord> = wordsForSwap(board, a, b, dict)

    private fun act(changed: Collection<Pos>): MoveResult {
        val changedBoard = board
        val words = WordFinder.wordsThrough(changedBoard, changed.filter { changedBoard[it].hasLetter }, dict)
        val fx = if (words.isEmpty()) Effects() else effectsOf(changedBoard, words, emptySet())
        return apply(changedBoard, fx)
    }

    /**
     * Works out what bursting [words] (plus [smashed] cells, from boosters) does, without changing anything.
     */
    private fun effectsOf(b: Board, words: List<FoundWord>, smashed: Set<Pos>, crateFullBreak: Boolean = false): Effects {
        val wordCells = words.flatMap { it.cells }.toSet()
        val cleared = HashSet<Pos>()
        val crateHits = ArrayList<CrateHit>()
        val chains = ArrayList<Pos>()
        val ice = ArrayList<Pos>()
        val gold = ArrayList<Pos>()
        val gifts = ArrayList<Pos>()
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
                t.locked && p in wordCells -> {}
                else -> cleared += p
            }
            if (t.isGold) gold += p
            if (t.isGift) { gifts += p; boosters++ }
            if (t.isIce) ice += p
            if (t.isChain) chains += p
        }
        // Crates next to a word take one hit each. Smashed crates take one hit (or break fully with the Axe).
        val hitCrates = LinkedHashSet<Pos>()
        for (p in wordCells) for (n in b.neighbours(p)) if (b[n].isCrate) hitCrates += n
        for (p in smashed) if (b[p].isCrate) hitCrates += p
        for (p in hitCrates) {
            val t = b[p]
            val broken = crateFullBreak && p in smashed || t.hp <= 1
            val prize = if (broken) (t.prize ?: Prize(PrizeKind.COINS, 10)) else null
            crateHits += CrateHit(p, broken, prize)
            if (broken) {
                cleared += p
                when (prize!!.kind) {
                    PrizeKind.COINS -> coins += prize.amount
                    PrizeKind.MOVES -> bonusMoves += prize.amount
                    PrizeKind.BOOSTER -> boosters += prize.amount
                }
            }
        }
        // Chains next to a word snap (the tile stays, now free to move).
        for (p in wordCells) for (n in b.neighbours(p)) if (b[n].isChain && n !in chains) chains += n

        for (w in words) {
            val isTheme = w.word in theme
            if (isTheme) themeHits += w.word
            var c = w.length
            var pts = w.length * 10 * maxOf(1, w.length - 2)
            if (isTheme) { c *= 2; pts *= 2 }
            if (doubled) { c *= 2; pts *= 2 }
            coins += c
            points += pts
            meter += when { w.length >= 5 -> 3; w.length == 4 -> 2; else -> 1 }
            bonusMoves += when { w.length >= 5 -> 2; w.length == 4 -> 1; else -> 0 }
        }
        if (words.size >= 2) coins += 5
        points += 20 * (crateHits.size + chains.size + ice.size + gold.size)
        val bonusWord = !s.bonusFound && level.bonusWord != null && words.any { it.word == level.bonusWord }
        if (bonusWord) boosters++
        return Effects(
            words, cleared, crateHits, chains.distinct(), ice, gold, gifts, themeHits, bonusWord,
            bonusMoves, coins, points, meter, boosters, doubled,
        )
    }

    private fun statsAfter(st: Stats, fx: Effects): Stats {
        val letters = st.letters.toMutableMap()
        for (w in fx.words) for (ch in w.word) letters[ch] = (letters[ch] ?: 0) + 1
        return st.copy(
            words = st.words + fx.words,
            gold = st.gold + fx.gold.size,
            crates = st.crates + fx.crateHits.count { it.broken },
            ice = st.ice + fx.iceMelted.size,
            chains = st.chains + fx.chainsBroken.size,
            letters = letters,
            themeWords = st.themeWords + fx.themeWords.size,
        )
    }

    private fun apply(changedBoard: Board, fx: Effects): MoveResult {
        var b = changedBoard
        // Tiles that stay but change: hit crates, snapped chains, used locks.
        for (h in fx.crateHits) if (!h.broken) b = b.with(h.pos, b[h.pos].copy(hp = b[h.pos].hp - 1))
        for (p in fx.chainsBroken) if (p !in fx.cleared) b = b.with(p, b[p].copy(kind = TileKind.NORMAL))
        for (p in fx.iceMelted) if (p !in fx.cleared) b = b.with(p, b[p].copy(kind = TileKind.NORMAL))
        for (w in fx.words) for (p in w.cells) if (b[p].locked) b = b.with(p, b[p].copy(locked = false))
        val after = if (fx.cleared.isEmpty()) b else Refill.collapse(b, fx.cleared, s.rng, s.ids, dict, level.giftRate)
        s = s.copy(
            board = after,
            stats = statsAfter(s.stats, fx),
            movesLeft = s.movesLeft + fx.bonusMoves,
            coins = s.coins + fx.coins,
            score = s.score + fx.points,
            doubleNext = s.doubleNext && !fx.doubled,
            bonusFound = s.bonusFound || fx.bonusWord,
        )
        return finish(changedBoard, fx)
    }

    private fun finish(changedBoard: Board, fx: Effects): MoveResult {
        var leftover = 0
        var leftoverCoins = 0
        var shuffled: Board? = null
        val status = when {
            goalMet -> GameStatus.WON
            s.movesLeft <= 0 && s.freeMoves <= 0 -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }
        s = s.copy(status = status)
        if (status == GameStatus.WON) {
            leftover = s.movesLeft * LEFTOVER_MOVE_POINTS
            leftoverCoins = s.movesLeft * LEFTOVER_MOVE_COINS
            s = s.copy(score = s.score + leftover, coins = s.coins + leftoverCoins)
            undo = null
        } else if (status == GameStatus.PLAYING && !hasWordMove(board)) {
            shuffled = shuffled(board)
            s = s.copy(board = shuffled)
        }
        return MoveResult(changedBoard, fx, board, shuffled, status, leftover, leftoverCoins)
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
            Booster.HAMMER -> if (!ok(a)) null else smash(setOf(a!!), fullBreak = false)
            Booster.AXE -> {
                if (!ok(a)) null else {
                    val t = bd[a!!]
                    when {
                        t.isCrate -> smash(setOf(a), fullBreak = true)
                        t.isIce || t.isChain -> apply(bd, Effects(iceMelted = if (t.isIce) listOf(a) else emptyList(), chainsBroken = if (t.isChain) listOf(a) else emptyList()))
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
            Booster.PAINT, Booster.VOWEL_FLIP, Booster.DICE -> {
                val l = letter?.uppercaseChar()
                if (!ok(a) || !bd[a!!].movable || l == null || l !in 'A'..'Z' || (booster == Booster.VOWEL_FLIP && !Letters.isVowel(l))) null
                else {
                    val t = bd[a]
                    val kind = if (t.isJoker) TileKind.NORMAL else t.kind
                    s = s.copy(board = bd.with(a, t.copy(letter = l, kind = kind)))
                    act(listOf(a))
                }
            }
            Booster.SPIN -> {
                if (!ok(a)) null else {
                    val tl = a!!; val tr = Pos(tl.row, tl.col + 1); val bl = Pos(tl.row + 1, tl.col); val br = Pos(tl.row + 1, tl.col + 1)
                    val cells = listOf(tl, tr, br, bl)
                    if (!cells.all { bd.contains(it) && bd[it].movable }) null else {
                        var nb = bd
                        // Clockwise: each tile moves to the next corner.
                        for (i in cells.indices) nb = nb.with(cells[(i + 1) % 4], bd[cells[i]])
                        s = s.copy(board = nb)
                        act(cells)
                    }
                }
            }
            Booster.ROW_SLIDE, Booster.COLUMN_SLIDE -> {
                if (!ok(a)) null else {
                    val line = if (booster == Booster.ROW_SLIDE) WordFinder.rowLine(bd, a!!.row) else WordFinder.colLine(bd, a!!.col)
                    val movable = line.filter { bd[it].movable }
                    if (movable.size < 2) null else {
                        var nb = bd
                        val d = if (dir >= 0) 1 else -1
                        for (i in movable.indices) nb = nb.with(movable[(i + d + movable.size) % movable.size], bd[movable[i]])
                        s = s.copy(board = nb)
                        act(movable)
                    }
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
                val spots = bd.positions().filter { bd[it].kind == TileKind.NORMAL && !bd[it].locked }.toMutableList()
                if (spots.isEmpty()) null else {
                    var nb = bd
                    val changed = ArrayList<Pos>()
                    repeat(minOf(3, spots.size)) {
                        val p = spots.removeAt(s.rng.nextInt(spots.size))
                        val l = target ?: "AEIOU"[s.rng.nextInt(5)]
                        nb = nb.with(p, nb[p].copy(letter = l))
                        changed += p
                    }
                    s = s.copy(board = nb)
                    act(changed)
                }
            }
        }
        if (result == null) {
            s = before
            return null
        }
        if (booster != Booster.UNDO) undo = before
        return result
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
            ?: board.positions().filter { board[it].hasLetter }.maxByOrNull { "JQXZVKWYFB".indexOf(board[it].letter).let { i -> if (i < 0) -1 else 10 - i } }
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
        val words = WordFinder.wordsThrough(swapped, listOf(a, b), dict)
        if (words.isEmpty()) return 0.0
        val fx = effectsOf(swapped, words, emptySet())
        val after = statsAfter(s.stats, fx)
        var gain = 0.0
        for (t in level.targets) gain += (minOf(t.count, t.progress(after)) - minOf(t.count, t.progress(s.stats))) * 10.0
        gain += fx.bonusMoves * 4.0
        gain += words.sumOf { it.length } * 0.5
        return gain
    }

    private fun hasWordMove(b: Board): Boolean = b.swappablePairs().any { (x, y) -> wordsForSwap(b, x, y, dict).isNotEmpty() }

    /** Mixes the movable tiles until at least one swap makes a word. Crates, ice and chains stay put. */
    private fun shuffled(b: Board): Board {
        val spots = b.positions().filter { b[it].movable }
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
        const val LEFTOVER_MOVE_COINS = 3
        const val METER_SIZE = 8

        fun startBoard(level: Level): Board {
            val ids = IdCounter()
            var b = Board.fromRows(level.start, ids, level.marks)
            // Give each crate its prize.
            var i = 0
            for (p in b.positions()) if (b[p].isCrate) {
                val prize = level.cratePrizes.getOrNull(i++) ?: Prize(PrizeKind.COINS, 10)
                b = b.with(p, b[p].copy(prize = prize))
            }
            return b
        }

        fun wordsForSwap(board: Board, a: Pos, b: Pos, dict: Dictionary): List<FoundWord> =
            WordFinder.wordsThrough(board.swap(a, b), listOf(a, b), dict)
    }
}
