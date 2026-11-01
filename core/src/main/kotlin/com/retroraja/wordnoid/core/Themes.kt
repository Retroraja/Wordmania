package com.retroraja.wordnoid.core

/** Theme words for theme levels ("Find 3 animals"). Theme words give double coins and points. */
object Themes {
    data class Theme(val name: String, val emoji: String, val words: Set<String>)

    private fun t(name: String, emoji: String, words: String) =
        Theme(name, emoji, words.split(' ').filter { it.isNotBlank() }.map { it.uppercase() }.toSet())

    val all: List<Theme> = listOf(
        t("Animals", "🐾", "cat dog cow pig rat bat fox ant ape bee elk hen ram yak emu owl eel ox lion bear deer goat mole mule frog wolf duck lamb seal hare toad crab mouse horse camel zebra tiger sheep llama moose otter panda puppy kitten rabbit donkey monkey turtle beaver badger"),
        t("Birds", "🐦", "owl hen emu jay tit crow dove duck gull hawk kite lark rook swan wren crane eagle finch goose heron raven robin stork swift parrot pigeon puffin falcon magpie oriole thrush turkey"),
        t("Food", "🍎", "egg ham jam pie bun tea rice cake corn bean meat fish soup salt milk pear plum lime nut fig oat bread toast pasta pizza apple lemon mango melon grape honey sugar candy salad bacon cream cheese butter cookie orange carrot potato onion tomato"),
        t("Nature", "🌿", "sun sky sea fog dew mud ice oak elm log bud ivy tree leaf rain snow wind lake hill rock sand moss seed root rose fern pond star moon cloud storm river ocean grass stone plant flower forest valley desert island meadow"),
        t("Colours", "🎨", "red tan blue pink gold gray grey teal rose ruby navy jade lime cyan green white black brown amber ivory lemon olive coral peach plum mauve beige cream silver violet purple orange yellow indigo maroon"),
        t("Body", "💪", "arm leg ear eye toe lip hip rib jaw gum hand foot nose head hair neck knee chin skin back nail palm heel shin brow cheek mouth tooth teeth thumb wrist ankle elbow heart chest spine"),
        t("Home", "🏠", "bed mat cup pan pot jar mug rug tap fan bin sofa lamp door wall roof room sink desk bath fork dish bowl oven vase stool chair table couch shelf clock towel pillow carpet window mirror"),
        t("Sports", "⚽", "run ski bat net gym win cup golf polo swim race kick goal ball team dive jump yoga judo shot pass score match coach rugby skate track relay boxing tennis soccer hockey cycling"),
        t("Jobs", "👷", "vet cop chef cook maid monk poet nurse judge pilot actor baker guard clerk coach diver farmer doctor dentist driver singer tailor waiter barber banker lawyer artist teacher"),
        t("Weather", "⛅", "sun fog dew ice hot wet dry hail rain snow wind mist gale cold warm heat cool damp sunny windy storm cloud frost sleet foggy misty rainy snowy chilly breeze thunder"),
    )

    fun byName(name: String): Theme? = all.firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** Keeps only the theme words the dictionary accepts. */
    fun cleaned(theme: Theme, dict: Dictionary): Set<String> = theme.words.filter { dict.isWord(it.lowercase()) }.toSet()
}
