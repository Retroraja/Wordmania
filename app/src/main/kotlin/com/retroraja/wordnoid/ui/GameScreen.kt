package com.retroraja.wordnoid.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.ads.Ads
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.Feature
import com.retroraja.wordnoid.core.Game
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.core.Objective
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameScreen(
    level: Level,
    dict: Dictionary,
    progress: Progress,
    sounds: SoundFx,
    haptics: Haptics,
    startBoosters: List<Booster>,
    onMap: () -> Unit,
    onTown: () -> Unit,
    onRestart: () -> Unit,
    onNext: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    val bonus = if (progress.fails(level.number) >= 3) 3 else 0
    val c = remember { GameController(level, dict, progress, sounds, haptics, scope, bonus, startBoosters) }
    var intro by remember { mutableStateOf(introFor(level, progress)) }
    var paused by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (intro == null) c.begin() }
    LaunchedEffect(Unit) { if (bonus > 0) c.toast("+3 bonus moves to help you!", Offset(400f, 300f), Palette.green, big = true) }

    // 3D layer: on top of the game; the boss peeks over the board; hidden behind cards and dialogs.
    LaunchedEffect(Fx.ok) {
        Fx.backdrop(null)
        if (Fx.ok) { kotlinx.coroutines.delay(500); c.fxLayout(); c.fxBoss() }
    }
    DisposableEffect(Unit) { onDispose { Fx.call("clear") } }
    val covered = paused || more || intro != null || c.showLose || c.mysteryCards != null || c.buyAsk != null || c.letterPick != null
    LaunchedEffect(covered) { Fx.call("bossHold", Fx.obj("on" to covered)) }

    // Back button in a level opens the pause menu (Resume, Restart, Level Map, Home).
    androidx.activity.compose.BackHandler { if (!c.showWin && !c.showLose) paused = !paused }

    val shake = remember { Animatable(0f) }
    LaunchedEffect(c.shakeKey) {
        if (c.shakeKey > 0) for (i in 0 until 8) { shake.animateTo(if (i % 2 == 0) 14f - i else -14f + i, tween(35)) }
        shake.animateTo(0f, tween(40))
    }

    LivingBackground(level.number) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            with(density) { c.screenWidth = maxWidth.toPx(); c.screenHeight = maxHeight.toPx() }
            val sideW = maxWidth * 0.25f
            val rightW = maxWidth * 0.2f
            val stripH = 54.dp
            // The 3D boss needs room above the board; the flat boss bar is smaller.
            val bossH = if (!level.boss) 0.dp else if (Fx.ok) maxHeight * 0.24f else 56.dp
            val boardW = maxWidth - sideW - rightW - 44.dp
            val boardH = maxHeight - stripH - bossH - 34.dp
            // A smaller board than Build 1, so the panels and text can be bigger.
            val cell = min(boardW / level.cols, boardH / level.rows) * 0.94f
            Row(
                Modifier.fillMaxSize().offset { IntOffset(shake.value.roundToInt(), 0) }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LeftPanel(c, Modifier.width(sideW).fillMaxHeight(), onPause = { paused = true })
                Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (level.boss && !Fx.ok) BossBar(c, Modifier.height(bossH))
                    if (level.boss && Fx.ok) Spacer(Modifier.height(bossH))
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { BoardView(c, cell) }
                    Box(Modifier.height(stripH).clickable(remember { MutableInteractionSource() }, null) { c.dismissTutorial() }, contentAlignment = Alignment.Center) {
                        TikoStrip(c.stripText, highlight = c.tutorial != null || c.message != null)
                    }
                }
                RightPanel(c, Modifier.width(rightW).fillMaxHeight(), onMore = { more = true })
            }
            ParticleLayer(c.particles)
            Overlays(c)
            c.letterPick?.let { LetterPicker(it, onPick = { ch -> c.pickLetter(ch) }, onCancel = { c.cancelAim() }) }
            c.buyAsk?.let { BuyDialog(it, progress, onBuy = { c.buy(it) }, onClose = { c.closeBuy() }) }
            if (more) MoreBoosters(c, onClose = { more = false })
            c.mysteryCards?.let { MysteryCards(it, sounds, onPicked = { b -> c.mysteryPicked(b) }, onDone = { c.mysteryDone() }) }
            if (c.showWin) WinDialog(
                c, activity,
                onNext = { Ads.maybeInterstitial(activity, level.number) { onNext(level.number + 1) } },
                onTown = { Ads.maybeInterstitial(activity, level.number) { onTown() } },
            )
            if (c.showLose) LoseDialog(c, activity, onRetry = { c.giveUp(); onRestart() }, onMap = { c.giveUp(); onMap() })
            if (paused) PauseDialog(progress, c, activity, onResume = { paused = false }, onRestart = onRestart, onMap = onMap, onHome = onTown)
            intro?.let { card -> IntroCard(card, onGo = { intro = null; c.begin() }) }
        }
    }
}

