package com.retroraja.wordnoid.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.core.Levels
import com.retroraja.wordnoid.data.Progress
import kotlinx.coroutines.delay

@Composable
fun TitleScreen(ready: Boolean, progress: Progress, onPlay: () -> Unit, onToggleSound: () -> Unit) {
    WoodBackground {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                "WORDNOID".forEachIndexed { i, ch ->
                    val drop = remember { Animatable(-400f) }
                    LaunchedEffect(Unit) {
                        delay(90L * i)
                        drop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 300f))
                    }
                    val inf = rememberInfiniteTransition(label = "bob")
                    val bob by inf.animateFloat(-4f, 4f, infiniteRepeatable(tween(900 + i * 70), RepeatMode.Reverse), label = "bob$i")
                    LogoTile(ch, Modifier.offset(y = (drop.value + bob).dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            ArcadeText("Swap tiles. Make words. Clear the board!", 18.sp, Palette.cream)
            Spacer(Modifier.height(24.dp))
            val inf = rememberInfiniteTransition(label = "play")
            val pulse by inf.animateFloat(1f, 1.07f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
            if (ready) {
                ArcadeButton("PLAY", Modifier.scale(pulse), textSize = 30.sp) { onPlay() }
            } else {
                ArcadeText("Loading words…", 18.sp, Palette.gold)
            }
            Spacer(Modifier.height(14.dp))
            ArcadeText(if (progress.sound) "🔊 Sound on" else "🔇 Sound off", 14.sp, Palette.cream, Modifier.clickable { onToggleSound() }.padding(8.dp))
        }
    }
}

@Composable
private fun LogoTile(ch: Char, modifier: Modifier) {
    val color = letterColor(ch)
    Box(modifier.size(58.dp)) {
        Box(Modifier.fillMaxSize().background(color.darker(0.55f), RoundedCornerShape(10.dp)))
        Box(
            Modifier.fillMaxSize().padding(bottom = 5.dp)
                .background(Brush.verticalGradient(listOf(color.lighter(0.25f), color)), RoundedCornerShape(10.dp))
                .border(2.dp, Palette.gold, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { ArcadeText(ch.toString(), 34.sp, Color.White) }
    }
}

@Composable
fun MapScreen(progress: Progress, onBack: () -> Unit, onLevel: (Int) -> Unit) {
    BackHandler { onBack() }
    WoodBackground {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ArcadeButton("◀", color = Palette.wood, textSize = 18.sp) { onBack() }
            Spacer(Modifier.weight(1f))
            ArcadeText("WORDNOID", 26.sp, Palette.gold)
            Spacer(Modifier.weight(1f))
            ArcadeText("🪙 ${progress.coins}", 20.sp, Palette.gold, Modifier.arcadePanel().padding(horizontal = 14.dp, vertical = 6.dp))
        }
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = 70.dp, bottom = 20.dp, start = 30.dp, end = 30.dp)) {
            val levels = Levels.all
            val n = levels.size
            val w = maxWidth
            val h = maxHeight
            val node = 84.dp
            fun xFor(i: Int) = if (n == 1) w / 2 else (w - node) * (i.toFloat() / (n - 1)) + node / 2
            fun yFor(i: Int) = if (i % 2 == 0) h * 0.62f else h * 0.32f
            val density = LocalDensity.current
            // Dotted path between levels.
            Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until n - 1) {
                    with(density) {
                        drawLine(
                            Palette.gold.copy(alpha = 0.7f),
                            Offset(xFor(i).toPx(), yFor(i).toPx()), Offset(xFor(i + 1).toPx(), yFor(i + 1).toPx()),
                            strokeWidth = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)),
                        )
                    }
                }
            }
            levels.forEachIndexed { i, level ->
                val locked = level.number > progress.unlocked
                val current = level.number == progress.unlocked
                val stars = progress.stars(level.number)
                val inf = rememberInfiniteTransition(label = "node")
                val pulse by inf.animateFloat(1f, if (current) 1.1f else 1f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "p$i")
                Column(
                    Modifier.offset(xFor(i) - node / 2, yFor(i) - node / 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(node)
                            .scale(pulse)
                            .alpha(if (locked) 0.55f else 1f)
                            .background(
                                Brush.radialGradient(
                                    if (locked) listOf(Color(0xFF8A8A8A), Color(0xFF4A4A4A))
                                    else listOf(Palette.woodLight, Palette.wood)
                                ), CircleShape,
                            )
                            .border(4.dp, if (current) Palette.green else Palette.gold, CircleShape)
                            .clickable(enabled = !locked) { onLevel(level.number) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (locked) Text("🔒", style = TextStyle(fontSize = 28.sp))
                        else ArcadeText("${level.number}", 34.sp)
                    }
                    Row {
                        for (s in 1..3) ArcadeText(if (s <= stars) "★" else "☆", 20.sp, if (s <= stars) Palette.gold else Color(0x66FFFFFF))
                    }
                }
            }
        }
    }
}
