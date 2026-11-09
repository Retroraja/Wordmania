package com.retroraja.wordnoid.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.ads.Ads
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.data.Progress
import com.retroraja.wordnoid.data.QuizBank
import com.retroraja.wordnoid.data.Rewards
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Quiz Time: 3 questions of different kinds. All 3 right = 2 Letter Bricks + a booster, 2 right = 1 brick + 20 coins.
 * [world]/[slot] = a world quiz; null = today's mini quiz (a Daily Challenge).
 */
@Composable
fun QuizScreen(world: Int?, slot: Int, progress: Progress, dict: Dictionary?, sounds: SoundFx, onClose: () -> Unit, onFinished: (Int) -> Unit = {}) {
    var attempt by remember { mutableIntStateOf(0) }
    val seed = if (world != null) world * 1000L + slot * 100L + attempt + progress.quizScore(world, slot) * 7L + 11 else LocalDate.now().toEpochDay() * 3 + attempt
    val questions = remember(attempt) { QuizBank.quiz(seed, dict) }
    var index by remember(attempt) { mutableIntStateOf(0) }
    var right by remember(attempt) { mutableIntStateOf(0) }
    var picked by remember(attempt) { mutableStateOf<Int?>(null) }
    var finished by remember(attempt) { mutableStateOf(false) }
    var reward by remember(attempt) { mutableStateOf("") }
    val activity = LocalContext.current as Activity
    val skin = world ?: (progress.unlocked - 1) / 10
    // Update 1.1: every quiz opens with a start card (what it is, the prize, Start or Skip). Tiko explains it the first time.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!progress.seen("quiz_intro")) { progress.markSeen("quiz_intro"); sounds.play("calm:Quiz time! Answer 3 quick word questions and win prizes.") } }
    // Floating question marks and Tiko in 3D behind the quiz (Tiko cheers or shakes his head).
    LaunchedEffect(Fx.ok) { if (Fx.ok) Fx.backdrop("quiz") }
    QuizBackground(skin) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ArcadeButton("✕", color = Palette.slate, textSize = 16.sp, onClick = onClose)
                Spacer(Modifier.width(10.dp))
                ArcadeText(if (world == null) "🎓 Mini Quiz" else "🎓 Quiz Time!", 26.sp, Color.White)
                Spacer(Modifier.weight(1f))
                Text(if (started && !finished) "Question ${index + 1} of 3" else "", color = Color.White, fontSize = 15.sp, fontWeight = Heavy)
            }
            Spacer(Modifier.height(8.dp))
            if (!started) {
                Column(Modifier.width(500.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Tiko(Modifier.size(70.dp))
                    ArcadeText("QUIZ TIME!", 34.sp, Palette.gold)
                    Text("3 quick word questions. Tap the right answer.", color = Color.White, fontSize = 16.sp, fontWeight = Heavy, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    if (world != null) {
                        Text("🏆 3 right: +2 🧱 and a booster", color = Palette.gold, fontSize = 15.sp, fontWeight = Heavy)
                        Text("⭐ 2 right: +1 🧱 and 🪙20", color = Palette.gold, fontSize = 15.sp, fontWeight = Heavy)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ArcadeButton("Skip", color = Palette.slate, onClick = onClose)
                        ArcadeButton("▶ Start") { sounds.play("pop"); started = true }
                    }
                }
            } else if (!finished) {
                val q = questions[index]
                key(attempt, index) {
                    QuestionView(q, picked, sounds) { ok, choice ->
                        picked = choice
                        if (ok) { right++; sounds.play("ding") } else sounds.play("nope")
                        Fx.act(if (ok) "right" else "wrong", Fx.obj("x" to Fx.dp(Fx.screenW / 2), "y" to Fx.dp(Fx.screenH * 0.45f)))
                    }
                }
                if (picked != null) {
                    Spacer(Modifier.height(10.dp))
                    ArcadeButton(if (index < 2) "Next ▶" else "See result") {
                        picked = null
                        if (index < 2) index++ else {
                            finished = true
                            if (world != null) {
                                reward = when {
                                    right == 3 -> { val b = Rewards.randomBooster(progress.unlocked); progress.addBooster(b); progress.addBonusBricks(2); "+2 🧱 and ${b.emoji} ${b.title}" }
                                    right == 2 -> { progress.addBonusBricks(1); progress.addCoins(20); "+1 🧱 and 🪙20" }
                                    else -> ""
                                }
                                if (progress.quizScore(world, slot) < 2 || right > progress.quizScore(world, slot)) progress.setQuizScore(world, slot, right)
                            }
                            onFinished(right)
                            sounds.play(if (right >= 2) "win" else "lose")
                        }
                    }
                }
            } else {
                Column(Modifier.width(460.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Tiko(Modifier.size(70.dp))
                    ArcadeText("$right / 3 right!", 28.sp, Palette.gold)
                    Text(
                        when (right) { 3 -> "Perfect! You're a word wizard!"; 2 -> "Great job!"; else -> "Good try! Want another go?" },
                        color = Color.White, fontSize = 16.sp,
                    )
                    if (reward.isNotEmpty()) Text(reward, color = Palette.gold, fontSize = 18.sp, fontWeight = Heavy)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (right < 3) WatchButton("Try again", Ads.Spot.QUIZ_RETRY, activity) { attempt++ }
                        ArcadeButton("Done", onClick = onClose)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionView(q: QuizBank.Question, picked: Int?, sounds: SoundFx, onAnswer: (Boolean, Int) -> Unit) {
    var showFlash by remember { mutableStateOf(q.flash.isNotEmpty()) }
    var timeLeft by remember { mutableIntStateOf(q.seconds) }
    val tapped = remember { mutableStateListOf<Int>() }
    val pickedNow by rememberUpdatedState(picked)
    LaunchedEffect(Unit) { if (showFlash) { delay(3500); showFlash = false } }
    LaunchedEffect(showFlash) {
        if (!showFlash && q.seconds > 0) while (timeLeft > 0 && pickedNow == null) { delay(1000); timeLeft-- }
        if (!showFlash && q.seconds > 0 && timeLeft == 0 && pickedNow == null) {
            if (q.multi) onAnswer(tapped.toSet() == q.correctSet || (tapped.count { it in q.correctSet } >= 3 && tapped.count { it !in q.correctSet } <= 1), -2)
            else onAnswer(false, -1)
        }
    }
    Column(Modifier.width(620.dp).arcadePanel().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(q.type.title.uppercase(), color = Palette.gold, fontSize = 13.sp, fontWeight = Heavy)
        if (showFlash) {
            Text("Remember these words!", color = Color.White, fontSize = 18.sp, fontWeight = Heavy)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(10.dp)) {
                for (w in q.flash) ArcadeText(w, 24.sp, Color.White, Modifier.softCard().padding(horizontal = 10.dp, vertical = 4.dp))
            }
            return@Column
        }
        Text(q.prompt, color = Color.White, fontSize = 20.sp, fontWeight = Heavy, textAlign = TextAlign.Center)
        if (q.seconds > 0) Text("⏱ $timeLeft", color = if (timeLeft <= 3) Palette.red else Palette.gold, fontSize = 18.sp, fontWeight = Heavy)
        Spacer(Modifier.height(10.dp))
        for (row in q.options.indices.chunked(if (q.multi) 4 else 2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(4.dp)) {
            for (i in row) {
                val chosen = if (q.multi) i in tapped else picked == i
                val showRight = picked != null && (if (q.multi) i in q.correctSet else i == q.answer)
                val bg = when {
                    showRight -> Palette.greenDeep
                    picked != null && chosen -> Palette.red
                    chosen -> Palette.purple
                    else -> Color(0x33FFFFFF)
                }
                Box(
                    Modifier
                        .width(if (q.multi) 130.dp else 240.dp)
                        .background(bg, RoundedCornerShape(14.dp))
                        .border(2.dp, Palette.gold, RoundedCornerShape(14.dp))
                        .clickable(enabled = picked == null) {
                            if (q.multi) {
                                if (i in tapped) tapped.remove(i) else { tapped.add(i); sounds.play("tick") }
                                // Done as soon as every real word is tapped.
                                if (tapped.toSet() == q.correctSet) onAnswer(true, -2)
                            } else onAnswer(i == q.answer, i)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) { ArcadeText(q.options[i], 20.sp, Color.White) }
            }
        }
        if (q.multi && picked == null) {
            Spacer(Modifier.height(6.dp))
            ArcadeButton("Done", textSize = 15.sp) { onAnswer(tapped.count { it in q.correctSet } >= 3 && tapped.count { it !in q.correctSet } <= 1, -2) }
        }
    }
}


@Composable
private fun QuizBackground(skin: Int, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    if (Fx.ok) androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), content = content)
    else LivingBackground(skin * 10 + 1, content = content)
}
