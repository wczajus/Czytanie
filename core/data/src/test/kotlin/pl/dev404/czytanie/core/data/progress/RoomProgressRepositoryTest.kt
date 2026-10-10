package pl.dev404.czytanie.core.data.progress

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pl.dev404.czytanie.core.data.content.DictionaryImporter
import pl.dev404.czytanie.core.data.content.DictionaryParser
import pl.dev404.czytanie.core.data.content.TestDictionaries
import pl.dev404.czytanie.core.data.database.AttemptEntity
import pl.dev404.czytanie.core.data.database.CzytanieDatabase
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.ParentRating.NotYet
import pl.dev404.czytanie.core.domain.model.ParentRating.Read
import pl.dev404.czytanie.core.domain.model.ParentRating.WithHelp
import pl.dev404.czytanie.core.domain.model.WordId
import pl.dev404.czytanie.core.domain.progress.LessonProgress

@RunWith(RobolectricTestRunner::class)
class RoomProgressRepositoryTest {

    private lateinit var database: CzytanieDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            CzytanieDatabase::class.java,
        ).build()
    }

    private var now = Instant.parse("2026-10-10T12:00:00Z")

    private fun repository(): RoomProgressRepository {
        val importer = DictionaryImporter({ TestDictionaries.json() }, DictionaryParser(), database.dictionaryDao())
        val clock = object : Clock() {
            override fun getZone() = ZoneOffset.UTC

            override fun withZone(zone: java.time.ZoneId?) = this

            override fun instant(): Instant = now
        }
        return RoomProgressRepository(database.attemptDao(), database.dictionaryDao(), importer, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `ratings are saved with the current time and tempo, newest first in history`() = runTest {
        val repository = repository()
        repository.observeLessonProgress().first()

        repository.saveRatings(mapOf(WordId("ma") to NotYet), tempoMillis = 1200)
        now = now.plusSeconds(60)
        repository.saveRatings(mapOf(WordId("ma") to Read, WordId("mo") to WithHelp), tempoMillis = 900)

        val history = repository.observeHistory().first()
        history.map { it.wordText to it.rating } shouldContainExactly
            listOf("mo" to WithHelp, "ma" to Read, "ma" to NotYet)
        history.last().tempoMillis shouldBe 1200
        history.last().createdAtEpochMillis shouldBe Instant.parse("2026-10-10T12:00:00Z").toEpochMilli()
    }

    @Test
    fun `lesson is passed when enough words were read, by their latest rating`() = runTest {
        val repository = repository()
        repository.observeLessonProgress().first()[LessonId("l2")] shouldBe LessonProgress(0, 6)

        repository.saveRatings(listOf("ma", "mo", "mu", "me", "mi").associate { WordId(it) to Read }, 900)

        val progress = repository.observeLessonProgress().first().getValue(LessonId("l2"))
        progress shouldBe LessonProgress(readWords = 5, wordCount = 6)
        progress.isPassed shouldBe true
    }

    @Test
    fun `history keeps attempts of words removed from the dictionary and skips unknown ratings`() = runTest {
        val repository = repository()
        repository.observeLessonProgress().first()
        database.attemptDao().insert(
            AttemptEntity(profileId = 1, wordId = "gone", rating = "Read", createdAtEpochMillis = 2, tempoMillis = 900),
        )
        database.attemptDao().insert(
            AttemptEntity(profileId = 1, wordId = "ma", rating = "Future", createdAtEpochMillis = 1, tempoMillis = 900),
        )

        val history = repository.observeHistory().first()
        history.map { it.wordId.value to it.wordText } shouldContainExactly listOf("gone" to null)
    }
}
