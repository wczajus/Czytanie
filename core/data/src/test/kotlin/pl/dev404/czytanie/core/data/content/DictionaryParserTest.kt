package pl.dev404.czytanie.core.data.content

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class DictionaryParserTest {

    private val parser = DictionaryParser()

    @Test
    fun `parses the file format into domain models`() {
        val parsed = parser.parse(TestDictionaries.json(version = 3))

        parsed.issues.shouldBeEmpty()
        val dictionary = parsed.dictionary.shouldNotBeNull()
        dictionary.version shouldBe 3
        dictionary.words shouldHaveSize 6
        dictionary.words.first().units.map { it.grapheme to it.startIndex } shouldContainExactly
            listOf("m" to 0, "a" to 1)
        dictionary.levels.single().lessons.map { it.value } shouldContainExactly listOf("l2")
    }

    @Test
    fun `explicit phoneme ids are used`() {
        val json = """
            {"version": 1, "phonemes": [], "levels": [], "words": [
              {"id": "nic", "text": "nic", "units": ["n", "i", "c"], "syllables": ["nic"], "level": 6,
               "lesson": "l6", "phonemeIds": ["ni", "i", "c"]}
            ]}
        """.trimIndent()

        parser.parse(json).dictionary.shouldNotBeNull().words.single().units.map { it.phonemeId } shouldContainExactly
            listOf("ni", "i", "c")
    }

    @Test
    fun `word whose units do not spell the text is reported, not thrown`() {
        val json = """
            {"version": 1, "phonemes": [], "levels": [], "words": [
              {"id": "szafa", "text": "szafa", "units": ["s", "z", "a", "f"], "syllables": ["sza", "fa"],
               "level": 5, "lesson": "l5"}
            ]}
        """.trimIndent()

        val parsed = parser.parse(json)

        parsed.dictionary.shouldNotBeNull().words.shouldBeEmpty()
        parsed.issues.single().subject shouldBe "word szafa"
    }

    @Test
    fun `broken JSON is reported`() {
        val parsed = parser.parse("{ not json")

        parsed.dictionary.shouldBeNull()
        parsed.issues.single().message shouldContain "invalid JSON"
    }
}
