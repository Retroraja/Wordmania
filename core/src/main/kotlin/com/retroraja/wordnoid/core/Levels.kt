package com.retroraja.wordnoid.core

/** The game's levels. The starting letters are made by the level maker (see levelmaker/). */
object Levels {
    val all: List<Level> = GeneratedLevels.all
    fun get(number: Int): Level? = all.getOrNull(number - 1)
}
