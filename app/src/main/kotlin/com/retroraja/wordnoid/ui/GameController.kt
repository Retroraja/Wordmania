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
import com.retroraja.wordnoid.core.FoundWord
import com.retroraja.wordnoid.core.Game
import com.retroraja.wordnoid.core.GameStatus
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.core.MoveResult
import com.retroraja.wordnoid.core.Objective
import com.retroraja.wordnoid.core.Pos
import com.retroraja.wordnoid.core.PrizeKind
import com.retroraja.wordnoid.core.Stats
import com.retroraja.wordnoid.data.Daily
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** The big word shown in the middle of the board. */
data class WordBanner(val word: String, val praise: String, val lines: List<String>, val key: Int, val newWord: Boolean)

/** A floating "+1 move" style text. */
data class Toast(val id: Int, val text: String, val at: Offset, val color: Color, val big: Boolean = false)

/** A booster's animation on screen. */
data class BoosterFx(val booster: Booster, val cells: List<Pos>, val key: Int)

/** A tutorial step, told by Tiko in the strip under the board. [booster] glows on the panel; [cells] glow on the board; [swap] shows a moving hand. */
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
    /** The boss shakes when it is hit (a word) and when it attacks. */
    var bossHitKey by mutableIntStateOf(0); private set
    var bossAttackKey by mutableIntStateOf(0); private set
    /** Hides the flat board while its 3D copy falls apart (out of moves). */
    var boardHidden by mutableStateOf(false); private set
    var hint by mutableStateOf<Pair<Pos, Pos>?>(null); private set
    /** A locked letter the player tapped: the "🔓 Unlock" bubble shows over it. */
    var unlockAsk by mutableStateOf<Pos?>(null); private set
    var scan by mutableStateOf<Set<Pos>>(emptySet()); private set
    var armed by mutableStateOf<Booster?>(null); private set
    var aimFirst by mutableStateOf<Pos?>(null); private set
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
    var winCoins by mutableIntStateOf(0); private set
    var coinsThisLevel by mutableIntStateOf(0); private set
    var newWordsThisLevel by mutableIntStateOf(0); private set
    var longestWord by mutableStateOf(""); private set
    var wordRain by mutableStateOf(false); private set
    /** Every word made in this level, in order (shown on the left and on the win screen). */
    val foundWords = mutableStateListOf<String>()
    val toasts = mutableStateListOf<Toast>()
    /** A short message in Tiko's strip (overrides the normal talk for a moment). */
    var message by mutableStateOf<String?>(null); private set
    /** What Tiko says when nothing else is going on: the goal at the start, then encouragement. */
    var tikoSay by mutableStateOf(goalSpeech()); private set
    /** Hint words shown in the strip. */
    var hintWords by mutableStateOf<List<String>>(emptyList()); private set

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
    private var idleJob: Job? = null
    private val random = Random(level.seed)

    val wonAlready: Boolean get() = status == GameStatus.WON

    /** What Tiko says at the start: the goal in plain words. */
    private fun goalSpeech(): String {
        val goals = level.targets.joinToString(", then ") { it.text.lowercase() }.replaceFirstChar { it.uppercase() }
        return if (level.boss) "${worldOf(level.number).villain} is here! $goals before the moves run out!" else "Goal: $goals."
    }

    /** The text in Tiko's strip right now. */
    val stripText: String
        get() = tutorial?.text ?: message ?: (if (hintWords.isNotEmpty()) "💡 Try: " + hintWords.joinToString(", ") else lockText() ?: tikoSay)

    /** Locked letters on the board right now. */
    val lockedCells: List<Pos> get() = board.positions().filter { board[it].locked }

    private fun lockText(): String? {
        val locked = lockedCells
        if (locked.isEmpty()) return null
        val letters = locked.joinToString(" and ") { board[it].letter.toString() }
        return if (locked.size == 1) "🔒 $letters is locked. Tap it to unlock." else "🔒 $letters are locked. Tap one to unlock."
    }

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
        if (level.number == 1 && level.tutorialSwap != null) {
            tutorial = Tutorial("Swipe or tap these two glowing tiles to swap them and spell a word!", swap = level.tutorialSwap)
            return
        }
        val b = level.introBoosters.firstOrNull()
        if (b != null && progress.hasFreeUse(b)) {
            tutorial = Tutorial("NEW BOOSTER! ${b.emoji} ${b.title}: ${b.description}. Tap it, your first one is free!", booster = b, dismissible = true)
        }
    }

    fun dismissTutorial() {
        if (tutorial?.dismissible == true) tutorial = null
    }

    private fun afterWordTutorial() {
        if (level.number == 1 && tutorialStep == 0) {
            tutorialStep = 1
            tutorial = Tutorial("Stuck with a hard letter? Tap the 🔨 Hammer, then tap a tile to smash it. This one is free!", booster = Booster.HAMMER)
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
        val ask = unlockAsk
        unlockAsk = null
        val s = selected
        // Tapping a locked letter (with nothing picked) opens the Unlock bubble. It is also picked, so it can still be swapped.
        if (s == null && board[p].locked && ask != p) {
            unlockAsk = p
            selected = p
            sounds.play("tick")
            return
        }
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
        unlockAsk = null
        armIdleHint()
        trySwap(from, to)
    }

    /** The "🔓 Unlock" bubble was tapped: free, and words through the letter are checked right away. */
    fun unlockTapped() {
        val p = unlockAsk ?: return
        unlockAsk = null
        selected = null
        if (busy || status != GameStatus.PLAYING) return
        if (tutorial?.booster == Booster.LOCK || tutorial?.cells == listOf(p)) tutorial = null
        busy = true
        scope.launch {
            val id = board[p].id
            sounds.play("unlock")
            haptics.tap()
            val r = game.unlock(p)
            if (r == null) { busy = false; return@launch }
            flipping.add(id)
            if (fx3) Fx.call("booster", fxBoard(Fx.at(cellCenter(p), Fx.obj("k" to "UNLOCK")))) else particles.sparkle(cellCenter(p), Palette.gold, 14)
            board = r.changedBoard
            delay(250)
            flipping.clear()
            if (r.effects.words.isEmpty()) say("Unlocked! No word here yet.", 1600)
            playResult(r, null)
        }
    }

    fun closeUnlock() { unlockAsk = null }

    /** Long press on a booster button: what it does. */
    fun boosterInfo(b: Booster) { sounds.play("tick"); say("${b.emoji} ${b.title}: ${b.description}", 3200) }

    private fun say(text: String, ms: Long = 2400) {
        message = text
        scope.launch { delay(ms); if (message == text) message = null }
    }

    private fun notMovable(p: Pos) {
        val t = board[p]
        val text = when {
            t.isCrate -> "Make a word right next to the box to break it!"
            t.isIce -> "Frozen! Use this letter in a word to melt it."
            t.isChain -> "Chained! Make a word next to it to snap the chain."
            t.isBuddy -> "That's a Tiko friend! Use the letters under it in words so it drops to the bottom."
            else -> null
        }
        text?.let { say(it) }
        sounds.play("nope")
    }

    private fun trySwap(a: Pos, b: Pos) {
        if (!game.canSwap(a, b)) { sounds.play("nope"); return }
        // Guided steps can't be skipped: first do what Tiko asks.
        val t = tutorial
        if (t != null && !t.dismissible) {
            val ok = t.swap != null && (setOf(a, b) == setOf(t.swap.first, t.swap.second))
            if (!ok) {
                sounds.play("nope")
                say(if (t.swap != null) "Swap the two glowing tiles 👆" else "Tap the glowing ${t.booster?.emoji ?: "button"} first 👆", 1500)
                return
            }
        }
        hint = null
        hintWords = emptyList()
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
        val pick = options.firstOrNull() ?: return
        nudge = pick
        say("Stuck? Try ${pick.emoji} ${pick.title}!", 2600)
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

    /** "Stuck hint" from a rewarded video: shows words like the Hint booster, without using one. */
    fun freeHint() {
        val words = game.hintWords(3)
        if (words.isEmpty()) return
        hint = words.first().first
        hintWords = words.map { it.second }
        sounds.play("ding")
    }

    // ---------------------------------------------------------------- boosters

    fun boosterTapped(b: Booster) {
        if (busy || status != GameStatus.PLAYING) return
        if (armed == b) { cancelAim(); return }
        val t = tutorial
        if (t != null && !t.dismissible && armed == null && t.booster != b) {
            sounds.play("nope")
            say(if (t.swap != null) "First swap the two glowing tiles 👆" else "Tap the glowing ${t.booster?.emoji ?: "button"} 👆", 1500)
            return
        }
        if (progress.count(b) <= 0 && !progress.hasFreeUse(b)) { buyAsk = b; return }
        nudge = null
        when (b.aim) {
            Aim.NONE -> useNow(b)
            else -> {
                armed = b
                aimFirst = null
                val cells = when (b) {
                    Booster.MAGIC_SWAP -> game.suggestMagicSwap()?.toList() ?: emptyList()
                    else -> listOfNotNull(game.suggestTarget(b))
                }
                // Only point at a glowing tile when there really is one. Otherwise just say what to tap.
                tutorial = if (tutorial?.booster == b && cells.isNotEmpty()) Tutorial(aimText(b), cells = cells) else null
                if (tutorial == null) say(aimText(b), 4000)
                sounds.play("tick")
            }
        }
    }

    fun aimText(b: Booster): String = when (b.aim) {
        Aim.TILE -> "Tap any tile or box to use the ${b.title}"
        Aim.MOVABLE -> "Tap any tile for the ${b.title}"
        Aim.TWO_TILES -> if (aimFirst == null) "Tap the first tile to swap" else "Now tap the second tile"
        Aim.ROW -> "Tap any tile in the row to turn into a word"
        Aim.COLUMN -> "Tap any tile in the column to turn into a word"
        Aim.SQUARE -> "Tap the top-left tile of 4 tiles to spin"
        Aim.OBSTACLE -> "Tap a box, ice or chain"
        Aim.GOLD -> "Tap a gold tile"
        Aim.NONE -> ""
    }

    fun cancelAim() {
        armed = null
        aimFirst = null
        letterPick = null
        if (tutorial?.cells?.isNotEmpty() == true && tutorial?.booster == null) tutorial = null
        message = null
    }

    fun buy(b: Booster) {
        buyAsk = null
        val price = Rewards.price(b, progress.unlocked)
        if (progress.spend(price)) {
            progress.addBooster(b)
            sounds.play("cash")
            toast("+1 ${b.emoji}", center(), Palette.gold, big = true)
            // Buying from an empty slot uses it straight away.
            boosterTapped(b)
        } else {
            sounds.play("nope")
            say("Not enough coins. Win levels and open boxes to earn more!")
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
                    if (tutorial == null) say("Now tap the second tile", 4000)
                } else if (first == p) aimFirst = null
                else apply(b, first, p)
            }
            Aim.MOVABLE -> {
                if (!t.movable) { sounds.play("nope"); say("Pick a tile that can move"); return }
                if (b == Booster.LOCK && t.locked) { sounds.play("nope"); say("That letter is already locked. Pick another one."); return }
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

    fun pickLetter(c: Char) {
        val lp = letterPick ?: return
        letterPick = null
        apply(lp.booster, lp.pos, letter = c)
    }

    private fun useNow(b: Booster) {
        when (b) {
            Booster.HINT -> {
                val words = game.hintWords(3)
                if (words.isEmpty()) { sounds.play("nope"); return }
                consume(b)
                hint = words.first().first
                hintWords = words.map { it.second }
                fx = BoosterFx(b, words.first().first.toList(), ++keys)
                sounds.play("ding")
                if (tutorial?.booster == b) tutorial = null
                Daily.report(progress, Daily.Kind.BOOSTERS)
            }
            Booster.SCANNER -> {
                val cells = game.scan()
                consume(b)
                fx = BoosterFx(b, emptyList(), ++keys)
                sounds.play("whoosh")
                scope.launch { delay(700); scan = cells; delay(6000); scan = emptySet() }
                if (tutorial?.booster == b) tutorial = null
                Daily.report(progress, Daily.Kind.BOOSTERS)
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

    private fun apply(b: Booster, a: Pos?, second: Pos? = null, letter: Char? = null) {
        busy = true
        val before = board
        scope.launch {
            val cells = listOfNotNull(a, second) + when (b) {
                Booster.SPIN -> a?.let { listOf(Pos(it.row, it.col + 1), Pos(it.row + 1, it.col), Pos(it.row + 1, it.col + 1)) } ?: emptyList()
                else -> emptyList()
            }
            if (fx3) Fx.call("booster", fxBoard(Fx.obj("k" to b.name, "cells" to fxCells(cells))).also { o -> a?.let { Fx.at(cellCenter(it), o) } })
            else fx = BoosterFx(b, cells, ++keys)
            sounds.play(boosterSound(b))
            haptics.tap()
            delay(boosterLead(b))
            val r = game.useBooster(b, a, second, letter)
            if (r == null) {
                sounds.play("nope")
                say(
                    when (b) {
                        Booster.SPIN, Booster.ROW_SLIDE, Booster.COLUMN_SLIDE -> "No word can be made there, so your ${b.title} is kept. Try another spot!"
                        Booster.HAMMER -> "The Hammer can't smash gold tiles or Tiko friends. Gold is collected with words!"
                        Booster.TREASURE_FINDER -> "No treasure left to find!"
                        else -> "Can't use ${b.title} there"
                    }, 2600,
                )
                cancelAim()
                busy = false
                return@launch
            }
            consume(b)
            Daily.report(progress, Daily.Kind.BOOSTERS)
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
                Booster.TREASURE_FINDER -> for (i in r.chestsFound) treasureFound(i)
                else -> {}
            }
            if (b == Booster.LOCK && a != null) {
                toast("🔒 Locked!", cellCenter(a), Palette.gold, big = true)
                if (level.introBoosters.contains(Booster.LOCK) && !progress.seen("lock_tut")) {
                    progress.markSeen("lock_tut")
                    tutorial = Tutorial("Locked! It won't make words now. Move it anywhere, then tap it and tap 🔓 Unlock.", dismissible = true)
                }
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
        Booster.LOCK -> "lock"
        Booster.MAGNET -> "hum"
        Booster.LETTER_RAIN -> "rain"
        Booster.HINT -> "ding"
        Booster.SCANNER -> "whoosh"
        Booster.TREASURE_FINDER -> "sparkle"
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
        for (p in fx0.iceCracked) { if (fx3) Fx.call("iceCrack", Fx.at(cellCenter(p))) else particles.shards(cellCenter(p)); sounds.play("crack"); say("Cracked! One more hit melts the ice.", 1800) }
        if (r.rescued > 0) {
            sounds.play("fanfare")
            if (fx3) Fx.call("friend", Fx.at(center())) else {
                toast("💖 Friend rescued!", center(), Palette.green, big = true)
                particles.sparkle(center(), Palette.green, 20)
            }
        }
        if (booster != Booster.TREASURE_FINDER) for (i in r.chestsFound) treasureFound(i)
        sync()
        r.bossAttack?.let { atk ->
            bossAttackKey++
            if (fx3 && level.boss) {
                Fx.call("bossAttack", Fx.at(cellCenter(atk.pos), Fx.obj("freeze" to atk.freeze)))
                Fx.await("bossAttackLanded", 1600)
            }
            sounds.play(if (atk.freeze) "shatter" else "thunk")
            haptics.big()
            say("${worldOf(level.number).villain} ${if (atk.freeze) "froze a tile!" else "dropped a box!"}", 1800)
            delay(450)
            board = game.board
        }
        if (r.shuffled != null) {
            say("No words left. Shuffling!", 1000)
            sounds.play("shuffle")
            delay(500)
            flipping.addAll(r.shuffled!!.tiles().map { it.id })
            board = r.shuffled!!
            delay(500)
            flipping.clear()
        }
        when (r.status) {
            GameStatus.WON -> win(r)
            GameStatus.LOST -> lose()
            GameStatus.PLAYING -> {}
        }
        status = r.status
        busy = false
        if (meterFills > 0 && status == GameStatus.PLAYING) openMystery()
        if (fx0.words.isNotEmpty() && status == GameStatus.PLAYING) afterWordTutorial()
    }

    /** A treasure chest was dug out: a real prize. */
    private fun treasureFound(i: Int) {
        val cells = level.chests.getOrNull(i) ?: return
        val at = cellCenter(cells.first())
        if (fx3) Fx.call("chest", Fx.at(at, Fx.obj("n" to 3))) else particles.firework(at)
        sounds.play("fanfare")
        val prize = Rewards.randomBooster(level.number, random)
        progress.addBooster(prize)
        progress.addCoins(TREASURE_COINS)
        coinsThisLevel += TREASURE_COINS
        toast("💎 Treasure! +$TREASURE_COINS 🪙 ${prize.emoji}", at, Palette.gold, big = true)
    }

    private fun praise(len: Int) = when {
        len >= 7 -> "WORDNOID!"
        len == 6 -> "AMAZING!"
        len == 5 -> "AWESOME!"
        len == 4 -> "GREAT!"
        else -> "NICE!"
    }

    /** A word read right-to-left or bottom-to-top. */
    private fun reversed(w: FoundWord): Boolean =
        w.cells.size >= 2 && (w.cells[0].col > w.cells[1].col || w.cells[0].row > w.cells[1].row)

    private suspend fun celebrate(r: MoveResult) {
        val fx0 = r.effects
        var b0 = r.changedBoard
        if (fx0.words.isNotEmpty() && level.boss) bossHitKey++
        // 1. Each word lights up letter by letter with rising notes, tiles lift and flip.
        for ((wi, w) in fx0.words.withIndex()) {
            // A backwards word turns around on the board so it reads the normal way.
            if (reversed(w)) {
                var nb = b0
                val sorted = w.cells.sortedWith(compareBy({ it.row }, { it.col }))
                w.cells.forEachIndexed { i, p -> nb = nb.with(sorted[i], b0[p]) }
                b0 = nb
                board = nb
                sounds.play("whoosh")
                delay(380)
            }
            val cellsInOrder = w.cells.sortedWith(compareBy({ it.row }, { it.col }))
            val ids = cellsInOrder.map { b0[it].id }
            glowing.addAll(ids)
            for ((i, id) in ids.withIndex()) {
                popping.add(id)
                sounds.play("note${(i + wi * 2).coerceAtMost(9)}")
                delay(85)
            }
            val isNew = progress.isNewWord(w.word)
            if (isNew) {
                progress.addWord(w.word); progress.addCoins(Progress.NEW_WORD_COINS); coinsThisLevel += Progress.NEW_WORD_COINS; newWordsThisLevel++
                Daily.report(progress, Daily.Kind.NEW_WORDS)
            }
            foundWords.add(w.word)
            Daily.report(progress, Daily.Kind.WORDS)
            if (w.length >= 5) Daily.report(progress, Daily.Kind.LONG_WORD)
            if (w.word.length > longestWord.length) longestWord = w.word
            val moves = level.movesForWord(w.length)
            val lines = buildList {
                if (moves > 0) add("+$moves move${if (moves > 1) "s" else ""}")
                if (isNew) add("New word! +${Progress.NEW_WORD_COINS} 🪙")
                if (w.word in fx0.themeWords) add("${level.themeName()} word!")
                if (fx0.doubled) add("Double power!")
                if (fx0.bonusWord && w.word == level.bonusWord) add("Bonus word! 🎁")
            }
            banner = WordBanner(w.word, if (fx0.words.size >= 2 && wi == 1) "DOUBLE!" else praise(w.length), lines, ++keys, isNew)
            if (fx3) {
                val mid = cellCenter(cellsInOrder[cellsInOrder.size / 2])
                val first = cellCenter(cellsInOrder.first()); val last = cellCenter(cellsInOrder.last())
                Fx.call("word", Fx.obj("cells" to fxCells(cellsInOrder, b0), "cx" to Fx.dp((first.x + last.x) / 2), "cy" to Fx.dp((first.y + last.y) / 2),
                    "text" to (if (fx0.words.size >= 2 && wi == 1) "DOUBLE!" else praise(w.length)), "green" to isNew))
                if (level.boss) Fx.call("bossHit", Fx.at(mid, Fx.obj("hp" to (1f - liveProgress).toDouble())))
            }
            haptics.word()
            if (w.length >= 5) { shakeKey++; haptics.big(); sounds.play("fanfare"); sounds.play("say:" + praise(w.length).lowercase().trimEnd('!')) }
            // Words stay on screen long enough to read (about 1.5 seconds).
            delay(if (fx0.words.size > 1) 1100 else 1500)
        }
        // 2. Burst!
        val cleared = fx0.cleared
        for (p in cleared) {
            val t = b0[p]
            if (!t.isCrate && !fx3) particles.burst(cellCenter(p), tileColor(t), if (fx0.words.any { it.length >= 5 }) 1.4f else 1f)
        }
        if (fx0.words.isNotEmpty()) sounds.play("burst")
        if (fx0.words.any { it.length >= 5 }) {
            if (fx3) Fx.call("fireworks", Fx.obj("n" to 2)) else particles.firework(center())
            sounds.play("bang")
        }
        for (h in fx0.crateHits) {
            if (!fx3) particles.splinters(cellCenter(h.pos))
            sounds.play("crack")
            if (h.broken) Daily.report(progress, Daily.Kind.CRATES)
            h.prize?.let { prize ->
                val text = when (prize.kind) {
                    PrizeKind.COINS -> "+${prize.amount} 🪙"
                    PrizeKind.MOVES -> "+${prize.amount} move"
                    PrizeKind.BOOSTER -> "🎁 Booster!"
                }
                if (prize.kind == PrizeKind.COINS) coinsThisLevel += prize.amount
                // The prize jumps out big and shows its name.
                if (fx3) Fx.call("crate", Fx.at(cellCenter(h.pos), Fx.obj("text" to text, "to" to if (prize.kind == PrizeKind.MOVES) "moves" else "coins")))
                else {
                    particles.sparkle(cellCenter(h.pos), Palette.gold, 18)
                    toast(text, cellCenter(h.pos), Palette.gold, big = true)
                }
            }
            if (fx3 && h.prize == null && h.broken) Fx.call("crate", Fx.at(cellCenter(h.pos), Fx.obj("prize" to false)))
        }
        if (fx0.iceMelted.isNotEmpty()) sounds.play("shatter")
        for (p in fx0.iceMelted) if (fx3) Fx.call("ice", Fx.at(cellCenter(p))) else particles.shards(cellCenter(p))
        if (fx0.chainsBroken.isNotEmpty()) sounds.play("snap")
        for (p in fx0.chainsBroken) if (fx3) Fx.call("chain", Fx.at(cellCenter(p))) else particles.links(cellCenter(p))
        if (fx0.balloons.isNotEmpty()) sounds.play("pop")
        for (p in fx0.balloons) if (fx3) Fx.call("balloon", Fx.at(cellCenter(p), Fx.obj("cell" to Fx.dp(cellPx)))) else { particles.burst(cellCenter(p), Palette.pink, 1.2f); toast("🎈 Pop!", cellCenter(p), Palette.pink) }
        for (p in fx0.gold) if (fx3) Fx.call("gold", Fx.at(cellCenter(p), Fx.obj("n" to 2))) else { particles.sparkle(cellCenter(p)); particles.coins(cellCenter(p), goalAnchor, 2) }
        if (fx0.gold.isNotEmpty()) Daily.report(progress, Daily.Kind.GOLD, fx0.gold.size)
        // 3. Coins (from boxes) fly to the counter; moves and boosters are handed out.
        if (fx0.coins > 0) {
            val from = fx0.words.firstOrNull()?.cells?.let { cellCenter(it[it.size / 2]) } ?: center()
            if (fx3) Fx.call("coins", Fx.at(from, Fx.obj("n" to fx0.coins.coerceAtMost(8)))) else particles.coins(from, coinAnchor, fx0.coins)
            progress.addCoins(fx0.coins)
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
        scope.launch { delay(300); banner = null }
    }

    private suspend fun win(r: MoveResult) {
        idleJob?.cancel()
        tutorial = null
        hintWords = emptyList()
        delay(400)
        sounds.play("win")
        sounds.play("wordnoid")
        message = if (level.boss) "You beat ${worldOf(level.number).villain}!" else "LEVEL COMPLETE!"
        if (fx3) Fx.call("confetti", Fx.obj("x" to Fx.dp(screenWidth / 2), "y" to Fx.dp(screenHeight * 0.3f), "n" to 120, "spread" to 6)) else particles.confettiRain(screenWidth)
        // Leftover moves: tiles burst into coins (1 coin each, up to 10).
        val left = game.movesLeft
        if (left > 0 && fx3) {
            // Goal reached: the leftover tiles burst one by one into coins that fly to the coin box.
            wordRain = true
            val cells = board.positions().filter { board[it].hasLetter }.shuffled(random).take(left.coerceAtMost(10))
            Fx.call("goalBlast", Fx.obj("cells" to fxCells(cells), "cx" to Fx.dp(center().x), "cy" to Fx.dp(center().y)))
            Fx.await("goalBlastDone", 4000)
            wordRain = false
            if (r.leftoverCoins > 0) { progress.addCoins(r.leftoverCoins); coinsThisLevel += r.leftoverCoins }
        } else if (left > 0) {
            wordRain = true
            val cells = board.positions().filter { board[it].hasLetter }
            repeat(left.coerceAtMost(10)) {
                val p = cells[random.nextInt(cells.size)]
                particles.burst(cellCenter(p), tileColor(board[p]), 0.6f)
                particles.coins(cellCenter(p), coinAnchor, 1)
                sounds.play("pop")
                delay(140)
            }
            wordRain = false
            if (r.leftoverCoins > 0) { progress.addCoins(r.leftoverCoins); coinsThisLevel += r.leftoverCoins }
        }
        if (fx3) { Fx.call("fireworks", Fx.obj("n" to 5)); delay(1300) } else repeat(5) {
            particles.firework(Offset(screenWidth * (0.2f + random.nextFloat() * 0.6f), screenHeight * (0.15f + random.nextFloat() * 0.35f)))
            sounds.play(if (it % 2 == 0) "bang" else "crackle")
            delay(260)
        }
        message = null
        winStars = game.stars()
        winCoins = WIN_COINS + winStars * BRICK_COINS
        progress.addCoins(winCoins)
        coinsThisLevel += winCoins
        progress.levelWon(level.number, winStars)
        Daily.report(progress, Daily.Kind.WIN_LEVELS)
        if (winStars == 3) Daily.report(progress, Daily.Kind.THREE_BRICKS)
        if (winStars == 3 && random.nextDouble() < 0.25) winBooster = Rewards.randomBooster(level.number, random).also { progress.addBooster(it) }
        delay(300)
        showWin = true
    }

    /** Watched a video on the win screen: double the win coins. */
    fun doubleWinCoins() {
        progress.addCoins(winCoins)
        coinsThisLevel += winCoins
        sounds.play("cash")
        winCoins *= 2
    }

    private suspend fun lose() {
        idleJob?.cancel()
        delay(500)
        sounds.play("lose")
        if (fx3) {
            // The tiles turn grey and fall off the board.
            Fx.call("lose", Fx.obj("cells" to fxCells(board.positions().filter { board[it].hasLetter || board[it].isCrate })))
            delay(60)
            boardHidden = true
            Fx.await("loseDone", 3000)
        }
        showLose = true
    }

    /** Continue after running out of moves: 300 coins for more moves (more after many fails). */
    fun continueForCoins(): Boolean {
        val moves = if (level.boss) 5 else Progress.continueMoves(progress.fails(level.number))
        if (!progress.spend(Progress.CONTINUE_PRICE)) { sounds.play("nope"); return false }
        sounds.play("cash")
        addMoves(moves)
        return true
    }

    /** Continue after a rewarded video: +5 moves. */
    fun continueForAd() { addMoves(5) }

    private fun addMoves(n: Int) {
        game.continueWith(n)
        showLose = false
        boardHidden = false
        status = GameStatus.PLAYING
        sync()
        toast("+$n moves!", movesAnchor, Palette.green, big = true)
    }

    fun continueWithBooster(): Boolean {
        if (!progress.useBooster(Booster.PLUS_MOVES)) return false
        addMoves(3)
        sounds.play("fanfare")
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

    /** Goal progress 0..1 (the boss health bar goes down as this goes up). */
    val goalProgress: Float
        get() = level.targets.sumOf { minOf(it.count, it.progress(stats)) }.toFloat() / level.targets.sumOf { it.count }.coerceAtLeast(1)

    // ---------------------------------------------------------------- 3D layer

    private val fx3 get() = Fx.ok

    private fun fxCell(p: Pos, b: Board = board): JSONObject =
        Fx.at(cellCenter(p), Fx.obj("ch" to (if (b[p].hasLetter) b[p].letter.uppercaseChar().toString() else " "), "c" to Fx.hex(tileColor(b[p]))))

    private fun fxCells(ps: Collection<Pos>, b: Board = board): JSONArray = JSONArray().apply { ps.forEach { put(fxCell(it, b)) } }

    private fun fxBoard(o: JSONObject): JSONObject = o.put("bx", Fx.dp(boardOrigin.x)).put("by", Fx.dp(boardOrigin.y))
        .put("bw", Fx.dp(board.cols * cellPx)).put("bh", Fx.dp(board.rows * cellPx)).put("cell", Fx.dp(cellPx))

    /** Tells the 3D layer the cell size and where the counters are. Called when the board is laid out. */
    fun fxLayout() {
        if (cellPx <= 1f) return
        Fx.call("unit", Fx.obj("u" to Fx.dp(cellPx)))
        Fx.call("anchors", Fx.obj("coins" to Fx.xy(coinAnchor), "moves" to Fx.xy(movesAnchor), "goal" to Fx.xy(goalAnchor)))
    }

    /** The boss in 3D, peeking over the top of the board. */
    fun fxBoss() {
        if (!level.boss || cellPx <= 1f) return
        Fx.call("bossShow", Fx.obj("x" to Fx.dp(boardOrigin.x), "y" to Fx.dp(boardOrigin.y) - 8.0, "w" to Fx.dp(board.cols * cellPx),
            "hp" to (1f - goalProgress).toDouble(), "name" to worldOf(level.number).villain.uppercase()))
    }

    /** Goal progress straight from the game (before the screen catches up). */
    private val liveProgress: Float
        get() = level.targets.sumOf { minOf(it.count, it.progress(game.stats)) }.toFloat() / level.targets.sumOf { it.count }.coerceAtLeast(1)

    fun cellCenter(p: Pos): Offset = Offset(boardOrigin.x + (p.col + 0.5f) * cellPx, boardOrigin.y + (p.row + 0.5f) * cellPx)

    fun center(): Offset = Offset(boardOrigin.x + board.cols * cellPx / 2, boardOrigin.y + board.rows * cellPx / 2)

    fun toast(text: String, at: Offset, color: Color, big: Boolean = false) {
        val t = Toast(++keys, text, at, color, big)
        toasts.add(t)
        scope.launch { delay(1600); toasts.remove(t) }
    }

    fun boardAt(): Board = board

    fun objectiveDone(o: Objective): Boolean = o.done(stats)

    companion object {
        const val WIN_COINS = 10
        const val BRICK_COINS = 5
        const val TREASURE_COINS = 25
    }
}
