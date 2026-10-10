package pl.dev404.czytanie.feature.parent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.ParentRating
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.model.WordId
import pl.dev404.czytanie.core.domain.progress.ProgressRepository
import pl.dev404.czytanie.core.domain.settings.SettingsRepository

sealed interface LessonRatingUiState {
    data object Loading : LessonRatingUiState

    data object LessonNotFound : LessonRatingUiState

    data class Rating(
        val words: List<Word>,
        val ratings: Map<WordId, ParentRating> = emptyMap(),
        val isSaving: Boolean = false,
    ) : LessonRatingUiState {
        val canSave: Boolean get() = ratings.isNotEmpty() && !isSaving
    }

    /** Saved; the screen closes. */
    data object Saved : LessonRatingUiState
}

sealed interface LessonRatingEvent {
    /** Tapping the selected rating again clears it. */
    data class Rate(
        val wordId: WordId,
        val rating: ParentRating,
    ) : LessonRatingEvent

    data object Save : LessonRatingEvent
}

@HiltViewModel
class LessonRatingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    dictionaryRepository: DictionaryRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val lessonId = LessonId(checkNotNull(savedStateHandle[LessonRatingRoute.LESSON_ID_KEY]))
    private val mutableState = MutableStateFlow<LessonRatingUiState>(LessonRatingUiState.Loading)
    val uiState: StateFlow<LessonRatingUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val lesson = dictionaryRepository.lesson(lessonId)
            mutableState.value = if (lesson == null || lesson.words.isEmpty()) {
                LessonRatingUiState.LessonNotFound
            } else {
                LessonRatingUiState.Rating(lesson.words)
            }
        }
    }

    fun onEvent(event: LessonRatingEvent) {
        when (event) {
            is LessonRatingEvent.Rate -> mutableState.update { state ->
                if (state !is LessonRatingUiState.Rating) return@update state
                val current = state.ratings[event.wordId]
                val ratings = if (current ==
                    event.rating
                ) {
                    state.ratings - event.wordId
                } else {
                    state.ratings + (event.wordId to event.rating)
                }
                state.copy(ratings = ratings)
            }

            LessonRatingEvent.Save -> save()
        }
    }

    private fun save() {
        val state = mutableState.value as? LessonRatingUiState.Rating ?: return
        if (!state.canSave) return
        mutableState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val tempo = settingsRepository.settings.first().tempoMillis
            progressRepository.saveRatings(state.ratings, tempo)
            mutableState.value = LessonRatingUiState.Saved
        }
    }
}
