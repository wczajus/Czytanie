package pl.dev404.czytanie.core.data.content

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pl.dev404.czytanie.core.data.database.AttemptEntity
import pl.dev404.czytanie.core.data.database.CzytanieDatabase
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.WordId

@RunWith(RobolectricTestRunner::class)
class DictionaryImportTest {

    private lateinit var database: CzytanieDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            CzytanieDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun repository(json: String): RoomDictionaryRepository {
        val importer = DictionaryImporter({ json }, DictionaryParser(), database.dictionaryDao())
        return RoomDictionaryRepository(database.dictionaryDao(), importer)
    }

    @Test
    fun `first start imports the bundled dictionary`() = runTest {
        val repository = repository(TestDictionaries.json(version = 1))

        val levels = repository.observeLevels().first()

        levels.single().lessons shouldContainExactly listOf(LessonId("l2"))
        val lesson = repository.lesson(LessonId("l2")).shouldNotBeNull()
        lesson.words.map { it.text } shouldContainExactly listOf("ma", "mo", "mu", "me", "mi", "my")
        lesson.words.first().units.map { it.grapheme } shouldContainExactly listOf("m", "a")
        database.dictionaryDao().importedVersion() shouldBe 1
    }

    @Test
    fun `phoneme inventory comes with its recordings`() = runTest {
        val phonemes = repository(TestDictionaries.json(version = 1)).phonemes()

        phonemes.map { it.id } shouldContainExactly listOf("a", "e", "i", "m", "o", "u", "y")
        phonemes.first().audio shouldBe "audio/phonemes/a.ogg"
    }

    @Test
    fun `same version is not imported again`() = runTest {
        repository(TestDictionaries.json(version = 1)).word(WordId("ma")).shouldNotBeNull()

        // Different content under the same version: the stored dictionary must stay as it was.
        val second = repository(TestDictionaries.json(version = 1, firstSyllable = "ta"))

        second.word(WordId("ta")).shouldBeNull()
        second.word(WordId("ma")).shouldNotBeNull()
    }

    @Test
    fun `new version replaces the dictionary and keeps attempts`() = runTest {
        repository(TestDictionaries.json(version = 1)).word(WordId("ma")).shouldNotBeNull()
        database.attemptDao().insert(
            AttemptEntity(profileId = 1, wordId = "ma", rating = "Read", createdAtEpochMillis = 0, tempoMillis = 900),
        )

        val updated = repository(TestDictionaries.json(version = 2, firstSyllable = "ta"))

        updated.word(WordId("ta")).shouldNotBeNull()
        updated.word(WordId("ma")).shouldBeNull()
        database.dictionaryDao().importedVersion() shouldBe 2
        database.attemptDao().observeForProfile(1).first() shouldHaveSize 1
    }

    @Test
    fun `broken bundled dictionary is not imported`() = runTest {
        val repository = repository("{ broken")

        shouldThrow<InvalidDictionaryException> { repository.word(WordId("ma")) }
        database.dictionaryDao().importedVersion().shouldBeNull()
    }
}
