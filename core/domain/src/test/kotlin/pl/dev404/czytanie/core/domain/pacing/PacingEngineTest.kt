package pl.dev404.czytanie.core.domain.pacing

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.pacing.ReadingState.Idle
import pl.dev404.czytanie.core.domain.pacing.ReadingState.Paused
import pl.dev404.czytanie.core.domain.pacing.ReadingState.Pointing
import pl.dev404.czytanie.core.domain.pacing.ReadingState.WaitingForAudio
import pl.dev404.czytanie.core.domain.pacing.ReadingState.WordDone

@OptIn(ExperimentalCoroutinesApi::class)
class PacingEngineTest {

    private val mama = word("mama", listOf("m", "a", "m", "a"), listOf("ma", "ma"))
    private val szafa = word("szafa", listOf("sz", "a", "f", "a"), listOf("sza", "fa"))

    /** Engine plus a log of every state it emitted, stamped with virtual time. */
    private class Harness(
        scope: TestScope,
        val player: FakeSpeechPlayer,
        config: PacingConfig,
    ) {
        val engine = PacingEngine(scope.backgroundScope, player, config)
        val log = mutableListOf<Pair<Long, ReadingState>>()

        init {
            scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) {
                engine.state.collect { log += scope.testScheduler.currentTime to it }
            }
        }
    }

    private fun TestScope.harness(
        tempoMillis: Long = 500,
        phonemesEnabled: Boolean = true,
        durations: Map<String, Long> = emptyMap(),
        defaultDuration: Long = 0,
        failing: Set<String> = emptySet(),
    ) = Harness(
        scope = this,
        player = FakeSpeechPlayer({ testScheduler.currentTime }, durations, defaultDuration, failing),
        config = PacingConfig(tempoMillis, phonemesEnabled),
    )

    /** Runs everything scheduled up to and including [time]. */
    private fun TestScope.advanceTo(time: Long) {
        advanceTimeBy(time - testScheduler.currentTime)
        runCurrent()
    }

    @Test
    fun `starts idle`() = runTest {
        harness().engine.state.value shouldBe Idle
    }

    @Test
    fun `without phonemes each unit lasts exactly the tempo`() = runTest {
        val h = harness(tempoMillis = 500, phonemesEnabled = false)

        h.engine.start(mama)
        advanceTo(10_000)

        h.log shouldContainExactly listOf(
            0L to Idle,
            0L to Pointing(0, 500),
            500L to Pointing(1, 500),
            1000L to Pointing(2, 500),
            1500L to Pointing(3, 500),
            2000L to WordDone,
        )
        h.player.phonemes.shouldBeEmpty()
    }

    @Test
    fun `recording longer than tempo makes the arrow wait - step is 700 ms`() = runTest {
        val h = harness(tempoMillis = 500, defaultDuration = 700)

        h.engine.start(szafa)
        advanceTo(10_000)

        h.log.take(5) shouldContainExactly listOf(
            0L to Idle,
            0L to Pointing(0, 500),
            500L to WaitingForAudio(0),
            700L to Pointing(1, 500),
            1200L to WaitingForAudio(1),
        )
        h.log.last() shouldBe (2800L to WordDone)
    }

    @Test
    fun `recording shorter than tempo does not shorten the step`() = runTest {
        val h = harness(tempoMillis = 900, defaultDuration = 300)

        h.engine.start(szafa)
        advanceTo(10_000)

        h.log.map { it.second }.filterIsInstance<WaitingForAudio>().shouldBeEmpty()
        h.log.last() shouldBe (3600L to WordDone)
    }

    @Test
    fun `step length is the maximum of tempo and each recording`() = runTest {
        val h = harness(tempoMillis = 500, durations = mapOf("sz" to 800, "f" to 600))

        h.engine.start(szafa)
        advanceTo(10_000)

        h.player.phonemes.map { it.startedAt } shouldContainExactly listOf(0L, 800L, 1300L, 1900L)
        h.log.last() shouldBe (2400L to WordDone)
    }

    @Test
    fun `each recording starts when the arrow reaches its unit, in order`() = runTest {
        val h = harness(tempoMillis = 500)

        h.engine.start(szafa)
        advanceTo(10_000)

        h.player.phonemes shouldContainExactly listOf(
            FakeSpeechPlayer.Playback("sz", 0),
            FakeSpeechPlayer.Playback("a", 500),
            FakeSpeechPlayer.Playback("f", 1000),
            FakeSpeechPlayer.Playback("a", 1500),
        )
    }

    @Test
    fun `the whole word is never played by the engine`() = runTest {
        val h = harness()

        h.engine.start(szafa)
        advanceTo(10_000)

        h.player.words.shouldBeEmpty()
    }

    @Test
    fun `pause keeps the index and stops the arrow`() = runTest {
        val h = harness(tempoMillis = 500, phonemesEnabled = false)
        h.engine.start(mama)
        advanceTo(1250)

        h.engine.pause()
        advanceTo(60_000)

        h.engine.state.value shouldBe Paused(2)
        h.log.last() shouldBe (1250L to Paused(2))
    }

    @Test
    fun `resume replays the paused unit from its start`() = runTest {
        val h = harness(tempoMillis = 500)
        h.engine.start(mama)
        advanceTo(1250)
        h.engine.pause()
        advanceTo(5000)

        h.engine.resume()
        advanceTo(60_000)

        h.log.drop(h.log.indexOf(1250L to Paused(2)) + 1) shouldContainExactly listOf(
            5000L to Pointing(2, 500),
            5500L to Pointing(3, 500),
            6000L to WordDone,
        )
        h.player.phonemes.map { it.phonemeId to it.startedAt } shouldContainExactly listOf(
            "m" to 0L,
            "a" to 500L,
            "m" to 1000L,
            "m" to 5000L,
            "a" to 5500L,
        )
    }

    @Test
    fun `pause while waiting for audio stops the recording`() = runTest {
        val h = harness(tempoMillis = 500, defaultDuration = 2000)
        h.engine.start(szafa)
        advanceTo(800)
        h.engine.state.value shouldBe WaitingForAudio(0)

        h.engine.pause()
        runCurrent()

        h.engine.state.value shouldBe Paused(0)
        h.player.cancelled shouldContainExactly listOf("sz")
    }

    @Test
    fun `toggle pauses and resumes`() = runTest {
        val h = harness(phonemesEnabled = false)
        h.engine.start(mama)
        advanceTo(600)

        h.engine.togglePause()
        h.engine.state.value shouldBe Paused(1)

        h.engine.togglePause()
        runCurrent()
        h.engine.state.value shouldBe Pointing(1, 500)
    }

    @Test
    fun `pause and resume are ignored when nothing is moving`() = runTest {
        val h = harness(phonemesEnabled = false)

        h.engine.pause()
        h.engine.resume()
        h.engine.state.value shouldBe Idle

        h.engine.start(mama)
        advanceTo(10_000)
        h.engine.pause()
        h.engine.resume()
        h.engine.state.value shouldBe WordDone
    }

    @Test
    fun `restart after the word is done starts from the first unit`() = runTest {
        val h = harness(phonemesEnabled = false)
        h.engine.start(mama)
        advanceTo(10_000)

        h.engine.restart()
        runCurrent()

        h.engine.state.value shouldBe Pointing(0, 500)
    }

    @Test
    fun `starting a new word abandons the previous one`() = runTest {
        val h = harness(tempoMillis = 500, phonemesEnabled = false)
        h.engine.start(mama)
        advanceTo(700)

        h.engine.start(szafa)
        advanceTo(10_000)

        h.log.drop(3) shouldContainExactly listOf(
            700L to Pointing(0, 500),
            1200L to Pointing(1, 500),
            1700L to Pointing(2, 500),
            2200L to Pointing(3, 500),
            2700L to WordDone,
        )
    }

    @Test
    fun `tempo change applies from the next unit`() = runTest {
        val h = harness(tempoMillis = 500, phonemesEnabled = false)
        h.engine.start(mama)
        advanceTo(100)

        h.engine.config = PacingConfig(tempoMillis = 1000, phonemesEnabled = false)
        advanceTo(10_000)

        h.log.map { it.first to it.second }.drop(1) shouldContainExactly listOf(
            0L to Pointing(0, 500),
            500L to Pointing(1, 1000),
            1500L to Pointing(2, 1000),
            2500L to Pointing(3, 1000),
            3500L to WordDone,
        )
    }

    @Test
    fun `by syllables the arrow steps through syllables and plays their recordings`() = runTest {
        val h = harness(tempoMillis = 500, durations = mapOf("sza" to 700))
        h.engine.config = PacingConfig(tempoMillis = 500, pointingUnit = PointingUnit.Syllables)

        h.engine.start(szafa)
        advanceTo(10_000)

        h.log.map { it.second } shouldContainExactly listOf(
            Idle,
            Pointing(0, 500),
            WaitingForAudio(0),
            Pointing(1, 500),
            WordDone,
        )
        h.player.syllables shouldContainExactly listOf(
            FakeSpeechPlayer.Playback("sza", 0),
            FakeSpeechPlayer.Playback("fa", 700),
        )
        h.player.phonemes.shouldBeEmpty()
        h.log.last() shouldBe (1200L to WordDone)
    }

    @Test
    fun `pointing unit is kept for the whole word`() = runTest {
        val h = harness(tempoMillis = 500, phonemesEnabled = false)
        h.engine.config = PacingConfig(500, phonemesEnabled = false, pointingUnit = PointingUnit.Syllables)
        h.engine.start(szafa)
        advanceTo(100)
        h.engine.pause()

        h.engine.config = PacingConfig(tempoMillis = 500, phonemesEnabled = false)
        h.engine.resume()
        advanceTo(10_000)

        h.log.map { it.second }.filterIsInstance<Pointing>().map { it.index } shouldContainExactly listOf(0, 0, 1)
    }

    @Test
    fun `a broken recording does not stop reading`() = runTest {
        val h = harness(tempoMillis = 500, failing = setOf("f"))

        h.engine.start(szafa)
        advanceTo(10_000)

        h.engine.state.value shouldBe WordDone
        h.log.last() shouldBe (2000L to WordDone)
    }

    @Test
    fun `reset returns to idle and stops the arrow`() = runTest {
        val h = harness(phonemesEnabled = false)
        h.engine.start(mama)
        advanceTo(600)

        h.engine.reset()
        advanceTo(10_000)

        h.engine.state.value shouldBe Idle
        h.log.map { it.second } shouldNotContain WordDone
    }

    private fun word(
        text: String,
        units: List<String>,
        syllables: List<String>,
    ) = Word.fromAnnotation(text, text, units, syllables, level = 3, lessonId = "test")
}
