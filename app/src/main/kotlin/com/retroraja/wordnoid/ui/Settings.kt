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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
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

/** RetroRaja Games studio intro, then the Wordnoid logo. In 3D when the phone can draw it. Tap to skip. */
@Composable
fun StudioIntro(sounds: SoundFx, onDone: () -> Unit) {
    // Wait a moment for the 3D layer to start; old phones without it get the flat intro.
    var mode by remember { mutableIntStateOf(0) } // 0 waiting, 1 3D, 2 flat
    var done by remember { mutableStateOf(false) }
    fun finish() { if (!done) { done = true; onDone() } }
    LaunchedEffect(Unit) {
        val ok = kotlinx.coroutines.withTimeoutOrNull(2500) { snapshotFlow { Fx.ok }.first { it } } ?: false
        mode = if (ok) 1 else 2
        if (ok) {
            Fx.backdrop("intro")
            Fx.await("introName", 6800)
            sounds.play("wordnoid")
            Fx.await("introDone", 2500)
            delay(500)
            finish()
        }
    }
    when (mode) {
        1 -> Box(Modifier.fillMaxSize().clickable { finish() })
        0 -> Box(Modifier.fillMaxSize().background(Color(0xFF0B1440)))
        else -> FlatIntro(sounds) { finish() }
    }
}

@Composable
private fun FlatIntro(sounds: SoundFx, finish: () -> Unit) {
    val logo = remember { Animatable(0f) }
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        logo.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 200f))
        sounds.play("fanfare")
        delay(1300)
        step = 1
        sounds.play("wordnoid")
        delay(1600)
        finish()
    }
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF3566E8), Color(0xFF0B1440)))).clickable { finish() },
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
            Text("Word Match with Fun Boosters", color = Palette.gold, fontSize = 20.sp, fontWeight = Heavy, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * First time only: agree to the privacy policy and say if you are 13 or older (for child-safe ads).
 * Under 13: non-personalised, child-safe ads and no sign-in. The Wordnoid tiles bob behind in 3D.
 */
@Composable
fun FirstTimeScreen(progress: Progress, onDone: () -> Unit) {
    val context = LocalContext.current
    val thisYear = LocalDate.now().year
    LaunchedEffect(Fx.ok) { if (Fx.ok) Fx.backdrop("title") }
    val content: @Composable BoxScope.() -> Unit = {
        Box(Modifier.fillMaxSize().padding(bottom = 10.dp), contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.width(520.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tiko(Modifier.size(56.dp))
                    ArcadeText(" Welcome to Wordnoid!", 22.sp, Palette.gold)
                }
                Text(
                    "Spell words, beat the villains and rebuild Tiko's Town!",
                    color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center,
                )
                Text(
                    "By playing you agree to our Privacy Policy",
                    Modifier.padding(top = 4.dp).clickable {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))) } catch (_: Exception) {}
                    },
                    color = Palette.cyan, fontSize = 13.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ArcadeButton("I'm 13 or older") { progress.birthYear = thisYear - 20; onDone() }
                    ArcadeButton("I'm under 13", color = Palette.slate) { progress.birthYear = thisYear - 10; onDone() }
                }
            }
        }
    }
    if (Fx.ok) Box(Modifier.fillMaxSize(), content = content) else LivingBackground(1, content = content)
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
        "💖" to "Pink Tiko friends drop when the tiles under them clear. Bring them to the bottom!",
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
