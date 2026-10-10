package pl.dev404.czytanie.core.audio

import javax.inject.Inject
import javax.inject.Singleton
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer
import pl.dev404.czytanie.core.domain.model.Word

/**
 * Plays nothing and returns at once, so the reading screen works before real audio exists.
 * The arrow then simply follows the tempo.
 *
 * TODO(audio): replace with RecordedPhonemePlayer (SoundPool) and TtsWordPlayer in E4.
 */
@Singleton
class SilentSpeechPlayer @Inject constructor() : SpeechPlayer {
    override suspend fun playPhoneme(phonemeId: String) = Unit

    override suspend fun playWord(word: Word) = Unit
}
