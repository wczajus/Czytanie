package pl.dev404.czytanie.core.domain.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<ReadingSettings>

    suspend fun setTempo(tempoMillis: Long)

    suspend fun setPhonemesEnabled(enabled: Boolean)

    suspend fun setFontSize(fontSize: WordFontSize)
}
