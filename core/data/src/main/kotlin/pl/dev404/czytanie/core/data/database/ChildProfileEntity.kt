package pl.dev404.czytanie.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A child using the device. MVP has exactly one profile; the table is ready for more. */
@Entity(tableName = "child_profile")
data class ChildProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtEpochMillis: Long,
)
