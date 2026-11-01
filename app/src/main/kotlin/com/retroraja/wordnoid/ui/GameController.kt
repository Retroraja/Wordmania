package com.retroraja.wordnoid.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Aim
import com.retroraja.wordnoid.core.Board
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.Feature
import com.retroraja.wordnoid.core.Game
import com.retroraja.wordnoid.core.GameStatus
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.core.MoveResult
import com.retroraja.wordnoid.core.Pos
import com.retroraja.wordnoid.core.PrizeKind
import com.retroraja.wordnoid.core.Stats
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** The big word shown in the middle of the board. */
data class WordBanner(val word: String, val praise: String, val lines: List<String>, val key: Int, val newWord: Boolean)

/** A floating "+1 move" style text. */
data class Toast(val id: Int, val text: String, val at: Offset, val color: Color, val big: Boolean = false)

/** A booster's animation on screen. */
data class BoosterFx(val booster: Booster, val cells: List<Pos>, val key: Int)

/** A tutorial bubble. [booster] glows on the right panel; [cells] glow on the board; [swap] shows a moving hand. */
data class Tutorial(
    val text: String,
    val booster: Booster? = null,
    val cells: List<Pos> = emptyList(),
    val swap: Pair<Pos, Pos>? = null,
    val dismissible: Boolean = false,
)

data class LetterPick(val booster: Booster, val pos: Pos, val letters: List<Char>)

/**
 * Runs one level on screen: takes taps and swipes, runs the rules (core.Game), and plays every step
 * with animations, sounds and rewards.
 */
