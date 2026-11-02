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
fun LevelMapScreen(progress: Progress, sounds: SoundFx, onTown: () -> Unit, onWorld: (Int) -> Unit) {
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArcadeButton("🏠 Town", color = Palette.slate, textSize = 16.sp, onClick = onTown)
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
