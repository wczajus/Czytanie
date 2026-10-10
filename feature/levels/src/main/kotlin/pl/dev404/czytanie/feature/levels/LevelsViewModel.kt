package pl.dev404.czytanie.feature.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.progress.ProgressRepository

sealed interface LevelsUiState {
    data object Loading : LevelsUiState

    /** @property passedLessons lessons the parent has rated as read well enough (8 of 10 words). */
    data class Ready(
        val levels: List<Level>,
        val passedLessons: Set<LessonId> = emptySet(),
    ) : LevelsUiState
}

@HiltViewModel
class LevelsViewModel @Inject constructor(
    dictionaryRepository: DictionaryRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<LevelsUiState> =
        combine(dictionaryRepository.observeLevels(), progressRepository.observeLessonProgress()) { levels, progress ->
            LevelsUiState.Ready(levels, progress.filterValues { it.isPassed }.keys)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LevelsUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
