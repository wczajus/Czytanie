package pl.dev404.czytanie.core.domain.pacing

/**
 * State of the arrow while a word is read. The domain only says which unit is current and for
 * how long; the UI turns this into animation.
 */
sealed interface ReadingState {
    /** Nothing started yet. */
    data object Idle : ReadingState

    /**
     * The arrow points at unit [index]. The step lasts at least [tempoMillis]; the UI uses it as the
     * duration of the arrow's movement under this unit.
     */
    data class Pointing(
        val index: Int,
        val tempoMillis: Long,
    ) : ReadingState

    /**
     * The tempo of unit [index] has elapsed but its recording is still playing. The arrow stays
     * under the unit until it ends, so a recording is never cut off.
     */
    data class WaitingForAudio(val index: Int) : ReadingState

    /** The child tapped the word. Resuming restarts unit [index] from its beginning. */
    data class Paused(val index: Int) : ReadingState

    /** The arrow has passed the whole word. Unlocks "posłuchaj" and the parent's rating. */
    data object WordDone : ReadingState
}

/** Index of the unit the arrow is at, or null when no unit is current. */
val ReadingState.currentIndex: Int?
    get() = when (this) {
        is ReadingState.Pointing -> index
        is ReadingState.WaitingForAudio -> index
        is ReadingState.Paused -> index
        ReadingState.Idle, ReadingState.WordDone -> null
    }
