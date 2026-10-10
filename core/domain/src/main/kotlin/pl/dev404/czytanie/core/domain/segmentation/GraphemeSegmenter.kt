package pl.dev404.czytanie.core.domain.segmentation

import pl.dev404.czytanie.core.domain.model.ReadingUnit

/**
 * Proposes how a Polish word splits into reading units (graphemes).
 *
 * This is only a helper for adding words and for validation. The hand annotation in the
 * dictionary is the source of truth: Polish has exceptions no rule catches ("marznąć", "odzysk"),
 * so every proposal must be confirmed by a person. [Segmentation.hints] point at the spots
 * that deserve a second look.
 *
 * Rules, applied left to right, longest match first:
 * 1. "dzi" before a vowel is one unit (soft "dź"): "dziadek" → dzi a d e k.
 * 2. "si", "ci", "zi", "ni" before a vowel are one unit: "siano" → si a n o.
 * 3. Digraphs "dż", "dź", "dz", "sz", "cz", "rz", "ch" are one unit: "szafa" → sz a f a.
 * 4. "i" before a vowel that only softens b, p, m, w, f, k, g, l, h or ch joins it into one unit:
 *    "pies" → pi e s, "Wierna" → Wi e r n a. Loanwords read the "i" separately ("pianino"), so
 *    this always comes with a hint.
 * 5. Every other letter is its own unit.
 *
 * When "i" is the vowel of the syllable after s/c/z/n/dz ("nic", "zima", "dzisiaj"), the "i" is
 * its own unit and the preceding consonant gets the soft phoneme id ("ni", "zi", "dzi"),
 * because it is pronounced as ń/ź/dź.
 *
 * Case is preserved in graphemes; phoneme ids are lowercase.
 *
 * @param exceptions whole words (lowercase) whose split the rules get wrong, mapped to the
 *   correct graphemes in order.
 */
class GraphemeSegmenter(private val exceptions: Map<String, List<String>> = DEFAULT_EXCEPTIONS) {
    init {
        exceptions.forEach { (word, units) ->
            require(units.joinToString("") == word) { "Exception '$word' is split into $units" }
        }
    }

    fun segment(text: String): Segmentation {
        require(text.isNotEmpty()) { "Cannot segment empty text" }
        require(text.all { it.isLetter() }) { "Only single words of letters are supported: '$text'" }

        val lower = text.lowercase()
        check(lower.length == text.length) { "Lowercasing changed the length of '$text'" }
        exceptions[lower]?.let { graphemes ->
            return Segmentation(
                units = graphemes.toUnits(text),
                hints = listOf(SegmentationHint.FromExceptionList(startIndex = 0)),
            )
        }

        val units = mutableListOf<ReadingUnit>()
        val hints = mutableListOf<SegmentationHint>()
        var i = 0
        while (i < lower.length) {
            val length = unitLengthAt(lower, i)
            val end = i + length
            val grapheme = lower.substring(i, end)
            units += ReadingUnit(
                grapheme = text.substring(i, end),
                phonemeId = phonemeIdFor(grapheme, lower, end),
                startIndex = i,
                endIndex = end,
            )
            hintAt(lower, i, grapheme)?.let(hints::add)
            i = end
        }
        return Segmentation(units, hints)
    }

    private fun unitLengthAt(
        word: String,
        i: Int,
    ): Int {
        val softened = SOFT_BEFORE_VOWEL.firstOrNull { word.startsWith(it, i) && word.isVowelAt(i + it.length) }
        val digraph = DIGRAPHS.firstOrNull { word.startsWith(it, i) }
        val base = (softened ?: digraph)?.length ?: 1
        val softenedByI = softened == null && word.substring(i, i + base) in SOFTENED_BY_I &&
            word.getOrNull(i + base) == 'i' && word.isVowelAt(i + base + 1)
        return if (softenedByI) base + 1 else base
    }

