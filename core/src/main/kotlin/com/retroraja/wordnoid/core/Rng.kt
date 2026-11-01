package com.retroraja.wordnoid.core

/**
 * A small random number generator (SplitMix64) whose state can be copied.
 * The same seed always gives the same numbers on every phone, so levels play the same for everyone.
 */
class Rng(private var state: Long) {
    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** A number from 0 until [bound]. */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }

    fun nextDouble(): Double = (nextLong() ushr 11) * (1.0 / (1L shl 53))

    fun copy(): Rng = Rng(state)
}
