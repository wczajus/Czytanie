package pl.dev404.czytanie.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempt WHERE profileId = :profileId ORDER BY createdAtEpochMillis DESC")
    fun observeForProfile(profileId: Long): Flow<List<AttemptEntity>>
}
