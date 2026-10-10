package pl.dev404.czytanie.core.domain.pacing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer
import pl.dev404.czytanie.core.domain.model.Word

/** Speech player whose recordings take a controlled amount of virtual time. */
class FakeSpeechPlayer(
    private val now: () -> Long,
    private val durationsMillis: Map<String, Long> = emptyMap(),
    private val defaultDurationMillis: Long = 0,
    private val failingPhonemes: Set<String> = emptySet(),
) : SpeechPlayer {

    data class Playback(
        val phonemeId: String,
        val startedAt: Long,
    )

    val phonemes = mutableListOf<Playback>()
    val cancelled = mutableListOf<String>()
    val syllables = mutableListOf<Playback>()
    val words = mutableListOf<Word>()

    override suspend fun playPhoneme(phonemeId: String) {
        phonemes += Playback(phonemeId, now())
        if (phonemeId in failingPhonemes) error("Missing recording for $phonemeId")
        try {
            delay(durationsMillis[phonemeId] ?: defaultDurationMillis)
        } catch (e: CancellationException) {
            cancelled += phonemeId
            throw e
        }
    }

    override suspend fun playSyllable(syllable: String) {
        syllables += Playback(syllable, now())
        delay(durationsMillis[syllable] ?: defaultDurationMillis)
    }

    override suspend fun playWord(word: Word) {
        words += word
    }
}
