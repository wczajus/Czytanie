package pl.dev404.czytanie.feature.reading

import android.content.Context
import android.os.Looper
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.collections.shouldContainExactly
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.ui.theme.CzytanieTheme

/** A child reads a whole lesson: start, the arrow passes each word, listen, next, praise. */
@RunWith(RobolectricTestRunner::class)
class ReadingLessonFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val speechPlayer = FakeSpeechPlayer(phonemeMillis = 100, wordMillis = 400)

    private fun text(@StringRes id: Int) = context.getString(id)

    @Test
    fun readsTheWholeLesson() {
        val viewModel = ReadingViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ReadingRoute.LESSON_ID_KEY to TestWords.LESSON_ID)),
            dictionaryRepository = FakeDictionaryRepository(),
            settingsRepository = FakeSettingsRepository(ReadingSettings(tempoMillis = TEMPO)),
            speechPlayer = speechPlayer,
        )
        composeRule.setContent {
            CzytanieTheme { ReadingScreenRoute(onBack = {}, viewModel = viewModel) }
        }

        TestWords.lesson.words.forEach { word ->
            composeRule.onNodeWithTag(READING_WORD_TAG).assertExists()
            composeRule.onNodeWithText(text(R.string.reading_listen)).assertIsNotEnabled()
            composeRule.onNodeWithText(text(R.string.reading_start)).performClick()

            advance(TEMPO / 2)
            // While the arrow moves, the whole word cannot be heard yet.
            composeRule.onNodeWithText(text(R.string.reading_listen)).assertIsNotEnabled()

            advance(TEMPO * word.units.size)
            composeRule.onNodeWithText(text(R.string.reading_listen)).assertIsEnabled().performClick()
            advance(WORD_MILLIS)
            composeRule.onNodeWithText(text(R.string.reading_next)).assertIsEnabled().performClick()
            advance(0)
        }

        composeRule.onNodeWithText(text(R.string.reading_lesson_finished_title)).assertExists()
        speechPlayer.words shouldContainExactly TestWords.lesson.words
    }

    /** Runs the main looper (where `viewModelScope` lives) [millis] forward, then lets Compose settle. */
    private fun advance(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis + 1))
        composeRule.waitForIdle()
    }

    private companion object {
        const val TEMPO = 300L
        const val WORD_MILLIS = 400L
    }
}
