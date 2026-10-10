package pl.dev404.czytanie.core.domain.content

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.segmentation.GraphemeSegmenter

class LevelRulesTest {

    private val segmenter = GraphemeSegmenter()

    /** Builds a word from the segmenter's split and syllables given as "sza-fa". */
    private fun word(
        text: String,
        syllables: String,
        level: Int,
    ): Word {
        val units = Regex("\\p{L}+").findAll(text).flatMap { segmenter.segment(it.value).units }.toList()
        return Word.fromAnnotation(
            id = text,
            text = text,
            units = units.map { it.grapheme },
            syllables = syllables.split("-", " "),
            level = level,
            lessonId = "l$level",
            phonemeIds = units.map { it.phonemeId },
        )
    }

    @ParameterizedTest(name = "{0} fits level {2}")
    @CsvSource(
        "a, a, 1",
        "y, y, 1",
        "ma, ma, 2",
        "ty, ty, 2",
        "mama, ma-ma, 3",
        "lokomotywa, lo-ko-mo-ty-wa, 3",
        "kot, kot, 4",
        "sklep, sklep, 4",
        "mama, ma-ma, 4",
        "szafa, sza-fa, 5",
        "drzewo, drze-wo, 5",
        "siano, sia-no, 6",
        "koń, koń, 6",
        "zima, zi-ma, 6",
        // Digraphs with a diacritic letter belong to level 6
        "dźwig, dźwig, 6",
        "dżem, dżem, 6",
        // Level 7 holds short sentences
        "Ala ma kota., A-la ma ko-ta, 7",
    )
    fun `word fits its level`(
        text: String,
        syllables: String,
        level: Int,
    ) {
        LevelRules.violations(word(text, syllables, level)).shouldBeEmpty()
    }

    @ParameterizedTest(name = "{0} does not fit level {2}")
    @CsvSource(
        // Level 1 is a single vowel
        "ma, ma, 1",
        "ą, ą, 1",
        // Level 2 is one simple open syllable
        "mama, ma-ma, 2",
        "kot, kot, 2",
        "sza, sza, 2",
        // Level 3: open syllables of simple graphemes only
        "kot, kot, 3",
        "lalka, lal-ka, 3",
        "szafa, sza-fa, 3",
        "zima, zi-ma, 3",
        "góra, gó-ra, 3",
        // Level 4: no digraphs, softening or diacritics
        "szafa, sza-fa, 4",
        "koń, koń, 4",
        // Level 5 needs a digraph and nothing from level 6
        "kot, kot, 5",
        "rzeźba, rzeź-ba, 5",
        "dźwig, dźwig, 5",
        "dżem, dżem, 5",
        // Level 6 needs softening or a diacritic
        "kot, kot, 6",
        // Sentences only on level 7, and level 7 only sentences
        "Ala ma kota., A-la ma ko-ta, 6",
        "kot, kot, 7",
    )
    fun `word does not fit the level`(
        text: String,
        syllables: String,
        level: Int,
    ) {
        LevelRules.violations(word(text, syllables, level)).shouldNotBeEmpty()
    }
}
