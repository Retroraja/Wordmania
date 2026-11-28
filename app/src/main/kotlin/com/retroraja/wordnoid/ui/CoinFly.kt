package com.retroraja.wordnoid.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.retroraja.wordnoid.audio.SoundFx
import com.retroraja.wordnoid.data.Progress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Every reward flies into its counter. The coins/bricks are saved at once, but the counters on screen
 * hold them back ([coinHold], [brickHold]) and count up as the 3D coins land.
 */
object CoinFly {
    /** Its own scope, so a flight always finishes and the counter never stays held back after leaving a screen. */
    private val main = kotlinx.coroutines.MainScope()
    /** Where the coin and brick counters are on screen right now (px). Set by CoinBadge/BrickBadge. */
    var coinAt by mutableStateOf(Offset.Zero)
    var brickAt by mutableStateOf(Offset.Zero)
    var coinHold by mutableIntStateOf(0); private set
    var brickHold by mutableIntStateOf(0); private set
    /** Goes up each time something lands, so a counter can do a little bump. */
    var landed by mutableIntStateOf(0); private set

    fun holdCoins(n: Int) { if (n > 0) coinHold += n }
    fun holdBricks(n: Int) { if (n > 0) brickHold += n }

    /** Adds [n] coins and flies them from [from] (px; the screen centre when null) into the coin counter. */
    fun coins(scope: CoroutineScope, progress: Progress, sounds: SoundFx, n: Int, from: Offset? = null) {
        if (n <= 0) return
        progress.addCoins(n); holdCoins(n); release(scope, sounds, n, from)
    }

    fun bricks(scope: CoroutineScope, progress: Progress, sounds: SoundFx, n: Int, from: Offset? = null) {
        if (n <= 0) return
        progress.addBonusBricks(n); holdBricks(n); releaseBricks(scope, sounds, n, from)
    }

    /** Flies coins that were already added and held back. */
    fun release(scope: CoroutineScope, sounds: SoundFx, n: Int, from: Offset? = null) {
        if (n <= 0) return
        main.launch { fly(sounds, n, false, from) }
    }

    fun releaseBricks(scope: CoroutineScope, sounds: SoundFx, n: Int, from: Offset? = null) {
        if (n <= 0) return
        main.launch { fly(sounds, n, true, from) }
    }

    /** 1.5: coins and bricks fly as light 2D pictures (drawn by [CoinFlyLayer]); the 3D ones were slow on cheaper phones. */
    data class Flight(val id: Int, val from: Offset, val to: Offset, val n: Int, val brick: Boolean)
    val flights = androidx.compose.runtime.mutableStateListOf<Flight>()
    private var ids = 0
    fun flight(from: Offset, to: Offset, n: Int, brick: Boolean) {
        val f = Flight(++ids, from, to, n.coerceIn(1, 8), brick)
        flights.add(f)
        main.launch { delay(1400); flights.remove(f) }
    }

    private suspend fun fly(sounds: SoundFx, n: Int, brick: Boolean, from: Offset?) {
        val dm = android.content.res.Resources.getSystem().displayMetrics
        val start = from ?: Offset(maxOf(dm.widthPixels, dm.heightPixels) / 2f, minOf(dm.widthPixels, dm.heightPixels) * 0.5f)
        val to = if (brick) brickAt else coinAt
        if (to != Offset.Zero) {
            flight(start, to, if (brick) n.coerceAtMost(6) else (n / 12 + 3).coerceAtMost(8), brick)
            delay(950)
        } else delay(250)
        val steps = if (brick) n.coerceIn(1, 8) else 10
        for (i in 1..steps) {
            val give = n * i / steps - n * (i - 1) / steps
            if (brick) brickHold = (brickHold - give).coerceAtLeast(0) else coinHold = (coinHold - give).coerceAtLeast(0)
            landed++
            sounds.play(if (brick) "clack" else "coin")
            delay(if (brick) 120 else 55)
        }
    }
}

/** Draws the flying coins and bricks over everything (2D pictures on an arc, each a little later than the last). */
@androidx.compose.runtime.Composable
fun CoinFlyLayer() {
    for (f in CoinFly.flights) androidx.compose.runtime.key(f.id) {
        for (i in 0 until f.n) {
            val t = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
            androidx.compose.runtime.LaunchedEffect(Unit) {
                delay(i * 70L)
                t.animateTo(1f, androidx.compose.animation.core.tween(720, easing = androidx.compose.animation.core.FastOutSlowInEasing))
            }
            val k = t.value
            if (k > 0f && k < 1f) {
                // A curve: out a little to the side and up, then into the counter.
                val side = (i - f.n / 2f) * 28f
                val mx = (f.from.x + f.to.x) / 2 + side
                val my = minOf(f.from.y, f.to.y) - 120f
                val x = (1 - k) * (1 - k) * f.from.x + 2 * (1 - k) * k * mx + k * k * f.to.x
                val y = (1 - k) * (1 - k) * f.from.y + 2 * (1 - k) * k * my + k * k * f.to.y
                val s = 1.2f - 0.5f * k
                androidx.compose.material3.Text(
                    if (f.brick) "🧱" else "🪙",
                    androidx.compose.ui.Modifier
                        .offset { androidx.compose.ui.unit.IntOffset((x - 24).toInt(), (y - 24).toInt()) }
                        .graphicsLayer { scaleX = s; scaleY = s },
                    fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp),
                )
            }
        }
    }
}
