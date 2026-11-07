package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.ads.Ads
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.data.Progress

/** Level Map: a grid of world cards. Locked worlds look frosted. */
@Composable
fun LevelMapScreen(
    progress: Progress, sounds: SoundFx, onTown: () -> Unit, onWorld: (Int) -> Unit,
    zoomTo: Int? = null, onLevel: (Int) -> Unit = {}, onQuiz: (Int, Int) -> Unit = { _, _ -> },
) {
    if (Fx.ok) { LevelMap3D(progress, sounds, onTown, zoomTo, onLevel, onQuiz); return }
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TownButton(progress, onClick = onTown)
                Spacer(Modifier.width(12.dp))
                ArcadeText("Level Map", 28.sp, Color.White)
                Spacer(Modifier.weight(1f))
                BrickBadge(progress); Spacer(Modifier.width(8.dp)); CoinBadge(progress)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (w in WORLDS) {
                    val first = w.index * 10 + 1
                    val open = progress.unlocked >= first
                    val bricks = progress.worldBricks(w.index)
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(0.9f)
                            .background(Brush.verticalGradient(listOf(w.top, w.bottom)), RoundedCornerShape(18.dp))
                            .border(3.dp, if (open) Palette.gold else Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                            .clickable { if (open) { sounds.play("pop"); onWorld(w.index) } else sounds.play("nope") }
                            .padding(8.dp)
                            .alpha(if (open) 1f else 0.55f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Text(if (open) w.emoji else "🔒", fontSize = 40.sp)
                        ArcadeText(w.name, 17.sp, Color.White)
                        Text("Levels $first-${first + 9}", color = Color.White, fontSize = 12.sp)
                        Text("${w.villainEmoji} Boss: ${w.villain}", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.Center)
                        Text("🧱 $bricks / 30", color = Palette.gold, fontSize = 14.sp, fontWeight = Heavy)
                    }
                }
            }
            Ads.Banner()
        }
    }
}

/** One world: 2 rows of levels, purple QUIZ cards after the 4th and 8th levels, and the Boss card last. */
@Composable
fun WorldScreen(world: Int, progress: Progress, sounds: SoundFx, onBack: () -> Unit, onTown: () -> Unit, onLevel: (Int) -> Unit, onQuiz: (Int) -> Unit) {
    val w = WORLDS[world]
    val first = world * 10 + 1
    val anim = rememberInfiniteTransition(label = "world")
    val pulse by anim.animateFloat(1f, 1.1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    LivingBackground(first) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeButton("◀ Level Map", color = Palette.slate, textSize = 15.sp, onClick = onBack)
                Spacer(Modifier.width(8.dp))
                ArcadeButton("🏠", color = Palette.slate, textSize = 15.sp, onClick = onTown)
                Spacer(Modifier.width(12.dp))
                ArcadeText("${w.emoji} ${w.name}", 26.sp, Color.White)
                Spacer(Modifier.weight(1f))
                Text("🧱 ${progress.worldBricks(world)} / 30", color = Color.White, fontSize = 16.sp, fontWeight = Heavy)
            }
            Spacer(Modifier.weight(1f))
            val rows = listOf(
                listOf(Card.L(0), Card.L(1), Card.L(2), Card.L(3), Card.Q(0), Card.L(4)),
                listOf(Card.L(5), Card.L(6), Card.L(7), Card.Q(1), Card.L(8), Card.L(9)),
            )
            for (row in rows) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    row.forEachIndexed { i, card ->
                        when (card) {
                            is Card.L -> {
                                val n = first + card.i
                                val done = progress.bricks(n) > 0
                                val next = n == progress.unlocked
                                val open = n <= progress.unlocked && Levels.get(n) != null
                                val boss = card.i == 9
                                Column(
                                    Modifier
                                        .size(if (boss) 104.dp else 86.dp)
                                        .scale(if (next) pulse else 1f)
                                        .background(
                                            Brush.verticalGradient(
                                                if (boss) listOf(Color(0xFFFF6B6B), Color(0xFF9E1F1F))
                                                else if (open) listOf(Color(0xFF5B8CFF), Palette.royalDeep) else listOf(Color(0xFFB0BEC5), Color(0xFF78909C)),
                                            ),
                                            RoundedCornerShape(18.dp),
                                        )
                                        .border(if (next) 4.dp else 2.dp, if (next) Color.White else Palette.gold, RoundedCornerShape(18.dp))
                                        .clickable { if (open) { sounds.play("pop"); onLevel(n) } else sounds.play("nope") },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    if (boss) Text(w.villainEmoji, fontSize = 26.sp)
                                    ArcadeText(if (open) "$n" else "🔒", if (boss) 22.sp else 26.sp, Color.White)
                                    if (done) Text("🧱".repeat(progress.bricks(n)), fontSize = 12.sp)
                                    else if (next) Text("PLAY", color = Palette.gold, fontSize = 13.sp, fontWeight = Heavy)
                                    if (boss) Text("BOSS", color = Color.White, fontSize = 11.sp, fontWeight = Heavy)
                                }
                            }
                            is Card.Q -> {
                                // Quiz after the 4th (slot 0) and 8th (slot 1) level of the world.
                                val after = first + if (card.slot == 0) 3 else 7
                                val open = progress.unlocked > after
                                val score = progress.quizScore(world, card.slot)
                                Column(
                                    Modifier
                                        .size(86.dp)
                                        .background(Brush.verticalGradient(listOf(Color(0xFFB98CFF), Color(0xFF5E2DB8))), RoundedCornerShape(18.dp))
                                        .border(2.dp, Palette.gold, RoundedCornerShape(18.dp))
                                        .alpha(if (open) 1f else 0.6f)
                                        .clickable { if (open) { sounds.play("pop"); onQuiz(card.slot) } else sounds.play("nope") },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("🎓", fontSize = 24.sp)
                                    ArcadeText("QUIZ", 16.sp, Color.White)
                                    Text(if (score >= 0) "✓ $score/3" else if (open) "2🧱 + 🎁" else "🔒", color = Palette.gold, fontSize = 11.sp, fontWeight = Heavy)
                                }
                            }
                        }
                        if (i < row.size - 1) Text("➜", color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Ads.Banner()
        }
    }
}

