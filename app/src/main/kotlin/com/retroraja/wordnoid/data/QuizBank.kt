package com.retroraja.wordnoid.data

import com.retroraja.wordnoid.core.Dictionary
import com.retroraja.wordnoid.core.Themes
import kotlin.random.Random

/**
 * Quiz questions in 14 kinds. Questions are made from word lists, so each kind has plenty of different questions
 * (types can repeat, the questions don't).
 */
object QuizBank {
    enum class Type(val title: String) {
        ODD_ONE_OUT("Odd one out"), EMOJI_MATCH("Emoji match"), MISSING_LETTER("Missing letter"), UNSCRAMBLE("Unscramble"),
        OPPOSITES("Opposites"), SAME_MEANING("Same meaning"), HIDDEN_WORD("Hidden word"), FAST_TAP("Fast tap"),
        EMOJI_RIDDLE("Emoji riddle"), RHYME("Rhyme time"), WORD_LADDER("Word ladder"), SPELLING("Spelling"),
        MEMORY_FLASH("Memory flash"), WORD_HUNT("Word hunt"),
    }

    /**
     * One question. [answer] is the index of the right option. [flash]: words shown first for a few seconds (memory).
     * [seconds]: a time limit. [multi]: Word hunt, tap every real word (answers in [correctSet]).
     */
    data class Question(
        val type: Type,
        val prompt: String,
        val options: List<String>,
        val answer: Int,
        val flash: List<String> = emptyList(),
        val seconds: Int = 0,
        val multi: Boolean = false,
        val correctSet: Set<Int> = emptySet(),
    )

    private val EMOJI = listOf(
        "🍎" to "APPLE", "🐶" to "DOG", "🐱" to "CAT", "🌞" to "SUN", "🌙" to "MOON", "⭐" to "STAR", "🐟" to "FISH", "🌳" to "TREE",
        "🏠" to "HOUSE", "🚗" to "CAR", "🍌" to "BANANA", "🎂" to "CAKE", "🐝" to "BEE", "🦁" to "LION", "🐸" to "FROG", "🍕" to "PIZZA",
        "🌧️" to "RAIN", "❄️" to "SNOW", "🔥" to "FIRE", "🌹" to "ROSE", "🐮" to "COW", "🐷" to "PIG", "🦆" to "DUCK", "🐻" to "BEAR",
        "🍋" to "LEMON", "🍇" to "GRAPE", "🥚" to "EGG", "🥛" to "MILK", "🎈" to "BALLOON", "📚" to "BOOK", "⚽" to "BALL", "🚀" to "ROCKET",
        "🐢" to "TURTLE", "🦋" to "BUTTERFLY", "🌈" to "RAINBOW", "🍪" to "COOKIE", "👑" to "CROWN", "🔑" to "KEY", "⏰" to "CLOCK", "🛏️" to "BED",
    )

    private val OPPOSITES = listOf(
        "HOT" to "COLD", "BIG" to "SMALL", "UP" to "DOWN", "FAST" to "SLOW", "DAY" to "NIGHT", "HAPPY" to "SAD", "OPEN" to "CLOSED",
        "WET" to "DRY", "TALL" to "SHORT", "FULL" to "EMPTY", "YES" to "NO", "IN" to "OUT", "LIGHT" to "DARK", "OLD" to "NEW",
        "HARD" to "SOFT", "LOUD" to "QUIET", "NEAR" to "FAR", "RICH" to "POOR", "EARLY" to "LATE", "WIN" to "LOSE", "PUSH" to "PULL",
        "LEFT" to "RIGHT", "TOP" to "BOTTOM", "FIRST" to "LAST", "CLEAN" to "DIRTY", "STRONG" to "WEAK", "ASLEEP" to "AWAKE",
        "SWEET" to "SOUR", "THICK" to "THIN", "BUY" to "SELL", "GIVE" to "TAKE", "START" to "STOP", "ALWAYS" to "NEVER", "BEFORE" to "AFTER",
    )

    private val SYNONYMS = listOf(
        "BIG" to "LARGE", "SMALL" to "TINY", "HAPPY" to "GLAD", "FAST" to "QUICK", "SHUT" to "CLOSE", "BEGIN" to "START", "END" to "FINISH",
        "SICK" to "ILL", "SAD" to "UNHAPPY", "GIFT" to "PRESENT", "ROCK" to "STONE", "SHOUT" to "YELL", "LOOK" to "SEE", "SPEAK" to "TALK",
        "ANGRY" to "MAD", "SMART" to "CLEVER", "JUMP" to "LEAP", "PRETTY" to "LOVELY", "RICH" to "WEALTHY", "SCARED" to "AFRAID",
        "CHILD" to "KID", "HOME" to "HOUSE", "NEAR" to "CLOSE", "CORRECT" to "RIGHT", "REPLY" to "ANSWER", "STORE" to "SHOP",
        "MIX" to "STIR", "SHINY" to "BRIGHT", "COLD" to "CHILLY", "FUNNY" to "SILLY", "LITTLE" to "SMALL", "STRANGE" to "ODD",
    )

