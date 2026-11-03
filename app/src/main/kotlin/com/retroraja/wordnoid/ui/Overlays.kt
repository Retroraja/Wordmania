package com.retroraja.wordnoid.ui

import android.app.Activity
import com.retroraja.wordnoid.ads.Ads
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Aim
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Tile
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

/** The big word, floating texts and booster animations. */
@Composable
fun Overlays(c: GameController) {
    c.banner?.let { b -> key(b.key) { WordBannerView(b) } }
    for (t in c.toasts) key(t.id) { ToastView(t) }
    c.fx?.let { f -> key(f.key) { BoosterFxView(f, c) } }
    if (c.wordRain) MessageBar("☔ Leftover moves burst into coins!")
}

@Composable
private fun WordBannerView(b: WordBanner) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
    Box(Modifier.fillMaxSize().padding(top = 6.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value; rotationX = (1 - appear.value) * 70f; alpha = appear.value.coerceIn(0f, 1f) }
                .background(Brush.verticalGradient(listOf(Color(0xEE3B1A6B), Color(0xEE180A33))), RoundedCornerShape(20.dp))
                .border(3.dp, Brush.horizontalGradient(listOf(Palette.gold, Palette.pink, Palette.cyan, Palette.gold)), RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                b.word.forEachIndexed { i, ch ->
                    val pop = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { delay(i * 60L); pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f)) }
                    Tile3D(Tile(i.toLong(), ch), 40.dp, Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value; rotationY = (1 - pop.value) * 180f })
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeText(b.praise, 24.sp, Palette.gold)
                if (b.newWord) Text(
                    " NEW WORD", Modifier.padding(start = 6.dp).background(Palette.pink, RoundedCornerShape(8.dp)).padding(horizontal = 5.dp),
                    color = Color.White, fontSize = 11.sp, fontWeight = Heavy,
                )
            }
            if (b.lines.isNotEmpty()) Text(b.lines.joinToString("   "), color = Palette.cream, fontSize = 13.sp, fontWeight = Heavy)
        }
    }
}

@Composable
private fun ToastView(t: Toast) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) { rise.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
    val density = LocalDensity.current
    val pop = (rise.value * 6f).coerceAtMost(1f)
    Text(
        t.text,
        Modifier
            .offset { IntOffset((t.at.x - with(density) { 60.dp.toPx() }).roundToInt(), (t.at.y - 70f * rise.value - 20f).roundToInt()) }
            .graphicsLayer { scaleX = 0.5f + 0.5f * pop; scaleY = 0.5f + 0.5f * pop; alpha = if (rise.value > 0.7f) (1f - rise.value) / 0.3f else 1f }
            .width(120.dp),
        textAlign = TextAlign.Center,
        style = TextStyle(fontFamily = ArcadeFont, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, fontSize = if (t.big) 20.sp else 15.sp, color = t.color, shadow = OutlineShadow),
    )
}

