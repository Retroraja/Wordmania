package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Level
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.core.Tile
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.Rewards
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

// ---------------------------------------------------------------- home

@Composable
fun HomeScreen(
    ready: Boolean,
    progress: Progress,
    sounds: SoundFx,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onWordbook: () -> Unit,
) {
    var daily by remember { mutableStateOf(if (progress.dailyGiftDay() > 0) progress.dailyGiftDay() else 0) }
    var settings by remember { mutableStateOf(false) }
    val particles = remember { ParticleField() }
    val anim = rememberInfiniteTransition(label = "home")
    val bounce by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "bounce")
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = with(LocalDensity.current) { maxWidth.toPx() }
            val h = with(LocalDensity.current) { maxHeight.toPx() }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(2600)
                    particles.firework(Offset(w * (0.15f + Random.nextFloat() * 0.7f), h * (0.1f + Random.nextFloat() * 0.25f)))
                }
            }
            ParticleLayer(particles)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                // 3D logo: tiles drop in, then bob gently in a wave.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    "WORDNOID".forEachIndexed { i, ch ->
                        val drop = remember { Animatable(-400f) }
                        LaunchedEffect(Unit) { delay(i * 90L); drop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 300f)); sounds.play("drop") }
                        val wave = kotlin.math.sin((bounce * 3.14f) + i * 0.7f) * 8f
                        Tile3D(
                            Tile(i.toLong(), ch), 62.dp,
                            Modifier.graphicsLayer { translationY = drop.value + wave; rotationZ = if (i % 2 == 0) -4f else 4f; rotationX = 12f },
                        )
                    }
                }
                Text("Swap · Spell · Smash!", color = Palette.cream, fontSize = 18.sp, fontWeight = Heavy, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(18.dp))
                ArcadeButton(if (ready) "▶  PLAY" else "Loading words…", enabled = ready, textSize = 28.sp, modifier = Modifier.scale(1f + 0.04f * bounce)) {
                    sounds.play("pop"); onPlay()
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeButton("🛒 Shop", color = Palette.purple, textSize = 16.sp, onClick = onShop)
                    ArcadeButton("📖 Wordbook ${progress.wordbookSize}", color = Color(0xFF1B6FA8), textSize = 16.sp, onClick = onWordbook)
                    ArcadeButton("⚙️", color = Color(0xFF6B3A08), textSize = 16.sp) { settings = true }
                }
            }
            CoinBadge(progress, Modifier.align(Alignment.TopEnd).padding(12.dp))
            if (daily > 0) DailyGiftDialog(daily, progress, sounds, particles) { daily = 0 }
            if (settings) DimBox {
                Column(Modifier.arcadePanel().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ArcadeText("Settings", 24.sp, Palette.gold)
                    Spacer(Modifier.height(8.dp))
                    SettingsToggles(progress)
                    Spacer(Modifier.height(10.dp))
                    ArcadeButton("Done") { settings = false }
                }
            }
        }
    }
}

