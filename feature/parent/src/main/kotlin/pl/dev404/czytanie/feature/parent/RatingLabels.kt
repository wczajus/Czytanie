package pl.dev404.czytanie.feature.parent

import androidx.annotation.StringRes
import pl.dev404.czytanie.core.domain.model.ParentRating

@get:StringRes
internal val ParentRating.label: Int
    get() = when (this) {
        ParentRating.Read -> R.string.parent_rating_read
        ParentRating.WithHelp -> R.string.parent_rating_with_help
        ParentRating.NotYet -> R.string.parent_rating_not_yet
    }
