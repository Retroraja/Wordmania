package com.retroraja.wordnoid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.GameStatus
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.data.Progress
import kotlinx.coroutines.delay

/** One-time intro cards for levels that bring something new. */
private fun introFor(level: Level): Pair<String, String>? = when (level.number) {
    1 -> "How to play" to "Swap two neighbouring tiles to spell a word of 3+ letters, in any direction: across, down, or backwards.\n\nEvery swap uses 1 move, and tiles stay where you put them, so plan ahead!"
    2 -> "Tip" to "Words of 4 or more letters give you +3 moves!"
    4 -> "New goal: long word" to "This level needs at least one word with 4 or more letters.\nLong words also give +3 moves."
    else -> null
}

@Composable
fun GameScreen(
    level: Level,
    dict: Dictionary,
    progress: Progress,
    sounds: SoundFx,
    attempt: Int,
    onMap: () -> Unit,
    onRestart: () -> Unit,
    onNext: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val helpMoves = if (progress.fails(level.number) >= Progress.HELP_AFTER_FAILS) Progress.EXTRA_MOVES else 0
    val c = remember(level.number, attempt) {
        GameController(level, dict, helpMoves, sounds, scope) { strong ->
            if (progress.vibration) haptics.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }
    }
    var paused by remember { mutableStateOf(false) }
    var intro by remember(level.number) {
        mutableStateOf(introFor(level)?.takeIf { !progress.tutorialSeen("intro_${level.number}") })
    }

    LaunchedEffect(c) {
        if (level.tutorialSwap != null && !progress.tutorialSeen("swap")) c.tutorial = level.tutorialSwap
        if (helpMoves > 0) c.showPopup("Bonus +$helpMoves moves", "A little help for this level", big = false, millis = 2600)
    }
    // Tutorial is done after the first swap.
    LaunchedEffect(c.swapsMade) { if (c.swapsMade > 0) progress.markTutorialSeen("swap") }

    // Idle hint: on early levels, wiggle a good swap after 10 seconds without a move.
    LaunchedEffect(c, c.lastAction, paused, intro) {
        if (!level.hints || level.number > 3 || paused || intro != null) return@LaunchedEffect
        delay(10_000)
        if (!c.busy && c.status == GameStatus.PLAYING && c.tutorial == null) c.showHint()
    }

    // Save the result (coins are added only once, even after buying extra moves).
    LaunchedEffect(c.status) {
        when (c.status) {
            GameStatus.WON -> { progress.levelWon(level.number, c.game.stars(), c.coins - c.coinsSaved, Levels.all.size); c.coinsSaved = c.coins }
            GameStatus.LOST -> { progress.levelLost(level.number, c.coins - c.coinsSaved); c.coinsSaved = c.coins }
            else -> {}
        }
    }

    BackHandler { if (!c.busy) paused = !paused }

    WoodBackground {
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            LeftPanel(c, progress, Modifier.width(170.dp).fillMaxHeight())
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                BoardView(c, Modifier.fillMaxSize())
                PopupView(c.popup, c.combo, Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
                if (c.tutorial != null) {
                    ArcadeText(
                        "Swap these two tiles to make a word!", 15.sp, Palette.gold,
                        Modifier.align(Alignment.BottomCenter).background(Color(0xCC000000), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            RightPanel(c, progress, level, Modifier.width(92.dp).fillMaxHeight(), onPause = { paused = true }, sounds = sounds)
        }

        intro?.let { (title, text) ->
            Dialog(title) {
                ArcadeText(text, 16.sp, Palette.cream, Modifier.widthIn(max = 420.dp))
                Spacer(Modifier.height(16.dp))
                ArcadeButton("Let's go!") { progress.markTutorialSeen("intro_${level.number}"); intro = null; c.clearHint() }
            }
        }

        if (paused) {
            Dialog("Paused") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeButton("Resume") { paused = false; c.clearHint() }
                    ArcadeButton("Restart", color = Palette.goldDeep) { paused = false; onRestart() }
                    ArcadeButton("Map", color = Palette.wood) { paused = false; onMap() }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeButton(if (progress.sound) "Sound: on" else "Sound: off", color = Palette.wood, textSize = 16.sp) {
                        progress.toggleSound(); sounds.enabled = progress.sound
                    }
                    ArcadeButton(if (progress.vibration) "Vibration: on" else "Vibration: off", color = Palette.wood, textSize = 16.sp) { progress.toggleVibration() }
                }
            }
        }

        if (c.status == GameStatus.WON) WinDialog(c, level, onMap, onNext)
        if (c.status == GameStatus.LOST) LostDialog(c, level, progress, onMap, onRestart)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LeftPanel(c: GameController, progress: Progress, modifier: Modifier) {
    Column(modifier.arcadePanel().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ArcadeText("LEVEL ${c.level.number}", 18.sp, Palette.gold)
        Spacer(Modifier.height(6.dp))
        ArcadeText(c.level.goal.title, 12.sp, Palette.cream, Modifier.fillMaxWidth())
        ArcadeText(c.level.goal.progressText(c.found), 20.sp, Palette.green)
        Spacer(Modifier.height(8.dp))
        // Moves
        val pulse = remember { Animatable(1f) }
        LaunchedEffect(c.moves) { pulse.snapTo(1.2f); pulse.animateTo(1f, spring(dampingRatio = 0.35f)) }
        val low = c.moves <= 3 && c.status == GameStatus.PLAYING
        Box(
            Modifier
                .size(74.dp)
                .scale(pulse.value)
                .background(Brush.radialGradient(listOf(if (low) Palette.red else Palette.greenDeep, Palette.frame)), CircleShape)
                .border(3.dp, Palette.gold, CircleShape),
            contentAlignment = Alignment.Center,
        ) { ArcadeText("${c.moves}", 32.sp) }
        ArcadeText("MOVES", 12.sp, Palette.gold)
        Spacer(Modifier.height(8.dp))
        ArcadeText("Score ${c.score}", 14.sp)
        ArcadeText("🪙 ${progress.coins + c.coins - c.coinsSaved}", 14.sp, Palette.gold)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            c.found.takeLast(6).forEach { w ->
                ArcadeText(
                    w.word, 11.sp, Color.White,
                    Modifier.background(Palette.greenDeep, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun RightPanel(c: GameController, progress: Progress, level: Level, modifier: Modifier, onPause: () -> Unit, sounds: SoundFx) {
    Column(modifier.arcadePanel().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RoundIcon("⏸", "Pause") { if (!c.busy) onPause() }
        RoundIcon(if (progress.sound) "🔊" else "🔇", "Sound") { progress.toggleSound(); sounds.enabled = progress.sound }
        val hintFree = level.number <= 2
        RoundIcon("💡", if (hintFree) "Hint" else "Hint 20") {
            if (c.busy || c.status != GameStatus.PLAYING) return@RoundIcon
            if (hintFree || progress.spend(20)) {
                if (!c.showHint()) c.showPopup("No easy word", "Try moving tiles to set one up", big = false)
            } else c.showPopup("Not enough coins", "Make words to earn coins", big = false)
        }
    }
}

@Composable
private fun RoundIcon(icon: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(48.dp)
                .background(Brush.verticalGradient(listOf(Palette.woodLight, Palette.wood)), CircleShape)
                .border(2.dp, Palette.gold, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { ArcadeText(icon, 20.sp) }
        ArcadeText(label, 10.sp, Palette.cream)
    }
}

@Composable
private fun PopupView(popup: Popup?, combo: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(visible = popup != null, enter = scaleIn(spring(dampingRatio = 0.45f)) + fadeIn(), exit = fadeOut()) {
            val p = popup ?: return@AnimatedVisibility
            Column(
                Modifier.background(Color(0xB3000000), RoundedCornerShape(16.dp)).padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArcadeText(p.title, if (p.big) 38.sp else 24.sp, Palette.gold)
                if (p.subtitle.isNotEmpty()) ArcadeText(p.subtitle, 15.sp, Palette.green)
            }
        }
        if (combo >= 2) {
            val s = remember { Animatable(0.5f) }
            LaunchedEffect(combo) { s.snapTo(1.6f); s.animateTo(1f, spring(dampingRatio = 0.3f)) }
            ArcadeText("COMBO x$combo!", 26.sp, Palette.green, Modifier.scale(s.value))
        }
    }
}

@Composable
fun Dialog(title: String, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.arcadePanel().padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArcadeText(title, 28.sp, Palette.gold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun WinDialog(c: GameController, level: Level, onMap: () -> Unit, onNext: (Int) -> Unit) {
    val stars = c.game.stars()
    val next = Levels.get(level.number + 1)
    Dialog(if (next == null) "ALL LEVELS DONE!" else "LEVEL COMPLETE!") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..3) {
                val s = remember { Animatable(0f) }
                LaunchedEffect(Unit) { delay(250L * i); s.animateTo(1f, spring(dampingRatio = 0.4f)) }
                ArcadeText(if (i <= stars) "★" else "☆", 46.sp, if (i <= stars) Palette.gold else Color(0x66FFFFFF), Modifier.scale(s.value))
            }
        }
        ArcadeText("Score ${c.score}", 20.sp)
        ArcadeText("+${c.coins} 🪙   ·   ${c.found.size} words   ·   best: ${c.found.maxByOrNull { it.length }?.word ?: "-"}", 15.sp, Palette.cream)
        if (next == null) ArcadeText("More levels are coming soon!", 15.sp, Palette.green)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArcadeButton("Map", color = Palette.wood) { onMap() }
            if (next != null) ArcadeButton("Next ▶") { onNext(next.number) }
        }
    }
}

@Composable
private fun LostDialog(c: GameController, level: Level, progress: Progress, onMap: () -> Unit, onRestart: () -> Unit) {
    Dialog("OUT OF MOVES") {
        ArcadeText(level.goal.remainingText(c.found), 20.sp, Palette.green, Modifier.widthIn(max = 420.dp))
        ArcadeText("Don't give up, you were close!", 14.sp, Palette.cream)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val canBuy = progress.coins >= Progress.EXTRA_MOVES_PRICE
            ArcadeButton("+${Progress.EXTRA_MOVES} moves (${Progress.EXTRA_MOVES_PRICE} 🪙)", color = Palette.goldDeep, enabled = canBuy) {
                if (progress.spend(Progress.EXTRA_MOVES_PRICE)) c.buyMoves(Progress.EXTRA_MOVES)
            }
            ArcadeButton("Try again") { onRestart() }
            ArcadeButton("Map", color = Palette.wood) { onMap() }
        }
        ArcadeText("You have ${progress.coins} 🪙", 13.sp, Palette.gold, Modifier.padding(top = 8.dp))
    }
}
