package pl.dev404.czytanie.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.WordFontSize

class DataStoreSettingsRepositoryTest {

    @TempDir
    lateinit var dir: File

    private val file get() = File(dir, "settings.preferences_pb")

    private fun TestScope.repository(scope: CoroutineScope = backgroundScope) = DataStoreSettingsRepository(
        PreferenceDataStoreFactory.create(scope = scope) { file },
    )

    @Test
    fun `defaults are slow tempo with phonemes on`() = runTest {
        repository().settings.first() shouldBe ReadingSettings(
            tempoMillis = 900,
            phonemesEnabled = true,
            fontSize = WordFontSize.Large,
        )
    }

    @Test
    fun `changes are emitted`() = runTest {
        val repository = repository()

        repository.settings.test {
            awaitItem().tempoMillis shouldBe 900
            repository.setTempo(600)
            awaitItem().tempoMillis shouldBe 600
            repository.setPhonemesEnabled(false)
            awaitItem().phonemesEnabled shouldBe false
            repository.setFontSize(WordFontSize.ExtraLarge)
            awaitItem().fontSize shouldBe WordFontSize.ExtraLarge
        }
    }

    @Test
    fun `tempo is kept within the allowed range`() = runTest {
        val repository = repository()

        repository.setTempo(50)
        repository.settings.first().tempoMillis shouldBe 300
        repository.setTempo(10_000)
        repository.settings.first().tempoMillis shouldBe 1500
    }

    @Test
    fun `settings survive a restart`() = runTest {
        val firstScope = TestScope(StandardTestDispatcher(testScheduler))
        val first = repository(firstScope)
        first.setTempo(1200)
        first.setFontSize(WordFontSize.Medium)
        firstScope.cancel()
        testScheduler.advanceUntilIdle()

        val second = repository()
        second.settings.first() shouldBe ReadingSettings(1200, true, WordFontSize.Medium)
    }
}
