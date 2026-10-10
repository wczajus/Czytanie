package pl.dev404.czytanie.feature.reading

import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.pacing.ReadingState
import pl.dev404.czytanie.core.domain.settings.WordFontSize

sealed interface ReadingUiState {
    data object Loading : ReadingUiState

    data object LessonNotFound : ReadingUiState

    /**
     * One word of the lesson.
     *
     * @property wordIndex position of [word] in the lesson, from 0.
     * @property isPlayingWord the whole word is being played after the child asked for it.
     * @property canPlayWordAudio the word can be heard at all: it has a recording or a Polish
     *   TTS voice is installed. Without either, "posłuchaj" is not offered.
     */
    data class Reading(
        val word: Word,
        val wordIndex: Int,
        val wordCount: Int,
        val readingState: ReadingState,
        val fontSize: WordFontSize,
        val isPlayingWord: Boolean = false,
        val canPlayWordAudio: Boolean = true,
    ) : ReadingUiState {
        val isWordDone: Boolean get() = readingState == ReadingState.WordDone

        /** The whole word may be heard only after the arrow has passed it: the child reads first. */
        val canListen: Boolean get() = canPlayWordAudio && isWordDone && !isPlayingWord

        val canGoNext: Boolean get() = isWordDone

        /** Repeating makes sense once the arrow has started. */
        val canRepeat: Boolean get() = readingState != ReadingState.Idle

        /** The picture is a reward for reading, never a hint before it. */
        val image: String? get() = word.image.takeIf { isWordDone }
    }

    data class LessonFinished(val wordCount: Int) : ReadingUiState
}

sealed interface ReadingEvent {
    /** The main button: start, pause or continue, depending on the arrow. */
    data object MainAction : ReadingEvent

    /** Tapping the word: starts the arrow, or pauses and resumes it. */
    data object WordTapped : ReadingEvent

    data object Repeat : ReadingEvent

    data object Listen : ReadingEvent

    data object Next : ReadingEvent

    data object ReadLessonAgain : ReadingEvent
}
