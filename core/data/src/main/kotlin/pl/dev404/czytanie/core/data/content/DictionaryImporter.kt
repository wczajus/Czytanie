package pl.dev404.czytanie.core.data.content

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.dev404.czytanie.core.data.database.DictionaryDao
import pl.dev404.czytanie.core.data.database.LessonEntity
import pl.dev404.czytanie.core.data.database.LevelEntity
import pl.dev404.czytanie.core.data.database.PhonemeEntity
import pl.dev404.czytanie.core.data.database.StoredUnit
import pl.dev404.czytanie.core.data.database.WordEntity
import pl.dev404.czytanie.core.domain.content.errors
import pl.dev404.czytanie.core.domain.model.Dictionary

/**
 * Imports the bundled dictionary into Room on first start and whenever its `version` changes.
 * Safe to call often: after the first check in a process it returns immediately.
 */
@Singleton
class DictionaryImporter @Inject constructor(
    private val source: DictionarySource,
    private val parser: DictionaryParser,
    private val dao: DictionaryDao,
) {
    private val mutex = Mutex()

    @Volatile
    private var upToDate = false

    /**
     * @throws InvalidDictionaryException when the bundled file is broken. The content test keeps
     *   that from shipping; at runtime the previously imported dictionary stays in place.
     */
    suspend fun ensureUpToDate() {
        if (upToDate) return
        mutex.withLock {
            if (upToDate) return
            val parsed = parser.parse(source.read())
            val dictionary = parsed.dictionary
            if (dictionary == null || parsed.issues.errors.isNotEmpty()) {
                throw InvalidDictionaryException(parsed.issues.errors.joinToString("\n"))
            }
            if (dao.importedVersion() != dictionary.version) import(dictionary)
            upToDate = true
        }
    }

    private suspend fun import(dictionary: Dictionary) {
        val positionInLesson = dictionary.words.groupBy { it.lessonId }
            .flatMap { (_, words) -> words.mapIndexed { position, word -> word.id to position } }
            .toMap()
        dao.replaceAll(
            version = dictionary.version,
            phonemes = dictionary.phonemes.map { PhonemeEntity(it.id, it.grapheme, it.audio) },
            levels = dictionary.levels.map { LevelEntity(it.id, it.name) },
            lessons = dictionary.levels.flatMap { level ->
                level.lessons.mapIndexed { position, lessonId -> LessonEntity(lessonId.value, level.id, position) }
            },
            words = dictionary.words.map { word ->
                WordEntity(
                    id = word.id.value,
                    text = word.text,
                    units = word.units.map { StoredUnit(it.grapheme, it.phonemeId) },
                    syllables = word.syllables.map { it.text },
                    level = word.level,
                    lessonId = word.lessonId.value,
                    position = positionInLesson.getValue(word.id),
                    image = word.image,
                    wordAudio = word.wordAudio,
                )
            },
        )
    }
}

class InvalidDictionaryException(message: String) : IllegalStateException(message)
