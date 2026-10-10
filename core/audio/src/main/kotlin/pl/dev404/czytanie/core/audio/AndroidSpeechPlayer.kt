package pl.dev404.czytanie.core.audio

import javax.inject.Inject
import javax.inject.Singleton
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.pacing.PointingUnit

/**
 * Phonemes and syllables always come from recordings. A whole word comes from its recording when the
 * dictionary has one, otherwise from a Polish TTS voice.
 */
@Singleton
class AndroidSpeechPlayer @Inject constructor(
    private val phonemes: RecordedPhonemePlayer,
    private val recordings: RecordingPlayer,
    private val speech: WordSpeech,
) : SpeechPlayer {

    override suspend fun prepare(
        words: List<Word>,
        pointingUnit: PointingUnit,
    ) {
        when (pointingUnit) {
            PointingUnit.Graphemes -> phonemes.prepare(words.flatMap { word -> word.units.map { it.phonemeId } })
            PointingUnit.Syllables -> phonemes.prepareSyllables(words.flatMap { it.syllables }.map { it.text })
        }
    }

    override suspend fun playPhoneme(phonemeId: String) = phonemes.play(phonemeId)

    override suspend fun playSyllable(syllable: String) = phonemes.playSyllable(syllable)

    override suspend fun playWord(word: Word) {
        val playedRecording = word.wordAudio?.let { recordings.play(it) } ?: false
        if (!playedRecording && speech.isAvailable()) speech.speak(word.text)
    }

    override suspend fun canPlayWord(word: Word): Boolean = word.wordAudio != null || speech.isAvailable()
}
