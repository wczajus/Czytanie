package pl.dev404.czytanie.feature.levels

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pl.dev404.czytanie.core.domain.model.LessonId

/** Start destination: list of levels and lessons. */
@Serializable
data object LevelsRoute

fun NavGraphBuilder.levelsScreen(onLessonClick: (LessonId) -> Unit) {
    composable<LevelsRoute> { LevelsScreenRoute(onLessonClick = onLessonClick) }
}
