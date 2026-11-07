package com.retroraja.wordnoid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.data.Progress
import kotlinx.coroutines.delay

/** Update 1: win streak, level chest, "build ready" nudges. */

/** Free boosters a win streak gives at the start of [level]: 1 win Hammer, 2 + Joker, 3+ + Magic Swap. Only unlocked ones, never in Levels 1-4. */
fun streakBoosters(streak: Int, level: Int): List<Booster> {
    if (streak <= 0 || level <= 4) return emptyList()
    return listOf(Booster.HAMMER, Booster.JOKER, Booster.MAGIC_SWAP).take(streak.coerceAtMost(3)).filter { it.introLevel < level }
}

/** What the full level chest gives. */
data class ChestPrize(val coins: Int, val boosters: List<Booster>)

/** The fire badge: "🔥 3 wins in a row". Hidden at 0. */
@Composable
fun StreakBadge(streak: Int, level: Int, modifier: Modifier = Modifier) {
    if (streak <= 0) return
    val anim = rememberInfiniteTransition(label = "fire")
    val f by anim.animateFloat(0.95f, 1.08f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "f")
    val free = streakBoosters(streak, level)
    Row(
        modifier.scale(f)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFA23A), Color(0xFFE5432D))), RoundedCornerShape(50))
            .border(2.dp, Color(0xFFFFE08A), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🔥 $streak win${if (streak > 1) "s" else ""} in a row", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        if (free.isNotEmpty()) Text("  Free: " + free.joinToString(" ") { it.emoji }, color = Color(0xFFFFF3C4), fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

/** The level chest: 5 slots, one per win. [popSlot] (0-based) fills with a pop. */
@Composable
fun ChestSlots(filled: Int, modifier: Modifier = Modifier, popSlot: Int = -1, small: Boolean = false) {
    val s = if (small) 14.dp else 20.dp
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("🧰", fontSize = if (small) 15.sp else 20.sp)
        Spacer(Modifier.width(2.dp))
        for (i in 0 until Progress.CHEST_SLOTS) {
            val on = i < filled
            val pop = remember { Animatable(if (i == popSlot) 0f else 1f) }
            LaunchedEffect(popSlot) { if (i == popSlot) { delay(700); pop.snapTo(2.2f); pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 420f)) } }
            Box(
                Modifier.size(s).scale(if (i == popSlot) pop.value.coerceAtLeast(0.01f) else 1f)
                    .background(if (on) Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFE8A417))) else Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color(0x22FFFFFF))), RoundedCornerShape(5.dp))
                    .border(1.5.dp, if (on) Color(0xFF9A5A00) else Color(0x66FFFFFF), RoundedCornerShape(5.dp)),
            )
        }
    }
}

/** The full chest opens: full screen, coins and boosters. */
@Composable
fun ChestOpenOverlay(prize: ChestPrize, sounds: com.retroraja.wordnoid.audio.SoundFx, onClose: () -> Unit) {
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { sounds.play("fanfare"); pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f)); sounds.play("sparkle") }
    val anim = rememberInfiniteTransition(label = "chest")
    val spin by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(6000)), label = "s")
    Box(
        Modifier.fillMaxSize().background(Color(0xD0081440)).clickable(remember { MutableInteractionSource() }, null) {},
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(320.dp).graphicsLayer { rotationZ = spin }.background(Brush.radialGradient(listOf(Color(0x88FFE27A), Color(0x00FFE27A))), CircleShape))
        Column(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }, horizontalAlignment = Alignment.CenterHorizontally) {
            ArcadeText("LEVEL CHEST!", 38.sp, Palette.gold)
            Text("🧰", fontSize = 84.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🪙 +${prize.coins}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(Color(0x33FFFFFF), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 4.dp))
                for (b in prize.boosters) Text("${b.emoji} ${b.title}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.background(Color(0x33FFFFFF), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp))
            }
            Spacer(Modifier.height(14.dp))
            ArcadeButton("Collect!", textSize = 26.sp) { sounds.play("cash"); onClose() }
        }
    }
}

/** A bouncing red "!" badge (town task ready). Put it in a Box over the button's top-right corner. */
@Composable
fun BangBadge(modifier: Modifier = Modifier) {
    val anim = rememberInfiniteTransition(label = "bang")
    val b by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "b")
    Box(
        modifier.offset(x = 6.dp, y = (-8 - 5 * b).dp).size(22.dp).background(Color(0xFFE53935), CircleShape).border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text("!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) }
}

/** Town button with a "!" badge while a town task can be built. */
@Composable
fun TownButton(progress: Progress, text: String = "🏠 Town", textSize: androidx.compose.ui.unit.TextUnit = 16.sp, onClick: () -> Unit) {
    Box {
        ArcadeButton(text, color = Palette.slate, textSize = textSize, onClick = onClick)
        @Suppress("UNUSED_VARIABLE") val v = progress.version
        if (canBuildNow(progress)) BangBadge(Modifier.align(Alignment.TopEnd))
    }
}

/** Win screen: the Town button turns into a gold, glowing "Build ready!" with Tiko's hand pointing down at it. */
@Composable
fun BuildReadyButton(onClick: () -> Unit) {
    val anim = rememberInfiniteTransition(label = "build")
    val g by anim.animateFloat(1f, 1.08f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "g")
    val hand by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "h")
    Box(contentAlignment = Alignment.Center) {
        ArcadeButton("🔨 Build ready!", color = Color(0xFFE8A417), textSize = 20.sp, modifier = Modifier.scale(g), onClick = onClick)
        Text("👇", fontSize = 28.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-34 - 8 * hand).dp))
    }
}
