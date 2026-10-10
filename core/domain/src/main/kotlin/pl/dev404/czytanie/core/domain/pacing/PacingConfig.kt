package pl.dev404.czytanie.core.domain.pacing

/**
 * @property tempoMillis minimum time the arrow spends on one unit.
 * @property phonemesEnabled whether each unit's recording is played when the arrow reaches it.
 */
data class PacingConfig(
    val tempoMillis: Long = DEFAULT_TEMPO_MILLIS,
    val phonemesEnabled: Boolean = true,
) {
    init {
        require(tempoMillis in TEMPO_RANGE_MILLIS) { "Tempo $tempoMillis ms outside $TEMPO_RANGE_MILLIS" }
    }

    companion object {
        const val DEFAULT_TEMPO_MILLIS = 900L
        val TEMPO_RANGE_MILLIS = 300L..1500L
    }
}
