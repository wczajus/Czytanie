package pl.dev404.czytanie.core.domain.pacing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer
import pl.dev404.czytanie.core.domain.model.ReadingUnit
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.pacing.ReadingState.Idle
import pl.dev404.czytanie.core.domain.pacing.ReadingState.Paused
import pl.dev404.czytanie.core.domain.pacing.ReadingState.Pointing
import pl.dev404.czytanie.core.domain.pacing.ReadingState.WaitingForAudio
import pl.dev404.czytanie.core.domain.pacing.ReadingState.WordDone

/**
 * Moves the arrow through the units of a word.
 *
 * ```
 * Idle ──start──▶ Pointing(0)
 * Pointing(i) ──tempo elapsed, recording still playing──▶ WaitingForAudio(i)
 * Pointing(i) ──tempo elapsed, recording done (or phonemes off)──▶ Pointing(i+1)
 * WaitingForAudio(i) ──recording done──▶ Pointing(i+1)
 * Pointing(i) | WaitingForAudio(i) ──pause──▶ Paused(i) ──resume──▶ Pointing(i)
 * last unit done ──▶ WordDone
 * ```
 *
 * A step therefore lasts `max(tempo, recording)`: a recording is never cut off. The recording
 * starts together with the step, so the sound plays the moment the arrow reaches the unit.
 *
 * Time comes only from coroutine [delay] on [scope], so tests drive it with virtual time
 * (`runTest` / `advanceTimeBy`). The whole word is never played here: that is the child's
 * choice after [WordDone].
 *
 * Commands are expected on the thread of [scope]'s dispatcher (the main thread in the app).
 * Stale steps are additionally guarded by a run generation, so a cancelled step can never
 * overwrite the state of a newer one.
 */
class PacingEngine(
    private val scope: CoroutineScope,
    private val speechPlayer: SpeechPlayer,
    initialConfig: PacingConfig = PacingConfig(),
) {
    private val mutableState = MutableStateFlow<ReadingState>(Idle)
    val state: StateFlow<ReadingState> = mutableState.asStateFlow()

    /** Takes effect from the next step; the current one finishes with the old tempo. */
    var config: PacingConfig = initialConfig

    private var word: Word? = null
    private var job: Job? = null
    private var generation = 0

    /** Starts reading [word] from its first unit, abandoning any word in progress. */
    fun start(word: Word) {
        this.word = word
        runFrom(0)
    }

    /** Starts the current word again from its first unit. No-op before [start]. */
    fun restart() {
        if (word != null) runFrom(0)
    }

    /** Stops the arrow on the current unit. No-op when nothing is moving. */
    fun pause() {
        val index = when (val current = state.value) {
            is Pointing -> current.index
            is WaitingForAudio -> current.index
            else -> return
        }
        cancelRun()
        mutableState.value = Paused(index)
    }

    /** Continues from the paused unit, replaying it from its beginning. No-op unless paused. */
    fun resume() {
        val paused = state.value as? Paused ?: return
        runFrom(paused.index)
    }

    /** Tapping the word: pauses a moving arrow, resumes a paused one. */
    fun togglePause() {
        if (state.value is Paused) resume() else pause()
    }

    /** Forgets the word and returns to [Idle]. */
    fun reset() {
        cancelRun()
        word = null
        mutableState.value = Idle
    }

    private fun runFrom(startIndex: Int) {
        val units = checkNotNull(word).units
        val runGeneration = cancelRun()
        job = scope.launch {
            for (index in startIndex until units.size) {
                runStep(runGeneration, index, units[index])
            }
            emit(runGeneration, WordDone)
        }
    }

    private suspend fun runStep(
        runGeneration: Int,
        index: Int,
        unit: ReadingUnit,
    ) = coroutineScope {
        val stepConfig = config
        emit(runGeneration, Pointing(index, stepConfig.tempoMillis))
        val recording = if (stepConfig.phonemesEnabled) launch { playSafely(unit.phonemeId) } else null
        delay(stepConfig.tempoMillis)
        if (recording?.isActive == true) {
            emit(runGeneration, WaitingForAudio(index))
            recording.join()
        }
    }

    /** A broken or missing recording must not stop the child from reading. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun playSafely(phonemeId: String) {
        try {
            speechPlayer.playPhoneme(phonemeId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Treated as a recording of zero length; the tempo still applies.
        }
    }

    private fun emit(
        runGeneration: Int,
        newState: ReadingState,
    ) {
        if (runGeneration == generation) mutableState.value = newState
    }

    /** Cancels the running word, if any, and returns the generation for the next run. */
    private fun cancelRun(): Int {
        job?.cancel()
        job = null
        return ++generation
    }
}
