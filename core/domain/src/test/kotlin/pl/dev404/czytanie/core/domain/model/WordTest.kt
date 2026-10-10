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
    fun `units of a sentence skip spaces and punctuation`() {
        val word = Word.fromAnnotation(
            id = "ala-ma-kota",
            text = "Ala ma kota.",
            units = listOf("A", "l", "a", "m", "a", "k", "o", "t", "a"),
            syllables = listOf("A", "la", "ma", "ko", "ta"),
            level = 7,
            lessonId = "l7",
        )

        word.units.map { it.startIndex } shouldContainExactly listOf(0, 1, 2, 4, 5, 7, 8, 9, 10)
        word.units.first().phonemeId shouldBe "a"
        word.syllables.map { it.startIndex } shouldContainExactly listOf(0, 1, 4, 7, 9)
    }

    @Test
    fun `units must not skip letters`() {
        shouldThrow<IllegalArgumentException> {
            Word(
                id = WordId("ala-ma"),
                text = "Ala ma",
                units = listOf(ReadingUnit("A", "a", 0, 1), ReadingUnit("m", "m", 4, 5), ReadingUnit("a", "a", 5, 6)),
                syllables = listOf(Syllable("Ala", 0, 3), Syllable("ma", 4, 6)),
                level = 7,
                lessonId = LessonId("l7"),
            )
        }
    }

    @Test
    fun `units must not cover spaces`() {
        shouldThrow<IllegalArgumentException> {
            Word.fromAnnotation(
                id = "ala-ma",
                text = "Ala ma",
                units = listOf("A", "l", "a", " ", "m", "a"),
                syllables = listOf("A", "la", "ma"),
                level = 7,
                lessonId = "l7",
            )
        }
    }

    @Test
    fun `unit range must match its grapheme`() {
        shouldThrow<IllegalArgumentException> { ReadingUnit("sz", "sz", 0, 1) }
    }
}
