package pl.dev404.czytanie.core.domain.content

/** A problem found in the dictionary. [Severity.Error] blocks the content; warnings ask for a human check. */
data class DictionaryIssue(
    val severity: Severity,
    val subject: String,
    val message: String,
) {
    enum class Severity { Error, Warning }

    override fun toString() = "$severity [$subject]: $message"

    companion object {
        fun error(
            subject: String,
            message: String,
        ) = DictionaryIssue(Severity.Error, subject, message)

        fun warning(
            subject: String,
            message: String,
        ) = DictionaryIssue(Severity.Warning, subject, message)
    }
}

val List<DictionaryIssue>.errors get() = filter { it.severity == DictionaryIssue.Severity.Error }

val List<DictionaryIssue>.warnings get() = filter { it.severity == DictionaryIssue.Severity.Warning }
