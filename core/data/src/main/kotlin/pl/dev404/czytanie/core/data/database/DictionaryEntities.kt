package pl.dev404.czytanie.core.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Version of the dictionary currently imported. Single row with [ID]. */
@Entity(tableName = "dictionary_meta")
data class DictionaryMetaEntity(
    @PrimaryKey val id: Int = ID,
    val version: Int,
) {
    companion object {
        const val ID = 0
    }
}

@Entity(tableName = "phoneme")
data class PhonemeEntity(
    @PrimaryKey val id: String,
    val grapheme: String,
    val audio: String,
)

@Entity(tableName = "level")
data class LevelEntity(
    @PrimaryKey val id: Int,
    val name: String,
)

@Entity(tableName = "lesson", indices = [Index("levelId")])
data class LessonEntity(
    @PrimaryKey val id: String,
    val levelId: Int,
    /** Order of the lesson within its level. */
    val position: Int,
)

@Entity(tableName = "word", indices = [Index("lessonId")])
data class WordEntity(
    @PrimaryKey val id: String,
    val text: String,
    /** Units in reading order; indices are recomputed from the order. */
    val units: List<StoredUnit>,
    val syllables: List<String>,
    val level: Int,
    val lessonId: String,
    /** Order of the word within its lesson. */
    val position: Int,
    val image: String?,
    val wordAudio: String?,
)

@Serializable
data class StoredUnit(
    val grapheme: String,
    val phonemeId: String,
)
