package pl.dev404.czytanie.core.audio

import javax.inject.Inject
import javax.inject.Singleton
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer
import pl.dev404.czytanie.core.domain.model.Word

/**
 * Phonemes always come from recordings. A whole word comes from its recording when the
 * dictionary has one, otherwise from a Polish TTS voice.
 */
@Singleton
class AndroidSpeechPlayer @Inject constructor(
    private val phonemes: RecordedPhonemePlayer,
    private val recordings: RecordingPlayer,
    private val speech: WordSpeech,
) : SpeechPlayer {

    override suspend fun prepare(words: List<Word>) {
        phonemes.prepare(words.flatMap { word -> word.units.map { it.phonemeId } })
    }

    override suspend fun playPhoneme(phonemeId: String) = phonemes.play(phonemeId)

    override suspend fun playWord(word: Word) {
        val playedRecording = word.wordAudio?.let { recordings.play(it) } ?: false
        if (!playedRecording && speech.isAvailable()) speech.speak(word.text)
    }

    override suspend fun canPlayWord(word: Word): Boolean = word.wordAudio != null || speech.isAvailable()
}
