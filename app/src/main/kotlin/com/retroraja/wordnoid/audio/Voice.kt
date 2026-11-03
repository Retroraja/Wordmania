package com.retroraja.wordnoid.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Short cheerful voice lines ("Wordnoooid!", "Awesome!") with the phone's own text-to-speech,
 * so there are no voice files. A high, bouncy voice. Silent if the phone has no speech engine.
 */
class Voice(context: Context) {
    private var ready = false
    var enabled = true
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            runCatching {
                tts.language = Locale.US
                tts.setPitch(1.45f)
                tts.setSpeechRate(0.95f)
                ready = true
            }
        }
    }

    fun say(text: String) {
        if (!enabled || !ready) return
        runCatching { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text) }
    }

    fun release() { runCatching { tts.stop(); tts.shutdown() } }

    companion object {
        /** The game's name, said long and happy. */
        const val WORDNOID = "Word, nooooid!"
    }
}
