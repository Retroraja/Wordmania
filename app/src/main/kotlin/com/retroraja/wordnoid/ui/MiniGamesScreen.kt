package com.retroraja.wordnoid.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.retroraja.wordnoid.ads.Ads
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.data.MiniGames
import com.retroraja.wordnoid.data.Progress
import org.json.JSONObject

/** Update 1.1: the Mini Games menu (town button, opens after Level 5). Each game: 1 free play a day, plus one video play a day. */
@Composable
fun MiniGamesScreen(progress: Progress, sounds: SoundFx, won: Int, onBack: () -> Unit, onPlay: (MiniGames.Game) -> Unit) {
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE") val v = progress.version
    // Coins from the round just played fly into the counter.
    LaunchedEffect(won) { if (won > 0) CoinFly.release(scope, sounds, won) }
    val tier = MiniGames.tier(progress.unlocked)
    LivingBackground(progress.unlocked.coerceAtMost(50)) {
        Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ScreenTop("🪙 Mini Games", progress, onBack)
            Text(
                "Every game: 1 FREE play a day. Each round is new, and its size follows your level (${MiniGames.tierName(tier)}).",
                color = Color.White, fontSize = 13.sp, fontWeight = Heavy, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniGames.LAUNCHED.forEach { g -> GameCard(g, progress, sounds, activity, Modifier.weight(1f).fillMaxHeight(), onPlay) }
            }
            Spacer(Modifier.height(6.dp))
            val adLeft = !progress.miniAdUsed && Ads.spotAvailable(Ads.Spot.MINI_PLAY)
            Text(
                "🎬 Extra play: after a free play is used, a short video gives one more play of any game. " + if (adLeft) "1 left today." else "Used today, back tomorrow!",
                color = Palette.cream, fontSize = 12.sp, textAlign = TextAlign.Center,
            )
            Text("Coming soon: " + MiniGames.COMING.joinToString(" · "), color = Color(0xFFCFD8FF), fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GameCard(g: MiniGames.Game, progress: Progress, sounds: SoundFx, activity: Activity, modifier: Modifier, onPlay: (MiniGames.Game) -> Unit) {
    var note by remember { mutableStateOf<String?>(null) }
    val how = MiniGames.howToPlay(progress, g)
    val isNew = !progress.seen("mg_played_${g.id}")
    Box(modifier) {
        Column(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFEAF0FF))), RoundedCornerShape(16.dp))
                .border(3.dp, Color(0xFFFFE08A), RoundedCornerShape(16.dp))
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier.fillMaxWidth().height(70.dp).background(
                    when (g) {
                        MiniGames.Game.LETTER_WHEEL -> Brush.verticalGradient(listOf(Color(0xFF8A5CFF), Color(0xFFFF9EC7)))
                        MiniGames.Game.PICTURE_HUNT -> Brush.verticalGradient(listOf(Color(0xFFFF9FD2), Color(0xFFA8ECB1)))
                        MiniGames.Game.CRACKER_BLAST -> Brush.verticalGradient(listOf(Color(0xFF3B1D6E), Color(0xFF140A33)))
                    }, RoundedCornerShape(12.dp),
                ),
                contentAlignment = Alignment.Center,
            ) { Text(g.emoji, fontSize = 40.sp) }
            Text(g.title, color = Palette.royal, fontSize = 18.sp, fontWeight = Heavy)
            Text(g.about, color = Color(0xFF3A4170), fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
            Text("Best: ${progress.miniBest(g.id)} coins", color = Color(0xFF5A6390), fontSize = 11.sp, fontWeight = Heavy)
            when (how) {
                "free" -> ArcadeButton("▶ FREE PLAY", textSize = 15.sp) {
                    sounds.play("pop"); progress.useMiniFree(g.id); progress.markSeen("mg_played_${g.id}"); onPlay(g)
                }
                "ad" -> if (Ads.spotAvailable(Ads.Spot.MINI_PLAY)) ArcadeButton("🎬 Video: play", color = Palette.royal, textSize = 14.sp) {
                    Ads.showRewarded(activity, Ads.Spot.MINI_PLAY, onReward = { note = null; progress.useMiniAd(); progress.markSeen("mg_played_${g.id}"); onPlay(g) }, onFail = { note = it })
                } else ArcadeButton("Back tomorrow", color = Palette.slate, enabled = false, textSize = 14.sp) {}
                else -> ArcadeButton("Back tomorrow", color = Palette.slate, enabled = false, textSize = 14.sp) {}
            }
            note?.let { Text(it, color = Palette.red, fontSize = 10.sp) }
        }
        if (how == "free") Text(
            "FREE", color = Color.White, fontSize = 11.sp, fontWeight = Heavy,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp).background(Palette.red, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
        )
        if (isNew) Text(
            "NEW", color = Color.White, fontSize = 11.sp, fontWeight = Heavy,
            modifier = Modifier.align(Alignment.TopStart).offset(x = (-4).dp, y = (-6).dp).background(Palette.pink, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/** One round of a mini game. The game itself is the page assets/mini/mini.html; it talks back through the "MG" bridge. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniPlayScreen(game: MiniGames.Game, progress: Progress, sounds: SoundFx, onDone: (coins: Int) -> Unit) {
    val activity = LocalContext.current as Activity
    var web by remember { mutableStateOf<WebView?>(null) }
    val done by rememberUpdatedState(onDone)
    // The back button asks first (the game shows "Leave? You keep your coins").
    // If the page doesn't answer (or the player keeps pressing back), the round just closes.
    var backs by remember { mutableIntStateOf(0) }
    BackHandler {
        if (++backs >= 3) { done(0); return@BackHandler }
        val w = web ?: run { done(0); return@BackHandler }
        w.evaluateJavascript("(function(){var b=document.querySelector('#s-${game.id} .back');if(b&&!finished){b.click();return 1}return 0})()") { r -> if (r != "1") done(0) }
    }
    DisposableEffect(Unit) { onDispose { web?.destroy() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val main = Handler(Looper.getMainLooper())
            WebView(ctx).apply {
                web = this
                setBackgroundColor(android.graphics.Color.rgb(26, 31, 92))
                overScrollMode = WebView.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.mediaPlaybackRequiresUserGesture = true
                webViewClient = WebViewClient()
                var finished = false
                addJavascriptInterface(object {
                    @JavascriptInterface fun config(): String = JSONObject()
                        .put("game", game.id).put("tier", MiniGames.tier(progress.unlocked)).put("hints", MiniGames.FREE_HINTS).toString()
                    @JavascriptInterface fun words(): String =
                        runCatching { ctx.assets.open("play_words.txt").use { String(it.readBytes()) } }.getOrDefault("")
                    @JavascriptInterface fun sfx(name: String) { main.post { sounds.play(name) } }
                    @JavascriptInterface fun ad(kind: String) {
                        main.post {
                            val spot = if (kind == "hearts") Ads.Spot.MINI_HEARTS else Ads.Spot.MINI_HINT
                            val reply = { ok: Boolean -> evaluateJavascript("adDone('${if (kind == "hearts") "hearts" else "hint"}',$ok)", null) }
                            if (!Ads.spotAvailable(spot)) reply(false)
                            else Ads.showRewarded(activity, spot, onReward = { if (kind != "hearts") progress.useMiniHint(game.id); reply(true) }, onFail = { reply(false) })
                        }
                    }
                    @JavascriptInterface fun finish(coins: Int, score: Int) {
                        main.post {
                            if (finished) return@post
                            finished = true
                            val n = coins.coerceIn(0, 200)
                            if (n > 0) { progress.addCoins(n); CoinFly.holdCoins(n) }
                            progress.setMiniBest(game.id, score.coerceIn(0, 200))
                            done(n)
                        }
                    }
                }, "MG")
                loadUrl("file:///android_asset/mini/mini.html?g=${game.id}")
            }
        },
    )
}
