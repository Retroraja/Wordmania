package com.retroraja.wordnoid.ui

import androidx.compose.runtime.getValue
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
        scope.launch { fly(sounds, n, false, from) }
    }

    fun releaseBricks(scope: CoroutineScope, sounds: SoundFx, n: Int, from: Offset? = null) {
        if (n <= 0) return
        scope.launch { fly(sounds, n, true, from) }
    }

    private suspend fun fly(sounds: SoundFx, n: Int, brick: Boolean, from: Offset?) {
        val dm = android.content.res.Resources.getSystem().displayMetrics
        val start = from ?: Offset(maxOf(dm.widthPixels, dm.heightPixels) / 2f, minOf(dm.widthPixels, dm.heightPixels) * 0.5f)
        val to = if (brick) brickAt else coinAt
        if (Fx.ok && to != Offset.Zero) {
            Fx.call("fly", Fx.obj("x" to Fx.dp(start.x), "y" to Fx.dp(start.y), "n" to (if (brick) n.coerceAtMost(6) else (n / 12 + 3).coerceAtMost(8)),
                "brick" to brick, "to" to Fx.xy(to)))
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
