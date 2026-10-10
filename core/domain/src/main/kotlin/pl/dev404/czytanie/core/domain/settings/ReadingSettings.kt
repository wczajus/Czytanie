package pl.dev404.czytanie.core.domain.settings

import pl.dev404.czytanie.core.domain.pacing.PacingConfig
import pl.dev404.czytanie.core.domain.pacing.PointingUnit

/** Settings chosen by the parent. Defaults favour a slow, sound-supported first session. */
data class ReadingSettings(
    val tempoMillis: Long = PacingConfig.DEFAULT_TEMPO_MILLIS,
    val phonemesEnabled: Boolean = true,
    val fontSize: WordFontSize = WordFontSize.Large,
    val pointingUnit: PointingUnit = PointingUnit.Graphemes,
) {
    fun toPacingConfig() = PacingConfig(
        tempoMillis = tempoMillis.coerceIn(PacingConfig.TEMPO_RANGE_MILLIS),
        phonemesEnabled = phonemesEnabled,
        pointingUnit = pointingUnit,
    )
}

/** Size of the word on the reading screen. The UI maps it to concrete sizes. */
enum class WordFontSize { Medium, Large, ExtraLarge }