// ---------------------------------------------------------------- boss

/** The world's villain at the top of a boss level. Its health bar goes down as the goals fill up. */
@Composable
private fun BossBar(c: GameController, modifier: Modifier) {
    val world = worldOf(c.level.number)
    val anim = rememberInfiniteTransition(label = "boss")
    val float by anim.animateFloat(-4f, 4f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "float")
    val hit = remember { Animatable(0f) }
    LaunchedEffect(c.bossHitKey) { if (c.bossHitKey > 0) { hit.snapTo(1f); hit.animateTo(0f, tween(500)) } }
    val attack = remember { Animatable(1f) }
    LaunchedEffect(c.bossAttackKey) { if (c.bossAttackKey > 0) { attack.snapTo(1.6f); attack.animateTo(1f, spring(dampingRatio = 0.3f)) } }
    val health by animateFloatAsState(1f - c.goalProgress, tween(600), label = "health")
    Row(modifier.fillMaxWidth().arcadePanel().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            world.villainEmoji, fontSize = 30.sp,
            modifier = Modifier.graphicsLayer {
                translationY = float * density
                translationX = sin(hit.value * 30f) * 10f * hit.value
                scaleX = attack.value; scaleY = attack.value
                alpha = 1f - hit.value * 0.4f
            },
        )
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text("${world.villain} · ${world.bossTitle}", color = Color.White, fontSize = 13.sp, fontWeight = Heavy, maxLines = 1)
            Box(Modifier.fillMaxWidth().height(12.dp).background(Color(0x55000000), RoundedCornerShape(6.dp))) {
                Box(Modifier.fillMaxWidth(health.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(Palette.red, Color(0xFFFF8A65))), RoundedCornerShape(6.dp)))
            }
        }
        Text("Every 4 moves it attacks!", color = Palette.cream, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp).width(90.dp), lineHeight = 12.sp)
    }
}

// ---------------------------------------------------------------- left panel

