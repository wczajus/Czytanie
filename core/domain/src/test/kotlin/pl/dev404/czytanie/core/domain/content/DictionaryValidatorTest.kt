package pl.dev404.czytanie.core.domain.content

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import pl.dev404.czytanie.core.domain.model.Dictionary
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.Phoneme
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.model.WordId

class DictionaryValidatorTest {

    private val validator = DictionaryValidator()
    private val allAssets: (String) -> Boolean = { true }

    private fun phoneme(id: String) = Phoneme(id, id, "audio/phonemes/$id.ogg")

    private fun level2Word(
        text: String,
        lesson: String = "l2",
        level: Int = 2,
    ) = Word.fromAnnotation(
        id = text,
        text = text,
        units = text.map { it.toString() },
        syllables = listOf(text),
        level = level,
        lessonId = lesson,
    )

    private val sixSyllables = listOf("ma", "mo", "mu", "me", "mi", "my").map { level2Word(it) }

    private fun dictionary(
        words: List<Word> = sixSyllables,
        phonemes: List<Phoneme> = listOf("m", "a", "o", "u", "e", "i", "y").map(::phoneme),
        levels: List<Level> = listOf(Level(2, "Sylaby otwarte", listOf(LessonId("l2")))),
    ) = Dictionary(version = 1, phonemes = phonemes, levels = levels, words = words)

    @Test
    fun `valid dictionary has no issues`() {
        validator.validate(dictionary(), allAssets).shouldBeEmpty()
    }

    @Test
    fun `missing recording is an error`() {
        val issues = validator.validate(dictionary()) { it != "audio/phonemes/m.ogg" }

        issues.errors shouldHaveSize 1
        issues.errors.single().message shouldContain "audio/phonemes/m.ogg"
    }

    @Test
    fun `unit without phoneme entry is an error`() {
        val issues = validator.validate(
            dictionary(phonemes = listOf("a", "o", "u", "e", "i", "y").map(::phoneme)),
            allAssets,
        )

        issues.errors.map { it.subject }.toSet() shouldBe sixSyllables.map { "word ${it.id}" }.toSet()
    }

    @Test
    fun `word at the wrong level is an error`() {
        val words = sixSyllables.dropLast(1) + Word.fromAnnotation(
            id = "kot",
            text = "kot",
            units = listOf("k", "o", "t"),
            syllables = listOf("kot"),
            level = 2,
            lessonId = "l2",
        )
        val issues = validator.validate(
            dictionary(words = words, phonemes = listOf("m", "a", "o", "u", "e", "i", "k", "t").map(::phoneme)),
            allAssets,
        )

        issues.errors.single().subject shouldBe "word kot"
    }

    @Test
    fun `lesson of another level is an error`() {
        val words = sixSyllables.dropLast(1) + level2Word("ma", lesson = "l2").copy(level = 3)
        val issues = validator.validate(dictionary(words = words), allAssets)

        issues.errors.map { it.message }.any { it.contains("lesson l2 is level 2") } shouldBe true
    }

    @Test
    fun `lesson that is too short or unknown is an error`() {
        val issues = validator.validate(
            dictionary(words = sixSyllables.take(3) + level2Word("ta", lesson = "x")),
            allAssets,
        )

        issues.errors.map { it.subject } shouldContain "lesson l2"
        issues.errors.map { it.subject } shouldContain "word ta"
    }

    @Test
    fun `duplicate ids are errors`() {
        val issues = validator.validate(dictionary(words = sixSyllables + sixSyllables.first()), allAssets)

        issues.errors.map { it.subject } shouldContain "word ma"
    }

    @Test
    fun `syllable boundary inside a digraph is an error`() {
        val word = Word.fromAnnotation(
            id = "szafa",
            text = "szafa",
            units = listOf("sz", "a", "f", "a"),
            syllables = listOf("s", "zafa"),
            level = 5,
            lessonId = "l5",
        )
        val issues = validator.validate(
            Dictionary(
                version = 1,
                phonemes = listOf("sz", "a", "f").map(::phoneme),
                levels = listOf(Level(5, "Dwuznaki", listOf(LessonId("l5")))),
                words = List(6) { word.copy(id = WordId("szafa$it")) },
            ),
            allAssets,
        )

        issues.errors.map { it.message } shouldContain "a syllable boundary splits a unit"
    }

    @Test
    fun `annotation that differs from the segmenter is only a warning`() {
        val marznac = Word.fromAnnotation(
            id = "marznąć",
            text = "marznąć",
            units = listOf("m", "a", "r", "z", "n", "ą", "ć"),
            syllables = listOf("mar", "znąć"),
            level = 6,
            lessonId = "l6",
        )
        // Deliberately split differently from the segmenter's "si e ń".
        val sien = Word.fromAnnotation(
            id = "sień",
            text = "sień",
            units = listOf("s", "i", "e", "ń"),
            syllables = listOf("sień"),
            level = 6,
            lessonId = "l6",
        )
        val words = List(5) { marznac.copy(id = WordId("m$it")) } + sien
        val issues = validator.validate(
            Dictionary(
                version = 1,
                phonemes = listOf("m", "a", "r", "z", "n", "ą", "ć", "s", "i", "e", "ń").map(::phoneme),
                levels = listOf(Level(6, "Zmiękczenia", listOf(LessonId("l6")))),
                words = words,
            ),
            allAssets,
        )

        issues.errors.shouldBeEmpty()
        issues.warnings.map { it.subject } shouldContain "word sień"
        issues.warnings.map { it.subject } shouldContain "word m0"
    }

    @Test
    fun `sentence is compared with the segmenter word by word`() {
        fun sentence(
            id: String,
            units: List<String>,
        ) = Word.fromAnnotation(
            id = id,
            text = "Ala ma kota.",
            units = units,
            syllables = listOf("A", "la", "ma", "ko", "ta"),
            level = 7,
            lessonId = "l7",
        )
        val letters = listOf("A", "l", "a", "m", "a", "k", "o", "t", "a")
        val words = List(6) { sentence("s$it", letters) }
        val issues = validator.validate(
            Dictionary(
                version = 1,
                phonemes = listOf("a", "l", "m", "k", "o", "t").map(::phoneme),
                levels = listOf(Level(7, "Zdania", listOf(LessonId("l7")))),
                words = words,
            ),
            allAssets,
        )

        issues.shouldBeEmpty()
    }
}
