package pl.dev404.czytanie.core.audio

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.Syllable

/**
 * Plays phonemes and syllables from their recordings. Phonemes are looked up in the dictionary's
 * phoneme inventory, syllables by [Syllable.audioPathOf]. Clips are kept in memory once loaded; a
 * lesson preloads the ones it needs.
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
    private var phonemePaths: Map<String, String>? = null
    private val clips = mutableMapOf<String, Clip?>()

    suspend fun prepare(phonemeIds: Collection<String>) {
        phonemeIds.distinct().forEach { phonemePath(it)?.let { path -> clip(path) } }
    }

    suspend fun play(phonemeId: String) {
        phonemePath(phonemeId)?.let { path -> clip(path)?.let { clipPlayer.play(it) } }
    }

    suspend fun prepareSyllables(syllables: Collection<String>) {
        syllables.map(Syllable::audioPathOf).distinct().forEach { clip(it) }
    }

    suspend fun playSyllable(syllable: String) {
        clip(Syllable.audioPathOf(syllable))?.let { clipPlayer.play(it) }
    }

    private suspend fun phonemePath(phonemeId: String): String? {
        val paths = phonemePaths ?: dictionary.phonemes().associate { it.id to it.audio }.also { phonemePaths = it }
        return paths[phonemeId]
    }

    private suspend fun clip(assetPath: String): Clip? = mutex.withLock {
        if (assetPath in clips) return@withLock clips[assetPath]
        val clip = clipPlayer.load(assetPath)
        clips[assetPath] = clip
        clip
    }
}
