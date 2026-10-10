package pl.dev404.czytanie.core.audio

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import pl.dev404.czytanie.core.domain.content.DictionaryRepository
import pl.dev404.czytanie.core.domain.model.Lesson
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.Phoneme
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.model.WordId
import pl.dev404.czytanie.core.domain.pacing.PointingUnit

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSpeechPlayerTest {

    private val dictionary = FakeDictionary(
        listOf(
            Phoneme("sz", "sz", "audio/phonemes/sz.ogg"),
            Phoneme("a", "a", "audio/phonemes/a.ogg"),
            Phoneme("f", "f", "audio/phonemes/f.ogg"),
        ),
    )
    private val clips = FakeClipPlayer(durationMillis = 400)
    private val recordings = FakeRecordingPlayer()
    private val speech = FakeWordSpeech(available = true)
    private val player = AndroidSpeechPlayer(RecordedPhonemePlayer(dictionary, clips), recordings, speech)

    private val szafa = Word.fromAnnotation(
        id = "szafa",
        text = "szafa",
        units = listOf("sz", "a", "f", "a"),
        syllables = listOf("sza", "fa"),
        level = 5,
        lessonId = "l5",
    )

    @Test
    fun `phonemes are played from their recordings and never through TTS`() = runTest {
        player.playPhoneme("sz")

        clips.played shouldContainExactly listOf("audio/phonemes/sz.ogg")
        speech.spoken.shouldBeEmpty()
    }

    @Test
    fun `a phoneme returns only after its recording has ended`() = runTest {
        var done = false
        launch {
            player.playPhoneme("a")
            done = true
        }
        advanceTimeBy(399)
        runCurrent()
        done shouldBe false
        advanceTimeBy(2)
        runCurrent()
        done shouldBe true
    }

    @Test
    fun `preparing a lesson loads each phoneme once`() = runTest {
        player.prepare(listOf(szafa, szafa))
        player.playPhoneme("a")

        clips.loaded shouldContainExactly
            listOf("audio/phonemes/sz.ogg", "audio/phonemes/a.ogg", "audio/phonemes/f.ogg")
    }

    @Test
    fun `syllables are played from their recordings and never through TTS`() = runTest {
        player.playSyllable("Żą")

        clips.played shouldContainExactly listOf("audio/syllables/z2a1.ogg")
        speech.spoken.shouldBeEmpty()
    }

    @Test
    fun `preparing a lesson by syllables loads each syllable once`() = runTest {
        player.prepare(listOf(szafa, szafa), PointingUnit.Syllables)
        player.playSyllable("sza")

        clips.loaded shouldContainExactly listOf("audio/syllables/sza.ogg", "audio/syllables/fa.ogg")
    }

    @Test
    fun `phoneme without a recording is skipped`() = runTest {
        player.playPhoneme("ch")

        clips.played.shouldBeEmpty()
        speech.spoken.shouldBeEmpty()
    }

    @Test
    fun `word with a recording plays the recording`() = runTest {
        player.playWord(szafa.copy(wordAudio = "audio/words/szafa.ogg"))

        recordings.played shouldContainExactly listOf("audio/words/szafa.ogg")
        speech.spoken.shouldBeEmpty()
    }

    @Test
    fun `word without a recording is read by TTS`() = runTest {
        player.playWord(szafa)

        speech.spoken shouldContainExactly listOf("szafa")
    }

    @Test
    fun `broken word recording falls back to TTS`() = runTest {
        recordings.succeeds = false
        player.playWord(szafa.copy(wordAudio = "audio/words/szafa.ogg"))

        speech.spoken shouldContainExactly listOf("szafa")
    }

    @Test
    fun `without a Polish voice only recorded words can be heard`() = runTest {
        speech.available = false

        player.canPlayWord(szafa) shouldBe false
        player.canPlayWord(szafa.copy(wordAudio = "audio/words/szafa.ogg")) shouldBe true
        player.playWord(szafa)
        speech.spoken.shouldBeEmpty()
    }
}

private class FakeDictionary(private val phonemes: List<Phoneme>) : DictionaryRepository {
    override fun observeLevels(): Flow<List<Level>> = emptyFlow()

    override suspend fun lesson(id: LessonId): Lesson? = null

    override suspend fun word(id: WordId): Word? = null

    override suspend fun phonemes(): List<Phoneme> = phonemes
}

private class FakeClipPlayer(private val durationMillis: Long) : ClipPlayer {
    val loaded = mutableListOf<String>()
    val played = mutableListOf<String>()
    private val paths = mutableMapOf<Int, String>()

    override suspend fun load(assetPath: String): Clip {
        loaded += assetPath
        val id = loaded.size
        paths[id] = assetPath
        return Clip(id, durationMillis)
    }

    override suspend fun play(clip: Clip) {
        played += paths.getValue(clip.soundId)
        delay(clip.durationMillis)
    }
}

private class FakeRecordingPlayer : RecordingPlayer {
    var succeeds = true
    val played = mutableListOf<String>()

    override suspend fun play(assetPath: String): Boolean {
        played += assetPath
        return succeeds
    }
}

private class FakeWordSpeech(var available: Boolean) : WordSpeech {
    val spoken = mutableListOf<String>()

    override suspend fun isAvailable() = available

    override suspend fun speak(text: String) {
        spoken += text
    }
}
