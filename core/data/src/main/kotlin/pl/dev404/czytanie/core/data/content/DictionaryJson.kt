package pl.dev404.czytanie.core.data.content

import kotlinx.serialization.Serializable

/**
 * File format of `assets/content/dictionary.json`. Asset paths (`audio`, `image`, `wordAudio`) are
 * relative to the assets root, e.g. `audio/phonemes/sz.ogg`.
 */
@Serializable
internal data class DictionaryJson(
    val version: Int,
    val phonemes: List<PhonemeJson>,
    val levels: List<LevelJson>,
    val words: List<WordJson>,
)

@Serializable
internal data class PhonemeJson(
    val id: String,
    val grapheme: String,
    val audio: String,
    val note: String? = null,
)

@Serializable
internal data class LevelJson(
    val id: Int,
    val name: String,
    val lessons: List<String>,
)

/**
 * @property phonemeIds optional phoneme id per unit, for units read differently from their
 *   letters (e.g. "n" in "nic" read as "ni"); defaults to the lowercase unit.
 */
@Serializable
internal data class WordJson(
    val id: String,
    val text: String,
    val units: List<String>,
    val syllables: List<String>,
    val level: Int,
    val lesson: String,
    val phonemeIds: List<String>? = null,
    val image: String? = null,
    val wordAudio: String? = null,
)
