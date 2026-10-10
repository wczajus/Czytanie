package pl.dev404.czytanie.core.data.progress

import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import pl.dev404.czytanie.core.data.content.DictionaryImporter
import pl.dev404.czytanie.core.data.database.AttemptDao
import pl.dev404.czytanie.core.data.database.AttemptEntity
import pl.dev404.czytanie.core.data.database.AttemptWithWord
import pl.dev404.czytanie.core.data.database.DictionaryDao
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.ParentRating
import pl.dev404.czytanie.core.domain.model.WordId
import pl.dev404.czytanie.core.domain.progress.Attempt
import pl.dev404.czytanie.core.domain.progress.LessonProgress
import pl.dev404.czytanie.core.domain.progress.ProgressRepository

class RoomProgressRepository @Inject constructor(
    private val attemptDao: AttemptDao,
    private val dictionaryDao: DictionaryDao,
    private val importer: DictionaryImporter,
    private val clock: Clock,
) : ProgressRepository {

    override suspend fun saveRatings(
        ratings: Map<WordId, ParentRating>,
        tempoMillis: Long,
    ) {
        val now = clock.millis()
        attemptDao.insertAll(
            ratings.map { (wordId, rating) ->
                AttemptEntity(
                    profileId = DEFAULT_PROFILE_ID,
                    wordId = wordId.value,
                    rating = rating.name,
                    createdAtEpochMillis = now,
                    tempoMillis = tempoMillis,
                )
            },
        )
    }

    override fun observeHistory(): Flow<List<Attempt>> =
        attemptDao.observeWithWords(DEFAULT_PROFILE_ID).map { rows -> rows.mapNotNull { it.toDomain() } }

    override fun observeLessonProgress(): Flow<Map<LessonId, LessonProgress>> =
        combine(dictionaryDao.observeWordLessons(), observeHistory()) { words, attempts ->
            words.groupBy({ LessonId(it.lessonId) }, { WordId(it.id) })
                .mapValues { (_, lessonWords) -> LessonProgress.of(lessonWords.toSet(), attempts) }
        }.onStart { importer.ensureUpToDate() }

    private fun AttemptWithWord.toDomain(): Attempt? {
        // A rating written by a newer app version is skipped rather than misread.
        val rating = ParentRating.entries.find { it.name == attempt.rating } ?: return null
        return Attempt(
            wordId = WordId(attempt.wordId),
            wordText = wordText,
            rating = rating,
            createdAtEpochMillis = attempt.createdAtEpochMillis,
            tempoMillis = attempt.tempoMillis,
        )
    }

    companion object {
        /** MVP has a single child; profiles come later (the table is ready). */
        const val DEFAULT_PROFILE_ID = 1L
    }
}
