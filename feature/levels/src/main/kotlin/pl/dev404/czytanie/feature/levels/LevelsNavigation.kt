package pl.dev404.czytanie.feature.levels

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** Start destination: list of levels and lessons. */
@Serializable
data object LevelsRoute

fun NavGraphBuilder.levelsScreen() {
    composable<LevelsRoute> { LevelsScreen() }
}