    private val RIDDLES = listOf(
        "☀️ + 🌻" to "SUNFLOWER", "🌧️ + 🌈" to "RAINBOW", "⭐ + 🐟" to "STARFISH", "🔥 + 👨‍🚒" to "FIREMAN", "🐝 + 🏠" to "BEEHIVE",
        "🦶 + ⚽" to "FOOTBALL", "🌙 + 💡" to "MOONLIGHT", "❄️ + ⛄" to "SNOWMAN", "🍓 + 🍰" to "CAKE", "🧈 + 🦋" to "BUTTERFLY",
        "🎂 + 🎉" to "BIRTHDAY", "🌊 + 🐚" to "SEASHELL", "🍿 + 🎬" to "MOVIE", "☁️ + 💥" to "THUNDER", "🐄 + 👦" to "COWBOY",
        "🌳 + 🏠" to "TREEHOUSE", "🍫 + 🥛" to "CHOCOLATE", "🔑 + 🕳️" to "KEYHOLE", "👀 + 🔍" to "SEARCH", "🪐 + 🚀" to "SPACE",
        "🎵 + 🎤" to "SONG", "🌅 + 🌄" to "SUNRISE", "🐟 + 🎣" to "FISHING", "🦷 + 🪥" to "TOOTHBRUSH", "📮 + 👨" to "POSTMAN",
        "🍯 + 🌙" to "HONEYMOON", "🐞 + 🐞" to "LADYBUGS", "🎃 + 🌙" to "HALLOWEEN", "🌧️ + 🧥" to "RAINCOAT", "👣 + 🖨️" to "FOOTPRINT",
    )

    /** Everyday words for the word games (3-6 letters). */
    private val COMMON: List<String> by lazy {
        (Themes.all.filter { !it.isNames }.flatMap { it.words } + listOf(
            "BOOK", "PLAY", "JUMP", "SING", "READ", "BALL", "GAME", "SHIP", "BOAT", "KITE", "BELL", "RING", "SONG", "GIFT", "TOYS",
            "SMILE", "HAPPY", "WATER", "LIGHT", "MUSIC", "PAPER", "CHAIR", "TABLE", "HOUSE", "TRAIN", "PLANE", "BRAVE", "SMART", "CLOUD",
            "DREAM", "LUCKY", "MAGIC", "PARTY", "QUEEN", "KING", "CROWN", "TOWER", "BRIDGE", "GARDEN", "FLOWER", "WINDOW", "ROCKET",
            "PLANET", "SILVER", "YELLOW", "PURPLE", "ORANGE", "BASKET", "CANDLE", "PENCIL", "MONKEY", "RABBIT", "TURTLE", "DRAGON",
        )).filter { it.length in 3..6 }.distinct()
    }

    private val ALPHA = "ABCDEFGHIJKLMNOPRSTUW"

