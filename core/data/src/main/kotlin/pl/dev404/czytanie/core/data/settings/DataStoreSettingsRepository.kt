package pl.dev404.czytanie.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import pl.dev404.czytanie.core.domain.pacing.PacingConfig
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.SettingsRepository
import pl.dev404.czytanie.core.domain.settings.WordFontSize

class DataStoreSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    SettingsRepository {

    override val settings: Flow<ReadingSettings> = dataStore.data
        // A corrupted or unreadable file must not lock the child out: fall back to defaults.
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun setTempo(tempoMillis: Long) {
        dataStore.edit { it[Keys.TEMPO] = tempoMillis.coerceIn(PacingConfig.TEMPO_RANGE_MILLIS) }
    }

    override suspend fun setPhonemesEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.PHONEMES_ENABLED] = enabled }
    }

    override suspend fun setFontSize(fontSize: WordFontSize) {
        dataStore.edit { it[Keys.FONT_SIZE] = fontSize.name }
    }

    private fun Preferences.toSettings(): ReadingSettings {
        val defaults = ReadingSettings()
        return ReadingSettings(
            tempoMillis = (this[Keys.TEMPO] ?: defaults.tempoMillis).coerceIn(PacingConfig.TEMPO_RANGE_MILLIS),
            phonemesEnabled = this[Keys.PHONEMES_ENABLED] ?: defaults.phonemesEnabled,
            fontSize = WordFontSize.entries.firstOrNull { it.name == this[Keys.FONT_SIZE] } ?: defaults.fontSize,
        )
    }

    private object Keys {
        val TEMPO = longPreferencesKey("tempo_millis")
        val PHONEMES_ENABLED = booleanPreferencesKey("phonemes_enabled")
        val FONT_SIZE = stringPreferencesKey("word_font_size")
    }
}