private sealed interface Card {
    data class L(val i: Int) : Card
    data class Q(val slot: Int) : Card
}


/**
 * The 3D level map: floating islands. Tap an island and the camera flies down to it: 10 levels as stepping
 * stones, 2 quiz treasure chests, the boss castle, and Tiko standing on the next level.
 */
@Composable
private fun LevelMap3D(progress: Progress, sounds: SoundFx, onTown: () -> Unit, zoomTo: Int?, onLevel: (Int) -> Unit, onQuiz: (Int, Int) -> Unit) {
    val cur = ((progress.unlocked.coerceAtMost(50) - 1) / 10).coerceIn(0, WORLDS.size - 1)
    var view by remember { mutableIntStateOf(zoomTo ?: cur) }
    var zoom by remember { mutableStateOf<Int?>(null) }
    var spots by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var chests by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val open = WORLDS.count { progress.unlocked >= it.index * 10 + 1 }
    fun levelsOf(w: Int) = org.json.JSONArray().apply {
        for (k in 0 until 10) { val n = w * 10 + 1 + k; put(if (progress.bricks(n) > 0 && n != progress.unlocked) 1 else if (n == progress.unlocked) 2 else if (n < progress.unlocked) 1 else 0) }
    }
    fun quizOf(w: Int) = org.json.JSONArray().apply { put(if (progress.unlocked > w * 10 + 4) 1 else 0); put(if (progress.unlocked > w * 10 + 8) 1 else 0) }
    LaunchedEffect(Unit) {
        val names = org.json.JSONArray().apply { WORLDS.forEach { put("${it.index + 1} ${it.name}") } }
        Fx.backdrop("map", Fx.obj("names" to names, "open" to open, "world" to cur, "home" to Fx.obj("levels" to levelsOf(cur), "quiz" to quizOf(cur))))
        if (zoomTo != null) zoom = zoomTo
    }
    LaunchedEffect(view) { Fx.act("world", Fx.obj("w" to view)) }
    LaunchedEffect(zoom) {
        spots = emptyList(); chests = emptyList()
        val z = zoom
        if (z == null) { Fx.act("unzoom", Fx.obj()); return@LaunchedEffect }
        Fx.act("zoom", Fx.obj("w" to z, "levels" to levelsOf(z), "quiz" to quizOf(z)))
        val js = Fx.await("mapSpots", 6000) ?: return@LaunchedEffect
        val o = org.json.JSONObject(js)
        fun pts(a: org.json.JSONArray) = List(a.length()) { i -> a.getJSONArray(i).let { Offset(it.getDouble(0).toFloat(), it.getDouble(1).toFloat()) } }
        spots = pts(o.getJSONArray("stones")); chests = pts(o.getJSONArray("chests"))
    }
    androidx.activity.compose.BackHandler(zoom != null) { zoom = null }
    val w = WORLDS[view]
    val first = w.index * 10 + 1
    val isOpen = progress.unlocked >= first
    val anim = rememberInfiniteTransition(label = "map")
    val pulse by anim.animateFloat(1f, 1.15f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxSize()) {
        // Overview: tap the island to fly down to its levels.
        if (zoom == null) Box(Modifier.fillMaxSize().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {
            if (isOpen) { sounds.play("pop"); zoom = view } else sounds.play("nope")
        })
        // Zoomed: each stone and chest is a button with its number on top.
        zoom?.let { z ->
            val zf = z * 10 + 1
            spots.forEachIndexed { k, p ->
                val n = zf + k
                val openL = n <= progress.unlocked && Levels.get(n) != null
                val next = n == progress.unlocked
                val boss = k == 9
                Box(
                    Modifier.offset { androidx.compose.ui.unit.IntOffset((p.x * density - 32.dp.toPx()).toInt(), (p.y * density - 40.dp.toPx()).toInt()) }
                        .size(64.dp).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {
                            if (openL) { sounds.play("pop"); onLevel(n) } else sounds.play("nope")
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        if (boss) "${WORLDS[z].villainEmoji} $n" else if (openL) "$n" else "🔒",
                        fontFamily = ArcadeFont, fontSize = if (next) 22.sp else 18.sp, color = Color.White,
                        modifier = Modifier.scale(if (next) pulse else 1f)
                            .background(if (next) Palette.greenDeep else if (boss) Palette.red else if (openL) Palette.goldDeep else Color(0xAA5A6070), RoundedCornerShape(50))
                            .border(2.dp, Color.White, RoundedCornerShape(50)).padding(horizontal = 8.dp),
                    )
                }
            }
            chests.forEachIndexed { q, p ->
                val after = zf + if (q == 0) 3 else 7
                val openQ = progress.unlocked > after
                val score = progress.quizScore(z, q)
                Box(
                    Modifier.offset { androidx.compose.ui.unit.IntOffset((p.x * density - 36.dp.toPx()).toInt(), (p.y * density - 46.dp.toPx()).toInt()) }
                        .size(72.dp).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {
                            if (openQ) { sounds.play("pop"); onQuiz(z, q) } else sounds.play("nope")
                        },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text(
                        if (score >= 0) "QUIZ ✓" else if (openQ) "QUIZ!" else "🔒 QUIZ", fontFamily = ArcadeFont, fontSize = 14.sp, color = Color.White,
                        modifier = Modifier.scale(if (openQ && score < 0) pulse else 1f).background(Palette.purple, RoundedCornerShape(50)).border(2.dp, Color.White, RoundedCornerShape(50)).padding(horizontal = 8.dp),
                    )
                }
            }
        }
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (zoom != null) ArcadeButton("◀ Islands", color = Palette.slate, textSize = 16.sp) { sounds.play("whoosh"); zoom = null }
            else TownButton(progress, onClick = onTown)
            Spacer(Modifier.width(12.dp))
            ArcadeText(zoom?.let { "${WORLDS[it].emoji} ${WORLDS[it].name}" } ?: "Level Map", 28.sp, Color.White)
        }
        Row(Modifier.align(Alignment.TopEnd).padding(10.dp)) { BrickBadge(progress); Spacer(Modifier.width(8.dp)); CoinBadge(progress) }
        if (zoom == null) {
            if (view > 0) ArcadeButton("◀", color = Palette.royal, textSize = 26.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp)) { view--; sounds.play("whoosh") }
            if (view < WORLDS.size - 1) ArcadeButton("▶", color = Palette.royal, textSize = 26.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)) { view++; sounds.play("whoosh") }
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).arcadePanel().padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column {
                    ArcadeText("${w.emoji} ${w.name}", 20.sp, Palette.gold)
                    Text("Levels $first-${first + 9} · ${w.villainEmoji} Boss: ${w.villain} · 🧱 ${progress.worldBricks(w.index)}/30", color = Color.White, fontSize = 12.sp, fontWeight = Heavy)
                }
                if (isOpen) ArcadeButton("Open ▶", textSize = 20.sp) { sounds.play("pop"); zoom = view }
                else Text("🔒 Beat ${WORLDS[(view - 1).coerceAtLeast(0)].villain} first", color = Palette.cream, fontSize = 14.sp, fontWeight = Heavy)
            }
        } else Text(
            "🧱 ${progress.worldBricks(zoom!!)}/30 · Tap a stone to play",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).arcadePanel().padding(horizontal = 14.dp, vertical = 6.dp),
            color = Color.White, fontSize = 15.sp, fontWeight = Heavy,
        )
    }
}
