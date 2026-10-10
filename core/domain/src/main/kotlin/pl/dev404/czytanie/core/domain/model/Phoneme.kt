package pl.dev404.czytanie.core.domain.model

/**
 * An entry of the phoneme inventory: a sound with its recording.
 * Phonemes and syllables are only ever played from recordings, never from TTS.
 */
data class Phoneme(
    val id: String,
    val grapheme: String,
    val audio: String,
)
