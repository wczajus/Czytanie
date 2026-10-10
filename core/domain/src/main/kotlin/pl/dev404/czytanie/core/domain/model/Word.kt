package pl.dev404.czytanie.core.domain.model

/**
 * A word from the hand-annotated dictionary. The annotation ([units], [syllables]) is the source
 * of truth; the constructor only guarantees it is internally consistent with [text].
 *
 * On level 7 the text is a short sentence: units and syllables then cover the letters only, and
 * the gaps between them hold spaces and punctuation, which the arrow skips.
 *
 * @property image path of the picture shown after the child has read the word, if any.
 * @property wordAudio path of a recording of the whole word; when absent, TTS is used.
 */
data class Word(
    val id: WordId,
    val text: String,
    val units: List<ReadingUnit>,
    val syllables: List<Syllable>,
    val level: Int,
    val lessonId: LessonId,
    val image: String? = null,
    val wordAudio: String? = null,
) {
    init {
        require(text.isNotEmpty()) { "Word '$id' has empty text" }
        require(level in Level.RANGE) { "Word '$id' has level $level outside ${Level.RANGE}" }
        requireSpelled(units.map { Triple(it.grapheme, it.startIndex, it.endIndex) }, "units")
        // "w" and "z" have no vowel and are read with the next word: "w domu" → "w do", "mu".
        requireSpelled(syllables.map { Triple(it.text, it.startIndex, it.endIndex) }, "syllables", spansWords = true)
    }

    /**
     * Parts in order spell every letter of [text]; only separators may lie between them. With
     * [spansWords] a part may also contain separators inside it, never at its edges.
     */
    private fun requireSpelled(
        parts: List<Triple<String, Int, Int>>,
        what: String,
        spansWords: Boolean = false,
    ) {
        require(parts.isNotEmpty()) { "Word '$id' has no $what" }
        var covered = 0
        for ((part, start, end) in parts) {
            require(start >= covered && text.substring(covered, start).all(::isSeparator)) {
                "Word '$id': $what skip letters or overlap at index $start"
            }
            val separatorsAllowed = if (spansWords) {
                part.first().isLetter() && part.last().isLetter()
            } else {
                part.none(::isSeparator)
            }
            require(end <= text.length && text.substring(start, end) == part && separatorsAllowed) {
                "Word '$id': $what ${parts.map { it.first }} do not spell '$text'"
            }
            covered = end
        }
        require(text.substring(covered).all(::isSeparator)) { "Word '$id': $what do not cover the whole text" }
    }

    companion object {
        /**
         * Builds a word from the dictionary format, where units and syllables are plain strings
         * in reading order. Indices are computed from the order, skipping separators in [text].
         *
         * @param phonemeIds phoneme id per unit; defaults to the lowercase grapheme.
         */
        @Suppress("LongParameterList") // Mirrors the dictionary JSON schema one to one.
        fun fromAnnotation(
            id: String,
            text: String,
            units: List<String>,
            syllables: List<String>,
            level: Int,
            lessonId: String,
            phonemeIds: List<String> = units.map { it.lowercase() },
            image: String? = null,
            wordAudio: String? = null,
        ): Word {
            require(phonemeIds.size == units.size) {
                "Word '$id': ${phonemeIds.size} phoneme ids for ${units.size} units"
            }
            val unitStarts = units.startOffsetsIn(text)
            val syllableStarts = syllables.startOffsetsIn(text)
            return Word(
                id = WordId(id),
                text = text,
                units = units.mapIndexed { i, grapheme ->
                    ReadingUnit(grapheme, phonemeIds[i], unitStarts[i], unitStarts[i] + grapheme.length)
                },
                syllables = syllables.mapIndexed { i, syllable ->
                    Syllable(syllable, syllableStarts[i], syllableStarts[i] + syllable.length)
                },
                level = level,
                lessonId = LessonId(lessonId),
                image = image,
                wordAudio = wordAudio,
            )
        }

        /** Spaces and punctuation between the words of a sentence: never part of a unit. */
        fun isSeparator(char: Char): Boolean = !char.isLetter()

        private fun List<String>.startOffsetsIn(text: String): List<Int> {
            var offset = 0
            return map { part ->
                while (offset < text.length && isSeparator(text[offset])) offset++
                offset.also { offset += part.length }
            }
        }
    }
}

@JvmInline
value class WordId(val value: String) {
    override fun toString() = value
}

@JvmInline
value class LessonId(val value: String) {
    override fun toString() = value
}
