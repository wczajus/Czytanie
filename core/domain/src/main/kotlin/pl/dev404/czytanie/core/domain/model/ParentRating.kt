package pl.dev404.czytanie.core.domain.model

/** How the parent rated one reading. Only encouragement-friendly categories, no error counting. */
enum class ParentRating {
    /** "przeczytał" */
    Read,

    /** "z pomocą" */
    WithHelp,

    /** "jeszcze nie" */
    NotYet,
}
