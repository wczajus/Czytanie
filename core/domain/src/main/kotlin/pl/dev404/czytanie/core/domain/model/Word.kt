package pl.dev404.czytanie.core.domain.model

/**
 * A word from the hand-annotated dictionary. The annotation ([units], [syllables]) is the source
 * of truth; the constructor only guarantees it is internally consistent with [text].
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
        requireContiguous(units.map { it.startIndex to it.endIndex }, "units")
        require(units.joinToString("") { it.grapheme } == text) {
            "Word '$id': units ${units.map { it.grapheme }} do not spell '$text'"
        }
        requireContiguous(syllables.map { it.startIndex to it.endIndex }, "syllables")
        require(syllables.joinToString("") { it.text } == text) {
            "Word '$id': syllables ${syllables.map { it.text }} do not spell '$text'"
        }
    }

    private fun requireContiguous(
        ranges: List<Pair<Int, Int>>,
        what: String,
    ) {
        require(ranges.isNotEmpty()) { "Word '$id' has no $what" }
        var expectedStart = 0
        for ((start, end) in ranges) {
            require(start == expectedStart) { "Word '$id': $what are not contiguous at index $start" }
            expectedStart = end
        }
        require(expectedStart == text.length) { "Word '$id': $what do not cover the whole text" }
    }

    companion object {
        /**
         * Builds a word from the dictionary format, where units and syllables are plain strings
         * in reading order. Indices are computed from the order.
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
            val unitStarts = units.startOffsets()
            val syllableStarts = syllables.startOffsets()
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

        private fun List<String>.startOffsets(): List<Int> = runningFold(0) { offset, part -> offset + part.length }
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
