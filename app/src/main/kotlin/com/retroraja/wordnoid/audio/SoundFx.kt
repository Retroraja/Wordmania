package com.retroraja.wordnoid.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
class SoundFx(context: Context) {
    private val rate = 44100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // One SoundPool plays every sound (Android limits how many AudioTracks an app may open).
    private val pool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
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
            add("pop", pop(0.5))
            add("burst", mix(noiseHit(0.18, 0.45, 0.6), concat(silence(0.03), bell(2093.0, 0.25, 0.25))))
            add("crack", concat(woodKnock(0.6), woodKnock(0.45)))
            add("shatter", mix(noiseHit(0.35, 0.4, 0.95), bell(2637.0, 0.4, 0.2), concat(silence(0.05), bell(3136.0, 0.35, 0.18))))
            add("snap", mix(bell(1760.0, 0.18, 0.3), bell(2349.0, 0.12, 0.25), noiseHit(0.05, 0.3, 0.9)))
            add("bang", boom())
            add("crackle", crackle())
            add("whoosh", whoosh(0.4, rising = true))
            add("rewind", whoosh(0.5, rising = false))
            add("sparkle", concat(*doubleArrayOf(1568.0, 2093.0, 2637.0, 3136.0, 2637.0, 3520.0).map { bell(it, 0.06, 0.25) }.toTypedArray()))
            add("flip", concat(bell(880.0, 0.04, 0.3), bell(1318.5, 0.08, 0.3)))
            add("thunk", mix(boomShort(), woodKnock(0.5)))
            add("chop", concat(woodKnock(0.7), silence(0.02), noiseHit(0.12, 0.4, 0.7)))
            add("click", concat(bell(2637.0, 0.03, 0.35), silence(0.03), bell(1760.0, 0.05, 0.35)))
            add("hum", hum())
            add("rain", rain())
            add("fanfare", concat(bell(783.99, 0.1, 0.4), bell(987.77, 0.1, 0.4), bell(1174.66, 0.1, 0.4), mix(bell(1567.98, 0.7, 0.35), bell(1174.66, 0.7, 0.3), bell(987.77, 0.7, 0.25))))
            add("cash", concat(*List(8) { i -> bell(if (i % 2 == 0) 1975.5 else 2637.0, 0.05, 0.25) }.toTypedArray()))
            add("star", mix(boomShort(), bell(1318.5, 0.5, 0.35), bell(1975.5, 0.5, 0.25)))
            add("nope", concat(bell(311.1, 0.09, 0.3), bell(277.2, 0.14, 0.3)))
            add("swap", whoosh(0.16, rising = true))
            add("drop", concat(bell(659.25, 0.05, 0.25), bell(523.25, 0.08, 0.25)))
        }
    }

    fun play(name: String) {
        if (!enabled) return
        val id = ids[name] ?: return
        runCatching { pool.play(id, 1f, 1f, 1, 0, 1f) }
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
        runCatching { pool.release() }
        ids.clear()
    }

    /** Saves the sound as a small WAV file and loads it into the pool. */
    private fun add(name: String, data: ShortArray) {
        runCatching {
            val file = File(dir, "$name.wav")
            val bytes = ByteBuffer.allocate(44 + data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bytes.put("RIFF".toByteArray()).putInt(36 + data.size * 2).put("WAVE".toByteArray())
            bytes.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
            bytes.put("data".toByteArray()).putInt(data.size * 2)
            for (v in data) bytes.putShort(v)
            FileOutputStream(file).use { it.write(bytes.array()) }
            ids[name] = pool.load(file.path, 1)
        }
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

    private fun silence(seconds: Double): ShortArray = ShortArray((rate * seconds).toInt())

    private fun noiseHit(seconds: Double, volume: Double, bright: Double): ShortArray {
        val n = (rate * seconds).toInt()
        val rnd = Random(11)
        var last = 0.0
        return toPcm(DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val x = rnd.nextDouble(-1.0, 1.0)
            last = last * (1 - bright) + x * bright
            volume * last * exp(-t * 6 / seconds)
        })
    }

    private fun pop(volume: Double): ShortArray {
        val n = (rate * 0.12).toInt()
        var phase = 0.0
        return toPcm(DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val f = 400 + 900 * exp(-t * 25)
            phase += 2 * PI * f / rate
            volume * sin(phase) * exp(-t * 30)
        })
    }

    private fun woodKnock(volume: Double): ShortArray {
        val n = (rate * 0.09).toInt()
        val rnd = Random(3)
        return toPcm(DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            volume * (0.6 * sin(2 * PI * 330 * t) * exp(-t * 50) + 0.4 * rnd.nextDouble(-1.0, 1.0) * exp(-t * 80) + 0.3 * sin(2 * PI * 1250 * t) * exp(-t * 90))
        })
    }

    private fun boom(): ShortArray {
        val n = (rate * 0.9).toInt()
        val rnd = Random(9)
        var last = 0.0
        var phase = 0.0
        return toPcm(DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            last = last * 0.92 + rnd.nextDouble(-1.0, 1.0) * 0.08
            phase += 2 * PI * (40 + 60 * exp(-t * 8)) / rate
            0.6 * sin(phase) * exp(-t * 5) + 2.2 * last * exp(-t * 4)
        })
    }

    private fun boomShort(): ShortArray {
        val n = (rate * 0.25).toInt()
        var phase = 0.0
        return toPcm(DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            phase += 2 * PI * (55 + 120 * exp(-t * 25)) / rate
            0.7 * sin(phase) * exp(-t * 14)
        })
    }

    private fun crackle(): ShortArray {
        val n = (rate * 0.9).toInt()
        val rnd = Random(21)
        val s = DoubleArray(n)
        repeat(38) {
            val start = rnd.nextInt(n - 400)
            val vol = rnd.nextDouble(0.15, 0.45)
            for (k in 0 until 300) s[start + k] += vol * rnd.nextDouble(-1.0, 1.0) * exp(-k / 40.0)
        }
        return toPcm(s)
    }

    private fun whoosh(seconds: Double, rising: Boolean): ShortArray {
        val n = (rate * seconds).toInt()
        val rnd = Random(4)
        var lp = 0.0
        return toPcm(DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val p = t / seconds
            val cut = if (rising) 0.03 + 0.4 * p else 0.43 - 0.4 * p
            lp += (rnd.nextDouble(-1.0, 1.0) - lp) * cut
            0.9 * lp * sin(PI * p)
        })
    }

    private fun hum(): ShortArray {
        val n = (rate * 0.6).toInt()
        return toPcm(DoubleArray(n) { i ->
            val t = i.toDouble() / rate
            val env = minOf(1.0, t / 0.05) * minOf(1.0, (0.6 - t) / 0.1)
            0.3 * env * (sin(2 * PI * 110 * t) + 0.5 * sin(2 * PI * 220 * t + sin(2 * PI * 7 * t)) + 0.3 * sin(2 * PI * 330 * t))
        })
    }

    private fun rain(): ShortArray {
        val n = (rate * 0.8).toInt()
        val rnd = Random(8)
        val s = DoubleArray(n)
        repeat(30) {
            val start = rnd.nextInt(n - 2000)
            val f = rnd.nextDouble(1500.0, 3500.0)
            for (k in 0 until 1800) s[start + k] += 0.12 * sin(2 * PI * f * k / rate) * exp(-k / 300.0)
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
