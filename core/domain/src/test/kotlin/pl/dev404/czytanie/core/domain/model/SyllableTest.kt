package pl.dev404.czytanie.core.domain.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SyllableTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        "ma, audio/syllables/ma.ogg",
        "Sza, audio/syllables/sza.ogg",
        "żą, audio/syllables/z2a1.ogg",
        "źró, audio/syllables/z1ro1.ogg",
        "się, audio/syllables/sie1.ogg",
        "łeś, audio/syllables/l1es1.ogg",
        "ćń, audio/syllables/c1n1.ogg",
        "w do, audio/syllables/wdo.ogg",
    )
    fun `recording path is lowercase ascii`(
        syllable: String,
        expected: String,
    ) {
        Syllable.audioPathOf(syllable) shouldBe expected
        Syllable(syllable, 0, syllable.length).audio shouldBe expected
    }
}
