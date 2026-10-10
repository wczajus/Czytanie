package pl.dev404.czytanie.core.data.content

/** Small dictionaries in the file format, for parser and import tests. */
internal object TestDictionaries {
    private val syllables = listOf("ma", "mo", "mu", "me", "mi", "my")

    fun json(
        version: Int = 1,
        firstSyllable: String = "ma",
    ): String {
        val words = (listOf(firstSyllable) + syllables.drop(1)).joinToString(",\n") { s ->
            """{"id": "$s", "text": "$s", "units": ["${s[0]}", "${s[1]}"], """ +
                """"syllables": ["$s"], "level": 2, "lesson": "l2"}"""
        }
        val phonemes = listOf("m", "a", "o", "u", "e", "i", "y", firstSyllable[0].toString()).distinct()
            .joinToString(",\n") { """{"id": "$it", "grapheme": "$it", "audio": "audio/phonemes/$it.ogg"}""" }
        return """
            {
              "version": $version,
              "phonemes": [$phonemes],
              "levels": [{"id": 2, "name": "Sylaby otwarte", "lessons": ["l2"]}],
              "words": [$words]
            }
        """.trimIndent()
    }
}
