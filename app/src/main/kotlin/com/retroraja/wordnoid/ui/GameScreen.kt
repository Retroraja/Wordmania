package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.Game
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.core.Objective
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    level: Level,
    dict: Dictionary,
    progress: Progress,
    sounds: SoundFx,
    haptics: Haptics,
    startBoosters: List<Booster>,
    onMap: () -> Unit,
    onRestart: () -> Unit,
    onNext: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val bonus = if (progress.fails(level.number) >= 3) 3 else 0
    val c = remember { GameController(level, dict, progress, sounds, haptics, scope, bonus, startBoosters) }
    var intro by remember { mutableStateOf(introFor(level, progress)) }
    var paused by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (intro == null) c.begin() }
    LaunchedEffect(Unit) { if (bonus > 0) c.toast("+3 bonus moves to help you!", Offset(400f, 300f), Palette.green, big = true) }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(c.shakeKey) {
        if (c.shakeKey > 0) for (i in 0 until 8) { shake.animateTo(if (i % 2 == 0) 14f - i else -14f + i, tween(35)) }
        shake.animateTo(0f, tween(40))
    }

    LivingBackground(level.number) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            with(density) { c.screenWidth = maxWidth.toPx(); c.screenHeight = maxHeight.toPx() }
            val leftW = maxWidth * 0.23f
            val rightW = maxWidth * 0.13f
            val boardW = maxWidth - leftW - rightW - 40.dp
            val boardH = maxHeight - 36.dp
            val cell = min(boardW / level.cols, boardH / level.rows)
            Row(
                Modifier.fillMaxSize().offset { IntOffset(shake.value.roundToInt(), 0) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeftPanel(c, Modifier.width(leftW).fillMaxHeight())
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    BoardView(c, cell)
                }
                RightPanel(c, Modifier.width(rightW).fillMaxHeight(), onPause = { paused = true }, onMore = { more = true })
            }
            ParticleLayer(c.particles)
            Overlays(c)
            c.tutorial?.let { TutorialBubble(it, onDismiss = { c.dismissTutorial() }) }
            c.message?.let { MessageBar(it) }
            if (c.slideAt != null) SlideArrows(c)
            c.letterPick?.let { LetterPicker(it, onPick = { ch -> c.pickLetter(ch) }, onCancel = { c.cancelAim() }) }
            c.buyAsk?.let { BuyDialog(it, progress, onBuy = { c.buy(it) }, onClose = { c.closeBuy() }) }
            if (more) MoreBoosters(c, onClose = { more = false })
            c.mysteryCards?.let { MysteryCards(it, sounds, onPicked = { b -> c.mysteryPicked(b) }, onDone = { c.mysteryDone() }) }
            if (c.showWin) WinDialog(c, onNext = { onNext(level.number + 1) }, onMap = onMap)
            if (c.showLose) LoseDialog(c, onRetry = { c.giveUp(); onRestart() }, onMap = { c.giveUp(); onMap() })
            if (paused) PauseDialog(progress, onResume = { paused = false }, onRestart = onRestart, onMap = onMap)
            intro?.let { card -> IntroCard(card, onGo = { intro = null; c.begin() }) }
        }
    }
}

// ---------------------------------------------------------------- left panel

@Composable
private fun LeftPanel(c: GameController, modifier: Modifier) {
    val level = c.level
    val anim = rememberInfiniteTransition(label = "left")
    val beat by anim.animateFloat(1f, 1.12f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "beat")
    Column(
        modifier.arcadePanel().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArcadeText("LEVEL ${level.number}", 15.sp, Palette.gold)
            if (level.boss) Text(" 💀", fontSize = 13.sp)
        }
        // Goals.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (o in level.targets) GoalRow(o, c)
        }
        // Moves.
        val low = c.movesLeft <= 3
        Box(
            Modifier
                .size(60.dp)
                .onGloballyPositioned { c.movesAnchor = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }
                .scale(if (low) beat else 1f)
                .background(Brush.radialGradient(listOf(if (low) Palette.red else Palette.greenDeep, Color(0xFF0B2A16))), CircleShape)
                .border(3.dp, Palette.gold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ArcadeText("${c.movesLeft}", 22.sp)
                Text("MOVES", fontSize = 8.sp, color = Palette.cream, fontWeight = Heavy)
            }
            if (c.freeMoves > 0) Text("⏳", Modifier.align(Alignment.TopEnd).offset(6.dp, (-4).dp), fontSize = 14.sp)
            if (c.doubleNext) Text("🌟", Modifier.align(Alignment.TopStart).offset((-6).dp, (-4).dp), fontSize = 14.sp)
        }
        PowerMeter(c.progress.meter)
        // Coins.
        val shownCoins by animateIntAsState(c.progress.coins, tween(600), label = "coins")
        Row(
            Modifier
                .background(Color(0x66000000), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .onGloballyPositioned { c.coinAnchor = it.positionInRoot() + Offset(20f, it.size.height / 2f) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🪙", fontSize = 14.sp)
            ArcadeText(" $shownCoins", 14.sp, Palette.gold)
        }
        level.bonusWord?.let { w ->
            val shown = if (c.bonusFound) "$w ✓" else w.mapIndexed { i, ch -> if (i == 0) ch else '_' }.joinToString(" ")
            Text("Bonus: $shown", fontSize = 10.sp, color = if (c.bonusFound) Palette.green else Palette.cream, fontWeight = Heavy)
        }
    }
}

