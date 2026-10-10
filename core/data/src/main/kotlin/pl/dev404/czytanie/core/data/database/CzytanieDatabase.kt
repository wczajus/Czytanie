package pl.dev404.czytanie.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Local database: dictionary (imported from assets) and progress. Schemas are exported to
 * `core/data/schemas` and committed; CI fails when they are out of date.
 *
 * Version history:
 * 1. child_profile only (v0.1.0, no user data was ever stored).
 * 2. dictionary tables and attempts. Upgrading from 1 recreates the database.
 *
 * From version 2 on, every bump needs a migration and a migration test.
 */
@Database(
    entities = [
        ChildProfileEntity::class,
        DictionaryMetaEntity::class,
        PhonemeEntity::class,
        LevelEntity::class,
        LessonEntity::class,
        WordEntity::class,
        AttemptEntity::class,
    ],
    version = CzytanieDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class CzytanieDatabase : RoomDatabase() {
    abstract fun childProfileDao(): ChildProfileDao

    abstract fun dictionaryDao(): DictionaryDao

    abstract fun attemptDao(): AttemptDao

    companion object {
        const val NAME = "czytanie.db"
        const val VERSION = 2

        /** Version 1 held no user data, so it is dropped instead of migrated. */
        private const val FIRST_VERSION_WITHOUT_USER_DATA = 1

        fun build(
            context: Context,
            name: String = NAME,
        ): CzytanieDatabase = Room.databaseBuilder(context, CzytanieDatabase::class.java, name)
            .fallbackToDestructiveMigrationFrom(true, FIRST_VERSION_WITHOUT_USER_DATA)
            .build()
    }
}
