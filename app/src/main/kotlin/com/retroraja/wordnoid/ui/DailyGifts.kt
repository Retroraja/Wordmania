package com.retroraja.wordnoid.ui

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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

/** Update 1.1: the gift button opens Daily Rewards only (the 7-day calendar). The Spin wheel and Daily Challenges are gone. */
@Composable
fun DailyGiftsScreen(progress: Progress, sounds: SoundFx, onBack: () -> Unit) {
    val activity = LocalContext.current as Activity
    @Suppress("UNUSED_VARIABLE") val v = progress.version
    // In 3D the gift box is a scene behind this screen (as the approved preview).
    LaunchedEffect(Fx.ok) { if (Fx.ok) Fx.backdrop("gift", Fx.obj("opened" to (progress.dailyGiftDay() == 0), "offsetX" to 0.0)) }
    // Update 1.2: after the (shorter, dimmer) opening, a big clear card says exactly what you got.
    var youGot by remember { mutableStateOf<Daily.DayReward?>(null) }
    val content: @Composable BoxScope.() -> Unit = {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ScreenTop("🎁 Daily Rewards", progress, onBack)
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp), contentAlignment = Alignment.Center) { RewardsTab(progress, sounds, activity) { youGot = it } }
        }
        youGot?.let { r -> YouGotCard(r) { youGot = null; sounds.play("pop") } }
    }
    if (Fx.ok) Box(Modifier.fillMaxSize(), content = content) else LivingBackground(progress.unlocked.coerceAtMost(50), content = content)
}

@Composable
private fun RewardsTab(progress: Progress, sounds: SoundFx, activity: Activity, onGot: (Daily.DayReward) -> Unit) {
    val day = progress.dailyGiftDay()
    var got by remember { mutableStateOf<Daily.DayReward?>(null) }
    var doubled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                    Text(if (claimed) "✅" else if (i == 6) "🧰" else "🎁", fontSize = if (Fx.ok) 18.sp else 24.sp)
                    Text(g.text, fontSize = 10.sp, color = Palette.gold, fontWeight = Heavy, textAlign = TextAlign.Center, lineHeight = 12.sp)
                }
            }
        }
        // The 3D gift box sits in the middle of the screen.
        if (Fx.ok) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(12.dp))
        val g = got
        var opening by remember { mutableStateOf(false) }
        if (day > 0 && g == null) ArcadeButton("Open today's gift!", enabled = !opening) {
            val r = Daily.CALENDAR[day - 1]
            progress.claimDaily()
            progress.addCoins(r.coins); CoinFly.holdCoins(r.coins)
            r.booster?.let { progress.addBooster(it) }
            if (r.bricks > 0) { progress.addBonusBricks(r.bricks); CoinFly.holdBricks(r.bricks) }
            if (Fx.ok) {
                opening = true
                Fx.act("open", Fx.obj("emoji" to (r.booster?.emoji ?: "🪙"), "name" to (r.booster?.title ?: "${r.coins} coins"), "text" to "You got ${r.text}!"))
                scope.launch { Fx.await("giftOpened", 3000); sounds.play("cash"); got = r; opening = false; onGot(r)
                    CoinFly.release(scope, sounds, r.coins); CoinFly.releaseBricks(scope, sounds, r.bricks) }
            } else { sounds.play("fanfare"); sounds.play("cash"); got = r; onGot(r); CoinFly.release(scope, sounds, r.coins); CoinFly.releaseBricks(scope, sounds, r.bricks) }
        } else if (g != null) {
            Text("You got ${g.text}!", color = Palette.gold, fontSize = 20.sp, fontWeight = Heavy)
            if (!doubled) WatchButton("x2 today's gift", Ads.Spot.DOUBLE_DAILY, activity) {
                CoinFly.coins(scope, progress, sounds, g.coins); g.booster?.let { progress.addBooster(it) }; doubled = true; sounds.play("cash")
            }
        } else Text("Today's gift is collected. See you tomorrow!", color = Color.White, fontSize = 16.sp)
    }
}

/** The big "You got" card: one clear line per prize. */
@Composable
private fun YouGotCard(r: Daily.DayReward, onOk: () -> Unit) {
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f)) }
    DimBox(0.45f) {
        Column(
            Modifier.width(380.dp).scale(pop.value).arcadePanel().padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArcadeText("You got", 30.sp, Palette.gold)
            Spacer(Modifier.height(6.dp))
            if (r.coins > 0) Text("🪙 +${r.coins} coins", color = Color.White, fontSize = 30.sp, fontWeight = Heavy)
            r.booster?.let { Text("${it.emoji} ${it.title} x1", color = Color.White, fontSize = 30.sp, fontWeight = Heavy) }
            if (r.bricks > 0) Text("🧱 +${r.bricks} bricks", color = Color.White, fontSize = 30.sp, fontWeight = Heavy)
            Spacer(Modifier.height(10.dp))
            ArcadeButton("Great!", onClick = onOk)
        }
    }
}