@Composable
private fun GoalRow(o: Objective, c: GameController) {
    val left = o.left(c.stats)
    val done = left == 0
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (done) Color(0x5539E07A) else Color(0x44000000), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .onGloballyPositioned { if (o is Objective.Gold) c.goalAnchor = it.positionInRoot() + Offset(20f, it.size.height / 2f) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (o is Objective.Letter) {
            Tile3D(com.retroraja.wordnoid.core.Tile(0, o.letter), 20.dp)
        } else Text(o.icon, fontSize = 15.sp)
        Text(
            o.label, Modifier.weight(1f).padding(start = 4.dp),
            fontSize = 10.sp, color = Palette.cream, fontWeight = Heavy, maxLines = 2, lineHeight = 11.sp,
        )
        if (done) Text("✅", fontSize = 13.sp) else ArcadeText("$left", 15.sp, Palette.gold)
    }
}

@Composable
private fun PowerMeter(meter: Int) {
    val anim = rememberInfiniteTransition(label = "meter")
    val shine by anim.animateFloat(0.6f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "shine")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚡ POWER", fontSize = 9.sp, color = Palette.gold, fontWeight = Heavy)
        Row(
            Modifier.background(Color(0x88000000), RoundedCornerShape(6.dp)).border(1.dp, Palette.goldDeep, RoundedCornerShape(6.dp)).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (i in 0 until Game.METER_SIZE) {
                val on = i < meter
                Box(
                    Modifier.size(width = 10.dp, height = 12.dp).background(
                        if (on) Brush.verticalGradient(listOf(Palette.cyan.lighter(0.4f), Palette.cyan.copy(alpha = shine))) else Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF1A1A1A))),
                        RoundedCornerShape(3.dp),
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------- right panel

/** Up to 4 boosters on the panel: the ones this level teaches first, then the ones you own most of. */
fun panelBoosters(c: GameController): List<Booster> {
    val unlocked = Booster.unlockedAt(c.level.number)
    val intro = c.level.introBoosters
    val owned = unlocked.filter { it !in intro }.sortedByDescending { c.progress.count(it) * 10 + (if (c.progress.hasFreeUse(it)) 100 else 0) - it.price / 20 }
    return (intro + owned).distinct().take(4)
}

@Composable
private fun RightPanel(c: GameController, modifier: Modifier, onPause: () -> Unit, onMore: () -> Unit) {
    Column(
        modifier.arcadePanel().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        RoundButton("⏸", 34.dp, onClick = onPause)
        for (b in panelBoosters(c)) BoosterButton(b, c)
        if (Booster.unlockedAt(c.level.number).size > 4) RoundButton("⋯", 34.dp, label = "More", onClick = onMore)
    }
}

@Composable
fun RoundButton(text: String, size: androidx.compose.ui.unit.Dp, label: String? = null, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .background(Brush.radialGradient(listOf(Palette.goldDeep, Color(0xFF6B3A08))), CircleShape)
                .border(2.dp, Palette.gold, CircleShape)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(text, fontSize = (size.value * 0.45f).sp, color = Color.White, fontWeight = Heavy) }
        label?.let { Text(it, fontSize = 8.sp, color = Palette.cream, fontWeight = Heavy) }
    }
}

