package pl.dev404.czytanie.core.domain.model

/** The whole reading content: phoneme inventory, levels with their lessons, and words. */
data class Dictionary(
    val version: Int,
    val phonemes: List<Phoneme>,
    val levels: List<Level>,
    val words: List<Word>,
)

/**
 * A level of the progression (1 vowels … 7 short sentences). Levels from [FIRST_STORY_LEVEL] on
 * are stories (czytanki): one story per level, named after it, told in short sentences.
 *
 * @property lessons lesson ids in teaching order.
 */
data class Level(
    val id: Int,
    val name: String,
    val lessons: List<LessonId>,
) {
    companion object {
        const val FIRST_STORY_LEVEL = 8
        private const val MAX_LEVEL = 99

        /** Levels 1–7 from the design document, then the stories. */
        val RANGE = 1..MAX_LEVEL
    }
}

/** A lesson: an ordered set of words read in one short session. */
data class Lesson(
    val id: LessonId,
    val level: Int,
    val words: List<Word>,
) {
    companion object {
        /** A lesson is short: the design document asks for 8–10 words; level 1 has only 6 vowels. */
        val SIZE_RANGE = 6..10
    }
}
