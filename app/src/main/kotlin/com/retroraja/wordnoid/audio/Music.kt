package com.retroraja.wordnoid.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Background music, composed by code when the app starts (no music files, so no licence issues).
 * HOME: bright and bouncy. MAP: gentle walk. LEVEL: soft and soothing, so it doesn't distract.
 * Each piece loops seamlessly.
 */
class Music {
    enum class Piece { HOME, MAP, LEVEL }

    private val rate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tracks = HashMap<Piece, AudioTrack>()
    private var current: Piece? = null
    private var wanted: Piece? = null
    private var fadeJob: Job? = null
    /** True while the app is in the background (home button, screen off, an ad): nothing may start playing. */
    @Volatile private var paused = false
    var enabled = true
        set(value) {
            field = value
            if (!value) stopNow() else if (!paused) wanted?.let { w -> current = null; play(w) }
        }

    init {
        scope.launch {
            // The piece most likely needed first is made first.
            for (p in listOf(Piece.HOME, Piece.MAP, Piece.LEVEL)) {
                val data = compose(p)
                val t = runCatching { makeTrack(data) }.getOrNull() ?: continue
                synchronized(tracks) { tracks[p] = t }
                if (wanted == p && current != p && !paused) launch(Dispatchers.Main) { play(p) }
            }
        }
    }

    fun play(piece: Piece) {
        wanted = piece
        if (!enabled || paused || current == piece) return
        val next = synchronized(tracks) { tracks[piece] } ?: return
        val old = current?.let { synchronized(tracks) { tracks[it] } }
        current = piece
        fadeJob?.cancel()
        fadeJob = scope.launch {
            if (old != null) {
                for (i in 10 downTo 0) { runCatching { old.setVolume(VOLUME * i / 10f) }; delay(40) }
                runCatching { old.pause() }
            }
            runCatching {
                next.setVolume(0f)
                next.play()
            }
            for (i in 0..10) { runCatching { next.setVolume(VOLUME * i / 10f) }; delay(60) }
            // Went to the background during the fade: make sure it is silent.
            if (paused) stopNow()
        }
    }

    /** The app went to the background: stop everything, and nothing starts again until [resume]. */
    fun pauseAll() { paused = true; stopNow() }

    /** Back in the app: the wanted piece starts once (no double music: every other track is paused first). */
    fun resume() {
        paused = false
        val w = wanted ?: return
        stopNow()
        play(w)
    }

    private fun stopNow() {
        fadeJob?.cancel()
        synchronized(tracks) { tracks.values.forEach { runCatching { it.pause() } } }
        current = null
    }

    fun release() {
        stopNow()
        synchronized(tracks) { tracks.values.forEach { runCatching { it.release() } }; tracks.clear() }
    }

    private fun makeTrack(data: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        track.write(data, 0, data.size)
        track.setLoopPoints(0, data.size, -1)
        return track
    }

    // ---------------------------------------------------------------- the composer

    private class Song(val bpm: Double, val chords: List<IntArray>, val barsPerChord: Int, val repeats: Int)

    private fun midiFreq(n: Int): Double = 440.0 * 2.0.pow((n - 69) / 12.0)

