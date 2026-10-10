package pl.dev404.czytanie.core.audio

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Plays a whole recording from assets with [MediaPlayer], which reports when it has ended. */
class MediaPlayerRecordingPlayer @Inject constructor(@param:ApplicationContext private val context: Context) :
    RecordingPlayer {

    override suspend fun play(assetPath: String): Boolean = withContext(Dispatchers.Main) {
        val player = MediaPlayer()
        try {
            context.assets.openFd(assetPath).use { player.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            player.setAudioAttributes(SpeechAudioAttributes)
            player.prepare()
            suspendCancellableCoroutine { continuation ->
                player.setOnCompletionListener { if (continuation.isActive) continuation.resume(true) }
                player.setOnErrorListener { _, _, _ ->
                    if (continuation.isActive) continuation.resume(false)
                    true
                }
                player.start()
            }
        } catch (_: IOException) {
            false
        } catch (_: IllegalStateException) {
            false
        } finally {
            // Also stops playback when the caller is cancelled.
            player.release()
        }
    }
}
