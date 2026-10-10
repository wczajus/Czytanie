package pl.dev404.czytanie.feature.reading

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import pl.dev404.czytanie.core.domain.pacing.ReadingState
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.WordFontSize

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingViewModelTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcherExtension()

    private val speechPlayer = FakeSpeechPlayer(phonemeMillis = 0, wordMillis = WORD_MILLIS)

    private fun viewModel(
        lessonId: String = TestWords.LESSON_ID,
        settings: ReadingSettings = ReadingSettings(tempoMillis = TEMPO),
    ) = ReadingViewModel(
        savedStateHandle = SavedStateHandle(mapOf(ReadingRoute.LESSON_ID_KEY to lessonId)),
        dictionaryRepository = FakeDictionaryRepository(),
        settingsRepository = FakeSettingsRepository(settings),
        speechPlayer = speechPlayer,
    )

    private fun test(block: suspend TestScope.(ReadingViewModel, ReceiveTurbine<ReadingUiState>) -> Unit) =
        runTest(main.dispatcher) {
            val viewModel = viewModel()
            viewModel.uiState.test { block(viewModel, this) }
        }

    private suspend fun ReceiveTurbine<ReadingUiState>.reading(): ReadingUiState.Reading =
        expectMostRecentItem().shouldBeInstanceOf<ReadingUiState.Reading>()

    /** Lets the whole word pass under the arrow. */
    private fun TestScope.readWholeWord(units: Int) {
        advanceTimeBy(TEMPO * units + 1)
        runCurrent()
    }

    @Test
    fun `shows the first word of the lesson waiting for the child`() = test { _, states ->
        advanceUntilIdle()
        val state = states.reading()
        state.word shouldBe TestWords.mama
        state.wordIndex shouldBe 0
        state.wordCount shouldBe 2
        state.readingState shouldBe ReadingState.Idle
        state.canListen shouldBe false
        state.canRepeat shouldBe false
    }

    @Test
    fun `main action starts the arrow with the tempo from settings`() = test { viewModel, states ->
        advanceUntilIdle()
        viewModel.onEvent(ReadingEvent.MainAction)
        runCurrent()
        states.reading().readingState shouldBe ReadingState.Pointing(0, TEMPO)
    }

    @Test
    fun `listening is possible only after the arrow has passed the whole word`() = test { viewModel, states ->
        advanceUntilIdle()
        viewModel.onEvent(ReadingEvent.MainAction)
        runCurrent()
        viewModel.onEvent(ReadingEvent.Listen)
        runCurrent()
        states.reading().canListen shouldBe false
        speechPlayer.words.shouldBeEmpty()

        readWholeWord(TestWords.mama.units.size)
        val done = states.reading()
        done.readingState shouldBe ReadingState.WordDone
        done.canListen shouldBe true

        viewModel.onEvent(ReadingEvent.Listen)
        runCurrent()
        states.reading().isPlayingWord shouldBe true
        advanceTimeBy(WORD_MILLIS + 1)
        runCurrent()
        states.reading().isPlayingWord shouldBe false
        speechPlayer.words shouldContainExactly listOf(TestWords.mama)
    }

    @Test
    fun `the whole word is never played on its own`() = test { viewModel, states ->
        advanceUntilIdle()
        viewModel.onEvent(ReadingEvent.MainAction)
        readWholeWord(TestWords.mama.units.size)
        advanceUntilIdle()
        states.reading().readingState shouldBe ReadingState.WordDone
        speechPlayer.words.shouldBeEmpty()
        speechPlayer.phonemes shouldContainExactly listOf("m", "a", "m", "a")
    }

    @Test
    fun `tapping the word pauses and resumes the arrow`() = test { viewModel, states ->
        advanceUntilIdle()
        viewModel.onEvent(ReadingEvent.WordTapped)
        advanceTimeBy(TEMPO + 1)
        runCurrent()
        states.reading().readingState shouldBe ReadingState.Pointing(1, TEMPO)

        viewModel.onEvent(ReadingEvent.WordTapped)
        runCurrent()
        states.reading().readingState shouldBe ReadingState.Paused(1)
        advanceTimeBy(TEMPO * 10)
        runCurrent()
        states.expectNoEvents()

        viewModel.onEvent(ReadingEvent.WordTapped)
        runCurrent()
        states.reading().readingState shouldBe ReadingState.Pointing(1, TEMPO)
    }

    @Test
    fun `repeat starts the word again from its first unit`() = test { viewModel, states ->
        advanceUntilIdle()
        viewModel.onEvent(ReadingEvent.MainAction)
        readWholeWord(TestWords.mama.units.size)
        viewModel.onEvent(ReadingEvent.Repeat)
        runCurrent()
        states.reading().readingState shouldBe ReadingState.Pointing(0, TEMPO)
    }

    @Test
    fun `next is ignored until the word is read`() = test { viewModel, states ->
        advanceUntilIdle()
        viewModel.onEvent(ReadingEvent.MainAction)
        runCurrent()
        viewModel.onEvent(ReadingEvent.Next)
        runCurrent()
        states.reading().wordIndex shouldBe 0
    }

    @Test
    fun `next shows the following word and the last one finishes the lesson`() = test { viewModel, states ->
        advanceUntilIdle()
        viewModel.onEvent(ReadingEvent.MainAction)
        readWholeWord(TestWords.mama.units.size)
        viewModel.onEvent(ReadingEvent.Next)
        runCurrent()
        val second = states.reading()
        second.word shouldBe TestWords.szafa
        second.wordIndex shouldBe 1
        second.readingState shouldBe ReadingState.Idle

        viewModel.onEvent(ReadingEvent.MainAction)
        readWholeWord(TestWords.szafa.units.size)
        viewModel.onEvent(ReadingEvent.Next)
        runCurrent()
        states.expectMostRecentItem() shouldBe ReadingUiState.LessonFinished(wordCount = 2)

        viewModel.onEvent(ReadingEvent.ReadLessonAgain)
        runCurrent()
        val again = states.reading()
        again.wordIndex shouldBe 0
        again.readingState shouldBe ReadingState.Idle
    }

    @Test
    fun `the lesson's sounds are prepared when it opens`() = test { _, _ ->
        advanceUntilIdle()
        speechPlayer.prepared shouldContainExactly TestWords.lesson.words
    }

    @Test
    fun `listening is not offered when the word cannot be heard`() = runTest(main.dispatcher) {
        val silentPlayer = FakeSpeechPlayer(wordsCanBeHeard = false)
        val viewModel = ReadingViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ReadingRoute.LESSON_ID_KEY to TestWords.LESSON_ID)),
            dictionaryRepository = FakeDictionaryRepository(),
            settingsRepository = FakeSettingsRepository(ReadingSettings(tempoMillis = TEMPO)),
            speechPlayer = silentPlayer,
        )
        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.onEvent(ReadingEvent.MainAction)
            readWholeWord(TestWords.mama.units.size)
            val done = reading()
            done.canPlayWordAudio shouldBe false
            done.canListen shouldBe false
            viewModel.onEvent(ReadingEvent.Listen)
            advanceUntilIdle()
            silentPlayer.words.shouldBeEmpty()
        }
    }

    @Test
    fun `font size comes from settings`() = runTest(main.dispatcher) {
        val viewModel = viewModel(settings = ReadingSettings(fontSize = WordFontSize.ExtraLarge))
        viewModel.uiState.test {
            advanceUntilIdle()
            reading().fontSize shouldBe WordFontSize.ExtraLarge
        }
    }

    @Test
    fun `unknown lesson is reported instead of crashing`() = runTest(main.dispatcher) {
        val viewModel = viewModel(lessonId = "missing")
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem() shouldBe ReadingUiState.LessonNotFound
        }
    }

    private companion object {
        const val TEMPO = 500L
        const val WORD_MILLIS = 700L
    }
}
