package pl.dev404.czytanie.content

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.io.File
import org.junit.jupiter.api.Test
import pl.dev404.czytanie.core.data.content.DictionaryParser
import pl.dev404.czytanie.core.domain.content.DictionaryValidator
import pl.dev404.czytanie.core.domain.content.errors
import pl.dev404.czytanie.core.domain.content.warnings

/** Validates the WHOLE bundled dictionary on every build. */
class DictionaryContentTest {

    private val assets = File("src/main/assets")
    private val parsed = DictionaryParser().parse(File(assets, "content/dictionary.json").readText())

    @Test
    fun `dictionary parses without errors`() {
        parsed.issues.errors.shouldBeEmpty()
    }

    @Test
    fun `dictionary passes the validator`() {
        val dictionary = parsed.dictionary.shouldNotBeNull()

        val issues = DictionaryValidator().validate(dictionary) { File(assets, it).isFile }

        // Warnings point at words worth a manual check; print them, fail only on errors.
        issues.warnings.forEach { println(it) }
        issues.errors.shouldBeEmpty()
    }

    @Test
    fun `MVP ships levels 1 to 3`() {
        parsed.dictionary.shouldNotBeNull().levels.map { it.id } shouldBe listOf(1, 2, 3)
    }
}
