package pl.dev404.czytanie.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.SettingsRepository
import pl.dev404.czytanie.core.domain.settings.WordFontSize

sealed interface SettingsEvent {
    data class TempoChanged(val tempoMillis: Long) : SettingsEvent

    data class PhonemesToggled(val enabled: Boolean) : SettingsEvent

    data class FontSizeChosen(val fontSize: WordFontSize) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<ReadingSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.TempoChanged -> settingsRepository.setTempo(event.tempoMillis)
                is SettingsEvent.PhonemesToggled -> settingsRepository.setPhonemesEnabled(event.enabled)
                is SettingsEvent.FontSizeChosen -> settingsRepository.setFontSize(event.fontSize)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
