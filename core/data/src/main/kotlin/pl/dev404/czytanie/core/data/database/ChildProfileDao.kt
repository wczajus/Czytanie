package pl.dev404.czytanie.core.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profile ORDER BY id")
    fun observeAll(): Flow<List<ChildProfileEntity>>

    @Insert
    suspend fun insert(profile: ChildProfileEntity): Long
}