    private fun compose(piece: Piece): ShortArray {
        // Chords as MIDI notes (root position, middle register).
        val song = when (piece) {
            Piece.HOME -> Song(108.0, listOf(intArrayOf(60, 64, 67), intArrayOf(57, 60, 64), intArrayOf(53, 57, 60), intArrayOf(55, 59, 62)), 1, 2)
            Piece.MAP -> Song(92.0, listOf(intArrayOf(53, 57, 60), intArrayOf(50, 53, 57), intArrayOf(58, 62, 65), intArrayOf(48, 52, 55)), 1, 2)
            Piece.LEVEL -> Song(70.0, listOf(intArrayOf(60, 64, 67, 71), intArrayOf(57, 60, 64, 67), intArrayOf(53, 57, 60, 64), intArrayOf(55, 59, 62, 69)), 2, 1)
        }
        val beat = 60.0 / song.bpm
        val bar = beat * 4
        val bars = song.chords.size * song.barsPerChord * song.repeats
        val total = (bars * bar * rate).toInt()
        val buf = DoubleArray(total)
        val rnd = Random(piece.ordinal * 31 + 7)
        val pentatonic = intArrayOf(0, 2, 4, 7, 9)

        for (barIndex in 0 until bars) {
            val chord = song.chords[(barIndex / song.barsPerChord) % song.chords.size]
            val t0 = barIndex * bar
            val variation = barIndex >= bars / 2
            when (piece) {
                Piece.HOME -> {
                    // Bouncy marimba arpeggio in eighths, bass on beats 1 and 3, soft kick and shaker.
                    for (i in 0 until 8) {
                        val n = chord[i % chord.size] + 12 + (if (i >= 4) 12 * (i % 2) else 0)
                        marimba(buf, t0 + i * beat / 2, midiFreq(n), 0.16)
                        shaker(buf, t0 + i * beat / 2 + beat / 4, 0.035, rnd)
                    }
                    bass(buf, t0, midiFreq(chord[0] - 24), beat * 1.8, 0.26)
                    bass(buf, t0 + beat * 2, midiFreq(chord[0] - 24), beat * 1.8, 0.22)
                    kick(buf, t0, 0.32); kick(buf, t0 + beat * 2, 0.26)
                    pad(buf, t0, bar, chord.map { midiFreq(it) }, 0.05)
                    // A happy little tune on top.
                    var note = chord[0] + 24
                    for (i in 0 until 4) {
                        if (rnd.nextDouble() < (if (variation) 0.85 else 0.65)) {
                            val step = pentatonic[rnd.nextInt(pentatonic.size)]
                            note = 72 + step + (if (rnd.nextBoolean()) 12 else 0)
                            bell(buf, t0 + i * beat + (if (rnd.nextDouble() < 0.3) beat / 2 else 0.0), midiFreq(note), 0.09)
                        }
                    }
                }
                Piece.MAP -> {
                    for (i in 0 until 8) {
                        if (i % 2 == 0 || rnd.nextDouble() < 0.5) marimba(buf, t0 + i * beat / 2, midiFreq(chord[i % chord.size] + 12), 0.13)
                    }
                    bass(buf, t0, midiFreq(chord[0] - 12), beat * 3.5, 0.2)
                    pad(buf, t0, bar, chord.map { midiFreq(it) }, 0.07)
                    for (i in 0 until 4) shaker(buf, t0 + i * beat + beat / 2, 0.02, rnd)
                    if (variation) for (i in 0 until 2) bell(buf, t0 + i * beat * 2 + beat, midiFreq(77 + pentatonic[rnd.nextInt(5)]), 0.07)
                }
                Piece.LEVEL -> {
                    // Calm: long warm pad, slow bass, a few soft marimba notes and gentle bells.
                    pad(buf, t0, bar, chord.map { midiFreq(it) }, 0.09)
                    if (barIndex % song.barsPerChord == 0) bass(buf, t0, midiFreq(chord[0] - 12), bar * song.barsPerChord * 0.9, 0.18)
                    val slots = listOf(0.0, 1.5, 2.0, 3.0)
                    for (s in slots) if (rnd.nextDouble() < 0.6) {
                        marimba(buf, t0 + s * beat, midiFreq(chord[rnd.nextInt(chord.size)] + 12), 0.08)
                    }
                    if (rnd.nextDouble() < 0.5) bell(buf, t0 + beat * (1 + rnd.nextInt(3)), midiFreq(84 + pentatonic[rnd.nextInt(5)]), 0.045)
                }
            }
        }
        // A gentle echo, wrapped around so the loop has no seam.
        val d = (beat * 0.75 * rate).toInt()
        val wet = DoubleArray(total)
        for (pass in 0 until 2) for (i in 0 until total) {
            val j = (i - d + total) % total
            wet[i] = buf[i] + (buf[j] + wet[j]) * 0.28
        }
        // Soft limiter.
        var peak = 0.0
        for (v in wet) peak = maxOf(peak, kotlin.math.abs(v))
        val gain = if (peak > 0.95) 0.95 / peak else 1.0
        return ShortArray(total) { (kotlin.math.tanh(wet[it] * gain * 1.1) * 30000).toInt().toShort() }
    }

    /** Adds a sound made by [f] (seconds since start -> sample) at [start] seconds, wrapping around the loop. */
    private inline fun add(buf: DoubleArray, start: Double, seconds: Double, f: (Double) -> Double) {
        val s0 = (start * rate).toInt()
        val n = (seconds * rate).toInt()
        for (i in 0 until n) {
            val idx = (s0 + i) % buf.size
            buf[idx] += f(i.toDouble() / rate)
        }
    }

    private fun marimba(buf: DoubleArray, start: Double, freq: Double, vol: Double) = add(buf, start, 0.9) { t ->
        val env = minOf(1.0, t / 0.003) * exp(-t * 5.5)
        vol * env * (sin(2 * PI * freq * t) + 0.35 * sin(2 * PI * freq * 4 * t) * exp(-t * 18))
    }

    private fun bell(buf: DoubleArray, start: Double, freq: Double, vol: Double) = add(buf, start, 1.4) { t ->
        val env = minOf(1.0, t / 0.004) * exp(-t * 3.0)
        vol * env * (sin(2 * PI * freq * t) + 0.4 * sin(2 * PI * freq * 2.76 * t) * exp(-t * 6) + 0.2 * sin(2 * PI * freq * 5.4 * t) * exp(-t * 10))
    }

    private fun bass(buf: DoubleArray, start: Double, freq: Double, seconds: Double, vol: Double) = add(buf, start, seconds) { t ->
        val env = minOf(1.0, t / 0.01) * minOf(1.0, (seconds - t) / 0.08).coerceAtLeast(0.0) * exp(-t * 0.8)
        vol * env * (sin(2 * PI * freq * t) + 0.25 * sin(2 * PI * freq * 2 * t))
    }

    private fun pad(buf: DoubleArray, start: Double, seconds: Double, freqs: List<Double>, vol: Double) = add(buf, start, seconds * 1.15) { t ->
        val env = minOf(1.0, t / 0.6) * minOf(1.0, (seconds * 1.15 - t) / 0.5).coerceAtLeast(0.0)
        var v = 0.0
        for (f in freqs) {
            val vib = 1 + 0.002 * sin(2 * PI * 4.5 * t)
            v += sin(2 * PI * f * vib * t) + 0.6 * sin(2 * PI * f * 1.004 * t) + 0.15 * sin(2 * PI * f * 2 * t)
        }
        vol * env * v / freqs.size
    }

    private fun kick(buf: DoubleArray, start: Double, vol: Double) {
        var phase = 0.0
        add(buf, start, 0.25) { t ->
            val f = 50 + 90 * exp(-t * 30)
            phase += 2 * PI * f / rate
            vol * exp(-t * 14) * sin(phase)
        }
    }

    private fun shaker(buf: DoubleArray, start: Double, vol: Double, rnd: Random) {
        var last = 0.0
        add(buf, start, 0.06) { t ->
            val n = rnd.nextDouble(-1.0, 1.0)
            val hp = n - last
            last = n
            vol * hp * exp(-t * 70)
        }
    }

    companion object {
        const val VOLUME = 0.55f
    }
}
