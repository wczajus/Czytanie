package pl.dev404.czytanie.core.domain.pacing

import pl.dev404.czytanie.core.domain.model.ReadingUnit
import pl.dev404.czytanie.core.domain.model.Word

/** What the arrow points at, one step at a time. */
enum class PointingUnit {
    /** Graphemes ("sz", "a", "f", "a"), each with its phoneme recording. The default. */
    Graphemes,

    /** Syllables ("sza", "fa"), each with its syllable recording. */
    Syllables,
}

/**
 * The spans the arrow visits for [unit]. Syllables are returned as [ReadingUnit]s whose
 * [ReadingUnit.phonemeId] is the lowercase syllable, so the UI positions both the same way.
 */
fun Word.pointingUnits(unit: PointingUnit): List<ReadingUnit> = when (unit) {
    PointingUnit.Graphemes -> units

    PointingUnit.Syllables -> syllables.map {
        ReadingUnit(
            grapheme = it.text,
            phonemeId = it.text.lowercase(),
            startIndex = it.startIndex,
            endIndex = it.endIndex,
        )
    }
}
