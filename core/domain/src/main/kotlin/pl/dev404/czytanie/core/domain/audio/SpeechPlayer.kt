package pl.dev404.czytanie.core.domain.audio

import pl.dev404.czytanie.core.domain.model.Word

/**
 * Plays sounds for the reading screen. Every function returns only after playback has finished,
 * so the caller can wait for a recording before moving on. Cancelling the calling coroutine
 * must stop playback.
 *
 * Phonemes are always played from recordings; implementations must never use TTS for them,
 * because TTS reads letter names ("be") instead of sounds ("b").
 */
interface SpeechPlayer {
    /**
     * Loads what [words] need ahead of time (e.g. the phoneme recordings of a lesson), so the
     * first sound plays without delay. Playing something not prepared still works, just later.
     */
    suspend fun prepare(words: List<Word>) = Unit

    /** Plays the recording of a single phoneme, e.g. "sz". */
    suspend fun playPhoneme(phonemeId: String)

    /** Plays the whole word: its recording when available, otherwise TTS. */
    suspend fun playWord(word: Word)

    /**
     * Whether [playWord] can actually be heard: the word has a recording, or a Polish TTS voice is
     * installed. A word is never read in another language's voice.
     */
    suspend fun canPlayWord(word: Word): Boolean = true
}
