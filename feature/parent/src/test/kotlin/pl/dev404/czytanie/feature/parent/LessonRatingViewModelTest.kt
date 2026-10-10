package pl.dev404.czytanie.feature.parent

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.Lesson
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.ParentRating
import pl.dev404.czytanie.core.domain.model.Phoneme
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.model.WordId
import pl.dev404.czytanie.core.domain.progress.Attempt
import pl.dev404.czytanie.core.domain.progress.LessonProgress
import pl.dev404.czytanie.core.domain.progress.ProgressRepository
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.SettingsRepository
import pl.dev404.czytanie.core.domain.settings.WordFontSize

@OptIn(ExperimentalCoroutinesApi::class)
class LessonRatingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val mama = Word.fromAnnotation("mama", "mama", listOf("m", "a", "m", "a"), listOf("ma", "ma"), 3, "l3")
    private val tata = Word.fromAnnotation("tata", "tata", listOf("t", "a", "t", "a"), listOf("ta", "ta"), 3, "l3")
    private val progress = FakeProgressRepository()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(lessonId: String = "l3") = LessonRatingViewModel(
        savedStateHandle = SavedStateHandle(mapOf(LessonRatingRoute.LESSON_ID_KEY to lessonId)),
        dictionaryRepository = FakeDictionary(Lesson(LessonId("l3"), 3, listOf(mama, tata))),
        progressRepository = progress,
        settingsRepository = FakeSettings(ReadingSettings(tempoMillis = 1100)),
    )

    @Test
    fun `nothing can be saved before a word is rated`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.test {
            advanceUntilIdle()
            val state = expectMostRecentItem().shouldBeInstanceOf<LessonRatingUiState.Rating>()
            state.words shouldBe listOf(mama, tata)
            state.canSave shouldBe false
        }
    }

    @Test
    fun `only rated words are saved, with the current tempo`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.onEvent(LessonRatingEvent.Rate(mama.id, ParentRating.WithHelp))
            viewModel.onEvent(LessonRatingEvent.Rate(mama.id, ParentRating.Read))
            viewModel.onEvent(LessonRatingEvent.Save)
            advanceUntilIdle()

            expectMostRecentItem() shouldBe LessonRatingUiState.Saved
            progress.saved shouldBe listOf(mapOf(mama.id to ParentRating.Read) to 1100L)
        }
    }

    @Test
    fun `tapping the chosen rating again clears it`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.uiState.test {
            advanceUntilIdle()
            viewModel.onEvent(LessonRatingEvent.Rate(tata.id, ParentRating.NotYet))
            viewModel.onEvent(LessonRatingEvent.Rate(tata.id, ParentRating.NotYet))
            advanceUntilIdle()

            expectMostRecentItem().shouldBeInstanceOf<LessonRatingUiState.Rating>().ratings shouldBe emptyMap()
        }
    }

    @Test
    fun `unknown lesson is reported`() = runTest(dispatcher) {
        val viewModel = viewModel(lessonId = "missing")
        viewModel.uiState.test {
            advanceUntilIdle()
            expectMostRecentItem() shouldBe LessonRatingUiState.LessonNotFound
        }
    }
}

private class FakeDictionary(private val lesson: Lesson) : DictionaryRepository {
    override fun observeLevels(): Flow<List<Level>> = emptyFlow()

    override suspend fun lesson(id: LessonId): Lesson? = lesson.takeIf { it.id == id }

    override suspend fun word(id: WordId): Word? = null

    override suspend fun phonemes(): List<Phoneme> = emptyList()
}

private class FakeProgressRepository : ProgressRepository {
    val saved = mutableListOf<Pair<Map<WordId, ParentRating>, Long>>()

    override suspend fun saveRatings(
        ratings: Map<WordId, ParentRating>,
        tempoMillis: Long,
    ) {
        saved += ratings to tempoMillis
    }

    override fun observeHistory(): Flow<List<Attempt>> = emptyFlow()

    override fun observeLessonProgress(): Flow<Map<LessonId, LessonProgress>> = emptyFlow()
}

private class FakeSettings(initial: ReadingSettings) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun setTempo(tempoMillis: Long) = Unit

    override suspend fun setPhonemesEnabled(enabled: Boolean) = Unit

    override suspend fun setFontSize(fontSize: WordFontSize) = Unit
}
