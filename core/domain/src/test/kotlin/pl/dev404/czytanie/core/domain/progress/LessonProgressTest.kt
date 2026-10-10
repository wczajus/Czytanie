package pl.dev404.czytanie.core.domain.progress

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import pl.dev404.czytanie.core.domain.model.ParentRating
import pl.dev404.czytanie.core.domain.model.ParentRating.NotYet
import pl.dev404.czytanie.core.domain.model.ParentRating.Read
import pl.dev404.czytanie.core.domain.model.ParentRating.WithHelp
import pl.dev404.czytanie.core.domain.model.WordId

class LessonProgressTest {
    private val words = (1..10).map { WordId("w$it") }

    private fun attempt(
        word: Int,
        rating: ParentRating,
        at: Long = 0,
    ) = Attempt(WordId("w$word"), "w$word", rating, createdAtEpochMillis = at, tempoMillis = 900)

    @Test
    fun `eight of ten words read passes the lesson`() {
        val progress = LessonProgress.of(words, (1..8).map { attempt(it, Read) } + attempt(9, WithHelp))

        progress shouldBe LessonProgress(readWords = 8, wordCount = 10)
        progress.isPassed shouldBe true
    }

    @Test
    fun `seven of ten is not passed yet`() {
        LessonProgress.of(words, (1..7).map { attempt(it, Read) }).isPassed shouldBe false
    }

    @Test
    fun `only the latest rating of a word counts`() {
        val attempts =
            listOf(
                attempt(1, NotYet, at = 1),
                attempt(1, Read, at = 2),
                attempt(2, Read, at = 1),
                attempt(2, WithHelp, at = 2),
            )

        LessonProgress.of(words, attempts).readWords shouldBe 1
    }

    @Test
    fun `attempts of other lessons are ignored`() {
        LessonProgress.of(words.take(6), (1..10).map { attempt(it, Read) }) shouldBe LessonProgress(6, 6)
    }

    @Test
    fun `a six word lesson needs five words read`() {
        val six = words.take(6)
        LessonProgress.of(six, (1..4).map { attempt(it, Read) }).isPassed shouldBe false
        LessonProgress.of(six, (1..5).map { attempt(it, Read) }).isPassed shouldBe true
    }

    @Test
    fun `an empty lesson is never passed`() {
        LessonProgress.of(emptyList(), emptyList()).isPassed shouldBe false
    }
}
