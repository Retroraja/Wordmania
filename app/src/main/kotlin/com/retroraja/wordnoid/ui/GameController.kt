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
    val sounds: SoundFx,
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
    /** Tiles that glow while the theme hint is open: the letters of the example words. */
    val themeGlow = mutableStateListOf<Long>()
    fun glowThemeHint(words: List<String>) {
        themeGlow.clear()
        val left = board.tiles().toMutableList()
        for (w in words) for (ch in w) left.firstOrNull { it.letter == ch }?.let { themeGlow.add(it.id); left.remove(it) }
    }
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
    /** Word Scanner beam sweep (Update 1.3). */
    var scanBeamKey by mutableIntStateOf(0); private set
    /** Treasure Finder: tiles over the buried treasure that still need digging (Update 1.3). */
    var treasureGlow by mutableStateOf<Set<Pos>>(emptySet()); private set
    /** Vowel Rain: the letters falling now and where they land (Update 1.3). */
    var rainDrops by mutableStateOf<List<Pair<Pos, Char>>?>(null); private set
    var rainKey by mutableIntStateOf(0); private set
    /** Tiles that flash white after a raindrop letter landed on them. */
    val rainFlash = mutableStateListOf<Long>()
    var armed by mutableStateOf<Booster?>(null); private set
    /** Tiles where the armed booster really works (the others are dimmed). */
    var aimTargets by mutableStateOf<Set<Pos>>(emptySet()); private set
    var aimFirst by mutableStateOf<Pos?>(null); private set
    var letterPick by mutableStateOf<LetterPick?>(null); private set
    var fx by mutableStateOf<BoosterFx?>(null); private set
    var tutorial by mutableStateOf<Tutorial?>(null); private set
    var nudge by mutableStateOf<Booster?>(null); private set
    /**
     * Free boosters for this level only (Update 1.1): win-streak gifts, a pre-level Joker, and a Shuffle when no word is left.
     * They glow on the panel with a FREE badge. Nothing is ever used by itself: the player taps them.
     */
    val levelFree = mutableStateListOf<Booster>()
    /** A free Shuffle was given because no word was left (once until it is used). */
    private var stuckShuffle = false
    /** True if the next use of [b] costs nothing (a teaching freebie or a free one for this level). */
    fun isFree(b: Booster): Boolean = b in levelFree || progress.hasFreeUse(b)
    var mysteryCards by mutableStateOf<List<Booster>?>(null); private set
    var buyAsk by mutableStateOf<Booster?>(null); private set
    var showWin by mutableStateOf(false); private set
    /** Levels 1-3, first win: a big 3D "WORDNOID!" and straight to the town (no win window). */
    var bigWin by mutableStateOf(false); private set
    var autoTown by mutableStateOf(false); private set
    fun bigWinDone() { bigWin = false; showWin = true }
    /** A booster that just arrived from a mystery card glows on the panel for a moment. */
    var glowBooster by mutableStateOf<Booster?>(null); private set
    /** Where the booster panel is (mystery cards fly there). */
    var panelAnchor = Offset.Zero
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
    /** Words on back-to-back swaps: 2 = COMBO x2, and so on. */
    var combo by mutableIntStateOf(0); private set
    /** Set by a swap that continues a combo; the word celebration shows it once. */
    private var comboShow = 0
    private var dryMoves = 0
    private var meterFills = 0
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
        get() = tutorial?.text ?: message ?: (if (hintWords.isNotEmpty()) "💡 Swap the glowing tiles to spell ${hintWords.first()}!" + (if (hintWords.size > 1) " Or try: " + hintWords.drop(1).joinToString(", ") else "") else lockText() ?: tikoSay)

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
            // Pre-level boosters: ones that only add moves start working at once (they can't make a word).
            // A tile booster (Joker) waits on the panel as FREE, for the player to tap.
            for (b in startBoosters) {
                delay(250)
                if (b.aim == Aim.NONE && b !in WORD_MAKERS) applyStartBooster(b) else levelFree.add(b)
            }
            // Win streak gifts: FREE glowing buttons for this level. Never used by themselves (Update 1.1 fix).
            val streakBonus = streakCoins(progress.streak, level.number)
            if (streakBonus > 0) {
                delay(300)
                progress.addCoins(streakBonus)
                coinsThisLevel += streakBonus
                toast("🔥 Streak gift: +$streakBonus 🪙", center(), Palette.gold, big = true)
                sounds.play("cash")
            }
            val gifts = streakBoosters(progress.streak, level.number)
            if (gifts.isNotEmpty()) {
                levelFree.addAll(gifts)
                glowBooster = gifts.first()
                delay(300)
                toast("🔥 Streak gift: ${gifts.joinToString(" ") { it.emoji }} FREE!", center(), Palette.gold, big = true)
                sounds.play("sparkle")
                say("🔥 Win streak gift! Your free ${gifts.joinToString(", ") { it.title }} ${if (gifts.size > 1) "are" else "is"} on the panel. Tap to use when you like.", 4200)
                scope.launch { delay(4200); if (glowBooster in gifts) glowBooster = null }
            }
            startTutorial()
            twinTip()
            armIdleHint()
        }
    }

    /** First try of a "double tiles" goal (Update 1.3): the pink x2 tiles glow and Tiko says what to do. */
    private suspend fun twinTip() {
        if (level.targets.none { it is Objective.Twins } || progress.seen("twin_tip")) return
        progress.markSeen("twin_tip")
        val ids = board.tiles().filter { it.isTwin }.map { it.id }
        delay(600)
        glowing.addAll(ids)
        say("Goal: use the pink x2 tiles in words. Each one counts twice! 2️⃣", 5000)
        delay(5000)
        glowing.removeAll(ids)
    }

    /** A pre-level booster that only adds moves (+3 Moves, Free Move). Tile boosters never run by themselves. */
    private suspend fun applyStartBooster(b: Booster) {
        val r = game.useBooster(b) ?: return
        toast("${b.emoji} ${b.title}!", center(), Palette.gold, big = true)
        sounds.play("sparkle")
        playResult(r, b)
    }

    private fun startTutorial() {
        if (level.number == 1 && level.tutorialSwap != null) {
            tutorial = Tutorial("Swap these 2 glowing tiles! 👆", swap = level.tutorialSwap)
            return
        }
        val b = level.introBoosters.firstOrNull()
        if (b != null && progress.hasFreeUse(b)) {
            // The help card's "Try it": a hand points at the button, then at the best tile.
            tutorial = Tutorial("Tap the glowing ${b.emoji} ${b.title} button. Your first one is free!", booster = b, dismissible = b !in GUIDED)
        }
    }

    fun dismissTutorial() {
        if (tutorial?.dismissible == true) tutorial = null
    }

    private fun afterWordTutorial() {
        if (tutorial?.swap != null) {
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
            s == null -> { if (board[p].swappable) { selected = p; sounds.play("tick"); if (board[p].isIce) iceTip() } else notMovable(p) }
            s == p -> selected = null
            s.isNeighbour(p) -> { selected = null; trySwap(s, p) }
            else -> selected = if (board[p].swappable) p else null
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
            t.isIce -> "Ice costs 2 moves to swap. Use it in a word to melt it."
            t.isMystery -> "A hidden letter! Make a word right beside it to show the letter."
            t.isChain -> "Chained! Make a word next to it to snap the chain."
            t.isBuddy -> "That's a Tiko friend! Use the letters under it in words so it drops to the bottom."
            else -> null
        }
        text?.let { say(it) }
        sounds.play("nope")
    }

    /** Picking an ice tile: a short reminder that it costs 2 moves (shown until the player has swapped ice once). */
    private fun iceTip() { if (!progress.seen("ice_swapped")) say("🧊 Ice costs 2 moves to swap. Use it in a word to melt it!", 2600) }

    private fun trySwap(a: Pos, b: Pos) {
        if (!game.canSwap(a, b)) {
            sounds.play("nope")
            if (board.contains(a) && board.contains(b) && a.isNeighbour(b) && board[a].swappable && board[b].swappable && game.swapCost(a, b) > movesLeft)
                say("Not enough moves to swap ice (it costs ${game.swapCost(a, b)}).", 2000)
            else if (board.contains(b) && !board[b].swappable) notMovable(b)
            return
        }
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
        autoHint = false
        hintWords = emptyList()
        scan = emptySet()
        nudge = null
        busy = true
        val cost = game.swapCost(a, b)
        scope.launch {
            sounds.play("swap")
            if (cost >= 2) {
                progress.markSeen("ice_swapped")
                toast("-$cost moves 🧊", movesAnchor, Palette.cyan, big = true)
                sounds.play("crack")
            }
            val r = game.swap(a, b)
            board = r.changedBoard
            delay(200)
            if (r.effects.words.isEmpty()) {
                sounds.play("clack")
                dryMoves++
                combo = 0
                if (dryMoves >= 3) suggestBooster()
            } else { dryMoves = 0; combo++; if (combo >= 2) comboShow = combo }
            playResult(r, null)
        }
    }

    /**
     * Stuck: the best booster button glows (Update 1.1). Boosters the player has (or free ones) first,
     * otherwise one they can buy with coins. [lowMoves]: few moves left and the goal is still far.
     */
    private fun suggestBooster(lowMoves: Boolean = false) {
        if (tutorial != null || armed != null || status != GameStatus.PLAYING) return
        val order = if (lowMoves) listOf(Booster.PLUS_MOVES, Booster.MAGIC_SWAP, Booster.JOKER, Booster.HAMMER, Booster.ROW_SLIDE, Booster.FREE_MOVE)
        else listOf(Booster.HINT, Booster.MAGIC_SWAP, Booster.JOKER, Booster.SCANNER, Booster.HAMMER, Booster.SHUFFLE)
        val open = order.filter { it.introLevel <= level.number || it in levelFree }
        val pick = open.firstOrNull { progress.count(it) > 0 || isFree(it) } ?: open.firstOrNull() ?: return
        nudge = pick
        val have = progress.count(pick) > 0 || isFree(pick)
        say(when {
            lowMoves && have -> "Only $movesLeft moves left! Try ${pick.emoji} ${pick.title}!"
            lowMoves -> "Only $movesLeft moves left! ${pick.emoji} ${pick.title} can help (🪙${Rewards.price(pick, progress.unlocked)})."
            have -> "Stuck? Try ${pick.emoji} ${pick.title}!"
            else -> "Stuck? ${pick.emoji} ${pick.title} can help (🪙${Rewards.price(pick, progress.unlocked)})."
        }, 3200)
    }

    /** Few moves left and the goal still far: the best booster glows (once per level). */
    private var lowMovesNudged = false
    private fun checkLowMoves() {
        if (lowMovesNudged || status != GameStatus.PLAYING || level.number <= 5) return
        val need = level.targets.sumOf { it.left(stats) }
        if (movesLeft in 1..3 && need > movesLeft) { lowMovesNudged = true; suggestBooster(lowMoves = true) }
    }

    /** True while the hint on the board is the soft auto hint (it goes away as soon as the player touches the board). */
    var autoHint by mutableStateOf(false); private set

    /** Auto hint: after a few seconds with no move, two tiles that make a word glow softly. 5 s in Levels 1-10, 12 s later on. */
    private fun armIdleHint() {
        idleJob?.cancel()
        if (autoHint) { hint = null; autoHint = false }
        idleJob = scope.launch {
            delay(if (level.number <= 10) 5_000 else 12_000)
            if (!busy && status == GameStatus.PLAYING && armed == null && hint == null && tutorial == null && mysteryCards == null) {
                hint = game.hint()
                autoHint = hint != null
            }
            // Still nothing after about 8 more seconds: the best booster glows (from Level 6).
            delay(8_000)
            if (!busy && status == GameStatus.PLAYING && armed == null && tutorial == null && mysteryCards == null && nudge == null && level.number > 5) suggestBooster()
        }
    }

    /** "Stuck hint" from a rewarded video: shows words like the Hint booster, without using one. */
    fun freeHint() {
        val words = game.hintWords(3)
        if (words.isEmpty()) return
        hint = words.first().first
        autoHint = false
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
        if (progress.count(b) <= 0 && !isFree(b)) { buyAsk = b; return }
        nudge = null
        // Never let a booster "do nothing": if it can't work on this board, say why at once and keep it.
        val targets = if (b.aim == Aim.NONE) emptySet() else game.targets(b)
        if (if (b.aim == Aim.NONE) b != Booster.SCANNER && !game.canUseNow(b) else targets.isEmpty()) {
            sounds.play("nope")
            say(cantUseText(b), 3500)
            return
        }
        when (b.aim) {
            Aim.NONE -> useNow(b)
            else -> {
                armed = b
                aimTargets = targets
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

    /** Why [b] can't be used on this board right now (it is kept, nothing is spent). */
    fun cantUseText(b: Booster): String = "${b.emoji} " + when (b) {
        Booster.AXE -> "No box, ice, chain or balloon here, so the Axe is kept for later."
        Booster.MAGNET -> "No gold tile here, so the Magnet is kept for later."
        Booster.ROW_SLIDE -> "No row here can make a word, so the Row Slide is kept. Try after your next move!"
        Booster.COLUMN_SLIDE -> "No column here can make a word, so the Column Slide is kept. Try after your next move!"
        Booster.SPIN -> "No 4 tiles here can spin into a word, so the Spin is kept. Try after your next move!"
        Booster.UNDO -> "Nothing to undo yet. Make a move first!"
        Booster.HINT -> "No word can be made right now. Try a Shuffle!"
        Booster.TREASURE_FINDER -> "No hidden treasure on this level, so it is kept for later."
        Booster.HAMMER -> "Nothing here for the Hammer, so it is kept for later."
        else -> "${b.title} can't be used right now, so it is kept."
    }

    fun aimText(b: Booster): String = when (b.aim) {
        Aim.TILE -> "Tap any tile or box to use the ${b.title}"
        Aim.MOVABLE -> "Tap any tile for the ${b.title}"
        Aim.TWO_TILES -> if (aimFirst == null) "Tap the first tile to swap" else "Now tap the second tile"
        Aim.ROW -> "Tap a bright row to turn it into a word"
        Aim.COLUMN -> "Tap a bright column to turn it into a word"
        Aim.SQUARE -> "Tap a bright tile: it and the 3 tiles right and below spin into a word"
        Aim.OBSTACLE -> "Tap a bright box, ice, chain or balloon"
        Aim.GOLD -> "Tap a gold tile"
        Aim.NONE -> ""
    }

    fun cancelAim() {
        armed = null
        aimTargets = emptySet()
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
            // The bought booster lands on the panel with its count and glows; the player taps it to use it.
            glowBooster = b
            say("+1 ${b.emoji} ${b.title}! Tap it to use it.", 2600)
            scope.launch { delay(2600); if (glowBooster == b) glowBooster = null }
        } else {
            sounds.play("nope")
            say("Not enough coins. Win levels and open boxes to earn more!")
        }
    }

    fun closeBuy() { buyAsk = null }

    private fun aim(b: Booster, p: Pos) {
        val t = board[p]
        // A dim tile: the booster can't work there. Say so and stay armed (nothing is spent).
        if (b.aim != Aim.TWO_TILES && p !in aimTargets) {
            sounds.play("nope")
            say(when (b.aim) {
                Aim.ROW -> "That row can't make a word. Tap a bright row!"
                Aim.COLUMN -> "That column can't make a word. Tap a bright column!"
                else -> "The ${b.title} can't work there. Tap a bright tile!"
            }, 3000)
            return
        }
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
                autoHint = false
                hintWords = words.map { it.second }
                fx = BoosterFx(b, words.first().first.toList(), ++keys)
                sounds.play("ding")
                if (tutorial?.booster == b) tutorial = null
                Daily.report(progress, Daily.Kind.BOOSTERS)
            }
            Booster.SCANNER -> {
                // Update 1.3: a slow glowing beam with a sound, then the word tiles lift and glow until the next move.
                val cells = game.scan()
                consume(b)
                scanBeamKey++
                sounds.play("whoosh")
                scope.launch {
                    delay(700); sounds.play("hum")
                    delay(800); scan = cells; sounds.play("ding")
                    say(if (cells.isEmpty()) "🔍 No words to find right now. Try 🔀 Shuffle!"
                        else "🔍 Found ${cells.size} word tiles! Swap a glowing tile with its neighbour to make a word.", 5200)
                }
                if (tutorial?.booster == b) tutorial = null
                Daily.report(progress, Daily.Kind.BOOSTERS)
            }
            else -> apply(b, null)
        }
    }

    private fun consume(b: Booster) {
        // A free one for this level: nothing is taken from the bag.
        if (levelFree.remove(b)) { if (b == Booster.SHUFFLE) stuckShuffle = false; return }
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
            // Rain boosters: work out where the letters land first, so the drops fall on those tiles.
            val rain = b == Booster.LETTER_RAIN || b == Booster.VOWEL_FLIP
            val pre = if (rain) game.useBooster(b, a, second, letter) else null
            val cells = listOfNotNull(a, second) + when (b) {
                Booster.SPIN -> a?.let { listOf(Pos(it.row, it.col + 1), Pos(it.row + 1, it.col), Pos(it.row + 1, it.col + 1)) } ?: emptyList()
                Booster.LETTER_RAIN, Booster.VOWEL_FLIP -> pre?.changedBoard?.let { nb -> nb.positions().filter { nb[it].letter != before[it].letter } } ?: emptyList()
                else -> emptyList()
            }
            if (rain && pre != null) {
                // Big letters fall and land on the tiles they change, then those tiles flash (Update 1.3).
                rainDrops = cells.map { it to pre.changedBoard[it].letter }
                rainKey++
                sounds.play(boosterSound(b))
                haptics.tap()
                delay(650L + cells.size * 140L)
            } else {
                if (fx3) Fx.call("booster", fxBoard(Fx.obj("k" to b.name, "cells" to fxCells(cells))).also { o -> a?.let { Fx.at(cellCenter(it), o) } })
                else fx = BoosterFx(b, cells, ++keys)
                sounds.play(boosterSound(b))
                haptics.tap()
                delay(boosterLead(b))
            }
            val r = if (rain) pre else game.useBooster(b, a, second, letter)
            if (r == null) {
                sounds.play("nope")
                say(
                    when (b) {
                        Booster.SPIN, Booster.ROW_SLIDE, Booster.COLUMN_SLIDE -> "No word can be made there, so your ${b.title} is kept. Try another spot!"
                        Booster.HAMMER -> "The Hammer can't smash gold tiles or Tiko friends. Gold is collected with words!"
                        Booster.TREASURE_FINDER -> "Every treasure is already showing. Dig the glowing tiles with words!"
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
            }
            when (b) {
                Booster.UNDO -> { wobbleKey++ }
                Booster.SHUFFLE -> { flipping.addAll(board.tiles().map { it.id }) }
                Booster.JOKER, Booster.PAINT, Booster.DICE, Booster.LOCK -> a?.let { flipping.add(before[it].id) }
                Booster.LETTER_RAIN, Booster.VOWEL_FLIP -> {
                    val ids = r.changedBoard.positions().filter { r.changedBoard[it].letter != before[it].letter }.map { before[it].id }
                    rainDrops = null
                    sounds.play("pop")
                    rainFlash.clear(); rainFlash.addAll(ids)
                    scope.launch { delay(1600); rainFlash.removeAll(ids) }
                    say("${b.emoji} ${ids.size} new letters landed! The flashing tiles changed.", 2600)
                }
                Booster.TREASURE_FINDER -> {
                    treasureGlow = game.treasureGlow
                    say("🗺️ Treasure found under the glowing 💎 tiles! Use them in words to dig it out.", 4200)
                }
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
        Booster.PAINT, Booster.DICE -> "flip"
        Booster.UNDO -> "rewind"
        Booster.SHUFFLE, Booster.SPIN, Booster.ROW_SLIDE, Booster.COLUMN_SLIDE -> "whoosh"
        Booster.FREE_MOVE, Booster.PLUS_MOVES, Booster.DOUBLE -> "fanfare"
        Booster.LOCK -> "lock"
        Booster.MAGNET -> "hum"
        Booster.LETTER_RAIN, Booster.VOWEL_FLIP -> "rain"
        Booster.HINT -> "ding"
        Booster.SCANNER -> "whoosh"
        Booster.TREASURE_FINDER -> "sparkle"
    }

    private fun boosterLead(b: Booster): Long = when (b) {
        Booster.HAMMER, Booster.AXE -> 380
        Booster.MAGNET -> 450
        Booster.LOCK -> 300
        Booster.LETTER_RAIN, Booster.VOWEL_FLIP -> 500
        else -> 200
    }

    // ---------------------------------------------------------------- playing out a move

    private fun sync() {
        treasureGlow = game.treasureGlow
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
        for (i in r.chestsFound) treasureFound(i)
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
        // No word left: never shuffled by itself. The Shuffle button glows FREE and the player taps it.
        if ((r.stuck || game.stuck) && r.status == GameStatus.PLAYING && !stuckShuffle) {
            stuckShuffle = true
            if (Booster.SHUFFLE !in levelFree) levelFree.add(Booster.SHUFFLE)
            nudge = Booster.SHUFFLE
            sounds.play("ding")
            say("No words left! Tap the FREE 🔀 Shuffle to mix the letters.", 5000)
        }
        when (r.status) {
            GameStatus.WON -> win(r)
            GameStatus.LOST -> lose()
            GameStatus.PLAYING -> {}
        }
        status = r.status
        busy = false
        if (fx0.words.isNotEmpty() && nudge != Booster.SHUFFLE) nudge = null
        checkLowMoves()
        if (meterFills > 0 && status == GameStatus.PLAYING) openMystery()
        if (fx0.words.isNotEmpty() && status == GameStatus.PLAYING) afterWordTutorial()
    }

    /** A treasure chest was dug out: a real prize. */
    private fun treasureFound(i: Int) {
        val cells = level.chests.getOrNull(i) ?: return
        val at = cellCenter(cells.first())
        if (fx3) Fx.call("chest", Fx.at(at, Fx.obj("n" to 3))) else particles.firework(at)
        sounds.play("fanfare")
        val coins = TREASURE_COINS + Rewards.BOOSTER_COINS
        progress.addCoins(coins)
        coinsThisLevel += coins
        toast("💎 Treasure! +$coins 🪙", at, Palette.gold, big = true)
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
                if (w.cells.any { it in fx0.twins }) add("x2 tile: counts twice!")
                if (w.cells.any { it in fx0.powerFired }) add("⚡ Power blast!")
                if (fx0.bonusWord && w.word == level.bonusWord) add("Bonus word! 🎁")
            }
            val comboNow = if (wi == 0) comboShow else 0
            val praiseText = when {
                comboNow >= 2 -> "COMBO x$comboNow!"
                fx0.words.size >= 2 && wi == 1 -> "DOUBLE!"
                else -> praise(w.length)
            }
            val lines2 = if (comboNow >= 2) lines + "Combo +${COMBO_COINS * comboNow} 🪙" else lines
            if (comboNow >= 2) {
                progress.addCoins(COMBO_COINS * comboNow); coinsThisLevel += COMBO_COINS * comboNow
                shakeKey++; haptics.big(); sounds.play("fanfare"); sounds.play("coin")
            }
            banner = WordBanner(w.word, praiseText, lines2, ++keys, isNew)
            if (fx3) {
                val mid = cellCenter(cellsInOrder[cellsInOrder.size / 2])
                val first = cellCenter(cellsInOrder.first()); val last = cellCenter(cellsInOrder.last())
                Fx.call("word", Fx.obj("cells" to fxCells(cellsInOrder, b0), "cx" to Fx.dp((first.x + last.x) / 2), "cy" to Fx.dp((first.y + last.y) / 2),
                    "text" to praiseText, "green" to isNew))
                // The fall itself is always played by win() (also when a booster or crate finishes the level), so a hit never knocks him out.
                if (level.boss) Fx.call("bossHit", Fx.at(mid, Fx.obj("hp" to (1f - liveProgress).toDouble().coerceAtLeast(0.03))))
            }
            haptics.word()
            if (w.length >= 5) { shakeKey++; haptics.big(); sounds.play("fanfare"); sounds.play("say:" + praise(w.length).lowercase().trimEnd('!')) }
            // Words stay on screen long enough to read (about 1.5 seconds).
            delay(if (fx0.words.size > 1) 1100 else 1500)
        }
        comboShow = 0
        // Power tiles: a new one glows where the long word's moved tile was; a used one blasts its row (and column).
        for (p in fx0.powerFired) {
            if (fx3) Fx.call("power", fxBoard(Fx.at(cellCenter(p), Fx.obj("row" to p.row, "col" to p.col, "cross" to (b0[p].hp >= 2)))))
            else particles.firework(cellCenter(p))
            sounds.play("bang"); shakeKey++; haptics.big()
        }
        for (p in fx0.revealed) { if (fx3) Fx.call("reveal", Fx.at(cellCenter(p))) else particles.sparkle(cellCenter(p), Palette.cyan, 14); sounds.play("sparkle") }
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
            if (!h.broken) {
                // Iron Crate, first hit: the bands snap off (it is a plain crate now, 1 hit left).
                if (fx3) Fx.call("ironSnap", Fx.at(cellCenter(h.pos), Fx.obj("cell" to Fx.dp(cellPx)))) else particles.shards(cellCenter(h.pos))
                toast("🔩 1 hit left!", cellCenter(h.pos), Palette.gold)
                haptics.tap()
            }
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
        if (fx0.boosters > 0) {
            // Gift tiles, crate prizes and the bonus word give coins (Update 1.3).
            val coins = fx0.boosters * Rewards.BOOSTER_COINS
            progress.addCoins(coins)
            coinsThisLevel += coins
            scope.launch { delay(400L); sounds.play("cash"); toast("🎁 +$coins 🪙", center(), Palette.pink, big = true) }
        }
        // The Power Meter starts at Level 5 (before that it's hidden and doesn't charge).
        if (fx0.meter > 0 && level.number >= METER_LEVEL) {
            meterFills += progress.addMeter(fx0.meter)
        }
        delay(120)
        // 4. Tiles fall in.
        board = r.boardAfter
        if (fx0.powerMade.isNotEmpty()) {
            sounds.play("sparkle")
            if (!progress.seen("power_tip")) { progress.markSeen("power_tip"); say("🌈 A Power tile! Use it in a new word to blast its whole row!", 3600) }
        }
        popping.clear()
        glowing.clear()
        sync()
        delay(420)
        scope.launch { delay(300); banner = null }
    }

    private suspend fun win(r: MoveResult) {
        idleJob?.cancel()
        val auto = level.number <= AUTO_TOWN_LEVELS && progress.bricks(level.number) == 0
        tutorial = null
        hintWords = emptyList()
        delay(400)
        // Boss levels: the villain's full fall is shown first ("You beat ...!"), and only then the goal blast.
        if (level.boss) {
            message = "You beat ${worldOf(level.number).villain}!"
            sounds.play("fanfare")
            if (fx3) {
                Fx.call("bossHold", Fx.obj("on" to false))
                Fx.call("bossDefeat")
                Fx.await("bossGone", 2600)
                delay(700)
                Fx.call("bossHide")
            } else delay(1200)
        }
        sounds.play("win")
        message = if (level.boss) "You beat ${worldOf(level.number).villain}!" else "LEVEL COMPLETE!"
        if (fx3) Fx.call("confetti", Fx.obj("x" to Fx.dp(screenWidth / 2), "y" to Fx.dp(screenHeight * 0.3f), "n" to 120, "spread" to 6)) else particles.confettiRain(screenWidth)
        // Leftover moves: tiles burst into coins (1 coin each, up to 10).
        val left = game.movesLeft
        if (fx3) {
            // Goal reached (as the approved preview): GOAL COMPLETE!, then every tile on the board bursts from the middle out;
            // one coin flies to the coin box for each leftover move (up to 10).
            val cells = board.positions().filter { board[it].hasLetter }
            Fx.call("goalBlast", Fx.obj("cells" to fxCells(cells), "cx" to Fx.dp(center().x), "cy" to Fx.dp(center().y), "coins" to left.coerceAtMost(10)))
            Fx.await("blastTiles", 600)
            boardHidden = true
            Fx.await("goalBlastDone", 7000)
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
        if (fx3) {} else repeat(5) {
            particles.firework(Offset(screenWidth * (0.2f + random.nextFloat() * 0.6f), screenHeight * (0.15f + random.nextFloat() * 0.35f)))
            sounds.play(if (it % 2 == 0) "bang" else "crackle")
            delay(260)
        }
        message = null
        progress.streakWin()
        chestSlot = progress.chest
        if (progress.chestWin()) {
            chestSlot = Progress.CHEST_SLOTS - 1
            val coins = CHEST_COINS + 2 * Rewards.BOOSTER_COINS
            progress.addCoins(coins)
            chestPrize = ChestPrize(coins, emptyList())
        }
        winStars = game.stars()
        winCoins = WIN_COINS + winStars * BRICK_COINS
        progress.addCoins(winCoins)
        coinsThisLevel += winCoins
        progress.levelWon(level.number, winStars)
        Daily.report(progress, Daily.Kind.WIN_LEVELS)
        if (winStars == 3) Daily.report(progress, Daily.Kind.THREE_BRICKS)
        // Update 1.3: no random booster for 3 bricks (rewards are coins only).
        delay(300)
        // Every win: the big 3D "WORDNOID!" first. Levels 1-3 then go to town; later levels show the win screen.
        autoTown = auto
        bigWin = true
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
            // As the approved preview: tiles go grey and tumble off, Tiko gets sad next to the board, "OUT OF MOVES!".
            Fx.call("lose", Fx.obj("cells" to fxCells(board.positions().filter { board[it].hasLetter || board[it].isCrate }),
                "tx" to Fx.dp(boardOrigin.x - cellPx * 0.9f), "ty" to Fx.dp(boardOrigin.y + board.rows * cellPx - cellPx * 0.9f), "ts" to Fx.dp(cellPx * 1.6f)))
            delay(60)
            boardHidden = true
            Fx.await("loseDone", 3500)
            Fx.call("loseHide")
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
        if (status != GameStatus.WON && !streakKept) progress.streakLost()
    }

    /** Leaving a level that isn't won (map, home, restart) counts as a loss for the streak. */
    fun quit() { if (status != GameStatus.WON && !streakKept) progress.streakLost() }

    /** Lose screen: a video keeps the streak (once a day). The player still has to win or try again. */
    var streakKept by mutableStateOf(false); private set
    fun keepStreak() { progress.useStreakSave(); streakKept = true }

    /** The level chest slot that filled with this win (for the pop on the win card), or -1. */
    var chestSlot by mutableIntStateOf(-1); private set
    /** Set when this win filled the chest: it opens full screen. */
    var chestPrize by mutableStateOf<ChestPrize?>(null); private set
    fun chestCollected() { chestPrize = null }

    // ---------------------------------------------------------------- Power Meter mystery cards

    private fun openMystery() {
        if (meterFills <= 0) return
        meterFills--
        sounds.play("fanfare")
        // The first time (Level 5) the cards are the three boosters the player has just learned.
        mysteryCards = if (!progress.seen("mystery_first")) { progress.markSeen("mystery_first"); listOf(Booster.HAMMER, Booster.JOKER, Booster.MAGIC_SWAP) }
        else Rewards.mysteryCards(progress.unlocked.coerceAtLeast(level.number), random)
        if (!progress.seen("meter_voice")) {
            progress.markSeen("meter_voice")
            sounds.play("say:Power Meter full! Pick a card for a free booster!")
        } else if (!meterSaid) sounds.play("say:Pick a card!")
        meterSaid = true
    }

    fun mysteryPicked(b: Booster) {
        progress.addBooster(b)
        sounds.play("sparkle")
        lastMystery = b
    }

    private var lastMystery: Booster? = null
    private var meterSaid = false

    fun mysteryDone() {
        mysteryCards = null
        // The booster lands on the panel and glows; Tiko says what it's for.
        lastMystery?.let { b ->
            glowBooster = b
            val line = "It's yours! Use ${b.emoji} ${b.title} in any level when you're stuck."
            say(line, 4500)
            if (!progress.seen("mystery_voice")) { progress.markSeen("mystery_voice"); sounds.play("say:It's yours! Use it in any level when you're stuck.") }
            scope.launch { delay(3500); if (glowBooster == b) glowBooster = null }
        }
        lastMystery = null
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
        Fx.call("anchors", Fx.obj("coins" to Fx.xy(coinAnchor), "moves" to Fx.xy(movesAnchor), "goal" to Fx.xy(goalAnchor), "bag" to Fx.xy(panelAnchor)))
    }

    /** The boss in 3D, peeking over the top of the board. */
    fun fxBoss() {
        if (!level.boss || cellPx <= 1f) return
        Fx.call("bossShow", Fx.obj("x" to Fx.dp(boardOrigin.x), "y" to Fx.dp(boardOrigin.y) - 8.0, "w" to Fx.dp(board.cols * cellPx),
            "hp" to (1f - goalProgress).toDouble(), "name" to worldOf(level.number).villain.uppercase(), "kind" to level.world))
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
        const val COMBO_COINS = 5
        const val CHEST_COINS = 100
        const val BRICK_COINS = 5
        const val TREASURE_COINS = 25
        /** The Power Meter (and mystery cards) start at this level. */
        const val METER_LEVEL = 5
        /** Levels 1-3 skip the win window and go straight to the town. */
        const val AUTO_TOWN_LEVELS = 3
        /** Boosters whose first try is guided step by step (the player must use it). */
        val GUIDED = setOf(Booster.HAMMER, Booster.JOKER, Booster.MAGIC_SWAP)
        /** Boosters with no aim that still change letters (so they could make a word): never used at the start by themselves. */
        val WORD_MAKERS = setOf(Booster.SHUFFLE, Booster.VOWEL_FLIP, Booster.LETTER_RAIN, Booster.TREASURE_FINDER)
    }
}