/** Each booster has its own animation. */
@Composable
private fun BoosterFxView(f: BoosterFx, c: GameController) {
    val t = remember { Animatable(0f) }
    val duration = when (f.booster) {
        Booster.SCANNER -> 900
        Booster.UNDO, Booster.SHUFFLE -> 700
        else -> 650
    }
    LaunchedEffect(Unit) {
        t.animateTo(1f, tween(duration, easing = LinearEasing))
        if (f.booster == Booster.HAMMER || f.booster == Booster.AXE) f.cells.firstOrNull()?.let { p ->
            if (c.board[p].isCrate) c.particles.splinters(c.cellCenter(p)) else c.particles.burst(c.cellCenter(p), tileColor(c.board[p]), 0.8f)
        }
    }
    LaunchedEffect(Unit) {
        when (f.booster) {
            Booster.MAGIC_SWAP -> f.cells.forEach { c.particles.sparkle(c.cellCenter(it), Palette.cyan, 14) }
            Booster.JOKER -> f.cells.forEach { c.particles.sparkle(c.cellCenter(it), Palette.pink, 16) }
            Booster.PAINT -> f.cells.forEach { c.particles.burst(c.cellCenter(it), Palette.pink, 0.5f) }
            Booster.MAGNET -> f.cells.forEach { c.particles.sparkle(c.cellCenter(it), Palette.gold, 18); c.particles.coins(c.cellCenter(it), c.goalAnchor, 4) }
            Booster.DOUBLE -> c.particles.sparkle(c.center(), Palette.gold, 24)
            Booster.PLUS_MOVES, Booster.FREE_MOVE -> c.particles.sparkle(c.movesAnchor, Palette.green, 20)
            Booster.LOCK -> f.cells.forEach { c.particles.sparkle(c.cellCenter(it), Color.White, 8) }
            else -> {}
        }
    }
    val v = t.value
    val density = LocalDensity.current
    val cellPx = c.cellPx
    val first = f.cells.firstOrNull()
    val at: Offset = when (f.booster) {
        Booster.UNDO, Booster.SHUFFLE, Booster.DOUBLE, Booster.LETTER_RAIN -> c.center()
        Booster.PLUS_MOVES, Booster.FREE_MOVE -> c.movesAnchor
        Booster.SPIN -> first?.let { c.cellCenter(it) + Offset(cellPx / 2, cellPx / 2) } ?: c.center()
        Booster.SCANNER -> c.center()
        else -> first?.let { c.cellCenter(it) } ?: c.center()
    }
    if (f.booster == Booster.SCANNER) {
        // A light beam sweeping across the board.
        Canvas(Modifier.fillMaxSize()) {
            val x = c.boardOrigin.x + (c.board.cols * cellPx) * v
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Palette.cyan.copy(alpha = 0.7f), Color.White.copy(alpha = 0.9f), Palette.cyan.copy(alpha = 0.7f), Color.Transparent), startX = x - 80f, endX = x + 80f),
                Offset(x - 80f, c.boardOrigin.y), Size(160f, c.board.rows * cellPx),
            )
        }
    }
    if (f.booster == Booster.ROW_SLIDE || f.booster == Booster.COLUMN_SLIDE) {
        Canvas(Modifier.fillMaxSize()) {
            val p = first ?: return@Canvas
            if (f.booster == Booster.ROW_SLIDE) {
                val y = c.boardOrigin.y + (p.row + 0.5f) * cellPx
                drawRect(Palette.cyan.copy(alpha = 0.35f * (1 - v)), Offset(c.boardOrigin.x, y - cellPx / 2), Size(c.board.cols * cellPx, cellPx))
            } else {
                val x = c.boardOrigin.x + (p.col + 0.5f) * cellPx
                drawRect(Palette.cyan.copy(alpha = 0.35f * (1 - v)), Offset(x - cellPx / 2, c.boardOrigin.y), Size(cellPx, c.board.rows * cellPx))
            }
        }
    }
    val sizeDp = with(density) { (cellPx * 1.3f).toDp() }
    // Big emoji with a motion per booster.
    val (dx, dy, rot, scale) = when (f.booster) {
        Booster.HAMMER, Booster.AXE -> Quad(cellPx * 0.3f, -cellPx * 0.4f * (1 - v), -70f + 90f * easeIn(v), 1.6f)
        Booster.JOKER, Booster.PAINT, Booster.VOWEL_FLIP, Booster.DICE -> Quad(0f, -cellPx * 0.6f, 360f * v, 1.2f + 0.4f * sin(v * 3.14f))
        Booster.MAGIC_SWAP -> Quad(0f, -cellPx * 0.5f, 0f, 1f + 0.6f * sin(v * 3.14f))
        Booster.UNDO -> Quad(0f, 0f, -360f * v, 2.5f * sin(v * 3.14f))
        Booster.SHUFFLE -> Quad(0f, 0f, 540f * v, 2.5f * sin(v * 3.14f))
        Booster.SPIN -> Quad(0f, 0f, 720f * v, 1.8f)
        Booster.FREE_MOVE -> Quad(0f, -cellPx * 0.8f, 180f * v, 1.6f)
        Booster.PLUS_MOVES -> Quad(0f, -cellPx * (1.5f - 1.5f * v), 0f, 2f - v)
        Booster.LOCK -> Quad(0f, -cellPx * 1.5f * (1 - easeIn(v)), 0f, 1.4f)
        Booster.DOUBLE -> Quad(0f, 0f, 360f * v, 3f * sin(v * 3.14f))
        Booster.MAGNET -> Quad(sin(v * 40f) * 6f, -cellPx * 0.7f, 0f, 1.5f)
        Booster.LETTER_RAIN -> Quad(0f, -c.board.rows * cellPx * 0.5f, 0f, 2f)
        Booster.HINT -> Quad(0f, -cellPx * 0.7f, 0f, 1.3f + 0.3f * sin(v * 9f))
        else -> Quad(0f, 0f, 0f, 0f)
    }
    if (scale > 0f && v < 1f) {
        Text(
            f.booster.emoji,
            Modifier
                .offset { IntOffset((at.x + dx - cellPx * 0.65f).roundToInt(), (at.y + dy - cellPx * 0.65f).roundToInt()) }
                .size(sizeDp)
                .graphicsLayer { rotationZ = rot; scaleX = scale; scaleY = scale; alpha = if (v > 0.85f) (1 - v) / 0.15f else 1f },
            fontSize = (sizeDp.value * 0.7f).sp,
            textAlign = TextAlign.Center,
        )
    }
    if (f.booster == Booster.LETTER_RAIN && v < 1f) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until 14) {
                val x = c.boardOrigin.x + ((i * 0.37f + 0.1f) % 1f) * c.board.cols * cellPx
                val y = c.boardOrigin.y + ((v * 1.6f + i * 0.13f) % 1f) * c.board.rows * cellPx
                drawLine(Palette.cyan, Offset(x, y), Offset(x - 4f, y - 26f), strokeWidth = 4f)
            }
        }
    }
}

