package pl.dev404.czytanie.core.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Phoneme clips in a [SoundPool]: low latency, so the sound starts the moment the arrow arrives.
 * SoundPool does not report the end of playback, so the length is read once when loading.
 */
@Singleton
class SoundPoolClipPlayer @Inject constructor(@param:ApplicationContext private val context: Context) : ClipPlayer {

    private val pendingLoads = ConcurrentHashMap<Int, CompletableDeferred<Boolean>>()

    // Created on the main thread (see load), so load callbacks arrive on the main looper.
    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(SpeechAudioAttributes)
            .build()
            .apply {
                setOnLoadCompleteListener { _, soundId, status -> pendingLoads.remove(soundId)?.complete(status == 0) }
            }
    }

    override suspend fun load(assetPath: String): Clip? = try {
        val durationMillis = withContext(Dispatchers.IO) { durationOf(assetPath) }
        val loaded = CompletableDeferred<Boolean>()
        val soundId = withContext(Dispatchers.Main) {
            // Registered before the main looper can deliver the load callback.
            context.assets.openFd(assetPath).use { soundPool.load(it, 1) }.also { pendingLoads[it] = loaded }
        }
        if (loaded.await()) Clip(soundId, durationMillis) else null
    } catch (_: IOException) {
        null
    }

    override suspend fun play(clip: Clip) {
        val streamId = soundPool.play(clip.soundId, FULL_VOLUME, FULL_VOLUME, 1, 0, NORMAL_RATE)
        if (streamId == 0) return
        try {
            delay(clip.durationMillis)
        } finally {
            soundPool.stop(streamId)
        }
    }

    private fun durationOf(assetPath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            context.assets.openFd(assetPath).use {
                retriever.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: IllegalArgumentException) {
            0L
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val MAX_STREAMS = 2
        const val FULL_VOLUME = 1f
        const val NORMAL_RATE = 1f
    }
}
