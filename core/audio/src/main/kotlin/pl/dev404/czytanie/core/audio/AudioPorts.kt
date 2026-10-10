package pl.dev404.czytanie.core.audio

/** A short recording loaded into memory, with its length (SoundPool reports no end of playback). */
data class Clip(
    val soundId: Int,
    val durationMillis: Long,
)

/** Plays short clips from assets: phonemes (and syllables later). */
interface ClipPlayer {
    /** Loads the clip at [assetPath], or returns null when it is missing or unreadable. */
    suspend fun load(assetPath: String): Clip?

    /** Plays [clip] and returns when it has ended. Cancelling stops it. */
    suspend fun play(clip: Clip)
}

/** Plays a longer recording from assets to its end, e.g. a whole word. */
interface RecordingPlayer {
    /** Returns false when the recording could not be played. Cancelling stops it. */
    suspend fun play(assetPath: String): Boolean
}

/** Reads whole words aloud with a Polish TTS voice. Never used for phonemes or syllables. */
interface WordSpeech {
    /** Whether an offline Polish voice is installed. */
    suspend fun isAvailable(): Boolean

    /** Reads [text] and returns when it has been spoken. Cancelling stops it. */
    suspend fun speak(text: String)
}