private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)

private fun easeIn(v: Float) = v * v

// ---------------------------------------------------------------- tutorial

@Composable
fun TutorialBubble(t: Tutorial, onDismiss: () -> Unit) {
    val anim = rememberInfiniteTransition(label = "tut")
    val bob by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bob")
    Box(Modifier.fillMaxSize().padding(bottom = 6.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier
                .graphicsLayer { translationY = -6f * bob }
                .background(Brush.verticalGradient(listOf(Color(0xF2FFF4DE), Color(0xF2FFE0A8))), RoundedCornerShape(16.dp))
                .border(3.dp, Palette.goldDeep, RoundedCornerShape(16.dp))
                .clickable(remember { MutableInteractionSource() }, null) { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (t.booster != null) "👉" else "💬", fontSize = 22.sp)
            Text(
                t.text, Modifier.padding(start = 8.dp).width(420.dp),
                color = Color(0xFF1B2140), fontSize = 15.sp, fontWeight = Heavy, lineHeight = 19.sp,
            )
            if (t.dismissible) Text("  OK", color = Palette.greenDeep, fontSize = 15.sp, fontWeight = Heavy)
        }
    }
}

// ---------------------------------------------------------------- booster helpers

@Composable
fun LetterPicker(lp: LetterPick, onPick: (Char) -> Unit, onCancel: () -> Unit) {
    DimBox(0.5f) {
        Column(Modifier.arcadePanel().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeText("${lp.booster.emoji} Pick a letter", 20.sp, Palette.gold)
            Spacer(Modifier.height(8.dp))
            val rows = lp.letters.chunked(if (lp.letters.size > 6) 9 else lp.letters.size)
            for (r in rows) Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(2.dp)) {
                for (ch in r) Tile3D(Tile(ch.code.toLong(), ch), if (lp.letters.size > 6) 40.dp else 58.dp, Modifier.clickable { onPick(ch) })
            }
            Spacer(Modifier.height(6.dp))
            Text("Cancel", Modifier.clickable { onCancel() }, color = Palette.cream, fontSize = 14.sp)
        }
    }
}

