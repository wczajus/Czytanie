package pl.dev404.czytanie.core.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.dev404.czytanie.core.domain.content.DictionaryRepository

/**
 * Plays phonemes from their recordings, looked up in the dictionary's phoneme inventory.
 * Clips are kept in memory once loaded; a lesson preloads the ones it needs.
 *
 * A missing recording is skipped silently: the arrow still follows the tempo, and the dictionary
 * validator is what catches missing files.
 */
@Singleton
class RecordedPhonemePlayer @Inject constructor(
    private val dictionary: DictionaryRepository,
    private val clipPlayer: ClipPlayer,
) {
    private val mutex = Mutex()
    private var audioPaths: Map<String, String>? = null
    private val clips = mutableMapOf<String, Clip?>()

    suspend fun prepare(phonemeIds: Collection<String>) {
        phonemeIds.distinct().forEach { clip(it) }
    }

    suspend fun play(phonemeId: String) {
        clip(phonemeId)?.let { clipPlayer.play(it) }
    }

    private suspend fun clip(phonemeId: String): Clip? = mutex.withLock {
        if (phonemeId in clips) return@withLock clips[phonemeId]
        val paths = audioPaths ?: dictionary.phonemes().associate { it.id to it.audio }.also { audioPaths = it }
        val clip = paths[phonemeId]?.let { clipPlayer.load(it) }
        clips[phonemeId] = clip
        clip
    }
}
