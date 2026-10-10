package pl.dev404.czytanie.core.data.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** An attempt with the word's current spelling, or null if the word left the dictionary. */
data class AttemptWithWord(
    @Embedded val attempt: AttemptEntity,
    val wordText: String?,
)

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Insert
    suspend fun insertAll(attempts: List<AttemptEntity>)

    @Query("SELECT * FROM attempt WHERE profileId = :profileId ORDER BY createdAtEpochMillis DESC")
    fun observeForProfile(profileId: Long): Flow<List<AttemptEntity>>

    @Query(
        """
        SELECT attempt.*, word.text AS wordText FROM attempt
        LEFT JOIN word ON word.id = attempt.wordId
        WHERE attempt.profileId = :profileId
        ORDER BY attempt.createdAtEpochMillis DESC, attempt.id DESC
        """,
    )
    fun observeWithWords(profileId: Long): Flow<List<AttemptWithWord>>
}