    /** s/c/z/n/dz followed by "i" acting as the syllable vowel are read soft. */
    private fun phonemeIdFor(
        grapheme: String,
        word: String,
        end: Int,
    ): String = if (grapheme in SOFTENABLE && word.getOrNull(end) == 'i') grapheme + "i" else grapheme

    private fun hintAt(
        word: String,
        i: Int,
        grapheme: String,
    ): SegmentationHint? = when {
        grapheme == "rz" && word.isConsonantAt(i + 2) ->
            SegmentationHint.PossiblySeparateLetters(i, grapheme)

        grapheme.startsWith("dz") && PREFIXES_ENDING_WITH_D.any { word.startsWith(it) && it.length == i + 1 } ->
            SegmentationHint.PossiblySeparateLetters(i, grapheme)

        grapheme.length == 1 && grapheme !in SOFTENABLE && grapheme[0].isConsonant() &&
            word.getOrNull(i + 1) == 'i' && word.isVowelAt(i + 2) ->
            SegmentationHint.IBeforeVowel(i + 1)

        grapheme.endsWith('i') && grapheme.dropLast(1) in SOFTENED_BY_I ->
            SegmentationHint.IBeforeVowel(i + grapheme.length - 1)

        else -> null
    }

    private fun List<String>.toUnits(text: String): List<ReadingUnit> {
        var offset = 0
        return map { part ->
            val start = offset
            offset += part.length
            ReadingUnit(text.substring(start, offset), part.lowercase(), start, offset)
        }
    }

    companion object {
        private const val VOWELS = "aąeęioóuy"
        private val DIGRAPHS = listOf("dż", "dź", "dz", "sz", "cz", "rz", "ch")

        // Longest first, so "dzi" wins over "zi".
        private val SOFT_BEFORE_VOWEL = listOf("dzi", "si", "ci", "zi", "ni")
        private val SOFTENABLE = setOf("s", "c", "z", "n", "dz")
        private val SOFTENED_BY_I = setOf("b", "p", "m", "w", "f", "k", "g", "l", "h", "ch")
        private val PREFIXES_ENDING_WITH_D = listOf("od", "nad", "pod", "przed")

        /** Words where a digraph's letters are read separately. Extend as the dictionary grows. */
        val DEFAULT_EXCEPTIONS: Map<String, List<String>> = mapOf(
            "marznąć" to listOf("m", "a", "r", "z", "n", "ą", "ć"),
            "zamarznąć" to listOf("z", "a", "m", "a", "r", "z", "n", "ą", "ć"),
            "odzysk" to listOf("o", "d", "z", "y", "s", "k"),
            "odzyskać" to listOf("o", "d", "z", "y", "s", "k", "a", "ć"),
            "odzew" to listOf("o", "d", "z", "e", "w"),
            "nadzór" to listOf("n", "a", "d", "z", "ó", "r"),
        )

        private fun Char.isVowel() = this in VOWELS

        private fun Char.isConsonant() = isLetter() && !isVowel()

        private fun String.isVowelAt(index: Int) = getOrNull(index)?.isVowel() == true

        private fun String.isConsonantAt(index: Int) = getOrNull(index)?.isConsonant() == true
    }
}

data class Segmentation(
    val units: List<ReadingUnit>,
    val hints: List<SegmentationHint>,
) {
    val graphemes: List<String> get() = units.map { it.grapheme }
}

/** A place where the proposed split may be wrong and a person should check it. */
sealed interface SegmentationHint {
    val startIndex: Int

    /** The split comes from the exception list, not from the rules. */
    data class FromExceptionList(override val startIndex: Int) : SegmentationHint

    /** A digraph that may be two separate sounds, e.g. "rz" in "marznąć" or "dz" in "odzysk". */
    data class PossiblySeparateLetters(
        override val startIndex: Int,
        val grapheme: String,
    ) : SegmentationHint

    /**
     * "i" before a vowel after a consonant other than s/c/z/n: it may only soften the consonant
     * ("miasto") or be a separate sound ("Maria", "pianino").
     */
    data class IBeforeVowel(override val startIndex: Int) : SegmentationHint
}