@Composable
fun BuyDialog(b: Booster, progress: Progress, onBuy: () -> Unit, onClose: () -> Unit) {
    val price = Rewards.price(b, progress.unlocked)
    DimBox {
        Column(Modifier.width(360.dp).arcadePanel().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(b.emoji, fontSize = 44.sp)
            ArcadeText(b.title, 24.sp, Palette.gold)
            Text(b.description, color = Palette.cream, fontSize = 14.sp, textAlign = TextAlign.Center)
            if (price < b.price) Text("Booster of the day: half price!", color = Palette.green, fontSize = 13.sp, fontWeight = Heavy)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArcadeButton("Buy 🪙$price", enabled = progress.coins >= price, onClick = onBuy)
                ArcadeButton("Close", color = Palette.slate, onClick = onClose)
            }
            Text("You have 🪙${progress.coins}", color = Palette.cream, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun MoreBoosters(c: GameController, onClose: () -> Unit) {
    DimBox {
        Column(Modifier.arcadePanel().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeText("All boosters", 20.sp, Palette.gold)
            Spacer(Modifier.height(6.dp))
            val all = Booster.unlockedAt(c.level.number)
            for (row in all.chunked(7)) Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(6.dp)) {
                for (b in row) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BoosterButton(b, c, 46.dp, onUsed = onClose)
                    Text(b.title, fontSize = 9.sp, color = Palette.cream, fontWeight = Heavy)
                }
            }
            Spacer(Modifier.height(4.dp))
            ArcadeButton("Close", textSize = 16.sp, onClick = onClose)
        }
    }
}

// ---------------------------------------------------------------- mystery cards

@Composable
fun MysteryCards(cards: List<Booster>, sounds: SoundFx, onPicked: (Booster) -> Unit, onDone: () -> Unit) {
    var picked by remember { mutableStateOf(-1) }
    DimBox(0.7f) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeText("⚡ POWER METER FULL! ⚡", 26.sp, Palette.gold)
            Text(if (picked < 0) "Pick a mystery card" else "You got ${cards[picked].emoji} ${cards[picked].title}!", color = Palette.cream, fontSize = 16.sp, fontWeight = Heavy)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                cards.forEachIndexed { i, b ->
                    val flip = remember { Animatable(0f) }
                    LaunchedEffect(picked) { if (picked >= 0) { delay(if (i == picked) 0L else 500L); flip.animateTo(180f, tween(500)) } }
                    val faceUp = flip.value > 90f
                    val enter = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { delay(i * 120L); enter.animateTo(1f, spring(dampingRatio = 0.5f)) }
                    Box(
                        Modifier
                            .size(width = 100.dp, height = 140.dp)
                            .graphicsLayer { rotationY = flip.value; cameraDistance = 14f * density; scaleX = enter.value; scaleY = enter.value; alpha = if (picked >= 0 && i != picked && faceUp) 0.55f else 1f }
                            .background(
                                if (faceUp) Brush.verticalGradient(listOf(Color(0xFFFFF4DE), Color(0xFFFFD27A)))
                                else Brush.linearGradient(listOf(Palette.purple, Palette.pink, Palette.purple)),
                                RoundedCornerShape(14.dp),
                            )
                            .border(3.dp, if (i == picked) Palette.green else Palette.gold, RoundedCornerShape(14.dp))
                            .clickable(enabled = picked < 0) { picked = i; sounds.play("flip"); onPicked(b) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (faceUp) Column(Modifier.graphicsLayer { rotationY = 180f }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(b.emoji, fontSize = 40.sp)
                            Text(b.title, color = Color(0xFF1B2140), fontSize = 12.sp, fontWeight = Heavy, textAlign = TextAlign.Center)
                        } else ArcadeText("?", 48.sp, Color.White)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (picked >= 0) ArcadeButton("Collect!", onClick = onDone)
        }
    }
}

// ---------------------------------------------------------------- end of level

/** A "Watch a video" button for one of the rewarded ad spots. Hidden when the spot is used up for today. */
@Composable
fun WatchButton(text: String, spot: Ads.Spot, activity: Activity, onReward: () -> Unit) {
    if (!Ads.spotAvailable(spot)) return
    var note by remember { mutableStateOf<String?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ArcadeButton("▶ $text", color = Color(0xFFE0457B), textSize = 15.sp) {
            Ads.showRewarded(activity, spot, onReward = { note = null; onReward() }, onFail = { note = it })
        }
        note?.let { Text(it, color = Palette.cream, fontSize = 11.sp) }
    }
}

@Composable
fun WinDialog(c: GameController, activity: Activity, onNext: () -> Unit, onTown: () -> Unit) {
    var doubled by remember { mutableStateOf(false) }
    DimBox(0.45f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(460.dp).arcadePanel().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ArcadeText(if (c.level.boss) "BOSS DEFEATED!" else "LEVEL COMPLETE!", 26.sp, Palette.gold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    for (i in 1..3) {
                        val s = remember { Animatable(3f) }
                        val on = i <= c.winStars
                        LaunchedEffect(on) {
                            delay(250L + i * 300L)
                            if (on) c.particles.sparkle(c.center(), Palette.gold, 12)
                            s.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
                        }
                        Text("🧱", fontSize = if (i == 2) 46.sp else 36.sp, modifier = Modifier.scale(s.value).alpha(if (on) 1f else 0.2f))
                    }
                }
                Text("${c.winStars} Letter Brick${if (c.winStars > 1) "s" else ""} for Tiko's Town!", color = Color.White, fontSize = 14.sp, fontWeight = Heavy)
                Text("🪙 +${c.coinsThisLevel}   ·   Score ${c.score}", color = Palette.gold, fontSize = 16.sp, fontWeight = Heavy)
                if (c.foundWords.isNotEmpty()) Text(
                    "Your words: " + c.foundWords.joinToString(", "),
                    color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 3, lineHeight = 15.sp,
                )
                if (c.newWordsThisLevel > 0) Text("📖 ${c.newWordsThisLevel} new words in your Wordbook", color = Palette.cream, fontSize = 12.sp)
                c.winBooster?.let { Text("🎁 Prize: ${it.emoji} ${it.title}!", color = Palette.green, fontSize = 15.sp, fontWeight = Heavy) }
                if (c.level.bonusWord != null && !c.bonusFound) Text("The bonus word was ${c.level.bonusWord}", color = Palette.cream.copy(alpha = 0.8f), fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!doubled) WatchButton("x2 coins", Ads.Spot.DOUBLE_WIN_COINS, activity) { c.doubleWinCoins(); doubled = true }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeButton("🏠 Back to Town", color = Palette.slate, textSize = 17.sp, onClick = onTown)
                    if (c.level.number < com.retroraja.wordnoid.core.Levels.all.size) ArcadeButton("Next ▶", onClick = onNext)
                }
            }
            Box(Modifier.padding(start = 10.dp).width(250.dp)) { Ads.Banner(big = true) }
        }
    }
}

