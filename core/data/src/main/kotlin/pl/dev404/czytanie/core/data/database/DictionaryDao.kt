package pl.dev404.czytanie.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DictionaryDao {
    @Query("SELECT version FROM dictionary_meta WHERE id = 0")
    abstract suspend fun importedVersion(): Int?

    /** Replaces the whole dictionary atomically. Attempts and profiles are not touched. */
    @Transaction
    open suspend fun replaceAll(
        version: Int,
        phonemes: List<PhonemeEntity>,
        levels: List<LevelEntity>,
        lessons: List<LessonEntity>,
        words: List<WordEntity>,
    ) {
        deleteWords()
        deleteLessons()
        deleteLevels()
        deletePhonemes()
        insertPhonemes(phonemes)
        insertLevels(levels)
        insertLessons(lessons)
        insertWords(words)
        upsertMeta(DictionaryMetaEntity(version = version))
    }

    @Query("SELECT * FROM level ORDER BY id")
    abstract fun observeLevels(): Flow<List<LevelEntity>>

    @Query("SELECT * FROM lesson ORDER BY levelId, position")
    abstract fun observeLessons(): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lesson WHERE id = :id")
    abstract suspend fun lesson(id: String): LessonEntity?

    @Query("SELECT * FROM word WHERE lessonId = :lessonId ORDER BY position")
    abstract suspend fun wordsInLesson(lessonId: String): List<WordEntity>

    @Query("SELECT * FROM word WHERE id = :id")
    abstract suspend fun word(id: String): WordEntity?

    @Query("SELECT * FROM phoneme ORDER BY id")
    abstract suspend fun phonemes(): List<PhonemeEntity>

    @Query("SELECT id, lessonId FROM word")
    abstract fun observeWordLessons(): Flow<List<WordLesson>>

    @Query("SELECT COUNT(*) FROM word")
    abstract suspend fun wordCount(): Int

    @Query("DELETE FROM word")
    abstract suspend fun deleteWords()

    @Query("DELETE FROM lesson")
    abstract suspend fun deleteLessons()

    @Query("DELETE FROM level")
    abstract suspend fun deleteLevels()

    @Query("DELETE FROM phoneme")
    abstract suspend fun deletePhonemes()

    @Insert
    abstract suspend fun insertPhonemes(phonemes: List<PhonemeEntity>)

    @Insert
    abstract suspend fun insertLevels(levels: List<LevelEntity>)

    @Insert
    abstract suspend fun insertLessons(lessons: List<LessonEntity>)

    @Insert
    abstract suspend fun insertWords(words: List<WordEntity>)

    @Upsert
    abstract suspend fun upsertMeta(meta: DictionaryMetaEntity)
}

/** Which lesson a word belongs to. */
data class WordLesson(
    val id: String,
    val lessonId: String,
)
