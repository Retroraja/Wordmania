package com.retroraja.wordnoid.data

import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Update 1.1 "Suggest a word": players send words they think should count. Claude reviews the list before each update.
 * Only the word is sent (no name, no id). Light limits: letters only, 3-8 letters, 3 a day per phone.
 * Words wait on the phone until they can be sent (no internet, or before the online list is set up).
 */
object Suggestions {
    const val PER_DAY = 3

    /**
     * Firebase project for the online list (Firestore, free plan). Empty = not set up yet: words are kept on the phone
     * and go out by themselves after the update that fills these in.
     */
    const val FIRESTORE_PROJECT = ""
    const val FIRESTORE_KEY = ""

    /** Why [text] can't be sent, or null when it's fine. */
    fun problem(text: String, p: Progress): String? {
        val w = text.trim()
        return when {
            p.suggestionsToday >= PER_DAY -> "That's 3 words today. Send more tomorrow!"
            w.length !in 3..8 -> "Words need 3 to 8 letters."
            !w.all { it in 'a'..'z' || it in 'A'..'Z' } -> "Letters only, please."
            else -> null
        }
    }

    /** Keeps the word and tries to send everything waiting. */
    fun send(text: String, p: Progress) {
        p.suggestionSent()
        p.pendingSuggestions = p.pendingSuggestions + text.trim().uppercase()
        flush(p)
    }

    /** Sends the waiting words (in the background). Each word that goes out leaves the queue. */
    fun flush(p: Progress) {
        if (FIRESTORE_PROJECT.isEmpty()) return
        val words = p.pendingSuggestions
        if (words.isEmpty()) return
        thread(isDaemon = true) {
            val sent = words.filter { post(it) }.toSet()
            if (sent.isNotEmpty()) android.os.Handler(android.os.Looper.getMainLooper()).post { p.pendingSuggestions = p.pendingSuggestions - sent }
        }
    }

    private fun post(word: String): Boolean = runCatching {
        val url = URL("https://firestore.googleapis.com/v1/projects/$FIRESTORE_PROJECT/databases/(default)/documents/suggestions?key=$FIRESTORE_KEY")
        val c = url.openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 8000; c.readTimeout = 8000
        c.setRequestProperty("Content-Type", "application/json")
        val body = """{"fields":{"word":{"stringValue":"$word"},"day":{"integerValue":"${java.time.LocalDate.now().toEpochDay()}"}}}"""
        c.outputStream.use { it.write(body.toByteArray()) }
        val ok = c.responseCode in 200..299
        c.disconnect(); ok
    }.getOrDefault(false)
}
