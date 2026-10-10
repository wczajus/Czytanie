package pl.dev404.czytanie.core.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WordTest {

    private fun szafa(
        units: List<String> = listOf("sz", "a", "f", "a"),
        syllables: List<String> = listOf("sza", "fa"),
        level: Int = 5,
    ) = Word.fromAnnotation(
        id = "szafa",
        text = "szafa",
        units = units,
        syllables = syllables,
        level = level,
        lessonId = "l5-sz",
    )

    @Test
    fun `annotation is turned into units with indices`() {
        val word = szafa()

        word.units shouldContainExactly listOf(
            ReadingUnit("sz", "sz", 0, 2),
            ReadingUnit("a", "a", 2, 3),
            ReadingUnit("f", "f", 3, 4),
            ReadingUnit("a", "a", 4, 5),
        )
        word.syllables shouldContainExactly listOf(Syllable("sza", 0, 3), Syllable("fa", 3, 5))
        word.units.first().lastIndex shouldBe 1
    }

    @Test
    fun `custom phoneme ids are kept`() {
        val word = Word.fromAnnotation(
            id = "nic",
            text = "nic",
            units = listOf("n", "i", "c"),
            syllables = listOf("nic"),
            level = 4,
            lessonId = "l4",
            phonemeIds = listOf("ni", "i", "c"),
        )

        word.units.map { it.phonemeId } shouldContainExactly listOf("ni", "i", "c")
    }

    @Test
    fun `units that do not spell the text are rejected`() {
        shouldThrow<IllegalArgumentException> { szafa(units = listOf("s", "z", "a", "f")) }
    }

    @Test
    fun `syllables that do not spell the text are rejected`() {
        shouldThrow<IllegalArgumentException> { szafa(syllables = listOf("sza", "f")) }
    }

    @Test
    fun `level outside 1 to 7 is rejected`() {
        shouldThrow<IllegalArgumentException> { szafa(level = 8) }
    }

    @Test
    fun `units with gaps are rejected`() {
        shouldThrow<IllegalArgumentException> {
            Word(
                id = WordId("ma"),
                text = "ma",
                units = listOf(ReadingUnit("m", "m", 0, 1), ReadingUnit("a", "a", 2, 3)),
                syllables = listOf(Syllable("ma", 0, 2)),
                level = 2,
                lessonId = LessonId("l2"),
            )
        }
    }

    @Test
    fun `unit range must match its grapheme`() {
        shouldThrow<IllegalArgumentException> { ReadingUnit("sz", "sz", 0, 1) }
    }
}
