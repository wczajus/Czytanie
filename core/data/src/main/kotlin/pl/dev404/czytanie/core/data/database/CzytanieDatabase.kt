package pl.dev404.czytanie.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local database: dictionary (imported from assets) and progress. Schemas are exported to
 * `core/data/schemas` and every version bump needs a migration with a test.
 */
@Database(
    entities = [ChildProfileEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CzytanieDatabase : RoomDatabase() {
    abstract fun childProfileDao(): ChildProfileDao

    companion object {
        const val NAME = "czytanie.db"
    }
}