@Composable
fun CoinBadge(progress: Progress, modifier: Modifier = Modifier) {
    Row(
        modifier.background(Color(0xAA1B0E05), RoundedCornerShape(14.dp)).border(2.dp, Palette.gold, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🪙", fontSize = 18.sp)
        ArcadeText(" ${progress.coins}", 18.sp, Palette.gold)
    }
}

@Composable
fun DailyGiftDialog(day: Int, progress: Progress, sounds: SoundFx, particles: ParticleField, onClose: () -> Unit) {
    var opened by remember { mutableStateOf(false) }
    val gift = Rewards.DAILY[day - 1]
    val boosters = remember { mutableStateListOf<Booster>() }
    DimBox {
        Column(Modifier.width(520.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeText("🎁 Daily gift!", 26.sp, Palette.gold)
            Text("Come back every day for a bigger gift. Day 7 is the best!", color = Palette.cream, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Rewards.DAILY.forEachIndexed { i, g ->
                    val today = i + 1 == day
                    Column(
                        Modifier
                            .width(58.dp)
                            .background(if (today) Palette.greenDeep else if (i + 1 < day) Color(0xFF555555) else Color(0x66000000), RoundedCornerShape(10.dp))
                            .border(if (today) 3.dp else 1.dp, Palette.gold, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Day ${i + 1}", fontSize = 9.sp, color = Color.White, fontWeight = Heavy)
                        Text(if (i == 6) "🧰" else "🪙", fontSize = 18.sp)
                        Text("${g.coins}" + if (g.boosters > 0) "+🎁" else "", fontSize = 10.sp, color = Palette.gold, fontWeight = Heavy)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!opened) ArcadeButton("Open!") {
                opened = true
                progress.claimDaily()
                progress.addCoins(gift.coins)
                repeat(gift.boosters) { val b = Rewards.randomBooster(progress.unlocked); progress.addBooster(b); boosters += b }
                sounds.play("fanfare"); sounds.play("cash")
                particles.confettiRain(1600f, 60)
            } else {
                Text("+${gift.coins} 🪙 " + boosters.joinToString(" ") { "${it.emoji} ${it.title}" }, color = Palette.gold, fontSize = 18.sp, fontWeight = Heavy)
                Spacer(Modifier.height(8.dp))
                ArcadeButton("Great!", onClick = onClose)
            }
        }
    }
}

// ---------------------------------------------------------------- map

@Composable
fun MapScreen(progress: Progress, sounds: SoundFx, onBack: () -> Unit, onLevel: (Int) -> Unit, onShop: () -> Unit) {
    val total = Levels.all.size
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    var chest by remember { mutableStateOf<List<Booster>?>(null) }
    val anim = rememberInfiniteTransition(label = "map")
    val pulse by anim.animateFloat(1f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    val nodeGap = 96.dp
    LaunchedEffect(Unit) {
        val target = with(density) { (nodeGap * (progress.unlocked - 3).coerceAtLeast(0)).toPx() }.roundToInt()
        scroll.animateScrollTo(target, tween(900))
    }
    LivingBackground(progress.unlocked.coerceAtMost(total)) {
        Box(Modifier.fillMaxSize().horizontalScroll(scroll)) {
            val width = nodeGap * (total + 2)
            Box(Modifier.width(width).fillMaxHeight()) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val hPx = with(density) { maxHeight.toPx() }
                    val gapPx = with(density) { nodeGap.toPx() }
                    fun nodeAt(i: Int) = Offset(gapPx * (i + 1), hPx * (0.42f + 0.2f * kotlin.math.sin(i * 0.9f)))
                    // Chapter bands and the dashed path.
                    Canvas(Modifier.fillMaxSize()) {
                        for (ch in 0 until (total + 9) / 10) {
                            val (top, _) = chapterColors(ch * 10 + 1)
                            drawRect(top.copy(alpha = 0.18f), Offset(gapPx * (ch * 10 + 0.5f), 0f), androidx.compose.ui.geometry.Size(gapPx * 10, size.height))
                        }
                        for (i in 0 until total - 1) {
                            drawLine(
                                if (i + 1 < progress.unlocked) Palette.gold else Color(0x88FFFFFF), nodeAt(i), nodeAt(i + 1), strokeWidth = 8f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)),
                            )
                        }
                    }
                    for (ch in 0 until (total + 9) / 10) {
                        Text(
                            "Chapter ${ch + 1}: ${CHAPTER_NAMES.getOrElse(ch) { "" }}",
                            Modifier.offset { IntOffset((gapPx * (ch * 10 + 0.8f)).roundToInt(), 16) },
                            color = Color.White, fontSize = 15.sp, fontWeight = Heavy,
                        )
                    }
                    for (i in 0 until total) {
                        val n = i + 1
                        val lv = Levels.get(n)!!
                        val open = n <= progress.unlocked
                        val current = n == progress.unlocked
                        val c = nodeAt(i)
                        val size = if (lv.boss) 70.dp else 56.dp
                        val sizePx = with(density) { size.toPx() }
                        Column(
                            Modifier.offset { IntOffset((c.x - sizePx / 2).roundToInt(), (c.y - sizePx / 2).roundToInt()) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .size(size)
                                    .scale(if (current) pulse else 1f)
                                    .background(
                                        Brush.radialGradient(
                                            if (!open) listOf(Color(0xFF777777), Color(0xFF333333))
                                            else if (lv.boss) listOf(Palette.red.lighter(0.3f), Color(0xFF5A0F0F))
                                            else listOf(Palette.green.lighter(0.2f), Palette.greenDeep),
                                        ),
                                        CircleShape,
                                    )
                                    .border(3.dp, if (current) Color.White else Palette.gold, CircleShape)
                                    .clickable(enabled = open) { sounds.play("pop"); onLevel(n) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (open) ArcadeText("$n", if (lv.boss) 24.sp else 20.sp) else Text("🔒", fontSize = 20.sp)
                                if (lv.boss) Text("💀", Modifier.align(Alignment.TopEnd).offset(6.dp, (-6).dp), fontSize = 16.sp)
                                lv.introBoosters.firstOrNull()?.let { Text(it.emoji, Modifier.align(Alignment.BottomStart).offset((-8).dp, 6.dp), fontSize = 14.sp) }
                            }
                            val stars = progress.stars(n)
                            Text(if (open && stars > 0) "⭐".repeat(stars) else " ", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        // Top bar.
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ArcadeButton("◀", color = Color(0xFF6B3A08), textSize = 18.sp, onClick = onBack)
            Spacer(Modifier.width(10.dp))
            val ready = progress.chestReady
            Row(
                Modifier.background(Color(0xAA1B0E05), RoundedCornerShape(14.dp)).border(2.dp, if (ready) Palette.green else Palette.gold, RoundedCornerShape(14.dp))
                    .clickable(enabled = ready) {
                        progress.openChest()
                        progress.addCoins(50)
                        val got = List(2) { Rewards.randomBooster(progress.unlocked) }
                        got.forEach { progress.addBooster(it) }
                        sounds.play("fanfare")
                        chest = got
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🧰", fontSize = 18.sp, modifier = Modifier.scale(if (ready) pulse else 1f))
                Text(if (ready) " Star chest ready! Tap!" else " ⭐ ${progress.starsToNextChest()} to next chest", color = Color.White, fontSize = 13.sp, fontWeight = Heavy)
            }
            Spacer(Modifier.weight(1f))
            ArcadeButton("🛒", color = Palette.purple, textSize = 16.sp, onClick = onShop)
            Spacer(Modifier.width(8.dp))
            CoinBadge(progress)
        }
        chest?.let { got ->
            DimBox {
                Column(Modifier.arcadePanel().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ArcadeText("🧰 Star chest!", 26.sp, Palette.gold)
                    Text("+50 🪙   " + got.joinToString("   ") { "${it.emoji} ${it.title}" }, color = Palette.cream, fontSize = 16.sp, fontWeight = Heavy)
                    Spacer(Modifier.height(10.dp))
                    ArcadeButton("Great!") { chest = null }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- level start card

@Composable
fun LevelStartCard(level: Level, progress: Progress, sounds: SoundFx, onPlay: (List<Booster>) -> Unit, onClose: () -> Unit) {
    val options = listOf(Booster.PLUS_MOVES, Booster.JOKER, Booster.FREE_MOVE).filter { it.introLevel <= maxOf(level.number, 8) }
    val chosen = remember { mutableStateListOf<Booster>() }
    val canPick = level.number >= 8
    val firstFree = canPick && !progress.seen("prelevel_free")
    DimBox {
        Column(Modifier.width(500.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeText("Level ${level.number}", 28.sp, Palette.gold)
                if (level.boss) Text("  💀 BOSS", color = Palette.red, fontSize = 16.sp, fontWeight = Heavy)
            }
            Text("${level.moves} moves", color = Palette.cream, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (o in level.targets) Column(
                    Modifier.background(Color(0x55000000), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(o.icon, fontSize = 22.sp)
                    Text("${o.count} ${o.label}", color = Color.White, fontSize = 12.sp, fontWeight = Heavy, textAlign = TextAlign.Center)
                }
            }
            if (canPick) {
                Spacer(Modifier.height(10.dp))
                Text(if (firstFree) "NEW! Start with a booster. Your first one is FREE!" else "Start with a booster?", color = Palette.green, fontSize = 14.sp, fontWeight = Heavy)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    for (b in options) {
                        val on = b in chosen
                        val owned = progress.count(b)
                        val price = Rewards.price(b, progress.unlocked)
                        Column(
                            Modifier
                                .background(if (on) Palette.greenDeep else Color(0x55000000), RoundedCornerShape(12.dp))
                                .border(if (on) 3.dp else 1.dp, Palette.gold, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (on) chosen.remove(b)
                                    else if (firstFree && chosen.isEmpty() || owned > 0 || progress.coins >= price) chosen.add(b)
                                    else sounds.play("nope")
                                    sounds.play("tick")
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(b.emoji, fontSize = 26.sp)
                            Text(b.title, color = Color.White, fontSize = 11.sp, fontWeight = Heavy)
                            Text(if (firstFree && (chosen.isEmpty() || chosen.first() == b)) "FREE" else if (owned > 0) "x$owned" else "🪙$price", color = Palette.gold, fontSize = 11.sp, fontWeight = Heavy)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ArcadeButton("✕", color = Color(0xFF6B3A08), textSize = 18.sp, onClick = onClose)
                ArcadeButton("▶ PLAY", textSize = 22.sp) {
                    // Pay for the chosen boosters: free first time, then owned ones, then coins.
                    val paid = ArrayList<Booster>()
                    for ((i, b) in chosen.withIndex()) {
                        val ok = when {
                            firstFree && i == 0 -> { progress.markSeen("prelevel_free"); true }
                            progress.count(b) > 0 -> progress.useBooster(b)
                            else -> progress.spend(Rewards.price(b, progress.unlocked))
                        }
                        if (ok) paid += b
                    }
                    onPlay(paid)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- shop and wordbook

@Composable
fun ShopScreen(progress: Progress, sounds: SoundFx, onBack: () -> Unit) {
    var bought by remember { mutableStateOf<Booster?>(null) }
    val today = Rewards.boosterOfTheDay(progress.unlocked)
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeButton("◀", color = Color(0xFF6B3A08), textSize = 18.sp, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                ArcadeText("🛒 Booster Shop", 26.sp, Palette.gold)
                Spacer(Modifier.weight(1f))
                CoinBadge(progress)
            }
            Spacer(Modifier.height(6.dp))
            val items = Booster.entries.toList()
            Column(Modifier.verticalScroll(rememberScrollState())) {
                for (row in items.chunked(5)) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    for (b in row) {
                        val unlocked = b.introLevel <= progress.unlocked
                        val price = Rewards.price(b, progress.unlocked)
                        Column(
                            Modifier
                                .weight(1f)
                                .background(if (b == today && unlocked) Color(0xCC1C6B3A) else Color(0xCC2A160A), RoundedCornerShape(12.dp))
                                .border(2.dp, Palette.gold, RoundedCornerShape(12.dp))
                                .padding(6.dp)
                                .alpha(if (unlocked) 1f else 0.5f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(b.emoji, fontSize = 28.sp)
                            Text(b.title, color = Color.White, fontSize = 12.sp, fontWeight = Heavy)
                            Text(if (unlocked) "You have ${progress.count(b)}" else "Unlocks at level ${b.introLevel}", color = Palette.cream, fontSize = 10.sp)
                            if (b == today && unlocked) Text("½ PRICE TODAY", color = Palette.gold, fontSize = 10.sp, fontWeight = Heavy)
                            if (unlocked) ArcadeButton("🪙$price", textSize = 13.sp, enabled = progress.coins >= price) {
                                if (progress.spend(price)) { progress.addBooster(b); sounds.play("cash"); bought = b }
                            }
                        }
                    }
                }
            }
        }
        bought?.let { b ->
            LaunchedEffect(b) { delay(1200); bought = null }
            MessageBar("+1 ${b.emoji} ${b.title}!")
        }
    }
}

@Composable
fun WordbookScreen(progress: Progress, onBack: () -> Unit) {
    val words = remember { progress.wordbook() }
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeButton("◀", color = Color(0xFF6B3A08), textSize = 18.sp, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                ArcadeText("📖 Wordbook: ${words.size} words", 24.sp, Palette.gold)
            }
            Spacer(Modifier.height(6.dp))
            if (words.isEmpty()) Text("Every new word you make is saved here, with +5 coins the first time!", color = Palette.cream, fontSize = 16.sp)
            Column(Modifier.verticalScroll(rememberScrollState()).arcadePanel().padding(10.dp).fillMaxWidth()) {
                for ((letter, group) in words.groupBy { it.first() }) {
                    ArcadeText("$letter", 18.sp, Palette.gold)
                    Text(group.joinToString("  ·  "), color = Palette.cream, fontSize = 14.sp, fontWeight = Heavy)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