class GameController(
    val level: Level,
    private val dict: Dictionary,
    val progress: Progress,
    private val sounds: SoundFx,
    private val haptics: Haptics,
    private val scope: CoroutineScope,
    bonusMoves: Int,
    private val startBoosters: List<Booster>,
) {
    val game = Game(level, dict, bonusMoves)
    val particles = ParticleField()

    var board by mutableStateOf(game.board); private set
    var movesLeft by mutableIntStateOf(game.movesLeft); private set
    var stats by mutableStateOf(Stats()); private set
    var score by mutableIntStateOf(0); private set
    var freeMoves by mutableIntStateOf(0); private set
    var doubleNext by mutableStateOf(false); private set
    var bonusFound by mutableStateOf(false); private set
    var status by mutableStateOf(GameStatus.PLAYING); private set
    var busy by mutableStateOf(false); private set
    var selected by mutableStateOf<Pos?>(null); private set
    var popping = mutableStateListOf<Long>()
    var flipping = mutableStateListOf<Long>()
    var glowing = mutableStateListOf<Long>()
    var banner by mutableStateOf<WordBanner?>(null); private set
    var shakeKey by mutableIntStateOf(0); private set
    var wobbleKey by mutableIntStateOf(0); private set
    var hint by mutableStateOf<Pair<Pos, Pos>?>(null); private set
    var scan by mutableStateOf<Set<Pos>>(emptySet()); private set
    var armed by mutableStateOf<Booster?>(null); private set
    var aimFirst by mutableStateOf<Pos?>(null); private set
    var slideAt by mutableStateOf<Pos?>(null); private set
    var letterPick by mutableStateOf<LetterPick?>(null); private set
    var fx by mutableStateOf<BoosterFx?>(null); private set
    var tutorial by mutableStateOf<Tutorial?>(null); private set
    var nudge by mutableStateOf<Booster?>(null); private set
    var mysteryCards by mutableStateOf<List<Booster>?>(null); private set
    var buyAsk by mutableStateOf<Booster?>(null); private set
    var showWin by mutableStateOf(false); private set
    var showLose by mutableStateOf(false); private set
    var winStars by mutableIntStateOf(0); private set
    var winBooster by mutableStateOf<Booster?>(null); private set
    var milestoneGift by mutableStateOf(false); private set
    var coinsThisLevel by mutableIntStateOf(0); private set
    var newWordsThisLevel by mutableIntStateOf(0); private set
    var longestWord by mutableStateOf(""); private set
    var wordRain by mutableStateOf(false); private set
    val toasts = mutableStateListOf<Toast>()
    var message by mutableStateOf<String?>(null); private set

    // Screen geometry, reported by the views (pixels, in the full-screen coordinates).
    var boardOrigin = Offset.Zero
    var cellPx = 1f
    var coinAnchor = Offset.Zero
    var movesAnchor = Offset.Zero
    var goalAnchor = Offset.Zero
    var screenWidth = 1000f
    var screenHeight = 600f

    private var keys = 0
    private var dryMoves = 0
    private var meterFills = 0
    private var tutorialStep = 0
    private var hintJob: Job? = null
    private var idleJob: Job? = null
    private val random = Random(level.seed)

    val wonAlready: Boolean get() = status == GameStatus.WON

    // ---------------------------------------------------------------- start

    /** Called once the intro card is closed. */
    fun begin() {
        // Free first use of the boosters this level teaches.
        for (b in level.introBoosters) if (!progress.seen("intro_b_${b.name}")) {
            progress.markSeen("intro_b_${b.name}")
            progress.setFreeUse(b, true)
        }
        scope.launch {
            for (b in startBoosters) {
                delay(250)
                applyStartBooster(b)
            }
            startTutorial()
            armIdleHint()
        }
    }

    private suspend fun applyStartBooster(b: Booster) {
        val r = when (b) {
            Booster.JOKER -> game.suggestTarget(Booster.JOKER)?.let { game.useBooster(b, it) }
            else -> game.useBooster(b)
        } ?: return
        toast("${b.emoji} ${b.title}!", center(), Palette.gold, big = true)
        sounds.play("sparkle")
        playResult(r, b)
    }

    private fun startTutorial() {
        val n = level.number
        when {
            n == 1 && level.tutorialSwap != null -> tutorial = Tutorial("Swipe or tap these two tiles to swap them and spell a word!", swap = level.tutorialSwap)
            n == 2 -> tutorial = Tutorial("NEW! ✨ Magic Swap swaps ANY two tiles, even far apart. Tap it!", booster = Booster.MAGIC_SWAP)
            n == 3 -> tutorial = Tutorial("NEW! 🃏 The Joker turns a tile into a star that can be any letter. Tap it!", booster = Booster.JOKER)
            else -> {
                val b = level.introBoosters.firstOrNull()
                if (b != null && progress.hasFreeUse(b)) {
                    tutorial = Tutorial("NEW BOOSTER! ${b.emoji} ${b.title}: ${b.description}. Your first one is free!", booster = b, dismissible = true)
                }
            }
        }
    }

    fun dismissTutorial() {
        if (tutorial?.dismissible == true) tutorial = null
    }

    private fun afterWordTutorial() {
        if (level.number == 1 && tutorialStep == 0) {
            tutorialStep = 1
            tutorial = Tutorial("Stuck with a hard letter? Tap the 🔨 Hammer to smash it. This one is free!", booster = Booster.HAMMER)
        } else if (level.number == 1 && tutorialStep == 2) {
            tutorialStep = 3
            tutorial = Tutorial("Tip: tap 💡 Hint any time you're stuck. Your first hint is free!", booster = Booster.HINT, dismissible = true)
        } else if (tutorial?.swap != null) {
            tutorial = null
        }
    }

    // ---------------------------------------------------------------- input

    fun tap(p: Pos) {
        if (busy || status != GameStatus.PLAYING) return
        armIdleHint()
        val b = armed
        if (b != null) { aim(b, p); return }
        val s = selected
        when {
            s == null -> { if (board[p].movable) { selected = p; sounds.play("tick") } else notMovable(p) }
            s == p -> selected = null
            s.isNeighbour(p) -> { selected = null; trySwap(s, p) }
            else -> selected = if (board[p].movable) p else null
        }
    }

    fun drag(from: Pos, to: Pos) {
        if (busy || status != GameStatus.PLAYING || armed != null) return
        if (!board.contains(to)) return
        selected = null
        armIdleHint()
        trySwap(from, to)
    }

    private fun notMovable(p: Pos) {
        val t = board[p]
        val text = when {
            t.isCrate -> "Make a word next to the crate to smash it!"
            t.isIce -> "Frozen! Use this letter in a word to melt it."
            t.isChain -> "Chained! Make a word next to it to snap the chain."
            else -> null
        }
        text?.let { message = it; scope.launch { delay(2200); if (message == text) message = null } }
        sounds.play("nope")
    }

    private fun trySwap(a: Pos, b: Pos) {
        if (!game.canSwap(a, b)) { sounds.play("nope"); return }
        // Guided steps can't be skipped: first do what the bubble asks.
        val t = tutorial
        if (t != null && !t.dismissible) {
            val ok = t.swap != null && (setOf(a, b) == setOf(t.swap.first, t.swap.second))
            if (!ok) {
                sounds.play("nope")
                message = if (t.swap != null) "Swap the two glowing tiles 👆" else "Tap the glowing ${t.booster?.emoji ?: "tile"} first 👆"
                scope.launch { delay(1500); message = null }
                return
            }
        }
        hint = null
        scan = emptySet()
        nudge = null
        busy = true
        scope.launch {
            sounds.play("swap")
            val r = game.swap(a, b)
            board = r.changedBoard
            delay(200)
            if (r.effects.words.isEmpty()) {
                sounds.play("clack")
                dryMoves++
                if (dryMoves >= 3) suggestBooster()
            } else dryMoves = 0
            playResult(r, null)
        }
    }

    private fun suggestBooster() {
        val options = listOf(Booster.HINT, Booster.SCANNER, Booster.SHUFFLE, Booster.HAMMER, Booster.JOKER)
            .filter { it.introLevel <= level.number && (progress.count(it) > 0 || progress.hasFreeUse(it)) }
            .sortedBy { if (it in panelBoosters(this)) 0 else 1 }
        val pick = options.firstOrNull() ?: return
        nudge = pick
        message = "Stuck? Try ${pick.emoji} ${pick.title}!"
        scope.launch { delay(2600); if (message?.startsWith("Stuck") == true) message = null }
    }

    private fun armIdleHint() {
        idleJob?.cancel()
        if (level.number > 5) return
        idleJob = scope.launch {
            delay(10_000)
            if (!busy && status == GameStatus.PLAYING && armed == null && hint == null) {
                hint = game.hint()
            }
        }
    }

    // ---------------------------------------------------------------- boosters

    fun boosterTapped(b: Booster) {
        if (busy || status != GameStatus.PLAYING) return
        if (armed == b) { cancelAim(); return }
        val t = tutorial
        if (t != null && !t.dismissible && armed == null && t.booster != b) {
            sounds.play("nope")
            message = if (t.swap != null) "First swap the two glowing tiles 👆" else "Tap the glowing ${t.booster?.emoji ?: "tile"} 👆"
            scope.launch { delay(1500); message = null }
            return
        }
        if (progress.count(b) <= 0 && !progress.hasFreeUse(b)) { buyAsk = b; return }
        nudge = null
        when (b.aim) {
            Aim.NONE -> useNow(b)
            else -> {
                armed = b
                aimFirst = null
                if (tutorial?.booster == b) {
                    val cells = when (b) {
                        Booster.MAGIC_SWAP -> game.suggestMagicSwap()?.toList() ?: emptyList()
                        else -> listOfNotNull(game.suggestTarget(b))
                    }
                    tutorial = Tutorial(aimText(b), cells = cells)
                }
                sounds.play("tick")
            }
        }
    }

    fun aimText(b: Booster): String = when (b.aim) {
        Aim.TILE -> "Tap a tile to use ${b.title}"
        Aim.MOVABLE -> "Tap a tile for ${b.title}"
        Aim.TWO_TILES -> if (aimFirst == null) "Tap the first tile to swap" else "Now tap the second tile"
        Aim.ROW -> "Tap a tile in the row to slide"
        Aim.COLUMN -> "Tap a tile in the column to slide"
        Aim.SQUARE -> "Tap the top-left tile of 4 tiles to spin"
        Aim.OBSTACLE -> "Tap a crate, ice or chain"
        Aim.GOLD -> "Tap a gold tile"
        Aim.NONE -> ""
    }

    fun cancelAim() {
        armed = null
        aimFirst = null
        slideAt = null
        letterPick = null
    }

    fun buy(b: Booster) {
        buyAsk = null
        val price = Rewards.price(b, progress.unlocked)
        if (progress.spend(price)) {
            progress.addBooster(b)
            sounds.play("cash")
            toast("+1 ${b.emoji}", center(), Palette.gold, big = true)
        } else {
            sounds.play("nope")
            message = "Not enough coins. Make words to earn more!"
            scope.launch { delay(2200); message = null }
        }
    }

    fun closeBuy() { buyAsk = null }

    private fun aim(b: Booster, p: Pos) {
        val t = board[p]
        when (b.aim) {
            Aim.TWO_TILES -> {
                val first = aimFirst
                if (first == null) {
                    if (!t.movable) { sounds.play("nope"); return }
                    aimFirst = p
                    sounds.play("tick")
                    tutorial?.let { if (it.cells.size == 2) tutorial = it.copy(text = "Now tap the second glowing tile") }
                } else if (first == p) aimFirst = null
                else apply(b, first, p)
            }
            Aim.ROW, Aim.COLUMN -> { slideAt = p; sounds.play("tick") }
            Aim.MOVABLE -> {
                if (!t.movable) { sounds.play("nope"); return }
                when (b) {
                    Booster.PAINT -> letterPick = LetterPick(b, p, ('A'..'Z').toList())
                    Booster.VOWEL_FLIP -> letterPick = LetterPick(b, p, listOf('A', 'E', 'I', 'O', 'U'))
                    Booster.DICE -> { sounds.play("rain"); letterPick = LetterPick(b, p, game.diceOptions(p)) }
                    else -> apply(b, p)
                }
            }
            else -> apply(b, p)
        }
    }

    fun slide(dir: Int) {
        val b = armed ?: return
        val p = slideAt ?: return
        slideAt = null
        apply(b, p, dir = dir)
    }

    fun pickLetter(c: Char) {
        val lp = letterPick ?: return
        letterPick = null
        apply(lp.booster, lp.pos, letter = c)
    }

    private fun useNow(b: Booster) {
        when (b) {
            Booster.HINT -> {
                val h = game.hint()
                if (h == null) { sounds.play("nope"); return }
                consume(b)
                hint = h
                fx = BoosterFx(b, h.toList(), ++keys)
                sounds.play("ding")
                if (tutorial?.booster == b) tutorial = null
            }
            Booster.SCANNER -> {
                val cells = game.scan()
                consume(b)
                fx = BoosterFx(b, emptyList(), ++keys)
                sounds.play("whoosh")
                scope.launch { delay(700); scan = cells; delay(6000); scan = emptySet() }
                if (tutorial?.booster == b) tutorial = null
            }
            else -> apply(b, null)
        }
    }

    private fun consume(b: Booster) {
        val wasFree = progress.hasFreeUse(b)
        progress.useBooster(b)
        if (wasFree && !b.strong) {
            progress.addBooster(b)
            scope.launch { delay(900); toast("+1 ${b.emoji} ${b.title} to keep!", center(), Palette.green, big = true) }
        }
    }

    private fun apply(b: Booster, a: Pos?, second: Pos? = null, letter: Char? = null, dir: Int = 1) {
        busy = true
        val before = board
        scope.launch {
            val cells = listOfNotNull(a, second) + when (b) {
                Booster.SPIN -> a?.let { listOf(Pos(it.row, it.col + 1), Pos(it.row + 1, it.col), Pos(it.row + 1, it.col + 1)) } ?: emptyList()
                else -> emptyList()
            }
            fx = BoosterFx(b, cells, ++keys)
            sounds.play(boosterSound(b))
            haptics.tap()
            delay(boosterLead(b))
            val r = game.useBooster(b, a, second, letter, dir)
            if (r == null) {
                sounds.play("nope")
                message = "Can't use ${b.title} there"
                launch { delay(1800); message = null }
                busy = false
                return@launch
            }
            consume(b)
            cancelAim()
            if (tutorial?.booster == b || tutorial?.cells?.isNotEmpty() == true) {
                tutorial = null
                if (level.number == 1 && tutorialStep == 1) tutorialStep = 2
            }
            when (b) {
                Booster.UNDO -> { wobbleKey++ }
                Booster.SHUFFLE -> { flipping.addAll(board.tiles().map { it.id }) }
                Booster.JOKER, Booster.PAINT, Booster.VOWEL_FLIP, Booster.DICE, Booster.LOCK -> a?.let { flipping.add(before[it].id) }
                Booster.LETTER_RAIN -> flipping.addAll(r.changedBoard.positions().filter { r.changedBoard[it].letter != before[it].letter }.map { before[it].id })
                else -> {}
            }
            if (b == Booster.PLUS_MOVES) toast("+3 moves", movesAnchor, Palette.green, big = true)
            if (b == Booster.FREE_MOVE) toast("Next swap is free!", movesAnchor, Palette.cyan, big = true)
            if (b == Booster.DOUBLE) toast("Next word x2!", center(), Palette.gold, big = true)
            board = r.changedBoard
            sync()
            delay(250)
            flipping.clear()
            playResult(r, b)
        }
    }

    private fun boosterSound(b: Booster) = when (b) {
        Booster.HAMMER -> "thunk"
        Booster.AXE -> "chop"
        Booster.MAGIC_SWAP, Booster.JOKER -> "sparkle"
        Booster.PAINT, Booster.VOWEL_FLIP, Booster.DICE -> "flip"
        Booster.UNDO -> "rewind"
        Booster.SHUFFLE, Booster.SPIN, Booster.ROW_SLIDE, Booster.COLUMN_SLIDE -> "whoosh"
        Booster.FREE_MOVE, Booster.PLUS_MOVES, Booster.DOUBLE -> "fanfare"
        Booster.LOCK -> "click"
        Booster.MAGNET -> "hum"
        Booster.LETTER_RAIN -> "rain"
        Booster.HINT -> "ding"
        Booster.SCANNER -> "whoosh"
    }

    private fun boosterLead(b: Booster): Long = when (b) {
        Booster.HAMMER, Booster.AXE -> 380
        Booster.MAGNET -> 450
        Booster.LOCK -> 300
        Booster.LETTER_RAIN -> 500
        else -> 200
    }

    // ---------------------------------------------------------------- playing out a move

    private fun sync() {
        movesLeft = game.movesLeft
        stats = game.stats
        score = game.score
        freeMoves = game.freeMoves
        doubleNext = game.doubleNext
        bonusFound = game.bonusFound
    }

    private suspend fun playResult(r: MoveResult, booster: Booster?) {
        val fx0 = r.effects
        busy = true
        if (!fx0.isEmpty) celebrate(r) else if (r.boardAfter !== r.changedBoard) {
            board = r.boardAfter
        }
        sync()
        if (r.shuffled != null) {
            message = "No words left. Shuffling!"
            sounds.play("shuffle")
            delay(500)
            flipping.addAll(r.shuffled!!.tiles().map { it.id })
            board = r.shuffled!!
            delay(500)
            flipping.clear()
            message = null
        }
        when (r.status) {
            GameStatus.WON -> win(r)
            GameStatus.LOST -> lose()
            GameStatus.PLAYING -> {}
        }
        status = r.status
        busy = false
        if (meterFills > 0 && status == GameStatus.PLAYING) openMystery()
        if (fx0.words.isNotEmpty()) afterWordTutorial()
    }

    private fun praise(len: Int) = when {
        len >= 7 -> "WORDNOID!"
        len == 6 -> "AMAZING!"
        len == 5 -> "AWESOME!"
        len == 4 -> "GREAT!"
        else -> "NICE!"
    }

    private suspend fun celebrate(r: MoveResult) {
        val fx0 = r.effects
        val b0 = r.changedBoard
        // 1. Each word lights up letter by letter with rising notes, tiles lift and flip.
        for ((wi, w) in fx0.words.withIndex()) {
            val ids = w.cells.map { b0[it].id }
            glowing.addAll(ids)
            for ((i, id) in ids.withIndex()) {
                popping.add(id)
                sounds.play("note${(i + wi * 2).coerceAtMost(9)}")
                delay(85)
            }
            val isNew = progress.isNewWord(w.word)
            if (isNew) { progress.addWord(w.word); progress.addCoins(Progress.NEW_WORD_COINS); coinsThisLevel += Progress.NEW_WORD_COINS; newWordsThisLevel++ }
            if (w.word.length > longestWord.length) longestWord = w.word
            val lines = buildList {
                add("+${w.length} 🪙")
                if (w.length >= 5) add("+2 moves") else if (w.length == 4) add("+1 move")
                if (w.word in fx0.themeWords) add("${level.themeName()} word! x2")
                if (fx0.doubled) add("Double points!")
                if (fx0.bonusWord && w.word == level.bonusWord) add("Bonus word! 🎁")
            }
            banner = WordBanner(w.word, if (fx0.words.size >= 2 && wi == 1) "DOUBLE!" else praise(w.length), lines, ++keys, isNew)
            haptics.word()
            if (w.length >= 5) { shakeKey++; haptics.big(); sounds.play("fanfare") }
            delay(if (fx0.words.size > 1) 750 else 900)
        }
        // 2. Burst!
        val cleared = fx0.cleared
        for (p in cleared) {
            val t = b0[p]
            if (!t.isCrate) particles.burst(cellCenter(p), tileColor(t), if (fx0.words.any { it.length >= 5 }) 1.4f else 1f)
        }
        if (fx0.words.isNotEmpty()) sounds.play("burst")
        if (fx0.words.any { it.length >= 5 }) {
            particles.firework(center())
            sounds.play("bang")
        }
        for (h in fx0.crateHits) {
            particles.splinters(cellCenter(h.pos))
            sounds.play("crack")
            h.prize?.let { prize ->
                val text = when (prize.kind) {
                    PrizeKind.COINS -> "+${prize.amount} 🪙"
                    PrizeKind.MOVES -> "+${prize.amount} move"
                    PrizeKind.BOOSTER -> "🎁 Booster!"
                }
                toast(text, cellCenter(h.pos), Palette.gold)
            }
        }
        if (fx0.iceMelted.isNotEmpty()) sounds.play("shatter")
        for (p in fx0.iceMelted) particles.shards(cellCenter(p))
        if (fx0.chainsBroken.isNotEmpty()) sounds.play("snap")
        for (p in fx0.chainsBroken) particles.links(cellCenter(p))
        for (p in fx0.gold) { particles.sparkle(cellCenter(p)); particles.coins(cellCenter(p), goalAnchor, 2) }
        // 3. Coins fly to the counter; moves and boosters are handed out.
        if (fx0.coins > 0) {
            val from = fx0.words.firstOrNull()?.cells?.let { cellCenter(it[it.size / 2]) } ?: center()
            particles.coins(from, coinAnchor, fx0.coins)
            progress.addCoins(fx0.coins)
            coinsThisLevel += fx0.coins
            scope.launch { delay(700); sounds.play("coin") }
        }
        if (fx0.bonusMoves > 0) toast("+${fx0.bonusMoves} move${if (fx0.bonusMoves > 1) "s" else ""}", movesAnchor, Palette.green, big = true)
        repeat(fx0.boosters) {
            val prize = Rewards.randomBooster(level.number, random)
            progress.addBooster(prize)
            scope.launch { delay(400L + it * 500L); sounds.play("sparkle"); toast("${prize.emoji} ${prize.title}!", center(), Palette.pink, big = true) }
        }
        if (fx0.meter > 0) {
            meterFills += progress.addMeter(fx0.meter)
        }
        delay(120)
        // 4. Tiles fall in.
        board = r.boardAfter
        popping.clear()
        glowing.clear()
        sync()
        delay(420)
        scope.launch { delay(500); banner = null }
    }

    private suspend fun win(r: MoveResult) {
        idleJob?.cancel()
        tutorial = null
        delay(400)
        sounds.play("win")
        message = "LEVEL COMPLETE!"
        particles.confettiRain(screenWidth)
        // Word Rain: every leftover move pops a tile into points and coins.
        val left = game.movesLeft
        if (left > 0) {
            wordRain = true
            val cells = board.positions().filter { board[it].hasLetter }
            repeat(left.coerceAtMost(15)) {
                val p = cells[random.nextInt(cells.size)]
                particles.burst(cellCenter(p), tileColor(board[p]), 0.6f)
                particles.coins(cellCenter(p), coinAnchor, 1)
                sounds.play("pop")
                delay(140)
            }
            wordRain = false
            if (r.leftoverCoins > 0) { progress.addCoins(r.leftoverCoins); coinsThisLevel += r.leftoverCoins }
        }
        // Fireworks!
        repeat(5) {
            particles.firework(Offset(screenWidth * (0.2f + random.nextFloat() * 0.6f), screenHeight * (0.15f + random.nextFloat() * 0.35f)))
            sounds.play(if (it % 2 == 0) "bang" else "crackle")
            delay(260)
        }
        message = null
        winStars = game.stars()
        val winCoins = WIN_COINS + winStars * STAR_COINS
        progress.addCoins(winCoins)
        coinsThisLevel += winCoins
        progress.levelWon(level.number, winStars)
        if (winStars == 3 && random.nextDouble() < 0.3) winBooster = Rewards.randomBooster(level.number, random).also { progress.addBooster(it) }
        if (level.number in Progress.MILESTONES && !progress.seen("milestone_${level.number}")) {
            progress.markSeen("milestone_${level.number}")
            milestoneGift = true
            progress.addCoins(100)
            val gift = Rewards.randomBooster(level.number, random)
            progress.addBooster(gift)
            if (winBooster == null) winBooster = gift
        }
        delay(300)
        showWin = true
    }

    private suspend fun lose() {
        idleJob?.cancel()
        delay(500)
        sounds.play("lose")
        showLose = true
    }

    /** Continue after running out of moves: 150 coins for more moves (more after many fails). */
    fun continueForCoins(): Boolean {
        val moves = Progress.continueMoves(progress.fails(level.number))
        if (!progress.spend(Progress.CONTINUE_PRICE)) { sounds.play("nope"); return false }
        sounds.play("cash")
        game.continueWith(moves)
        showLose = false
        status = GameStatus.PLAYING
        sync()
        toast("+$moves moves!", movesAnchor, Palette.green, big = true)
        return true
    }

    fun continueWithBooster(): Boolean {
        if (!progress.useBooster(Booster.PLUS_MOVES)) return false
        game.continueWith(3)
        showLose = false
        status = GameStatus.PLAYING
        sync()
        sounds.play("fanfare")
        toast("+3 moves!", movesAnchor, Palette.green, big = true)
        return true
    }

    fun giveUp() {
        if (status == GameStatus.LOST) progress.levelLost(level.number)
    }

    // ---------------------------------------------------------------- Power Meter mystery cards

    private fun openMystery() {
        if (meterFills <= 0) return
        meterFills--
        sounds.play("fanfare")
        mysteryCards = Rewards.mysteryCards(progress.unlocked.coerceAtLeast(level.number), random)
    }

    fun mysteryPicked(b: Booster) {
        progress.addBooster(b)
        sounds.play("sparkle")
    }

    fun mysteryDone() {
        mysteryCards = null
        if (meterFills > 0) scope.launch { delay(300); openMystery() }
    }

    // ---------------------------------------------------------------- helpers

    fun cellCenter(p: Pos): Offset = Offset(boardOrigin.x + (p.col + 0.5f) * cellPx, boardOrigin.y + (p.row + 0.5f) * cellPx)

    fun center(): Offset = Offset(boardOrigin.x + board.cols * cellPx / 2, boardOrigin.y + board.rows * cellPx / 2)

    fun toast(text: String, at: Offset, color: Color, big: Boolean = false) {
        val t = Toast(++keys, text, at, color, big)
        toasts.add(t)
        scope.launch { delay(1600); toasts.remove(t) }
    }

    fun boardAt(): Board = board

    companion object {
        const val WIN_COINS = 20
        const val STAR_COINS = 10
    }
}
