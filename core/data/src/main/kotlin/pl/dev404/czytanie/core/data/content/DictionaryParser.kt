package pl.dev404.czytanie.core.data.content

import javax.inject.Inject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pl.dev404.czytanie.core.domain.content.DictionaryIssue
import pl.dev404.czytanie.core.domain.model.Dictionary
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.domain.model.Phoneme
import pl.dev404.czytanie.core.domain.model.Word

/** Result of parsing: the words that could be built, plus everything that could not. */
data class ParsedDictionary(
    val dictionary: Dictionary?,
    val issues: List<DictionaryIssue>,
)

/** Turns the dictionary JSON into domain models. Never throws on bad content; reports it instead. */
class DictionaryParser @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = false }

    fun parse(text: String): ParsedDictionary {
        val file = try {
            json.decodeFromString(DictionaryJson.serializer(), text)
        } catch (e: SerializationException) {
            return ParsedDictionary(null, listOf(DictionaryIssue.error("dictionary", "invalid JSON: ${e.message}")))
        } catch (e: IllegalArgumentException) {
            return ParsedDictionary(null, listOf(DictionaryIssue.error("dictionary", "invalid JSON: ${e.message}")))
        }

        val issues = mutableListOf<DictionaryIssue>()
        val words = file.words.mapNotNull { word ->
            try {
                word.toDomain()
            } catch (e: IllegalArgumentException) {
                issues += DictionaryIssue.error("word ${word.id}", e.message ?: "invalid word")
                null
            }
        }
        val dictionary = Dictionary(
            version = file.version,
            phonemes = file.phonemes.map { Phoneme(it.id, it.grapheme, it.audio) },
            levels = file.levels.map { level -> Level(level.id, level.name, level.lessons.map(::LessonId)) },
            words = words,
        )
        return ParsedDictionary(dictionary, issues)
    }

    private fun WordJson.toDomain() = Word.fromAnnotation(
        id = id,
        text = text,
        units = units,
        syllables = syllables,
        level = level,
        lessonId = lesson,
        phonemeIds = phonemeIds ?: units.map { it.lowercase() },
        image = image,
        wordAudio = wordAudio,
    )
}
