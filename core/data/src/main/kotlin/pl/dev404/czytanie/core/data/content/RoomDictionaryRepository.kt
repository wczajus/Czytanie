package pl.dev404.czytanie.core.data.content

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import pl.dev404.czytanie.core.data.database.DictionaryDao
import pl.dev404.czytanie.core.data.database.WordEntity
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.Lesson
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.Phoneme
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.model.WordId

class RoomDictionaryRepository @Inject constructor(
    private val dao: DictionaryDao,
    private val importer: DictionaryImporter,
) : DictionaryRepository {

    override fun observeLevels(): Flow<List<Level>> =
        combine(dao.observeLevels(), dao.observeLessons()) { levels, lessons ->
            val lessonsByLevel = lessons.groupBy { it.levelId }
            levels.map { level ->
                Level(
                    id = level.id,
                    name = level.name,
                    lessons = lessonsByLevel[level.id].orEmpty().sortedBy { it.position }.map { LessonId(it.id) },
                )
            }
        }.onStart { importer.ensureUpToDate() }

    override suspend fun lesson(id: LessonId): Lesson? {
        importer.ensureUpToDate()
        val lesson = dao.lesson(id.value) ?: return null
        return Lesson(id = id, level = lesson.levelId, words = dao.wordsInLesson(id.value).map { it.toDomain() })
    }

    override suspend fun word(id: WordId): Word? {
        importer.ensureUpToDate()
        return dao.word(id.value)?.toDomain()
    }

    override suspend fun phonemes(): List<Phoneme> {
        importer.ensureUpToDate()
        return dao.phonemes().map { Phoneme(id = it.id, grapheme = it.grapheme, audio = it.audio) }
    }

    private fun WordEntity.toDomain() = Word.fromAnnotation(
        id = id,
        text = text,
        units = units.map { it.grapheme },
        syllables = syllables,
        level = level,
        lessonId = lessonId,
        phonemeIds = units.map { it.phonemeId },
        image = image,
        wordAudio = wordAudio,
    )
}
