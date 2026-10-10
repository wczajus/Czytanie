package pl.dev404.czytanie.core.domain.pacing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PacingConfigTest {

    @Test
    fun `fast tempo of a tenth of a second is allowed`() {
        PacingConfig(tempoMillis = 100).tempoMillis shouldBe 100
    }

    @Test
    fun `tempo outside the range is rejected`() {
        shouldThrow<IllegalArgumentException> { PacingConfig(tempoMillis = 50) }
        shouldThrow<IllegalArgumentException> { PacingConfig(tempoMillis = 1600) }
    }
}
