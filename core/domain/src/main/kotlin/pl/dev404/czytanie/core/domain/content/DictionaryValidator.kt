package pl.dev404.czytanie.core.domain.content

import pl.dev404.czytanie.core.domain.content.DictionaryIssue.Companion.error
import pl.dev404.czytanie.core.domain.content.DictionaryIssue.Companion.warning
import pl.dev404.czytanie.core.domain.model.Dictionary
import pl.dev404.czytanie.core.domain.model.Lesson
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.segmentation.GraphemeSegmenter

/**
 * Checks the whole dictionary. Invariants that a single [Word] guarantees itself (units and
 * syllables spell the text) are not repeated here; this covers what needs the whole picture:
 * phoneme inventory, recordings, levels, lessons and level rules.
 *
 * Differences from [GraphemeSegmenter]'s proposal are warnings, never errors: the hand
 * annotation is the source of truth, the segmenter only points at places worth a second look.
 *
 * @param assetExists whether a path relative to the assets root exists.
 */
class DictionaryValidator(private val segmenter: GraphemeSegmenter = GraphemeSegmenter()) {
    fun validate(
        dictionary: Dictionary,
        assetExists: (String) -> Boolean,
    ): List<DictionaryIssue> = buildList {
        addAll(checkPhonemes(dictionary, assetExists))
        addAll(checkLevels(dictionary))
        addAll(checkWords(dictionary, assetExists))
    }

    private fun checkPhonemes(
        dictionary: Dictionary,
        assetExists: (String) -> Boolean,
    ) = buildList {
        dictionary.phonemes.duplicates { it.id }.forEach { add(error("phoneme $it", "duplicate phoneme id")) }
        dictionary.phonemes.filterNot { assetExists(it.audio) }.forEach {
            add(error("phoneme ${it.id}", "missing recording ${it.audio}"))
        }
    }

    private fun checkLevels(dictionary: Dictionary) = buildList {
        dictionary.levels.duplicates { it.id }.forEach { add(error("level $it", "duplicate level id")) }
        dictionary.levels.filter { it.id !in Level.RANGE }.forEach {
            add(error("level ${it.id}", "level outside ${Level.RANGE}"))
        }
        dictionary.levels.flatMap { it.lessons }.duplicates { it }.forEach {
            add(error("lesson $it", "lesson listed more than once"))
        }
        val wordsByLesson = dictionary.words.groupBy { it.lessonId }
        dictionary.levels.flatMap { it.lessons }.forEach { lessonId ->
            val size = wordsByLesson[lessonId].orEmpty().size
            if (size !in Lesson.SIZE_RANGE) {
                add(error("lesson $lessonId", "has $size words, expected ${Lesson.SIZE_RANGE}"))
            }
        }
    }

    private fun checkWords(
        dictionary: Dictionary,
        assetExists: (String) -> Boolean,
    ) = buildList {
        val phonemeIds = dictionary.phonemes.map { it.id }.toSet()
        val levelOfLesson = dictionary.levels.flatMap { level -> level.lessons.map { it to level.id } }.toMap()

        dictionary.words.duplicates { it.id }.forEach { add(error("word $it", "duplicate word id")) }
        for (word in dictionary.words) {
            val subject = "word ${word.id}"
            when (val lessonLevel = levelOfLesson[word.lessonId]) {
                null -> add(error(subject, "lesson ${word.lessonId} is not listed in any level"))
                word.level -> Unit
                else -> add(error(subject, "is level ${word.level} but lesson ${word.lessonId} is level $lessonLevel"))
            }
            word.units.filter { it.phonemeId !in phonemeIds }.forEach {
                add(error(subject, "unit '${it.grapheme}' has no phoneme '${it.phonemeId}'"))
            }
            if (!word.syllablesAlignWithUnits()) add(error(subject, "a syllable boundary splits a unit"))
            LevelRules.violations(word).forEach { add(error(subject, it)) }
            word.wordAudio?.takeUnless(assetExists)?.let { add(error(subject, "missing recording $it")) }
            word.image?.takeUnless(assetExists)?.let { add(error(subject, "missing image $it")) }
            addAll(compareWithSegmenter(word, subject))
        }
    }

    private fun compareWithSegmenter(
        word: Word,
        subject: String,
    ) = buildList {
        // The segmenter handles single words; sentences (level 7) are checked by hand.
        if (!word.text.all { it.isLetter() }) return@buildList
        val proposal = segmenter.segment(word.text)
        val annotated = word.units.map { it.grapheme to it.phonemeId }
        val proposed = proposal.units.map { it.grapheme to it.phonemeId }
        if (annotated != proposed) {
            add(warning(subject, "annotation $annotated differs from the segmenter's proposal $proposed"))
        }
        proposal.hints.forEach { add(warning(subject, "check manually: $it")) }
    }

    private fun Word.syllablesAlignWithUnits(): Boolean {
        val unitBoundaries = units.map { it.startIndex }.toSet()
        return syllables.all { it.startIndex in unitBoundaries }
    }

    private fun <T, K> List<T>.duplicates(key: (T) -> K): Set<K> =
        groupingBy(key).eachCount().filterValues { it > 1 }.keys
}