@Composable
private fun LeftPanel(c: GameController, modifier: Modifier, onPause: () -> Unit) {
    val level = c.level
    val world = worldOf(level.number)
    val anim = rememberInfiniteTransition(label = "left")
    val beat by anim.animateFloat(1f, 1.12f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "beat")
    Column(
        modifier.arcadePanel().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("⏸", 30.dp, onClick = onPause)
            Column(Modifier.padding(start = 6.dp).weight(1f)) {
                ArcadeText("LEVEL ${level.number}", 17.sp, Palette.gold)
                Text("${world.emoji} ${world.name}", fontSize = 10.sp, color = Palette.cream, maxLines = 1)
            }
            // Moves.
            val low = c.movesLeft <= 3
            Box(
                Modifier
                    .size(50.dp)
                    .onGloballyPositioned { c.movesAnchor = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }
                    .scale(if (low) beat else 1f)
                    .background(Brush.radialGradient(listOf(if (low) Palette.red else Palette.green, if (low) Color(0xFF8E1F12) else Palette.greenDeep)), CircleShape)
                    .border(3.dp, Palette.gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ArcadeText("${c.movesLeft}", 20.sp)
                    Text("moves", fontSize = 8.sp, color = Color.White, fontWeight = Heavy, lineHeight = 8.sp)
                }
                if (c.freeMoves > 0) Text("⏳", Modifier.align(Alignment.TopEnd).offset(6.dp, (-4).dp), fontSize = 13.sp)
                if (c.doubleNext) Text("🌟", Modifier.align(Alignment.TopStart).offset((-6).dp, (-4).dp), fontSize = 13.sp)
            }
        }
        // Goals in plain words, with a progress bar.
        for (o in level.targets) GoalRow(o, c)
        // Words found so far.
        Column(Modifier.weight(1f).fillMaxWidth().softCard(0.15f).padding(5.dp)) {
            Text("Words found: ${c.foundWords.size}", fontSize = 11.sp, color = Palette.gold, fontWeight = Heavy)
            Text(
                c.foundWords.reversed().take(12).joinToString("  "),
                fontSize = 11.sp, color = Color.White, lineHeight = 13.sp,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        // Bonus word: 2 letters shown, plus what it gives.
        level.bonusWord?.let { w ->
            val shown = if (c.bonusFound) "$w ✓" else w.mapIndexed { i, ch -> if (i < 2) ch else '_' }.joinToString(" ")
            Text(
                "🎁 Bonus word: $shown\nSpell it for a mystery card!",
                fontSize = 10.sp, lineHeight = 12.sp, color = if (c.bonusFound) Palette.green else Palette.cream, fontWeight = Heavy,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PowerMeter(c.progress.meter)
            Spacer(Modifier.weight(1f))
            val shownCoins by animateIntAsState(c.progress.coins, tween(600), label = "coins")
            Row(
                Modifier
                    .background(Color(0x44000000), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .onGloballyPositioned { c.coinAnchor = it.positionInRoot() + Offset(20f, it.size.height / 2f) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🪙", fontSize = 12.sp)
                ArcadeText(" $shownCoins", 13.sp, Palette.gold)
            }
        }
    }
}

@Composable
private fun GoalRow(o: Objective, c: GameController) {
    var themeOpen by remember { mutableStateOf(false) }
    val theme = (o as? Objective.Theme)?.let { com.retroraja.wordnoid.core.Themes.byName(it.theme) }
    if (themeOpen && theme != null) androidx.compose.ui.window.Popup(onDismissRequest = { themeOpen = false }) {
        Box(Modifier.clickable { themeOpen = false }.background(Color(0xEE1A2E7A), RoundedCornerShape(12.dp)).padding(4.dp)) { ThemeHelp(theme) }
    }
    val done = o.done(c.stats)
    val have = minOf(o.count, o.progress(c.stats))
    val frac by animateFloatAsState(have.toFloat() / o.count.coerceAtLeast(1), tween(500), label = "goal")
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (done) Color(0x6639E07A) else Color(0x33FFFFFF), RoundedCornerShape(10.dp))
            .then(if (theme != null) Modifier.clickable { themeOpen = !themeOpen } else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .onGloballyPositioned { if (o is Objective.Gold) c.goalAnchor = it.positionInRoot() + Offset(20f, it.size.height / 2f) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (o is Objective.Letter) Tile3D(com.retroraja.wordnoid.core.Tile(0, o.letter), 20.dp)
            else if (o is Objective.Rescue) Tiko(Modifier.size(20.dp), color = PINK_TIKO, hat = false, letter = null)
            else if (o is Objective.Gold) Tile3D(com.retroraja.wordnoid.core.Tile(0, 'G', kind = com.retroraja.wordnoid.core.TileKind.GOLD), 20.dp)
            else Text(o.icon, fontSize = 15.sp)
            Text(o.text + if (theme != null) " ⓘ" else "", Modifier.weight(1f).padding(start = 5.dp), fontSize = 11.sp, color = Color.White, fontWeight = Heavy, lineHeight = 12.sp, maxLines = 3)
            if (done) Text("✅", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(7.dp).background(Color(0x55000000), RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(if (done) Palette.green else Palette.gold, RoundedCornerShape(4.dp)))
            }
            Text(" $have/${o.count}", fontSize = 11.sp, color = Palette.gold, fontWeight = Heavy)
        }
    }
}

@Composable
private fun PowerMeter(meter: Int) {
    val anim = rememberInfiniteTransition(label = "meter")
    val shine by anim.animateFloat(0.6f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "shine")
    Column {
        Text("⚡ Power", fontSize = 9.sp, color = Palette.gold, fontWeight = Heavy)
        Row(
            Modifier.background(Color(0x66000000), RoundedCornerShape(6.dp)).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (i in 0 until Game.METER_SIZE) {
                val on = i < meter
                Box(
                    Modifier.size(width = 8.dp, height = 11.dp).background(
                        if (on) Brush.verticalGradient(listOf(Palette.cyan.lighter(0.4f), Palette.cyan.copy(alpha = shine))) else Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color(0x22FFFFFF))),
                        RoundedCornerShape(3.dp),
                    )
                )
            }
        }
    }
}

// ---------------------------------------------------------------- right panel

/**
 * The 6 booster slots: boosters you own first (biggest count first), then the ones this level teaches,
 * then unlocked boosters you don't own yet (they show a coin price: tap to buy and use).
 */
fun panelBoosters(c: GameController): List<Booster> {
    val unlocked = Booster.unlockedAt(c.level.number)
    val intro = c.level.introBoosters.filter { c.progress.hasFreeUse(it) }
    val owned = unlocked.filter { c.progress.count(it) > 0 }.sortedByDescending { c.progress.count(it) }
    val rest = unlocked.filter { it !in owned }.sortedBy { it.price }
    return (intro + owned + rest).distinct().take(6)
}

@Composable
private fun RightPanel(c: GameController, modifier: Modifier, onMore: () -> Unit) {
    Column(
        modifier.arcadePanel().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("Boosters", fontSize = 11.sp, color = Palette.gold, fontWeight = Heavy)
        val list = panelBoosters(c)
        for (row in list.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (b in row) BoosterButton(b, c, 54.dp, showName = true)
        }
        if (Booster.unlockedAt(c.level.number).size > 6) RoundButton("⋯", 32.dp, label = "More", onClick = onMore)
    }
}

@Composable
fun RoundButton(text: String, size: Dp, label: String? = null, onClick: () -> Unit) {
    Column(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .background(Brush.radialGradient(listOf(Palette.gold, Palette.goldDeep)), CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(text, fontSize = (size.value * 0.42f).sp, color = Color(0xFF14307F), fontWeight = Heavy) }
        label?.let { Text(it, fontSize = 9.sp, color = Palette.cream, fontWeight = Heavy) }
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun BoosterButton(b: Booster, c: GameController, size: Dp = 50.dp, showName: Boolean = false, onUsed: () -> Unit = {}) {
    val anim = rememberInfiniteTransition(label = "bb")
    val pulse by anim.animateFloat(1f, 1.15f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "p")
    val spotlight = c.tutorial?.booster == b || c.nudge == b
    val isArmed = c.armed == b
    val count = c.progress.count(b)
    val free = c.progress.hasFreeUse(b)
    val owned = free || count > 0
    val icon = Fx.icons[b.name]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .scale(if (spotlight || isArmed) pulse else 1f)
                .graphicsLayer { translationY = if (isArmed) -8f else 0f }
                .alpha(if (owned) 1f else 0.75f)
                .background(
                    Brush.radialGradient(listOf(if (isArmed) Palette.green else Color(0xFF8C6BFF), if (isArmed) Palette.greenDeep else Color(0xFF3B1F9E))),
                    CircleShape,
                )
                .border(if (spotlight) 4.dp else 2.dp, if (spotlight) Color.White else Palette.gold, CircleShape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onLongClick = { c.boosterInfo(b) },
                ) { c.boosterTapped(b); onUsed() },
            contentAlignment = Alignment.Center,
        ) {
            // The 3D picture of the tool when the phone can draw it, else the emoji.
            if (icon != null) androidx.compose.foundation.Image(icon, b.title, Modifier.fillMaxSize(0.92f))
            else Text(b.emoji, fontSize = (size.value * 0.5f).sp)
        }
        val badge = when {
            free -> "FREE"
            count > 0 -> "$count"
            else -> "🪙${Rewards.price(b, c.progress.unlocked)}"
        }
        Text(
            badge,
            Modifier.align(Alignment.BottomEnd).offset(6.dp, 4.dp)
                .background(if (free) Palette.green else if (count > 0) Palette.red else Color(0xFF2B2F4A), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            fontSize = 11.sp, color = Color.White, fontWeight = Heavy,
        )
    }
    if (showName) Text(b.title, fontSize = 10.sp, color = Palette.cream, fontWeight = Heavy, maxLines = 1, lineHeight = 11.sp)
    }
}

// ---------------------------------------------------------------- intro card

data class IntroInfo(val title: String, val emoji: String, val text: String, val feature: Feature? = null)

fun introFor(level: Level, progress: Progress): IntroInfo? {
    val key = "intro_level_${level.number}"
    if (progress.seen(key)) return null
    progress.markSeen(key)
    return when {
        level.number == 1 -> IntroInfo("How to play", "🔤", "Swap two neighbouring tiles to spell a word. Words count left to right, right to left, up or down. Only words made with the tile you moved count, so think before you swap!")
        level.number == 2 -> IntroInfo("Long words give moves", "📏", "3 and 4 letter words are fine, but 5-letter words give +1 move and 6+ letters give +2 moves. Plan bigger words to go further!")
        level.number == 3 -> {
            progress.primeMeter(Game.METER_SIZE - 1)
            IntroInfo(
                "Power Meter", "⚡",
                "👈 The Power Meter is on the left.\n1. Every word charges it.\n2. When it's full, pick a FREE mystery booster!\n3. Leftover power is saved for the next level.\nIt's almost full now. Make one word!",
            )
        }
        level.feature != null -> IntroInfo(level.feature!!.title, featureEmoji(level.feature!!), level.feature!!.text, level.feature)
        else -> null
    }
}

fun featureEmoji(f: Feature): String = when (f) {
    Feature.GOLD -> "🟨"
    Feature.CRATES -> "🎁"
    Feature.LETTERS -> "🔠"
    Feature.BONUS_WORD -> "🎁"
    Feature.PRE_BOOSTERS -> "🚀"
    Feature.GIFTS -> "🎀"
    Feature.THEME -> "🐾"
    Feature.LONG -> "📏"
    Feature.DOUBLE_CRATES -> "🧰"
    Feature.ICE -> "🧊"
    Feature.SPELL -> "✍️"
    Feature.CHAINS -> "⛓️"
    Feature.BOSS -> "💥"
    Feature.RESCUE -> "💖"
    Feature.BALLOONS -> "🎈"
    Feature.TREASURE -> "💎"
}

@Composable
fun IntroCard(info: IntroInfo, onGo: () -> Unit) {
    DimBox {
        Column(
            Modifier.width(460.dp).arcadePanel().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pop = remember { Animatable(0.3f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.4f)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tiko(Modifier.size(64.dp))
                // The Tiko friends are little pink Tikos.
                if (info.feature == Feature.RESCUE) Tiko(Modifier.size(48.dp).scale(pop.value).padding(start = 8.dp), color = PINK_TIKO, hat = false)
                else Text(info.emoji, fontSize = 40.sp, modifier = Modifier.scale(pop.value).padding(start = 8.dp))
            }
            ArcadeText(info.title, 26.sp, Palette.gold)
            // Tiko's quick demo of a new goal.
            info.feature?.let { FeatureDemo(it) }
            Spacer(Modifier.height(6.dp))
            Text(info.text, color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(Modifier.height(12.dp))
            ArcadeButton("Let's go!", onClick = onGo)
        }
    }
}

/** A tiny looping animation that shows how a new goal works. */
@Composable
private fun FeatureDemo(f: Feature) {
    val anim = rememberInfiniteTransition(label = "demo")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "t")
    val tile = { ch: Char -> com.retroraja.wordnoid.core.Tile(ch.code.toLong(), ch) }
    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        when (f) {
            Feature.BALLOONS -> {
                for (ch in "CAT") Tile3D(tile(ch), 34.dp, glow = if (t > 0.4f) Palette.gold else null)
                Text(if (t < 0.6f) "🎈" else "💥", fontSize = 28.sp, modifier = Modifier.graphicsLayer { translationY = -10f * sin(t * 6f) })
            }
            Feature.RESCUE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Tiko(Modifier.size(34.dp).graphicsLayer { translationY = 40f * t }, color = PINK_TIKO, hat = false)
                    Tile3D(tile('S'), 30.dp, Modifier.alpha(1f - t))
                }
                Text("➡ 🏁", fontSize = 22.sp)
            }
            Feature.TREASURE -> {
                Box(Modifier.size(70.dp, 36.dp).background(Color(0xFFF2D68A), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text("💎", fontSize = 22.sp, modifier = Modifier.alpha(t))
                }
                Text("Clear the tiles on the sand", fontSize = 12.sp, color = Color.White)
            }
            Feature.CRATES, Feature.DOUBLE_CRATES -> {
                for (ch in "SUN") Tile3D(tile(ch), 30.dp, glow = if (t > 0.4f) Palette.gold else null)
                Text(if (t < 0.6f) "🎁" else "✨🪙", fontSize = 24.sp)
            }
            Feature.ICE -> Text("🧊 ➡ 💧", fontSize = 26.sp)
            Feature.CHAINS -> Text("⛓️ ➡ 🔓", fontSize = 26.sp)
            else -> {}
        }
    }
}

/** A dark see-through layer that blocks taps behind a dialog. */
@Composable
fun DimBox(alpha: Float = 0.55f, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0xFF0B1440).copy(alpha = alpha)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun MessageBar(text: String) {
    Box(Modifier.fillMaxSize().padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            text,
            Modifier.background(Color(0xEE1A3A9C), RoundedCornerShape(14.dp)).border(2.dp, Palette.gold, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 6.dp),
            color = Color.White, fontSize = 15.sp, fontWeight = Heavy, style = TextStyle(shadow = OutlineShadow),
        )
    }
}