@Composable
fun LoseDialog(c: GameController, activity: Activity, onRetry: () -> Unit, onMap: () -> Unit) {
    val fails = c.progress.fails(c.level.number)
    val moves = Progress.continueMoves(fails)
    val anim = rememberInfiniteTransition(label = "deal")
    val glow by anim.animateFloat(0.9f, 1.08f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "g")
    DimBox {
        Column(Modifier.width(480.dp).arcadePanel().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tiko(Modifier.size(54.dp))
                ArcadeText(" Out of moves!", 26.sp, Palette.gold)
            }
            val left = c.level.targets.filter { it.left(c.stats) > 0 }.joinToString("\n") { "${it.icon} ${it.left(c.stats)} more: ${it.text}" }
            Text("So close! Still needed:\n$left", color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
            Spacer(Modifier.height(6.dp))
            if (c.level.boss) {
                // Boss levels: one simple choice, +5 moves (coins or a video) or try again.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcadeButton(
                        "+5 moves 🪙${Progress.CONTINUE_PRICE}",
                        enabled = c.progress.coins >= Progress.CONTINUE_PRICE, textSize = 16.sp,
                    ) { c.continueForCoins() }
                    WatchButton("+5 moves", Ads.Spot.BOSS_REMATCH, activity) { c.continueForAd() }
                }
                if (c.progress.coins < Progress.CONTINUE_PRICE) Text("You have 🪙${c.progress.coins}.", color = Palette.cream, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                ArcadeButton("Try again", color = Color(0xFFE5533D), textSize = 16.sp, onClick = onRetry)
            } else {
                if (moves > 5) Text(
                    "🔥 SPECIAL DEAL: +$moves MOVES! 🔥", Modifier.scale(glow).background(Palette.red, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 2.dp),
                    color = Color.White, fontSize = 16.sp, fontWeight = Heavy,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ArcadeButton(
                        "+$moves moves 🪙${Progress.CONTINUE_PRICE}",
                        enabled = c.progress.coins >= Progress.CONTINUE_PRICE, textSize = 16.sp,
                    ) { c.continueForCoins() }
                    WatchButton("+5 moves", Ads.Spot.CONTINUE, activity) { c.continueForAd() }
                }
                if (c.progress.coins < Progress.CONTINUE_PRICE) Text("You have 🪙${c.progress.coins}.", color = Palette.cream, fontSize = 12.sp)
                if (c.progress.count(Booster.PLUS_MOVES) > 0) {
                    Spacer(Modifier.height(6.dp))
                    ArcadeButton("Use ➕ +3 Moves (you have ${c.progress.count(Booster.PLUS_MOVES)})", color = Palette.purple, textSize = 15.sp) { c.continueWithBooster() }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeButton("Try again", color = Color(0xFFE5533D), textSize = 16.sp, onClick = onRetry)
                    ArcadeButton("Level Map", color = Palette.slate, textSize = 16.sp, onClick = onMap)
                }
                // A free Hammer after a fail, for a video.
                var gotHammer by remember { mutableStateOf(false) }
                if (!gotHammer) WatchButton("Free 🔨 Hammer for next try", Ads.Spot.FREE_HAMMER, activity) { c.progress.addBooster(Booster.HAMMER); gotHammer = true }
            }
        }
    }
}

