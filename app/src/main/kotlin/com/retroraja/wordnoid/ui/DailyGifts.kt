package com.retroraja.wordnoid.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.ads.Ads
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.data.Daily
import com.retroraja.wordnoid.data.Progress
import kotlinx.coroutines.launch

/** One "Daily Gifts" button with 3 tabs: Rewards (7-day calendar), Spin, Challenges. */
@Composable
fun DailyGiftsScreen(progress: Progress, sounds: SoundFx, onBack: () -> Unit, onMiniQuiz: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val activity = LocalContext.current as Activity
    @Suppress("UNUSED_VARIABLE") val v = progress.version
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ScreenTop("🎁 Daily Gifts", progress, onBack)
            Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("📅 Rewards", "🎡 Spin", "⭐ Challenges").forEachIndexed { i, t ->
                    ArcadeButton(t, color = if (tab == i) Palette.royal else Palette.slate, textSize = 15.sp) { tab = i }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (tab) {
                    0 -> RewardsTab(progress, sounds, activity)
                    1 -> SpinTab(progress, sounds, activity)
                    else -> ChallengesTab(progress, sounds, activity, onMiniQuiz)
                }
            }
        }
    }
}

@Composable
private fun RewardsTab(progress: Progress, sounds: SoundFx, activity: Activity) {
    val day = progress.dailyGiftDay()
    var got by remember { mutableStateOf<Daily.DayReward?>(null) }
    var doubled by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Come back every day. Missing a day doesn't reset it. Day 7 is the best!", color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Daily.CALENDAR.forEachIndexed { i, g ->
                val today = day > 0 && i + 1 == day
                val claimed = i < progress.dailyStreakDay || (day == 0 && i < progress.dailyStreakDay)
                Column(
                    Modifier
                        .width(92.dp)
                        .background(if (today) Palette.greenDeep else if (claimed) Color(0x55000000) else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .border(if (today) 3.dp else 1.dp, Palette.gold, RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Day ${i + 1}", fontSize = 12.sp, color = Color.White, fontWeight = Heavy)
                    Text(if (claimed) "✅" else if (i == 6) "🧰" else "🎁", fontSize = 24.sp)
                    Text(g.text, fontSize = 10.sp, color = Palette.gold, fontWeight = Heavy, textAlign = TextAlign.Center, lineHeight = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        val g = got
        if (day > 0 && g == null) ArcadeButton("Open today's gift!") {
            val r = Daily.CALENDAR[day - 1]
            progress.claimDaily()
            progress.addCoins(r.coins)
            r.booster?.let { progress.addBooster(it) }
            if (r.bricks > 0) progress.addBonusBricks(r.bricks)
            sounds.play("fanfare"); sounds.play("cash")
            got = r
        } else if (g != null) {
            Text("You got ${g.text}!", color = Palette.gold, fontSize = 20.sp, fontWeight = Heavy)
            if (!doubled) WatchButton("x2 today's gift", Ads.Spot.DOUBLE_DAILY, activity) {
                progress.addCoins(g.coins); g.booster?.let { progress.addBooster(it) }; doubled = true; sounds.play("cash")
            }
        } else Text("Today's gift is collected. See you tomorrow!", color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun SpinTab(progress: Progress, sounds: SoundFx, activity: Activity) {
    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Daily.Slot?>(null) }
    val n = Daily.WHEEL.size
    fun spin() {
        val i = Daily.spin()
        spinning = true
        result = null
        scope.launch {
            sounds.play("whoosh")
            // The pointer is at the top; slot i is centred at i * 360/n.
            val target = angle.value - angle.value % 360f + 360f * 5 + (360f - (i * 360f / n)) % 360f
            angle.animateTo(target, tween(3200, easing = FastOutSlowInEasing))
            val s = Daily.WHEEL[i]
            Daily.give(progress, s)
            result = s
            spinning = false
            sounds.play(if (s.amount >= 300) "win" else "cash")
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(240.dp).rotate(angle.value), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val colors = listOf(Color(0xFFFF7EB6), Color(0xFF6FC3FF), Color(0xFFFFD84A), Color(0xFF7ED957))
                    for (k in 0 until n) {
                        // Rare slots are drawn smaller.
                        drawArc(colors[k % 4], -90f - 180f / n + k * 360f / n, 360f / n, true, Offset.Zero, Size(size.width, size.height))
                    }
                    drawCircle(Color.White, size.width * 0.08f)
                }
                for (k in 0 until n) {
                    val s = Daily.WHEEL[k]
                    Text(
                        if (s.kind == Daily.SlotKind.COINS) "🪙${s.label}" else s.label,
                        Modifier.rotate(k * 360f / n).offset(y = (-88).dp),
                        fontSize = if (s.weight <= 6) 13.sp else 16.sp, fontWeight = Heavy, color = Color(0xFF1B2140),
                    )
                }
            }
            // Pointer at the top.
            Canvas(Modifier.size(30.dp).align(Alignment.TopCenter)) {
                drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2, size.height); close() }, Palette.red)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            result?.let { Text("You won ${if (it.kind == Daily.SlotKind.COINS) "🪙${it.amount}" else it.label}!", color = Palette.gold, fontSize = 22.sp, fontWeight = Heavy) }
            when {
                progress.freeSpinReady -> ArcadeButton("Free spin!", enabled = !spinning) { progress.useFreeSpin(); spin() }
                else -> {
                    Text("Free spin used today", color = Color.White, fontSize = 13.sp)
                    if (progress.adSpinReady) WatchButton("Extra spin", Ads.Spot.EXTRA_SPIN, activity) { progress.useAdSpin(); spin() }
                    val bought = progress.spinsBought
                    if (bought < Progress.SPIN_PRICES.size) ArcadeButton("Spin for 🪙${Progress.SPIN_PRICES[bought]}", enabled = !spinning && progress.coins >= Progress.SPIN_PRICES[bought], color = Palette.purple, textSize = 16.sp) {
                        if (progress.buySpin()) spin()
                    } else Text("Come back tomorrow for more spins!", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ChallengesTab(progress: Progress, sounds: SoundFx, activity: Activity, onMiniQuiz: () -> Unit) {
    val list = Daily.today()
    Column(Modifier.width(620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("3 new challenges every day. Finish all 3 for a bonus chest!", color = Color.White, fontSize = 14.sp)
        list.forEachIndexed { i, ch ->
            val have = progress.challengeProgress(i).coerceAtMost(ch.target)
            val done = Daily.done(progress, i)
            val claimed = progress.challengeClaimed(i)
            Row(Modifier.fillMaxWidth().arcadePanel().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (claimed) "✅" else "⭐", fontSize = 24.sp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(ch.text, color = Color.White, fontSize = 16.sp, fontWeight = Heavy)
                    Text("$have / ${ch.target}   ·   reward 🪙${ch.coins}", color = Palette.gold, fontSize = 12.sp)
                }
                when {
                    claimed -> Text("Done", color = Palette.green, fontSize = 14.sp, fontWeight = Heavy)
                    done -> ArcadeButton("Claim", textSize = 15.sp) { progress.claimChallenge(i); progress.addCoins(ch.coins); sounds.play("cash") }
                    ch.kind == Daily.Kind.QUIZ -> ArcadeButton("Play quiz", color = Palette.purple, textSize = 15.sp, onClick = onMiniQuiz)
                    else -> WatchButton("Skip", Ads.Spot.SKIP_CHALLENGE, activity) { progress.addChallenge(i, ch.target) }
                }
            }
        }
        val allClaimed = list.indices.all { progress.challengeClaimed(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (progress.challengeChestClaimed) Text("🧰 Bonus chest opened. New challenges tomorrow!", color = Color.White, fontSize = 14.sp)
            else ArcadeButton("🧰 Bonus chest: 🪙${Daily.CHEST_COINS} + 🧱1", enabled = allClaimed, modifier = Modifier.alpha(if (allClaimed) 1f else 0.6f)) {
                progress.claimChallengeChest(); progress.addCoins(Daily.CHEST_COINS); progress.addBonusBricks(1); sounds.play("fanfare")
            }
        }
    }
}