    fun make(type: Type, r: Random, dict: Dictionary?): Question {
        return when (type) {
            Type.ODD_ONE_OUT -> {
                val themes = Themes.all.filter { !it.isNames && it.words.size >= 6 }.shuffled(r)
                val a = themes[0]; val b = themes[1]
                val three = a.words.shuffled(r).take(3)
                val odd = b.words.first { it !in a.words && it !in three }
                choice(type, "Three are ${a.name.lowercase()} words. Which one does NOT belong?", odd, three, r)
            }
            Type.EMOJI_MATCH -> {
                val (e, w) = EMOJI.random(r)
                choice(type, "Which word matches $e ?", w, EMOJI.map { it.second }.filter { it != w }.shuffled(r).take(3), r)
            }
            Type.MISSING_LETTER -> {
                val w = COMMON.filter { it.length >= 4 }.random(r)
                val i = r.nextInt(w.length)
                val shown = w.substring(0, i) + "_" + w.substring(i + 1)
                val wrong = ALPHA.filter { it != w[i] && (dict == null || !dict.isWord((w.substring(0, i) + it + w.substring(i + 1)).lowercase())) }.toList().shuffled(r).take(3).map { "$it" }
                choice(type, "Which letter is missing?  $shown", "${w[i]}", wrong, r)
            }
            Type.UNSCRAMBLE -> {
                val w = COMMON.filter { it.length in 4..6 }.random(r)
                var s = w
                while (s == w) s = w.toList().shuffled(r).joinToString("")
                choice(type, "Unscramble:  $s", w, COMMON.filter { it.length == w.length && it != w }.shuffled(r).take(3), r)
            }
            Type.OPPOSITES -> {
                val (a, b) = OPPOSITES.random(r)
                choice(type, "What is the opposite of $a?", b, OPPOSITES.map { it.second }.filter { it != b }.shuffled(r).take(3), r)
            }
            Type.SAME_MEANING -> {
                val (a, b) = SYNONYMS.random(r)
                choice(type, "Which word means the same as $a?", b, SYNONYMS.map { it.second }.filter { it != b && it != a }.shuffled(r).take(3), r)
            }
            Type.HIDDEN_WORD -> {
                val pairs = COMMON.filter { it.length >= 5 }.mapNotNull { w ->
                    val subs = (0..w.length - 3).map { w.substring(it, it + 3) }.filter { s -> dict?.isWord(s.lowercase()) ?: (s in COMMON) }
                    if (subs.isEmpty()) null else w to subs.first()
                }
                val (w, s) = pairs.ifEmpty { listOf("PLANET" to "ANT") }.random(r)
                val wrong = COMMON.filter { it.length == 3 && !w.contains(it) }.shuffled(r).take(3)
                choice(type, "Which small word is hiding in $w?", s, wrong, r)
            }
            Type.FAST_TAP -> {
                val w = COMMON.random(r)
                val fakes = List(3) { fake(w, r, dict) }
                choice(type, "Quick! Tap the real word!", w, fakes, r).copy(seconds = 6)
            }
            Type.EMOJI_RIDDLE -> {
                val (e, w) = RIDDLES.random(r)
                choice(type, "Emoji riddle:  $e  = ?", w, RIDDLES.map { it.second }.filter { it != w }.shuffled(r).take(3), r)
            }
            Type.RHYME -> {
                val groups = COMMON.filter { it.length in 3..5 }.groupBy { it.takeLast(2) }.filter { it.value.size >= 2 }
                val g = groups.values.random(r)
                val (a, b) = g.shuffled(r).take(2)
                val wrong = COMMON.filter { it.takeLast(2) != a.takeLast(2) && it.length in 3..5 }.shuffled(r).take(3)
                choice(type, "Which word rhymes with $a?", b, wrong, r)
            }
            Type.WORD_LADDER -> {
                val pool = COMMON.filter { it.length in 3..4 }
                val pairs = pool.flatMap { a -> pool.filter { b -> b.length == a.length && b != a && a.indices.count { a[it] != b[it] } == 1 }.map { a to it } }
                val (a, b) = pairs.ifEmpty { listOf("CAT" to "COT") }.random(r)
                val wrong = pool.filter { it.length == a.length && a.indices.count { i -> a[i] != it[i] } >= 2 }.shuffled(r).take(3)
                choice(type, "Change just ONE letter of $a. Which word can you make?", b, wrong, r)
            }
            Type.SPELLING -> {
                val w = COMMON.filter { it.length >= 5 }.random(r)
                choice(type, "Which one is spelled right?", w, List(3) { fake(w, r, dict) }.distinct().let { l -> l + List(3 - l.size) { w.reversed() } }, r)
            }
            Type.MEMORY_FLASH -> {
                val shown = COMMON.shuffled(r).take(4)
                val right = shown.random(r)
                val wrong = COMMON.filter { it !in shown }.shuffled(r).take(3)
                choice(type, "Which word did you see?", right, wrong, r).copy(flash = shown)
            }
            Type.WORD_HUNT -> {
                val real = COMMON.shuffled(r).take(4)
                val fakes = real.map { fake(it, r, dict) }
                val all = (real + fakes).shuffled(r)
                Question(type, "Tap ALL the real words in 20 seconds!", all, -1, seconds = 20, multi = true, correctSet = all.indices.filter { all[it] in real }.toSet())
            }
        }
    }

    private fun choice(type: Type, prompt: String, right: String, wrong: List<String>, r: Random): Question {
        val all = (wrong.distinct().filter { it != right }.take(3) + right).shuffled(r)
        return Question(type, prompt, all, all.indexOf(right))
    }

    /** A believable fake: swap two letters or change a vowel, checked not to be a real word. */
    /** Fake words must never spell something rude (kids play this game). */
    private val RUDE = listOf("SEX", "SEXY", "ASS", "TIT", "FUK", "FUC", "CUM", "NUDE", "POO", "PEE", "DIK", "DIC", "COK", "PIS", "PORN", "BUT", "HEL", "DAM", "GOD", "KIL", "DIE", "GUN", "NAZ", "FAG", "GAY", "HOE", "SHT", "SHIT", "CRAP", "BOOB", "BUTT", "NIG")
    private fun rude(f: String) = RUDE.any { f.contains(it) }

    private fun fake(w: String, r: Random, dict: Dictionary?): String {
        repeat(20) {
            val c = w.toCharArray()
            if (r.nextBoolean() && w.length >= 3) {
                val i = r.nextInt(w.length - 1); val t = c[i]; c[i] = c[i + 1]; c[i + 1] = t
            } else {
                val vs = c.indices.filter { c[it] in "AEIOU" }
                if (vs.isNotEmpty()) { val i = vs.random(r); c[i] = "AEIOU".filter { it != c[i] }.random(r) }
            }
            val f = String(c)
            if (f != w && !rude(f) && (dict == null || !dict.isWord(f.lowercase()))) return f
        }
        return w + "E"
    }

    /** 3 questions of different kinds for a quiz. */
    fun quiz(seed: Long, dict: Dictionary?): List<Question> {
        val r = Random(seed)
        return Type.entries.shuffled(r).take(3).map { make(it, r, dict) }
    }
}
