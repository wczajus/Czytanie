package pl.dev404.czytanie.feature.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.dev404.czytanie.core.domain.progress.Attempt
import pl.dev404.czytanie.core.domain.progress.ProgressRepository

data class ParentPanelUiState(
    val isLoading: Boolean = true,
    val history: List<Attempt> = emptyList(),
)

@HiltViewModel
class ParentPanelViewModel @Inject constructor(progressRepository: ProgressRepository) : ViewModel() {
    val uiState: StateFlow<ParentPanelUiState> = progressRepository.observeHistory()
        .map { ParentPanelUiState(isLoading = false, history = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ParentPanelUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
