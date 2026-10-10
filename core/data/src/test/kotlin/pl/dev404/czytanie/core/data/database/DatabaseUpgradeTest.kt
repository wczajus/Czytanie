package pl.dev404.czytanie.core.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Version 1 (v0.1.0) never stored user data, so it is recreated instead of migrated. */
@RunWith(RobolectricTestRunner::class)
class DatabaseUpgradeTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "upgrade-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun `database version 1 is upgraded to the current version`() = runTest {
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { v1 ->
            v1.execSQL(
                "CREATE TABLE child_profile (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL)",
            )
            v1.version = 1
        }

        val database = CzytanieDatabase.build(context, name)

        database.dictionaryDao().importedVersion().shouldBeNull()
        database.childProfileDao().observeAll().first().shouldBeEmpty()
        database.close()
    }
}
