package com.retroraja.wordnoid.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * All game sounds are made here with code (no sound files), so there are no copyright issues.
 */
class SoundFx {
    private val rate = 44100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracks = HashMap<String, AudioTrack>()
    var enabled = true

    // A pentatonic scale, so any run of notes sounds nice.
    private val scale = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66, 1318.51, 1567.98, 1760.00)

    init {
        scope.launch {
            add("clack", clack())
            scale.forEachIndexed { i, f -> add("note$i", bell(f, 0.28, 0.45)) }
            add("coin", concat(bell(1318.5, 0.06, 0.35), bell(1975.5, 0.16, 0.35)))
            add("ding", bell(1046.5, 0.45, 0.5))
            add("combo", concat(*doubleArrayOf(1046.5, 1318.5, 1568.0, 2093.0).map { bell(it, 0.07, 0.4) }.toTypedArray()))
            add("win", concat(bell(523.25, 0.12, 0.45), bell(659.25, 0.12, 0.45), bell(783.99, 0.12, 0.45), mix(bell(1046.5, 0.6, 0.35), bell(1318.5, 0.6, 0.3), bell(783.99, 0.6, 0.3))))
            add("lose", concat(bell(392.0, 0.22, 0.35), bell(329.6, 0.22, 0.32), bell(261.6, 0.5, 0.3)))
            add("shuffle", sweep())
            add("tick", bell(1568.0, 0.05, 0.25))
        }
    }

    fun play(name: String) {
        if (!enabled) return
        val t = tracks[name] ?: return
        try {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.play()
        } catch (_: IllegalStateException) { }
    }

    /** Rising chime, one note per letter: longer words sound bigger. */
    fun word(letters: Int, startNote: Int = 0) {
        if (!enabled) return
        scope.launch {
            for (i in 0 until letters) {
                play("note${minOf(startNote + i, scale.size - 1)}")
                delay(70)
            }
        }
    }

    fun release() {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }

    private fun add(name: String, data: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        track.write(data, 0, data.size)
        tracks[name] = track
    }

    private fun toPcm(samples: DoubleArray): ShortArray =
        ShortArray(samples.size) { (samples[it].coerceIn(-1.0, 1.0) * 32000).toInt().toShort() }

    /** A soft bell: sine plus a little overtone, quick attack, smooth decay. */
    private fun bell(freq: Double, seconds: Double, volume: Double): ShortArray {
        val n = (rate * seconds).toInt()
        val s = DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val attack = minOf(1.0, t / 0.004)
            val env = attack * exp(-t * 6.0 / seconds)
            volume * env * (sin(2 * PI * freq * t) + 0.3 * sin(2 * PI * freq * 2 * t) + 0.1 * sin(2 * PI * freq * 3 * t)) / 1.4
        }
        return toPcm(s)
    }

    /** A wooden "clack": a short knock plus a low thump. */
    private fun clack(): ShortArray {
        val n = (rate * 0.07).toInt()
        val rnd = Random(5)
        var last = 0.0
        val s = DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val noise = rnd.nextDouble(-1.0, 1.0)
            last = last * 0.55 + noise * 0.45
            0.55 * last * exp(-t * 90) + 0.5 * sin(2 * PI * 210 * t) * exp(-t * 45) + 0.25 * sin(2 * PI * 820 * t) * exp(-t * 120)
        }
        return toPcm(s)
    }

    private fun sweep(): ShortArray {
        val n = (rate * 0.35).toInt()
        var phase = 0.0
        val s = DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val f = 300 + 1500 * t / 0.35
            phase += 2 * PI * f / rate
            0.3 * sin(phase) * exp(-t * 4) * minOf(1.0, t / 0.01)
        }
        return toPcm(s)
    }

    private fun concat(vararg parts: ShortArray): ShortArray {
        val out = ShortArray(parts.sumOf { it.size })
        var pos = 0
        for (p in parts) { p.copyInto(out, pos); pos += p.size }
        return out
    }

    private fun mix(vararg parts: ShortArray): ShortArray {
        val n = parts.maxOf { it.size }
        return ShortArray(n) { i -> parts.sumOf { p -> if (i < p.size) p[i].toInt() else 0 }.coerceIn(-32000, 32000).toShort() }
    }
}
