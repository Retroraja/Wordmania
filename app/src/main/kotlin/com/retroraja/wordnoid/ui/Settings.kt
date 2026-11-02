package com.retroraja.wordnoid.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.review.ReviewManagerFactory
import com.retroraja.wordnoid.R
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Tile
import com.retroraja.wordnoid.data.Progress
import kotlinx.coroutines.delay
import java.time.LocalDate

const val PRIVACY_URL = "https://github.com/Retroraja/Wordmania/blob/claude/first-playable/docs/privacy.md"

@Composable
fun SettingsScreen(progress: Progress, sounds: SoundFx, onBack: () -> Unit, onHowToPlay: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ScreenTop("⚙ Settings", progress, onBack)
            Row(Modifier.fillMaxSize().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).arcadePanel().padding(14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcadeText("Sound and play", 18.sp, Palette.gold)
                    SettingsToggles(progress)
                    ArcadeButton("❓ How to play", color = Palette.royal, textSize = 16.sp, onClick = onHowToPlay)
                    ArcadeButton("⭐ Rate us", color = Palette.purple, textSize = 16.sp) { askReview(activity, progress, force = true) }
                }
                Column(Modifier.weight(1f).arcadePanel().padding(14.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArcadeText("Account and privacy", 18.sp, Palette.gold)
                    Text("Your progress is saved on this phone and backed up by Android automatically.", color = Color.White, fontSize = 13.sp)
                    Text("Google Play Games sign-in and leaderboards will turn on when the game is on the Play Store.", color = Color.White, fontSize = 13.sp)
                    ArcadeButton("🔒 Privacy policy", color = Palette.slate, textSize = 15.sp) {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) } catch (_: Exception) {}
                    }
                    ArcadeButton("🛡 Ad choices", color = Palette.slate, textSize = 15.sp) {
                        com.google.android.ump.UserMessagingPlatform.showPrivacyOptionsForm(activity) {}
                    }
                    Text("Wordnoid by RetroRaja Games · version ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}", color = Palette.cream, fontSize = 11.sp)
                }
            }
        }
    }
}

/** In-app review ("Rate us"): after a boss or a finished district, at most once a month. */
fun askReview(activity: Activity, progress: Progress, force: Boolean = false) {
    if (!force && !progress.canAskRating()) return
    progress.lastRateAsk = LocalDate.now().toEpochDay()
    val manager = ReviewManagerFactory.create(activity)
    manager.requestReviewFlow().addOnCompleteListener { task ->
        if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
    }
}

/** RetroRaja Games studio intro, then the Wordnoid logo. About 3 seconds, tap to skip. */
@Composable
fun StudioIntro(sounds: SoundFx, onDone: () -> Unit) {
    val logo = remember { Animatable(0f) }
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        logo.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 200f))
        sounds.play("fanfare")
        delay(1300)
        step = 1
        delay(1600)
        onDone()
    }
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF3566E8), Color(0xFF0B1440)))).clickable { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        if (step == 0) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.retroraja_logo), "RetroRaja Games", Modifier.size(220.dp).scale(logo.value).graphicsLayer { rotationZ = (1 - logo.value) * -20f })
            ArcadeText("RETRORAJA GAMES", 22.sp, Color.White, Modifier.alpha(logo.value))
        } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                "WORDNOID".forEachIndexed { i, ch ->
                    val drop = remember { Animatable(-300f) }
                    LaunchedEffect(Unit) { delay(i * 70L); drop.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 300f)); sounds.play("drop") }
                    Tile3D(Tile(i.toLong(), ch), 58.dp, Modifier.graphicsLayer { translationY = drop.value; rotationZ = if (i % 2 == 0) -4f else 4f })
                }
            }
            Text("Swap · Spell · Build!", color = Color.White, fontSize = 18.sp, fontWeight = Heavy, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * First time only: a neutral birth-year question (for child-safe ads), then Tiko says hello.
 * Under 13: non-personalised, child-safe ads and no sign-in.
 */
@Composable
fun FirstTimeScreen(progress: Progress, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(if (progress.birthYear == 0) 0 else 1) }
    val thisYear = LocalDate.now().year
    var year by remember { mutableIntStateOf(thisYear - 12) }
    LivingBackground(1) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(Modifier.width(500.dp).arcadePanel().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Tiko(Modifier.size(80.dp))
                if (step == 0) {
                    ArcadeText("Hi! I'm Tiko!", 26.sp, Palette.gold)
                    Text("What year were you born?", color = Color.White, fontSize = 17.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                        ArcadeButton("−", color = Palette.slate, textSize = 22.sp) { year = (year - 1).coerceAtLeast(thisYear - 100) }
                        ArcadeText("  $year  ", 30.sp, Color.White)
                        ArcadeButton("+", color = Palette.slate, textSize = 22.sp) { year = (year + 1).coerceAtMost(thisYear) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (d in listOf(-10, -5, 5, 10)) Text(
                            if (d > 0) "+$d" else "$d",
                            Modifier.background(Color(0x33FFFFFF), RoundedCornerShape(8.dp)).clickable { year = (year + d).coerceIn(thisYear - 100, thisYear) }.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White, fontSize = 14.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ArcadeButton("OK") { progress.birthYear = year; step = 1 }
                } else {
                    ArcadeText("Welcome to Wordnoid!", 24.sp, Palette.gold)
                    Text(
                        "Villains scrambled the letters of our town! Spell words to beat them, earn Letter Bricks 🧱 and help me rebuild Tiko's Town.",
                        color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center, lineHeight = 21.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    ArcadeButton("Let's play!", onClick = onDone)
                }
            }
        }
    }
}

/** How to play: a short guide (from Settings). */
@Composable
fun HowToPlayScreen(progress: Progress, onBack: () -> Unit) {
    val items = listOf(
        "🔤" to "Swap two neighbouring tiles to spell a word: left to right, right to left, up or down. Only words made with the tile you moved count.",
        "📏" to "5-letter words give +1 move, 6+ letters give +2 moves.",
        "🎯" to "Each level has goals (shown on the left with a progress bar). Finish them before the moves run out.",
        "🎁" to "Make a word right next to a box to break it. Boxes hide prizes.",
        "🧊" to "Ice tiles melt when used in a word. The Hammer needs two hits.",
        "🧸" to "Tiko friends drop when the tiles under them clear. Bring them to the bottom!",
        "🎈" to "Make a word next to a balloon to pop it.",
        "💎" to "Clear the tiles on the sand to dig out treasure.",
        "⚡" to "Words fill the Power Meter. A full meter gives a mystery booster.",
        "🧱" to "Win levels for Letter Bricks and build up Tiko's Town!",
    )
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            ScreenTop("❓ How to play", progress, onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((e, t) in items) Row(Modifier.fillMaxWidth().arcadePanel().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(e, fontSize = 26.sp)
                    Text(t, color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}
