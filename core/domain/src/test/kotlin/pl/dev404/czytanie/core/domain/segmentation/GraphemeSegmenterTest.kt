package pl.dev404.czytanie.core.domain.segmentation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GraphemeSegmenterTest {

    private val segmenter = GraphemeSegmenter()

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        // Mandatory examples
        "szczoteczka, sz cz o t e cz k a",
        "siano, si a n o",
        "marznąć, m a r z n ą ć",
        // Vowels and open syllables (levels 1–3)
        "a, a",
        "mama, m a m a",
        "tata, t a t a",
        "lody, l o d y",
        "koza, k o z a",
        "ucho, u ch o",
        // Closed syllables and clusters (level 4)
        "kot, k o t",
        "dom, d o m",
        "sklep, s k l e p",
        "trawa, t r a w a",
        "jabłko, j a b ł k o",
        // Digraphs (level 5)
        "szafa, sz a f a",
        "czapka, cz a p k a",
        "chata, ch a t a",
        "rzeka, rz e k a",
        "chleb, ch l e b",
        "drzewo, d rz e w o",
        "grzyb, g rz y b",
        "mysz, m y sz",
        "pszczoła, p sz cz o ł a",
        "dzwon, dz w o n",
        "dżem, dż e m",
        "dźwig, dź w i g",
        // Softening before a vowel and diacritics (level 6)
        "ciocia, ci o ci a",
        "dziadek, dzi a d e k",
        "ziarno, zi a r n o",
        "nie, ni e",
        "wiśnia, w i ś ni a",
        "koń, k o ń",
        "źrebak, ź r e b a k",
        "jeż, j e ż",
        "żaba, ż a b a",
        "gęś, g ę ś",
        "ząb, z ą b",
        // "i" that only softens another consonant before a vowel joins it (D59)
        "pies, pi e s",
        "miasto, mi a s t o",
        "Wierna, Wi e r n a",
        "kiedy, ki e d y",
        "biedronka, bi e d r o n k a",
        "Popiołek, P o pi o ł e k",
        // "i" as the syllable vowel after a soft consonant stays a separate unit
        "zima, z i m a",
        "nic, n i c",
        "pani, p a n i",
        "dzisiaj, dz i si a j",
        // Exceptions where a digraph's letters are read separately
        "odzysk, o d z y s k",
        // Case is preserved
        "Ala, A l a",
        "Szymon, Sz y m o n",
    )
    fun `splits word into graphemes`(
        word: String,
        expected: String,
    ) {
        segmenter.segment(word).graphemes shouldContainExactly expected.split(" ")
    }

    @Test
    fun `units cover the text with exact indices`() {
        val units = segmenter.segment("szafa").units

        units.map { it.startIndex to it.endIndex } shouldContainExactly listOf(0 to 2, 2 to 3, 3 to 4, 4 to 5)
        units.joinToString("") { it.grapheme } shouldBe "szafa"
    }

    @Test
    fun `phoneme id of a digraph is the digraph`() {
        segmenter.segment("szafa").units.map { it.phonemeId } shouldContainExactly listOf("sz", "a", "f", "a")
    }

    @Test
    fun `soft consonant before syllable vowel i gets the soft phoneme`() {
        segmenter.segment("zima").units.map { it.phonemeId } shouldContainExactly listOf("zi", "i", "m", "a")
        segmenter.segment("nic").units.map { it.phonemeId } shouldContainExactly listOf("ni", "i", "c")
        segmenter.segment("dzisiaj").units.map { it.phonemeId } shouldContainExactly
            listOf("dzi", "i", "si", "a", "j")
    }

    @Test
    fun `phoneme ids are lowercase while graphemes keep case`() {
        val units = segmenter.segment("Szymon").units

        units.first().grapheme shouldBe "Sz"
        units.first().phonemeId shouldBe "sz"
    }

    @Test
    fun `regular words produce no hints`() {
        segmenter.segment("szafa").hints.shouldBeEmpty()
        segmenter.segment("rzeka").hints.shouldBeEmpty()
    }

    @Test
    fun `split from the exception list is marked`() {
        segmenter.segment("marznąć").hints shouldContainExactly listOf(SegmentationHint.FromExceptionList(0))
    }

    @Test
    fun `exception list ignores case`() {
        segmenter.segment("Odzysk").graphemes shouldContainExactly listOf("O", "d", "z", "y", "s", "k")
    }

    @Test
    fun `rz before a consonant is flagged for review`() {
        segmenter.segment("drzwi").hints shouldContain SegmentationHint.PossiblySeparateLetters(1, "rz")
    }

    @Test
    fun `dz right after a prefix is flagged for review`() {
        segmenter.segment("nadzieja").hints shouldContain SegmentationHint.PossiblySeparateLetters(2, "dzi")
    }

    @Test
    fun `i before a vowel after a non-sibilant consonant is flagged for review`() {
        segmenter.segment("pianino").hints shouldContain SegmentationHint.IBeforeVowel(1)
        segmenter.segment("Maria").hints shouldContain SegmentationHint.IBeforeVowel(3)
    }

    @Test
    fun `consonant softened by i gets its own phoneme`() {
        segmenter.segment("pies").units.first().phonemeId shouldBe "pi"
        segmenter.segment("Wierna").units.first().phonemeId shouldBe "wi"
    }

    @Test
    fun `custom exceptions override the rules`() {
        val custom = GraphemeSegmenter(exceptions = mapOf("tarzan" to listOf("t", "a", "r", "z", "a", "n")))

        custom.segment("tarzan").graphemes shouldContainExactly listOf("t", "a", "r", "z", "a", "n")
    }

    @Test
    fun `exception that does not spell its word is rejected`() {
        shouldThrow<IllegalArgumentException> { GraphemeSegmenter(mapOf("kot" to listOf("k", "o"))) }
    }

    @Test
    fun `text with non-letters is rejected`() {
        shouldThrow<IllegalArgumentException> { segmenter.segment("Ala ma kota.") }
        shouldThrow<IllegalArgumentException> { segmenter.segment("") }
    }
}
