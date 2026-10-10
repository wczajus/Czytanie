package pl.dev404.czytanie.feature.reading

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.pacing.ReadingState
import pl.dev404.czytanie.core.domain.settings.WordFontSize
import pl.dev404.czytanie.core.ui.theme.CzytanieTheme

/**
 * Reference screenshots of the reading screen: every word size in light and dark theme, with the
 * arrow under the digraph "sz" of "szafa" (it must sit under the middle of both letters), and a
 * level 7 sentence and a story paragraph.
 *
 * Record with `./gradlew recordRoborazziDebug`; `check` verifies them.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class ReadingScreenScreenshotTest(
    private val fontSize: WordFontSize,
    private val darkTheme: Boolean,
) {
    @get:Rule
    val composeRule = createComposeRule()

    private val theme get() = if (darkTheme) "dark" else "light"

    @Test
    fun arrowUnderDigraph() {
        capture(
            "reading_szafa_sz_${fontSize.name.lowercase()}_$theme",
            TestWords.szafa,
            ReadingState.Pointing(0, TEMPO),
        )
    }

    @Test
    fun arrowUnderLaterUnit() {
        if (fontSize != WordFontSize.Large) return
        capture("reading_szafa_f_$theme", TestWords.szafa, ReadingState.Paused(2))
    }

    @Test
    fun wordDone() {
        if (fontSize != WordFontSize.Large) return
        capture("reading_szafa_done_$theme", TestWords.szafa, ReadingState.WordDone)
    }

    /** A sentence: the arrow skips the space and sits under "k" of "kota"; the space has faded. */
    @Test
    fun sentence() {
        if (fontSize != WordFontSize.Large) return
        capture("reading_sentence_k_$theme", TestWords.alaMaKota, ReadingState.Paused(5))
    }

    /** A story paragraph wraps; the arrow sits under "Po" of "Popiołek" on a later line. */
    @Test
    fun passage() {
        if (fontSize != WordFontSize.Large) return
        val start = TestWords.passage.text.indexOf("Popiołek")
        val index = TestWords.passage.units.indexOfFirst { it.startIndex == start }
        capture("reading_passage_$theme", TestWords.passage, ReadingState.Paused(index))
    }

    private fun capture(
        name: String,
        word: Word,
        readingState: ReadingState,
    ) {
        composeRule.setContent {
            CzytanieTheme(darkTheme = darkTheme) {
                ReadingScreen(
                    state = ReadingUiState.Reading(
                        word = word,
                        wordIndex = 1,
                        wordCount = 8,
                        readingState = readingState,
                        fontSize = fontSize,
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/$name.png")
    }

    companion object {
        private const val TEMPO = 900L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-dark={1}")
        fun parameters(): List<Array<Any>> = WordFontSize.entries.flatMap { size ->
            listOf(false, true).map { dark -> arrayOf<Any>(size, dark) }
        }
    }
}
