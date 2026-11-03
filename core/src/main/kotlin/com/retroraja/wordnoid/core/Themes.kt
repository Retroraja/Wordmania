package com.retroraja.wordnoid.core

/**
 * Theme words for theme goals ("Make 2 food words"). Each list holds every fitting word in the game's word list,
 * plurals included (EAR, EARS, NOSE, NOSES). LevelsTest checks every word is in the word list.
 * Names are not normal words: they count only in a Names level (see [isNames]).
 */
object Themes {
    data class Theme(
        val name: String,
        val emoji: String,
        val words: Set<String>,
        /** Plain words for the level start card, e.g. "Things in a house". */
        val about: String = name,
        /** 3 easy examples shown on the level start card and the theme chip. */
        val examples: List<String> = emptyList(),
    ) {
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

    private fun t(name: String, emoji: String, about: String, examples: String, words: String) =
        Theme(
            name, emoji, words.split(' ').filter { it.isNotBlank() }.map { it.uppercase() }.toSet(),
            about, examples.split(' '),
        )

    val all: List<Theme> = listOf(
        t("Food", "🍕", "Things you eat or drink", "EGG CAKE SOUP",
            "apple banana bananas bean beans beef biscuit biscuits bread bun butter cabbage cake cakes candy carrot carrots cheese chicken chickens chips coffee cookie cookies corn cream curry dinner drink drinks egg eggs fish fishes flour fruit fruits grain ham honey jam jams jellies jelly juice lamb lunch meal meals meat milk nut nuts oat oil onion pasta pea pie pizza pizzas pork potato potatoes pudding rice roll rolls salad salt sandwich sauce snack soup sugar sweet tea toast tomato tomatoes waffle water"),
        t("Fruits", "🍓", "Fruits", "APPLE PEAR LIME",
            "apple banana bananas berry date dates fig fruit fruits grape kiwi lemon lime mango melon orange peach pear plum"),
        t("Animals", "🐾", "Animals, birds and bugs", "CAT DOG OWL",
            "animal animals ant ape bat bear bears bee bird birds bug bugs bull camel cat cats chicken chickens cod cow cows crab cub deer dog dogs duck ducks eagle elephant fish fishes flies fly fox frog frogs goat hen horse horses insect insects kid kids lamb lion mice mole monkey monkeys mouse owl panda parrot penguin pet pig pigeon pigs pup puppy rabbit rabbits rat rats seal seals shark sheep snail snake tiger toad whale whales wolf worm worms zebra"),
        t(COLOURS, "🎨", "Colours", "RED BLUE PINK",
            "amber black blacks blue blues brown cream gold gray green grey lime navy orange pink purple red scarlet silver tan white whites yellow"),
        t("Body", "💪", "Parts of your body", "ARM EAR NOSE",
            "ankle arm arms back backs beard beards bodies body bone bones brain brains cheek chest chin ear ears elbow eye eyes face faces feet finger fingers foot gum hair hairs hand hands head heads heart hearts heel heels hip jaw kidney knee knees leg legs lip lips liver lung lungs mouth muscle muscles nail nails neck nose noses rib shoulder skin skull stomach teeth throat throats thumb toe toes tongue tooth vein wrist"),
        t("Home", "🏠", "Things in a house", "BED DOOR LAMP",
            "basket bath baths bed bedroom beds bench bin blanket bowl brush bucket carpet chair chairs chest clock clocks closet computer cot couch cup cupboard cups curtain curtains desk dish dishes door doors fan fans fence flat floor floors fork frame frames garage garden gardens gate gates glass glasses hall halls home homes house houses hut jar kettle key keys kitchen knife lamp light lights lock locks mat mirror mirrors mop mug mugs oven pan phone phones plate plates radio radios roof room rooms rug sheet sheets shelf shelves shower showers sink sinks soap sofa stair stairs step steps stool table tables tap toilet toilets towel wall walls window windows"),
        t("Nature", "🌳", "Things in nature and the sky", "SUN TREE RAIN",
            "bark barks beach branch branches breeze bush cave cliff cloud clouds desert deserts dew dirt earth field fields flower flowers fog forest forests grass hail hay hill hills ice island islands jungle lake leaf leaves log logs mist mists moon moons mountain mud nest nests oak ocean planet planets plant plants pond rain rainbow rains river rivers rock rocks root roots rose sand sea seed shade shades sky snow soil star stars stem stems stone stones storm storms stream streams sun sunrise sunshine tree trees valley wave waves wind winds wood woods"),
        t("Family", "👪", "People in a family", "MOM DAD SON",
            "aunt babies baby brother brothers child children cousin dad daughter families family father fathers husband infant kid kids mom mother mothers mum mummy niece parent parents sister son sons twin twins uncle wife wives"),
        t(NUMBERS, "🔢", "Number words", "ONE TWO TEN",
            "dozen dozens eight eighteen eleven fifteen fifty five forty four fourteen hundred hundreds million millions nine one ones seven six sixteen sixty ten tens thirty thousand three twelve twenty two zero"),
        t(NAMES, "🙂", "Names of people", "SAM MIA LEO", "sam tom ben max leo dan tim jim bob ian jay ray joe ted ali raj ravi amit adam jack john mark paul luke ryan sean noah kyle ella mia zoe amy ann eva ivy kim liz meg sue lily rose emma anna jane kate lucy sara ruby lisa mary maya priya"),
    )

    fun byName(name: String): Theme? = all.firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** Theme words a level can use: dictionary words (names are taken as they are). */
    fun cleaned(theme: Theme, dict: Dictionary): Set<String> =
        if (theme.isNames) theme.words else theme.words.filter { dict.isWord(it.lowercase()) }.toSet()
}
