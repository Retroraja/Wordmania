package com.retroraja.wordnoid.data

import com.retroraja.wordnoid.core.Booster
import java.time.LocalDate
import kotlin.random.Random

/** Picking prize boosters, daily gifts and the shop's booster of the day. */
object Rewards {
    /** Strong boosters (Hint, Magic Swap, +3 Moves, Scanner) are 4 times rarer than the rest. */
    fun randomBooster(unlockedLevel: Int, random: Random = Random): Booster {
        val pool = Booster.unlockedAt(unlockedLevel).ifEmpty { listOf(Booster.HAMMER) }
        val weights = pool.map { if (it.strong) 1 else 4 }
        var r = random.nextInt(weights.sum())
        for ((i, b) in pool.withIndex()) {
            if (r < weights[i]) return b
            r -= weights[i]
        }
        return pool.last()
    }

    /** Three different boosters for the mystery cards. */
    fun mysteryCards(unlockedLevel: Int, random: Random = Random): List<Booster> {
        val out = LinkedHashSet<Booster>()
        var guard = 0
        while (out.size < 3 && guard++ < 100) out += randomBooster(unlockedLevel, random)
        while (out.size < 3) out += Booster.HAMMER
        return out.toList()
    }

    data class Gift(val coins: Int, val boosters: Int)

    val DAILY = listOf(Gift(20, 0), Gift(30, 0), Gift(40, 1), Gift(50, 0), Gift(60, 1), Gift(80, 1), Gift(100, 2))

    fun boosterOfTheDay(unlockedLevel: Int): Booster {
        val pool = Booster.unlockedAt(unlockedLevel).ifEmpty { listOf(Booster.HAMMER) }
        return pool[(LocalDate.now().toEpochDay() % pool.size).toInt()]
    }

    fun price(b: Booster, unlockedLevel: Int): Int = if (b == boosterOfTheDay(unlockedLevel)) b.price / 2 else b.price
}