@Composable
fun BoosterButton(b: Booster, c: GameController, size: androidx.compose.ui.unit.Dp = 50.dp, onUsed: () -> Unit = {}) {
    val anim = rememberInfiniteTransition(label = "bb")
    val pulse by anim.animateFloat(1f, 1.15f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "p")
    val spotlight = c.tutorial?.booster == b || c.nudge == b
    val isArmed = c.armed == b
    val count = c.progress.count(b)
    val free = c.progress.hasFreeUse(b)
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .scale(if (spotlight || isArmed) pulse else 1f)
                .graphicsLayer { translationY = if (isArmed) -8f else 0f }
                .background(
                    Brush.radialGradient(listOf(if (isArmed) Palette.green else Color(0xFF5B3BB5), if (isArmed) Palette.greenDeep else Color(0xFF241552))),
                    CircleShape,
                )
                .border(if (spotlight) 4.dp else 2.dp, if (spotlight) Color.White else Palette.gold, CircleShape)
                .clickable(remember { MutableInteractionSource() }, null) { c.boosterTapped(b); onUsed() },
            contentAlignment = Alignment.Center,
        ) { Text(b.emoji, fontSize = (size.value * 0.46f).sp) }
        val badge = when {
            free -> "FREE"
            count > 0 -> "$count"
            else -> "🪙${Rewards.price(b, c.progress.unlocked)}"
        }
        Text(
            badge,
            Modifier.align(Alignment.BottomEnd).offset(6.dp, 4.dp)
                .background(if (free) Palette.green else if (count > 0) Palette.red else Color(0xFF333333), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            fontSize = 9.sp, color = Color.White, fontWeight = Heavy,
        )
    }
}

// ---------------------------------------------------------------- intro card

data class IntroInfo(val title: String, val emoji: String, val text: String)

fun introFor(level: Level, progress: Progress): IntroInfo? {
    val key = "intro_level_${level.number}"
    if (progress.seen(key)) return null
    progress.markSeen(key)
    return when {
        level.number == 1 -> IntroInfo("How to play", "🔤", "Swap two neighbouring tiles to spell a word. Words count in any direction: left, right, up or down. Only words made with the tile you moved count, so think before you swap!")
        level.number == 2 -> IntroInfo("Boosters", "✨", "Boosters help when you're stuck. You'll learn a new one on many levels. Each word also fills your ⚡ Power Meter: fill it to win a free booster!")
        level.number == 3 -> IntroInfo("Long words", "📏", "4-letter words give +1 move. 5+ letters give +2 moves and a firework! Plan bigger words to go further.")
        level.number == 5 -> IntroInfo("Coins buy boosters", "🪙", "Every word earns coins. Tap a booster you don't have to buy it. Out of moves? Spend 150 coins to keep playing. Keep some coins saved!")
        level.feature != null -> IntroInfo(level.feature!!.title, featureEmoji(level.feature!!), level.feature!!.text)
        else -> null
    }
}

fun featureEmoji(f: com.retroraja.wordnoid.core.Feature): String = when (f) {
    com.retroraja.wordnoid.core.Feature.GOLD -> "🪙"
    com.retroraja.wordnoid.core.Feature.CRATES -> "📦"
    com.retroraja.wordnoid.core.Feature.LETTERS -> "🔠"
    com.retroraja.wordnoid.core.Feature.BONUS_WORD -> "🎁"
    com.retroraja.wordnoid.core.Feature.PRE_BOOSTERS -> "🚀"
    com.retroraja.wordnoid.core.Feature.GIFTS -> "🎀"
    com.retroraja.wordnoid.core.Feature.THEME -> "🐾"
    com.retroraja.wordnoid.core.Feature.LONG -> "📏"
    com.retroraja.wordnoid.core.Feature.DOUBLE_CRATES -> "🧰"
    com.retroraja.wordnoid.core.Feature.ICE -> "🧊"
    com.retroraja.wordnoid.core.Feature.SPELL -> "✍️"
    com.retroraja.wordnoid.core.Feature.CHAINS -> "⛓️"
    com.retroraja.wordnoid.core.Feature.BOSS -> "💀"
}

@Composable
fun IntroCard(info: IntroInfo, onGo: () -> Unit) {
    DimBox {
        Column(
            Modifier.width(420.dp).arcadePanel().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pop = remember { Animatable(0.3f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f)) }
            Text(info.emoji, fontSize = 44.sp, modifier = Modifier.scale(pop.value))
            ArcadeText(info.title, 26.sp, Palette.gold)
            Spacer(Modifier.height(8.dp))
            Text(info.text, color = Palette.cream, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(14.dp))
            ArcadeButton("Let's go!", onClick = onGo)
        }
    }
}

/** A dark see-through layer that blocks taps behind a dialog. */
@Composable
fun DimBox(alpha: Float = 0.6f, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun MessageBar(text: String) {
    Box(Modifier.fillMaxSize().padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text,
            Modifier.background(Color(0xDD1B0E05), RoundedCornerShape(14.dp)).border(2.dp, Palette.gold, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 6.dp),
            color = Palette.cream, fontSize = 15.sp, fontWeight = Heavy, style = TextStyle(shadow = OutlineShadow),
        )
    }
}
