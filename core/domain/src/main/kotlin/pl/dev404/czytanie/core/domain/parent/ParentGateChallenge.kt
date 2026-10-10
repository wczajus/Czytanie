package pl.dev404.czytanie.core.domain.parent

import kotlin.random.Random

/**
 * The parental gate: a multiplication a 5–7-year-old cannot solve, answered on a number pad.
 * Google Play's Families policy asks for such a gate in front of settings and the parent's area.
 */
data class ParentGateChallenge(
    val left: Int,
    val right: Int,
) {
    val answer: Int get() = left * right

    fun isCorrect(input: String): Boolean = input.toIntOrNull() == answer

    companion object {
        /** Factors from 3 to 9: the product always has two digits and is not a guess like 1 × n. */
        val FACTORS = 3..9

        fun random(random: Random = Random.Default) = ParentGateChallenge(
            left = FACTORS.random(random),
            right = FACTORS.random(random),
        )
    }
}
