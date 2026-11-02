package com.retroraja.wordnoid.core

/**
 * Theme words for theme goals ("Make 2 food words"). Only general, simple themes with short everyday words (3 to 5 letters).
 * Names are not normal words: they count only in a Names level (see [isNames]).
 */
object Themes {
    data class Theme(val name: String, val emoji: String, val words: Set<String>) {
        /** Names are only accepted as words while a Names goal is on. */
        val isNames: Boolean get() = name == NAMES
        /** "food words", "names", "numbers" for goal text. */
        val goalNoun: String get() = when (name) {
            NAMES -> "names"
            NUMBERS -> "numbers"
            COLOURS -> "colours"
            else -> name.lowercase().removeSuffix("s") + " words"
        }
    }

    const val NAMES = "Names"
    const val NUMBERS = "Numbers"
    const val COLOURS = "Colours"

    private fun t(name: String, emoji: String, words: String) =
        Theme(name, emoji, words.split(' ').filter { it.isNotBlank() }.map { it.uppercase() }.toSet())

    val all: List<Theme> = listOf(
        t("Food", "🍕", "egg ham jam pie bun tea rice cake corn bean meat fish soup salt milk nut oat bread toast pasta pizza honey sugar candy salad cream chips sauce juice snack"),
        t("Fruits", "🍓", "fig pear plum lime kiwi apple lemon mango melon grape peach berry date"),
        t("Animals", "🐾", "cat dog cow pig rat bat fox ant ape bee hen owl lion bear deer goat frog wolf duck lamb seal crab fish bird mouse horse camel zebra tiger sheep panda puppy snake shark whale"),
        t(COLOURS, "🎨", "red tan blue pink gold gray grey navy green white black brown cream"),
        t("Body", "💪", "arm leg ear eye toe lip hip rib jaw hand foot nose head hair neck knee chin skin back nail heel cheek mouth tooth thumb wrist ankle elbow heart chest"),
        t("Home", "🏠", "bed mat cup pan pot jar mug rug tap fan bin sofa lamp door wall roof room sink desk bath fork dish bowl oven stool chair table couch shelf clock towel"),
        t("Nature", "🌳", "sun sky sea fog dew mud ice oak log tree leaf rain snow wind lake hill rock sand seed root rose pond star moon cloud storm river grass stone plant"),
        t("Family", "👪", "mom mum dad son aunt baby twin niece uncle"),
        t(NUMBERS, "🔢", "one two six ten four five nine three seven eight zero"),
        t(NAMES, "🙂", "sam tom ben max leo dan tim jim bob ian jay ray joe ted ali raj ravi amit adam jack john mark paul luke ryan sean noah kyle ella mia zoe amy ann eva ivy kim liz meg sue lily rose emma anna jane kate lucy sara ruby lisa mary maya priya"),
    )

    fun byName(name: String): Theme? = all.firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** Theme words a level can use: dictionary words (names are taken as they are). */
    fun cleaned(theme: Theme, dict: Dictionary): Set<String> =
        if (theme.isNames) theme.words else theme.words.filter { dict.isWord(it.lowercase()) }.toSet()
}
