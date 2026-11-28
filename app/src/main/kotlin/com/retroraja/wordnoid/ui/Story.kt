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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
        // Update 1.3: coins instead of boosters.
        progress.addCoins(boosters * com.retroraja.wordnoid.data.Rewards.BOOSTER_COINS)
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
    // Select + Match update (lag fix): the story is shown as pictures with a slow zoom, not live 3D.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var loaded by remember { mutableStateOf<List<StoryPics.Pic>?>(null) }
    LaunchedEffect(Unit) {
        Fx.backdrop(null) // no live 3D behind the pictures
        val parts = buildList {
            add("$land-$part")
            if (part == "complete" && land < 4 && !progress.seen(Story.seenKey(land + 1, "intro"))) add("${land + 1}-intro")
        }
        loaded = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { parts.flatMap { StoryPics.load(ctx, it) } }
    }
    val pics = loaded ?: run { Box(Modifier.fillMaxSize().background(Color.Black)); return }
    if (pics.isEmpty()) { LaunchedEffect(Unit) { if (part == "complete") Story.landPrize(progress, land); finish() }; return }
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(pics) {
        for (i in pics.indices) {
            index = i
            delay(maxOf(1800L, (pics[i].ms * 1.3f).toLong()))
        }
        finish()
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.animation.Crossfade(targetState = index, animationSpec = androidx.compose.animation.core.tween(450), label = "story") { i ->
            val zoom = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(Unit) { zoom.animateTo(1.06f, androidx.compose.animation.core.tween(maxOf(1800, (pics[i].ms * 1.3f).toInt()) + 500, easing = androidx.compose.animation.core.LinearEasing)) }
            androidx.compose.foundation.Image(
                pics[i].image, null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = zoom.value; scaleY = zoom.value },
            )
        }
    }
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

/** Story pictures (assets/story/<land>-<part>/pics.json + webp files), made from the old 3D story. */
object StoryPics {
    class Pic(val image: ImageBitmap, val ms: Int)

    fun load(ctx: android.content.Context, name: String): List<Pic> = try {
        val json = org.json.JSONArray(ctx.assets.open("story/$name/pics.json").bufferedReader().use { it.readText() })
        (0 until json.length()).mapNotNull { i ->
            val o = json.getJSONObject(i)
            val bmp = ctx.assets.open("story/$name/${o.getString("f")}").use { android.graphics.BitmapFactory.decodeStream(it, null, android.graphics.BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 }) } ?: return@mapNotNull null
            Pic(bmp.asImageBitmap(), o.getInt("ms"))
        }
    } catch (e: Exception) { emptyList() }
}
