package pl.dev404.czytanie.feature.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.Level

sealed interface LevelsUiState {
    data object Loading : LevelsUiState

    data class Ready(val levels: List<Level>) : LevelsUiState
}

@HiltViewModel
class LevelsViewModel @Inject constructor(dictionaryRepository: DictionaryRepository) : ViewModel() {
    val uiState: StateFlow<LevelsUiState> = dictionaryRepository.observeLevels()
        .map<List<Level>, LevelsUiState> { LevelsUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), LevelsUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
