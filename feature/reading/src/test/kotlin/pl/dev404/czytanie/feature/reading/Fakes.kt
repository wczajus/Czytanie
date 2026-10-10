package pl.dev404.czytanie.feature.reading

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.Lesson
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.Phoneme
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.model.WordId
import pl.dev404.czytanie.core.domain.pacing.PointingUnit
import pl.dev404.czytanie.core.domain.segmentation.GraphemeSegmenter
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.SettingsRepository
import pl.dev404.czytanie.core.domain.settings.WordFontSize

internal object TestWords {
    const val LESSON_ID = "test-lesson"

    val szafa = Word.fromAnnotation(
        id = "szafa",
        text = "szafa",
        units = listOf("sz", "a", "f", "a"),
        syllables = listOf("sza", "fa"),
        level = 5,
        lessonId = LESSON_ID,
    )

    val mama = Word.fromAnnotation(
        id = "mama",
        text = "mama",
        units = listOf("m", "a", "m", "a"),
        syllables = listOf("ma", "ma"),
        level = 3,
        lessonId = LESSON_ID,
    )

    val alaMaKota = Word.fromAnnotation(
        id = "ala-ma-kota",
        text = "Ala ma kota.",
        units = listOf("A", "l", "a", "m", "a", "k", "o", "t", "a"),
        syllables = listOf("A", "la", "ma", "ko", "ta"),
        level = 7,
        lessonId = LESSON_ID,
    )

    /** A story paragraph (level 8) long enough to wrap over several lines. */
    val passage: Word = run {
        val text = "Jedynym stałym lokatorem zawsze opustoszałego domu był pan Popiołek. Listonosz. " +
            "Zajmował on dwa maciupeńkie pokoiki."
        val words = Regex("\\p{L}+").findAll(text).map { it.value }.toList()
        val segmenter = GraphemeSegmenter()
        Word.fromAnnotation(
            id = "passage",
            text = text,
            units = words.flatMap { segmenter.segment(it).graphemes },
            // Syllables do not matter on the reading screen; one per word keeps them valid.
            syllables = words,
            level = Level.FIRST_STORY_LEVEL,
            lessonId = LESSON_ID,
        )
    }

    val lesson = Lesson(LessonId(LESSON_ID), level = 3, words = listOf(mama, szafa))
}

internal class FakeDictionaryRepository(private val lessons: List<Lesson> = listOf(TestWords.lesson)) :
    DictionaryRepository {
    override fun observeLevels(): Flow<List<Level>> = emptyFlow()

    override suspend fun lesson(id: LessonId): Lesson? = lessons.find { it.id == id }

    override suspend fun word(id: WordId): Word? = lessons.flatMap { it.words }.find { it.id == id }

    override suspend fun phonemes(): List<Phoneme> = emptyList()
}

internal class FakeSettingsRepository(initial: ReadingSettings = ReadingSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<ReadingSettings> = state

    override suspend fun setTempo(tempoMillis: Long) = state.update { it.copy(tempoMillis = tempoMillis) }

    override suspend fun setPhonemesEnabled(enabled: Boolean) = state.update { it.copy(phonemesEnabled = enabled) }

    override suspend fun setFontSize(fontSize: WordFontSize) = state.update { it.copy(fontSize = fontSize) }

    override suspend fun setPointingUnit(pointingUnit: PointingUnit) = state.update {
        it.copy(pointingUnit = pointingUnit)
    }
}

/** Recordings of a controlled length; remembers what was played. */
internal class FakeSpeechPlayer(
    private val phonemeMillis: Long = 0,
    private val wordMillis: Long = 0,
    private val wordsCanBeHeard: Boolean = true,
) : SpeechPlayer {
    val phonemes = mutableListOf<String>()
    val syllables = mutableListOf<String>()
    val words = mutableListOf<Word>()
    val prepared = mutableListOf<Word>()

    override suspend fun prepare(
        words: List<Word>,
        pointingUnit: PointingUnit,
    ) {
        prepared += words
    }

    override suspend fun canPlayWord(word: Word) = wordsCanBeHeard

    override suspend fun playPhoneme(phonemeId: String) {
        phonemes += phonemeId
        delay(phonemeMillis)
    }

    override suspend fun playSyllable(syllable: String) {
        syllables += syllable
        delay(phonemeMillis)
    }

    override suspend fun playWord(word: Word) {
        words += word
        delay(wordMillis)
    }
}
