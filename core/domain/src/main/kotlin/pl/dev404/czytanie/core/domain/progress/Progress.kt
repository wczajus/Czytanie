package pl.dev404.czytanie.core.domain.progress

import kotlinx.coroutines.flow.Flow
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.ParentRating
import pl.dev404.czytanie.core.domain.model.WordId

/** One reading of a word as the parent rated it. */
data class Attempt(
    val wordId: WordId,
    /** The word as it was spelled when shown; null if it is no longer in the dictionary. */
    val wordText: String?,
    val rating: ParentRating,
    val createdAtEpochMillis: Long,
    val tempoMillis: Long,
)

/**
 * How far the child is in one lesson, counting each word's latest rating only: an earlier
 * "jeszcze nie" does not count against a word the child reads now.
 */
data class LessonProgress(
    val readWords: Int,
    val wordCount: Int,
) {
    /** The design document asks for 8 of 10 words; shorter lessons need the same 80 %. */
    val isPassed: Boolean get() = wordCount > 0 && readWords * PASS_DENOMINATOR >= wordCount * PASS_NUMERATOR

    companion object {
        private const val PASS_NUMERATOR = 4
        private const val PASS_DENOMINATOR = 5

        /**
         * @param lessonWords the words of the lesson.
         * @param attempts any attempts, in any order; other lessons' attempts are ignored.
         */
        fun of(
            lessonWords: Collection<WordId>,
            attempts: Collection<Attempt>,
        ): LessonProgress {
            val latest = attempts
                .filter { it.wordId in lessonWords }
                .groupBy { it.wordId }
                .mapValues { (_, wordAttempts) -> wordAttempts.maxBy { it.createdAtEpochMillis }.rating }
            return LessonProgress(
                readWords = latest.count { it.value == ParentRating.Read },
                wordCount = lessonWords.size,
            )
        }
    }
}

interface ProgressRepository {
    /** Saves the parent's ratings of one reading session, all with the same time and tempo. */
    suspend fun saveRatings(
        ratings: Map<WordId, ParentRating>,
        tempoMillis: Long,
    )

    /** All attempts, newest first. */
    fun observeHistory(): Flow<List<Attempt>>

    /** Progress of every lesson in the dictionary. */
    fun observeLessonProgress(): Flow<Map<LessonId, LessonProgress>>
}
