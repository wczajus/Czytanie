package pl.dev404.czytanie.core.domain.pacing

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.dev404.czytanie.core.domain.model.Word

class PointingUnitTest {

    private val sentence = Word.fromAnnotation(
        id = "ala-ma-kota",
        text = "Ala ma kota.",
        units = listOf("A", "l", "a", "m", "a", "k", "o", "t", "a"),
        syllables = listOf("A", "la", "ma", "ko", "ta"),
        level = 7,
        lessonId = "l7",
    )

    @Test
    fun `graphemes are the word's units`() {
        sentence.pointingUnits(PointingUnit.Graphemes) shouldBe sentence.units
    }

    @Test
    fun `syllables keep their positions in the text and get a lowercase id`() {
        val spans = sentence.pointingUnits(PointingUnit.Syllables)

        spans.map { it.grapheme } shouldContainExactly listOf("A", "la", "ma", "ko", "ta")
        spans.map { it.startIndex to it.endIndex } shouldContainExactly listOf(0 to 1, 1 to 3, 4 to 6, 7 to 9, 9 to 11)
        spans.first().phonemeId shouldBe "a"
    }
}