@Composable
fun PauseDialog(progress: Progress, c: GameController, activity: Activity, onResume: () -> Unit, onRestart: () -> Unit, onMap: () -> Unit) {
    DimBox {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(340.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArcadeText("Paused", 26.sp, Palette.gold)
                Text(c.level.targets.joinToString("\n") { "${it.icon} ${it.text}: ${minOf(it.count, it.progress(c.stats))}/${it.count}" }, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                SettingsToggles(progress)
                ArcadeButton("Resume", onClick = onResume)
                WatchButton("Stuck? Show me words", Ads.Spot.STUCK_HINT, activity) { c.freeHint(); onResume() }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcadeButton("Restart", color = Color(0xFFE5533D), textSize = 16.sp, onClick = onRestart)
                    ArcadeButton("Level Map", color = Palette.slate, textSize = 16.sp, onClick = onMap)
                }
            }
            Box(Modifier.padding(start = 10.dp).width(250.dp)) { Ads.Banner(big = true) }
        }
    }
}

@Composable
fun SettingsToggles(progress: Progress) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Toggle("🎵", "Music", progress.music) { progress.toggleMusic() }
        Toggle("🔊", "Sound", progress.sound) { progress.toggleSound() }
        Toggle("📳", "Vibrate", progress.vibration) { progress.toggleVibration() }
        Toggle("🔔", "Reminders", progress.notifications) { progress.toggleNotifications() }
    }
}

@Composable
private fun Toggle(icon: String, label: String, on: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .background(if (on) Palette.greenDeep else Color(0xFF444444), RoundedCornerShape(12.dp))
            .border(2.dp, Palette.gold, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 20.sp, modifier = Modifier.alpha(if (on) 1f else 0.4f))
        Text("$label ${if (on) "ON" else "OFF"}", fontSize = 10.sp, color = Color.White, fontWeight = Heavy)
    }
}
