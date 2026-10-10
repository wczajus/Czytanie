package pl.dev404.czytanie.core.domain.model

/**
 * One unit the arrow points at: a grapheme that is read as a single sound,
 * e.g. "a", "sz", "dzi". Never a single letter of a digraph.
 *
 * @property grapheme the exact characters of the unit as they appear in [Word.text].
 * @property phonemeId key into the phoneme inventory (recording to play). Usually equal to the
 *   lowercase grapheme, but may differ, e.g. the "n" in "nic" is read as the soft "ni" sound.
 * @property startIndex index of the first character of the unit in [Word.text] (inclusive).
 * @property endIndex index after the last character of the unit in [Word.text] (exclusive),
 *   so `text.substring(startIndex, endIndex) == grapheme`.
 */
data class ReadingUnit(
    val grapheme: String,
    val phonemeId: String,
    val startIndex: Int,
    val endIndex: Int,
) {
    init {
        require(grapheme.isNotEmpty()) { "Grapheme must not be empty" }
        require(phonemeId.isNotBlank()) { "Phoneme id must not be blank for '$grapheme'" }
        require(startIndex >= 0) { "startIndex must be >= 0, was $startIndex" }
        require(endIndex - startIndex == grapheme.length) {
            "Range [$startIndex, $endIndex) does not match grapheme '$grapheme'"
        }
    }

    /** Index of the last character of the unit (inclusive). Used to position the arrow. */
    val lastIndex: Int get() = endIndex - 1
}
