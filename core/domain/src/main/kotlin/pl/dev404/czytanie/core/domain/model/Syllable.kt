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
}
