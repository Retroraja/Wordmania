package com.retroraja.wordnoid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.core.Booster
import com.retroraja.wordnoid.data.Progress
import kotlinx.coroutines.delay

/**
 * The story of the 5 lands, told once per moment (approved preview: design/animations/story-preview.html):
 * - "intro": a villain scrambles the land (Land 1 before Level 1; later lands right after the bridge).
 * - "boss": the villain is beaten after the boss level.
 * - "complete": all town tasks done -> grand opening + prize, then the bridge and the next land's intro
 *   (Land 5: the grand finale instead).
 */
object Story {
    fun seenKey(land: Int, part: String) = "story_${part}_$land"

    /** The prize for finishing a land's town (shown on the story card). Given once. */
    fun landPrize(progress: Progress, land: Int) {
        if (progress.seen("story_prize_$land")) return
        progress.markSeen("story_prize_$land")
        val coins = if (land == 4) 1500 else 500 + land * 250
        val boosters = if (land == 4) 8 else 3 + land
        progress.addCoins(coins)
        val pool = Booster.unlockedAt(((land + 1) * 10).coerceAtMost(50)).filter { !it.strong }.ifEmpty { listOf(Booster.HAMMER) }
        repeat(boosters) { i -> progress.addBooster(pool[i % pool.size]) }
        // Grand finale bonus.
        if (land == 4) progress.addCoins(2000)
    }

    fun prizeText(land: Int): String {
        val d = DISTRICTS.getOrNull(land) ?: return ""
        return "🎁 ${d.building}: ${d.buildingGift} every day"
    }
}

@Composable
fun StoryScreen(progress: Progress, sounds: SoundFx, land: Int, part: String, onDone: () -> Unit) {
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (finished) return
        finished = true
        progress.markSeen(Story.seenKey(land, part))
        if (part == "complete") {
            if (land < 4) progress.markSeen(Story.seenKey(land + 1, "intro"))
            Story.landPrize(progress, land)
        }
        onDone()
    }
    if (!Fx.ok) { LaunchedEffect(Unit) { if (part == "complete") Story.landPrize(progress, land); finish() }; return }
    LaunchedEffect(Unit) {
        Fx.backdrop(
            "story",
            Fx.obj(
                "land" to land, "part" to part, "chain" to (part == "complete"),
                "bricks" to progress.bricksToSpend, "prize" to Story.prizeText(land),
                "skipIntro" to (land < 4 && progress.seen(Story.seenKey(land + 1, "intro"))),
            ),
        )
        // Intro ~21 s, boss ~16 s, complete + bridge + next intro ~45 s.
        Fx.await("storyDone", if (part == "complete") 70_000 else 35_000)
        finish()
    }
    DisposableEffect(Unit) { onDispose { Fx.backdrop(null) } }
    var canSkip by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(2500); canSkip = true }
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null) {}) {
        if (canSkip) Text(
            "Skip ⏭", color = Color.White, fontSize = 15.sp, fontFamily = BodyFont,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 16.dp)
                .background(Color(0x99000000), RoundedCornerShape(50)).clickable { sounds.play("pop"); finish() }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
