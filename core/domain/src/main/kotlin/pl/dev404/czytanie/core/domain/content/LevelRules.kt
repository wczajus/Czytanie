package pl.dev404.czytanie.core.domain.content

import pl.dev404.czytanie.core.domain.model.ReadingUnit
import pl.dev404.czytanie.core.domain.model.Syllable
import pl.dev404.czytanie.core.domain.model.Word

/**
 * What a word may contain at each level, so that a child never meets a grapheme before the level
 * that introduces it:
 *
 * 1. a single vowel;
 * 2. one open syllable: a simple consonant and a vowel ("ma");
 * 3. two or more open syllables of simple graphemes ("mama");
 * 4. simple graphemes only, closed syllables and clusters allowed ("kot", "sklep");
 * 5. at least one digraph, no soft consonants or diacritics ("szafa");
 * 6. at least one soft consonant or diacritic letter ("siano", "koń"), including the digraphs
 *    written with one ("dź", "dż");
 * 7. a short sentence of two or more words, with any graphemes ("Ala ma kota.");
 * 8 and later: stories (czytanki), sentences as on level 7.
 *
 * Levels 1-6 hold single words, without spaces or punctuation.
 */
object LevelRules {
    private val VOWELS = setOf("a", "e", "i", "o", "u", "y")
    private val DIGRAPHS = setOf("sz", "cz", "rz", "ch", "dz", "dź", "dż")
    private val SOFT_PHONEMES = setOf("si", "ci", "zi", "ni", "dzi")
    private val DIACRITIC_LETTERS = setOf('ą', 'ę', 'ó', 'ł', 'ś', 'ć', 'ź', 'ń', 'ż')

    private const val VOWELS_LEVEL = 1
    private const val SYLLABLES_LEVEL = 2
    private const val OPEN_WORDS_LEVEL = 3
    private const val SIMPLE_WORDS_LEVEL = 4
    private const val DIGRAPHS_LEVEL = 5
    private const val SOFT_LEVEL = 6
    private const val SENTENCE_LEVEL = 7
    private val WORD = Regex("\\p{L}+")

    /** Reasons why [word] does not fit its level; empty when it fits. */
    fun violations(word: Word): List<String> {
        val words = WORD.findAll(word.text).count()
        val shape = when {
            word.level >= SENTENCE_LEVEL && words < 2 -> "levels 7+ hold sentences of two or more words"
            word.level < SENTENCE_LEVEL && word.text.any(Word::isSeparator) -> "levels 1-6 hold single words"
            else -> null
        }
        return listOfNotNull(shape) + contentViolations(word)
    }

    private fun contentViolations(word: Word): List<String> {
        val units = word.units
        return when (word.level) {
            VOWELS_LEVEL -> listOfNotNull(
                "level 1 holds a single vowel".takeUnless { units.size == 1 && units[0].isVowel },
            )

            SYLLABLES_LEVEL -> listOfNotNull(
                "level 2 holds one open syllable: simple consonant + vowel".takeUnless {
                    word.syllables.size == 1 && word.isOpenSimpleSyllable(word.syllables[0]) &&
                        units.size == 2
                },
            )

            OPEN_WORDS_LEVEL -> listOfNotNull(
                "level 3 holds words of two or more open syllables".takeUnless { word.syllables.size >= 2 },
                "level 3 allows only open syllables of simple graphemes".takeUnless {
                    word.syllables.all { word.isOpenSimpleSyllable(it) }
                },
            )

            SIMPLE_WORDS_LEVEL -> listOfNotNull(
                "level 4 allows only simple graphemes".takeUnless { units.all { it.isSimple } },
            )

            DIGRAPHS_LEVEL -> listOfNotNull(
                "level 5 needs a digraph".takeUnless { units.any { it.isDigraph } },
                "level 5 must not contain soft consonants or diacritics".takeUnless { units.none { it.isSoft } },
            )

            SOFT_LEVEL -> listOfNotNull(
                "level 6 needs a soft consonant or a diacritic".takeUnless { units.any { it.isSoft } },
            )

            else -> emptyList()
        }
    }

    private val ReadingUnit.key get() = grapheme.lowercase()

    private val ReadingUnit.isVowel get() = key in VOWELS

    private val ReadingUnit.isDigraph get() = key in DIGRAPHS

    private val ReadingUnit.isSoft get() = phonemeId in SOFT_PHONEMES || key.any { it in DIACRITIC_LETTERS }

    private val ReadingUnit.isSimple get() = !isDigraph && !isSoft

    /** An open syllable made of simple graphemes: a vowel, optionally preceded by one consonant. */
    private fun Word.isOpenSimpleSyllable(syllable: Syllable): Boolean {
        val inSyllable = units.filter { it.startIndex >= syllable.startIndex && it.endIndex <= syllable.endIndex }
        if (inSyllable.any { !it.isSimple }) return false
        return when (inSyllable.size) {
            1 -> inSyllable[0].isVowel
            2 -> !inSyllable[0].isVowel && inSyllable[1].isVowel
            else -> false
        }
    }
}
