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
    onAutoTown: (coins: Int, bricks: Int) -> Unit = { _, _ -> onTown() },
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity
    val bonus = if (progress.fails(level.number) >= 3) 3 else 0
    val c = remember { GameController(level, dict, progress, sounds, haptics, scope, bonus, startBoosters) }
    remember(level.number) { Look.forLevel(level.number); 0 }
    // Cards before the level starts: the intro(s), then the new booster's help card.
    val intros = remember { mutableStateListOf<IntroInfo>().apply { addAll(introsFor(level, progress)) } }
    val intro = intros.firstOrNull()
    var help by remember { mutableStateOf(helpFor(level, progress)) }
    var paused by remember { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (intro == null && help == null) c.begin() }
    LaunchedEffect(Unit) { if (bonus > 0) c.toast("+3 bonus moves to help you!", Offset(400f, 300f), Palette.green, big = true) }

    // 3D layer: on top of the game; the boss peeks over the board; hidden behind cards and dialogs.
    LaunchedEffect(Fx.ok) {
        Fx.backdrop(null)
        if (Fx.ok) { kotlinx.coroutines.delay(500); c.fxLayout(); c.fxBoss() }
    }
    DisposableEffect(Unit) { onDispose { Fx.call("clear") } }
    val covered = paused || more || intro != null || help != null || c.showLose || c.mysteryCards != null || c.buyAsk != null || c.letterPick != null
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
                // Level 1 has no boosters yet: no panel.
                if (panelBoosters(c).isEmpty()) Spacer(Modifier.width(rightW))
                else RightPanel(c, covered = covered || c.showWin || c.bigWin || c.showLose, modifier = Modifier.width(rightW).fillMaxHeight().onGloballyPositioned { c.panelAnchor = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }, onMore = { more = true })
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
            if (c.bigWin) BigWin(c, sounds, onDone = { if (c.autoTown) onAutoTown(c.coinsThisLevel, c.winStars) else c.bigWinDone() })
            if (intro != null) IntroCard(intro, onGo = { intros.removeAt(0); if (intros.isEmpty() && help == null) c.begin() })
            else help?.let { b -> BoosterHelpCard(b, onTry = { help = null; c.begin() }) }
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
        modifier.skinPanel().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("⏸", 30.dp, onClick = onPause)
            Column(Modifier.padding(start = 6.dp).weight(1f)) {
                Text("LEVEL ${level.number}", fontFamily = ArcadeFont, fontSize = 19.sp, color = Look.skin.ink)
                Text("${world.emoji} ${world.name}", fontSize = 10.sp, color = Look.skin.ink.copy(alpha = 0.75f), maxLines = 1, fontWeight = Heavy)
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
            Text("Words found: ${c.foundWords.size}", fontSize = 11.sp, color = Look.skin.accent, fontWeight = Heavy)
            Text(
                c.foundWords.reversed().take(12).joinToString("  "),
                fontSize = 11.sp, color = Look.skin.ink, fontWeight = Heavy, lineHeight = 13.sp,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        // Bonus word: 2 letters shown, plus what it gives.
        level.bonusWord?.let { w ->
            val shown = if (c.bonusFound) "$w ✓" else w.mapIndexed { i, ch -> if (i < 2) ch else '_' }.joinToString(" ")
            Text(
                "🎁 Bonus word: $shown\nSpell it for a mystery card!",
                fontSize = 10.sp, lineHeight = 12.sp, color = if (c.bonusFound) Palette.greenDeep else Look.skin.ink, fontWeight = Heavy,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The Power Meter shows up at Level 5.
            if (level.number >= GameController.METER_LEVEL) PowerMeter(c.progress.meter)
            Spacer(Modifier.weight(1f))
            val shownCoins by animateIntAsState(c.progress.coins, tween(600), label = "coins")
            Row(
                Modifier
                    .background(Color.White.copy(alpha = if (Look.skin.light) 0.8f else 0.15f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .onGloballyPositioned { c.coinAnchor = it.positionInRoot() + Offset(20f, it.size.height / 2f) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🪙", fontSize = 12.sp)
                Text(" $shownCoins", fontFamily = ArcadeFont, fontSize = 14.sp, color = Look.skin.ink)
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
            .background(if (done) Color(0x6639E07A) else Color.White.copy(alpha = if (Look.skin.light) 0.85f else 0.15f), RoundedCornerShape(12.dp))
            .then(if (theme != null) Modifier.clickable { themeOpen = !themeOpen } else Modifier)
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .onGloballyPositioned { if (o is Objective.Gold) c.goalAnchor = it.positionInRoot() + Offset(20f, it.size.height / 2f) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (o is Objective.Letter) Tile3D(com.retroraja.wordnoid.core.Tile(0, o.letter), 20.dp)
            else if (o is Objective.Rescue) Tiko(Modifier.size(20.dp), color = PINK_TIKO, hat = false, letter = null)
            else if (o is Objective.Gold) Tile3D(com.retroraja.wordnoid.core.Tile(0, 'G', kind = com.retroraja.wordnoid.core.TileKind.GOLD), 20.dp)
            else Text(o.icon, fontSize = 15.sp)
            Text(o.text + if (theme != null) " ⓘ" else "", Modifier.weight(1f).padding(start = 5.dp), fontSize = 11.sp, color = Look.skin.ink, fontWeight = Heavy, lineHeight = 12.sp, maxLines = 3)
            if (done) Text("✅", fontSize = 13.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(7.dp).background(Look.skin.ink.copy(alpha = 0.18f), RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(if (done) Palette.green else Look.skin.accent, RoundedCornerShape(4.dp)))
            }
            Text(" $have/${o.count}", fontSize = 11.sp, color = Look.skin.ink, fontWeight = Heavy)
        }
    }
}

@Composable
private fun PowerMeter(meter: Int) {
    val anim = rememberInfiniteTransition(label = "meter")
    val shine by anim.animateFloat(0.6f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "shine")
    Column {
        Text("⚡ Power", fontSize = 9.sp, color = Look.skin.ink, fontWeight = Heavy)
        Row(
            Modifier.background(Look.skin.ink.copy(alpha = 0.55f), RoundedCornerShape(6.dp)).padding(2.dp),
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
private fun RightPanel(c: GameController, modifier: Modifier, covered: Boolean, onMore: () -> Unit) {
    val list = panelBoosters(c)
    // The tools are drawn live in 3D over the buttons: they turn and bob, jump and spin on a tap.
    val spots = remember { mutableStateMapOf<Booster, Pair<Offset, Float>>() }
    val live = Fx.ok
    val keys = list.joinToString { it.name }
    LaunchedEffect(live, keys, spots.toMap(), covered) {
        if (!live) return@LaunchedEffect
        if (covered) { Fx.call("panelHold", Fx.obj("on" to true)); return@LaunchedEffect }
        kotlinx.coroutines.delay(120)
        val slots = org.json.JSONArray()
        for (b in list) spots[b]?.let { (at, size) ->
            slots.put(Fx.obj("k" to b.name, "x" to Fx.dp(at.x), "y" to Fx.dp(at.y), "s" to Fx.dp(size)))
        }
        if (slots.length() == list.size) { Fx.call("panelShow", Fx.obj("slots" to slots)); Fx.call("panelHold", Fx.obj("on" to false)) }
    }
    LaunchedEffect(c.armed) { if (live) Fx.call("panelArm", Fx.obj("k" to (c.armed?.name ?: ""))) }
    DisposableEffect(Unit) { onDispose { Fx.call("panelHide") } }
    Column(
        modifier.skinPanel().padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("Boosters", fontSize = 12.sp, color = Look.skin.ink, fontWeight = Heavy)
        for (row in list.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (b in row) BoosterButton(b, c, 56.dp, showName = true, live = live, onPlaced = { at, size -> spots[b] = at to size })
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
fun BoosterButton(
    b: Booster, c: GameController, size: Dp = 50.dp, showName: Boolean = false,
    /** True when the 3D layer draws the tool live over this button (the right panel). */
    live: Boolean = false,
    onPlaced: ((Offset, Float) -> Unit)? = null,
    onUsed: () -> Unit = {},
) {
    val anim = rememberInfiniteTransition(label = "bb")
    val pulse by anim.animateFloat(1f, 1.12f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "p")
    val glow by anim.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "g")
    val spotlight = c.tutorial?.booster == b || c.nudge == b || c.glowBooster == b
    // The booster tutorial: a hand taps this button.
    val handHere = c.tutorial?.booster == b && c.armed == null && c.tutorial?.dismissible == false
    val isArmed = c.armed == b
    val count = c.progress.count(b)
    val free = c.progress.hasFreeUse(b)
    val owned = free || count > 0
    val icon = Fx.icons[b.name]
    val k = Look.skin
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(contentAlignment = Alignment.Center) {
        // Ready glow: a soft ring behind the button when you own it.
        if (owned) Box(Modifier.size(size + 10.dp).alpha(glow * (if (spotlight || isArmed) 1f else 0.55f))
            .background(Brush.radialGradient(listOf((if (isArmed) Palette.green else Palette.gold).copy(alpha = 0.85f), Color.Transparent)), CircleShape))
        // A chunky 3D button: a darker lip underneath, a glossy dome on top.
        Box(Modifier.size(size).offset(y = 4.dp).background(if (isArmed) Palette.greenDeep.darker(0.7f) else k.strongDeep.darker(0.75f), CircleShape))
        Box(
            Modifier
                .size(size)
                .scale(if (spotlight || isArmed) pulse else 1f)
                .graphicsLayer { translationY = if (isArmed) -6f * density else 0f }
                .onGloballyPositioned { co -> onPlaced?.invoke(co.positionInRoot() + Offset(co.size.width / 2f, co.size.height / 2f), co.size.width.toFloat()) }
                .alpha(if (owned) 1f else 0.8f)
                .background(
                    Brush.radialGradient(
                        listOf(if (isArmed) Palette.green.lighter(0.3f) else k.strong.lighter(0.45f), if (isArmed) Palette.greenDeep else k.strongDeep)
                    ),
                    CircleShape,
                )
                .border(if (spotlight) 4.dp else 3.dp, if (spotlight) Palette.gold else Color.White, CircleShape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onLongClick = { c.boosterInfo(b) },
                ) {
                    if (live) Fx.call("panelTap", Fx.obj("k" to b.name))
                    c.boosterTapped(b); onUsed()
                },
            contentAlignment = Alignment.Center,
        ) {
            // A glossy highlight on the dome.
            Box(Modifier.align(Alignment.TopCenter).padding(top = size * 0.08f).size(size * 0.55f, size * 0.22f).background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(50)))
            // Live 3D tool on top (drawn by the 3D layer); otherwise its 3D picture, else the emoji.
            if (!live) {
                if (icon != null) androidx.compose.foundation.Image(icon, b.title, Modifier.fillMaxSize(0.95f))
                else Text(b.emoji, fontSize = (size.value * 0.5f).sp)
            }
        }
        val badge = when {
            free -> "FREE"
            count > 0 -> "$count"
            else -> "🪙${Rewards.price(b, c.progress.unlocked)}"
        }
        if (handHere) {
            val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "hand")
            Text("👆", Modifier.align(Alignment.BottomCenter).offset(y = (22 + 8 * t).dp), fontSize = 30.sp)
        }
        // A 3D gold count badge (green when free, dark when you'd buy it).
        val (b1, b2) = when { free -> Palette.green to Palette.greenDeep; count > 0 -> Palette.gold to Palette.goldDeep; else -> Color(0xFF4A4F72) to Color(0xFF23263F) }
        Box(
            Modifier.align(Alignment.BottomEnd).offset(8.dp, 6.dp)
                .background(Brush.verticalGradient(listOf(b1.lighter(0.35f), b1, b2)), RoundedCornerShape(10.dp))
                .border(2.dp, Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) { Text(badge, fontFamily = ArcadeFont, fontSize = 13.sp, color = if (count > 0 && !free) Color(0xFF5A2E00) else Color.White) }
    }
    if (showName) Text(b.title, fontSize = 10.sp, color = if (live) Look.skin.ink else Palette.cream, fontWeight = Heavy, maxLines = 1, lineHeight = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------------------------------------------------------------- intro card

data class IntroInfo(val title: String, val emoji: String, val text: String, val feature: Feature? = null)

/** The cards shown before a level, once each: how to play, new goals, the Power Meter at Level 5. */
fun introsFor(level: Level, progress: Progress): List<IntroInfo> {
    val key = "intro_level_${level.number}"
    if (progress.seen(key)) return emptyList()
    progress.markSeen(key)
    val out = ArrayList<IntroInfo>()
    when (level.number) {
        1 -> out += IntroInfo("How to play", "🔤", "Swap two neighbouring tiles to spell a word. Words count left to right, right to left, up or down. Only words made with the tile you moved count, so think before you swap!")
        2 -> out += IntroInfo("Long words give moves", "📏", "3 and 4 letter words are fine, but 5-letter words give +1 move and 6+ letters give +2 moves. Plan bigger words to go further!")
        GameController.METER_LEVEL -> {
            progress.primeMeter(Game.METER_SIZE - 1)
            out += IntroInfo(
                "Power Meter", "⚡",
                "👈 The Power Meter is on the left.\n1. Every word charges it.\n2. When it's full, pick a FREE mystery booster!\n3. Leftover power is saved for the next level.\nIt's almost full now. Make one word!",
            )
        }
    }
    level.feature?.let { out += IntroInfo(it.title, featureEmoji(it), it.text, it) }
    return out
}

/** The new booster this level teaches, if the player hasn't had its free try yet. */
fun helpFor(level: Level, progress: Progress): Booster? =
    level.introBoosters.firstOrNull()?.takeIf { !progress.seen("intro_b_${it.name}") }

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

/** New booster: what it does, a 3D demo on a tiny board, then "Try it!" (a hand then shows where to tap). */
@Composable
fun BoosterHelpCard(b: Booster, onTry: () -> Unit) {
    val demo3d = Fx.ok && b in GameController.GUIDED
    DisposableEffect(Unit) { onDispose { Fx.call("demoClear") } }
    DimBox(0.6f) {
        Column(Modifier.width(470.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tiko(Modifier.size(52.dp))
                Column(Modifier.padding(start = 8.dp)) {
                    ArcadeText("NEW BOOSTER!", 15.sp, Palette.cyan)
                    ArcadeText("${b.title}", 26.sp, Palette.gold)
                }
                Spacer(Modifier.width(10.dp))
                val icon = Fx.icons[b.name]
                if (icon != null) androidx.compose.foundation.Image(icon, b.title, Modifier.size(64.dp)) else Text(b.emoji, fontSize = 44.sp)
            }
            Text(b.description + ".", color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 19.sp)
            if (demo3d) {
                // The 3D layer plays the demo over this box, again and again.
                var sent by remember { mutableStateOf(false) }
                Box(Modifier.padding(vertical = 6.dp).size(300.dp, 110.dp).background(Color(0x33000000), RoundedCornerShape(14.dp)).onGloballyPositioned { co ->
                    if (!sent) {
                        sent = true
                        val p = co.positionInRoot()
                        Fx.call("demo", Fx.obj("k" to b.name, "x" to Fx.dp(p.x), "y" to Fx.dp(p.y), "w" to Fx.dp(co.size.width.toFloat()), "h" to Fx.dp(co.size.height.toFloat())))
                    }
                })
            } else Spacer(Modifier.height(8.dp))
            Text("Your first one is FREE!", color = Palette.green, fontSize = 14.sp, fontWeight = Heavy)
            Spacer(Modifier.height(8.dp))
            ArcadeButton("Try it! 👆", onClick = onTry)
        }
    }
}

/** Every win: a big 3D "WORDNOID!" with Tiko's voice. Levels 1-3 then go to town, later levels show the win screen. */
@Composable
private fun BigWin(c: GameController, sounds: SoundFx, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        sounds.play("fanfare")
        sounds.play("wordnoid")
        if (Fx.ok) {
            Fx.call("bigWin", Fx.obj("text" to "WORDNOID!"))
            Fx.await("bigWinDone", 6000)
        } else kotlinx.coroutines.delay(2600)
        onDone()
    }
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {}, contentAlignment = Alignment.Center) {
        if (!Fx.ok) {
            val pop = remember { Animatable(0.2f) }
            LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.35f)) }
            ArcadeText("WORDNOID!", 72.sp, Palette.gold, modifier = Modifier.scale(pop.value))
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
