package pl.dev404.czytanie.core.domain.parent

import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import org.junit.jupiter.api.Test

class ParentGateChallengeTest {
    @Test
    fun `only the exact product opens the gate`() {
        val challenge = ParentGateChallenge(7, 8)

        challenge.isCorrect("56") shouldBe true
        challenge.isCorrect("15") shouldBe false
        challenge.isCorrect("") shouldBe false
        challenge.isCorrect("5") shouldBe false
    }

    @Test
    fun `random challenges always need a two digit answer`() {
        val random = Random(42)
        repeat(200) {
            val challenge = ParentGateChallenge.random(random)
            challenge.left shouldBeInRange ParentGateChallenge.FACTORS
            challenge.right shouldBeInRange ParentGateChallenge.FACTORS
            challenge.answer shouldBeInRange 9..81
        }
    }
}
