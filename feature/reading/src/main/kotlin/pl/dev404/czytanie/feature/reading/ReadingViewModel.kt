package pl.dev404.czytanie.feature.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.dev404.czytanie.core.domain.audio.SpeechPlayer
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.Lesson
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.WordId
import pl.dev404.czytanie.core.domain.pacing.PacingEngine
import pl.dev404.czytanie.core.domain.pacing.ReadingState
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.SettingsRepository

@HiltViewModel
class ReadingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dictionaryRepository: DictionaryRepository,
    settingsRepository: SettingsRepository,
    private val speechPlayer: SpeechPlayer,
) : ViewModel() {

    private val lessonId = LessonId(checkNotNull(savedStateHandle[ReadingRoute.LESSON_ID_KEY]))
    private val engine = PacingEngine(viewModelScope, speechPlayer)
    private val progress = MutableStateFlow<Progress>(Progress.Loading)
    private val settings = MutableStateFlow(ReadingSettings())
    private val isPlayingWord = MutableStateFlow(false)

    /** Words that cannot be heard (no recording and no Polish voice); empty until checked. */
    private val silentWords = MutableStateFlow<Set<WordId>>(emptySet())
    private var wordPlayback: Job? = null

    val uiState: StateFlow<ReadingUiState> =
        combine(progress, engine.state, settings, isPlayingWord, silentWords, ::toUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReadingUiState.Loading)

    init {
        settingsRepository.settings
            .onEach {
                settings.value = it
                engine.config = it.toPacingConfig()
            }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            val lesson = dictionaryRepository.lesson(lessonId)
            if (lesson == null || lesson.words.isEmpty()) {
                progress.value = Progress.NotFound
                return@launch
            }
            progress.value = Progress.AtWord(lesson, 0)
            launch { speechPlayer.prepare(lesson.words) }
            silentWords.value = lesson.words.filterNot { speechPlayer.canPlayWord(it) }.map { it.id }.toSet()
        }
    }

    private fun toUiState(
        progress: Progress,
        readingState: ReadingState,
        settings: ReadingSettings,
        isPlayingWord: Boolean,
        silentWords: Set<WordId>,
    ): ReadingUiState = when (progress) {
        Progress.Loading -> ReadingUiState.Loading

        Progress.NotFound -> ReadingUiState.LessonNotFound

        is Progress.Finished -> ReadingUiState.LessonFinished(progress.lesson.words.size)

        is Progress.AtWord -> ReadingUiState.Reading(
            word = progress.word,
            wordIndex = progress.index,
            wordCount = progress.lesson.words.size,
            readingState = readingState,
            fontSize = settings.fontSize,
            isPlayingWord = isPlayingWord,
            canPlayWordAudio = progress.word.id !in silentWords,
        )
    }

    fun onEvent(event: ReadingEvent) {
        when (event) {
            ReadingEvent.MainAction, ReadingEvent.WordTapped -> startOrTogglePause()
            ReadingEvent.Repeat -> repeat()
            ReadingEvent.Listen -> listen()
            ReadingEvent.Next -> next()
            ReadingEvent.ReadLessonAgain -> readAgain()
        }
    }

    private fun startOrTogglePause() {
        val atWord = progress.value as? Progress.AtWord ?: return
        when (engine.state.value) {
            ReadingState.Idle -> engine.start(atWord.word)
            ReadingState.WordDone -> Unit
            else -> engine.togglePause()
        }
    }

    private fun repeat() {
        if (progress.value !is Progress.AtWord || engine.state.value == ReadingState.Idle) return
        stopWordPlayback()
        engine.restart()
    }

    private fun listen() {
        val atWord = progress.value as? Progress.AtWord ?: return
        if (engine.state.value != ReadingState.WordDone || wordPlayback?.isActive == true) return
        if (atWord.word.id in silentWords.value) return
        isPlayingWord.value = true
        wordPlayback = viewModelScope.launch {
            try {
                speechPlayer.playWord(atWord.word)
            } finally {
                isPlayingWord.value = false
            }
        }
    }

    private fun next() {
        val atWord = progress.value as? Progress.AtWord ?: return
        if (engine.state.value != ReadingState.WordDone) return
        stopWordPlayback()
        engine.reset()
        val hasNextWord = atWord.index + 1 < atWord.lesson.words.size
        progress.value = if (hasNextWord) atWord.copy(index = atWord.index + 1) else Progress.Finished(atWord.lesson)
    }

    private fun readAgain() {
        val finished = progress.value as? Progress.Finished ?: return
        engine.reset()
        progress.value = Progress.AtWord(finished.lesson, 0)
    }

    private fun stopWordPlayback() {
        wordPlayback?.cancel()
        wordPlayback = null
        isPlayingWord.value = false
    }

    private sealed interface Progress {
        data object Loading : Progress

        data object NotFound : Progress

        data class AtWord(
            val lesson: Lesson,
            val index: Int,
        ) : Progress {
            val word get() = lesson.words[index]
        }

        data class Finished(val lesson: Lesson) : Progress
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
