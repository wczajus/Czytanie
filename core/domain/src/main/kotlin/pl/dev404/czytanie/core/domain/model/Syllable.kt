package pl.dev404.czytanie.core.domain.model

/** A syllable of a word, e.g. "sza" in "szafa". Indices follow the same convention as [ReadingUnit]. */
data class Syllable(
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
) {
    init {
        require(text.isNotEmpty()) { "Syllable must not be empty" }
        require(startIndex >= 0) { "startIndex must be >= 0, was $startIndex" }
        require(endIndex - startIndex == text.length) {
            "Range [$startIndex, $endIndex) does not match syllable '$text'"
        }
    }

    /** Asset path of the syllable's recording. */
    val audio: String get() = audioPathOf(text)

    companion object {
        private val ASCII = mapOf(
            'ą' to "a1", 'ć' to "c1", 'ę' to "e1", 'ł' to "l1", 'ń' to "n1",
            'ó' to "o1", 'ś' to "s1", 'ź' to "z1", 'ż' to "z2",
        )

        /**
         * Asset path of the recording of [syllable], e.g. "żą" → "audio/syllables/z2a1.ogg". Names
         * are lowercase ASCII, so they survive any tool and case-insensitive file systems. A syllable
         * spanning two words ("w do") is one sound, so the space is dropped ("wdo").
         */
        fun audioPathOf(syllable: String): String {
            val name = syllable.lowercase().filter(Char::isLetter).map { ASCII[it] ?: it.toString() }.joinToString("")
            return "audio/syllables/$name.ogg"
        }
    }
}
