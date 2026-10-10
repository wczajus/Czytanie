package pl.dev404.czytanie.core.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Whole words through the system TTS engine, with an offline Polish voice only.
 *
 * Without a Polish voice the word is not read at all: a voice of another language would teach
 * the wrong pronunciation. Network voices are skipped so that nothing the child reads leaves the
 * device. The engine starts on first use, not with the app.
 */
@Singleton
class TtsWordSpeech @Inject constructor(@param:ApplicationContext private val context: Context) : WordSpeech {

    private val mutex = Mutex()
    private var engine: TextToSpeech? = null
    private var unavailable = false
    private val utterances = ConcurrentHashMap<String, CancellableContinuation<Unit>>()
    private val nextUtterance = AtomicInteger()

    override suspend fun isAvailable(): Boolean = readyEngine() != null

    override suspend fun speak(text: String) {
        val tts = readyEngine() ?: return
        val id = "word-${nextUtterance.incrementAndGet()}"
        suspendCancellableCoroutine { continuation ->
            utterances[id] = continuation
            continuation.invokeOnCancellation {
                utterances.remove(id)
                tts.stop()
            }
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) finish(id)
        }
    }

    private suspend fun readyEngine(): TextToSpeech? = mutex.withLock {
        engine?.let { return@withLock it }
        if (unavailable) return@withLock null
        val status = CompletableDeferred<Int>()
        val tts = withContext(Dispatchers.Main) { TextToSpeech(context) { status.complete(it) } }
        val ready =
            status.await() == TextToSpeech.SUCCESS && withContext(Dispatchers.Main) { tts.useOfflinePolishVoice() }
        if (!ready) {
            tts.shutdown()
            unavailable = true
            return@withLock null
        }
        tts.setOnUtteranceProgressListener(progressListener)
        engine = tts
        tts
    }

    private fun TextToSpeech.useOfflinePolishVoice(): Boolean {
        if (isLanguageAvailable(POLISH) < TextToSpeech.LANG_AVAILABLE) return false
        val offlineVoice = voices.orEmpty()
            .filter { it.locale.language == POLISH.language && it.isInstalledOffline() }
            .maxByOrNull { it.quality }
        if (offlineVoice != null) return setVoice(offlineVoice) == TextToSpeech.SUCCESS
        // Some engines list no voices; accept the language default only if it works offline.
        return setLanguage(POLISH) >= TextToSpeech.LANG_AVAILABLE && voice?.isInstalledOffline() == true
    }

    private fun Voice.isInstalledOffline() =
        !isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features.orEmpty()

    private fun finish(utteranceId: String?) {
        utterances.remove(utteranceId ?: return)?.resume(Unit)
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = finish(utteranceId)

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String?) = finish(utteranceId)

        override fun onError(
            utteranceId: String?,
            errorCode: Int,
        ) = finish(utteranceId)

        override fun onStop(
            utteranceId: String?,
            interrupted: Boolean,
        ) = finish(utteranceId)
    }

    private companion object {
        val POLISH: Locale = Locale.forLanguageTag("pl-PL")
    }
}
