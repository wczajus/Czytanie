package pl.dev404.czytanie.core.data.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val units = ListSerializer(StoredUnit.serializer())
    private val strings = ListSerializer(String.serializer())

    @TypeConverter
    fun unitsToJson(value: List<StoredUnit>): String = Json.encodeToString(units, value)

    @TypeConverter
    fun unitsFromJson(json: String): List<StoredUnit> = Json.decodeFromString(units, json)

    @TypeConverter
    fun stringsToJson(value: List<String>): String = Json.encodeToString(strings, value)

    @TypeConverter
    fun stringsFromJson(json: String): List<String> = Json.decodeFromString(strings, json)
}
