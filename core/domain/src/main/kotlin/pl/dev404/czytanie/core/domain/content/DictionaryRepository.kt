package pl.dev404.czytanie.core.domain.content

import kotlinx.coroutines.flow.Flow
import pl.dev404.czytanie.core.domain.model.Lesson
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.Phoneme
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.model.WordId

/** Read access to the reading content. Implementations make sure the bundled dictionary is imported. */
interface DictionaryRepository {
    /** Levels in teaching order, each with its lessons in order. */
    fun observeLevels(): Flow<List<Level>>

    /** The lesson with its words in reading order, or null if it does not exist. */
    suspend fun lesson(id: LessonId): Lesson?

    suspend fun word(id: WordId): Word?

    /** The phoneme inventory with the recording of each phoneme. */
    suspend fun phonemes(): List<Phoneme>
}
