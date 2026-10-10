package pl.dev404.czytanie.core.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One reading of a word, rated by the parent. Keeps the word id as plain text (no foreign key),
 * so re-importing the dictionary never deletes the child's history.
 */
@Entity(tableName = "attempt", indices = [Index("profileId"), Index("wordId")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val wordId: String,
    /** [pl.dev404.czytanie.core.domain.model.ParentRating] name. */
    val rating: String,
    val createdAtEpochMillis: Long,
    val tempoMillis: Long,
)
