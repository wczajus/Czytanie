package pl.dev404.czytanie.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import pl.dev404.czytanie.feature.levels.LevelsRoute
import pl.dev404.czytanie.feature.levels.levelsScreen
import pl.dev404.czytanie.feature.parent.ParentPanelRoute
import pl.dev404.czytanie.feature.parent.lessonRatingScreen
import pl.dev404.czytanie.feature.parent.navigateToLessonRating
import pl.dev404.czytanie.feature.parent.navigateToParentPanel
import pl.dev404.czytanie.feature.parent.navigateToPrivacy
import pl.dev404.czytanie.feature.parent.parentPanelScreen
import pl.dev404.czytanie.feature.parent.privacyScreen
import pl.dev404.czytanie.feature.reading.navigateToReading
import pl.dev404.czytanie.feature.reading.readingScreen
import pl.dev404.czytanie.feature.settings.navigateToSettings
import pl.dev404.czytanie.feature.settings.settingsScreen

/** The only place that knows about all features; features never depend on each other. */
@Composable
fun CzytanieNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LevelsRoute,
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
    ) {
        // Popping to the list (not just once) keeps a quick double tap from emptying the back stack.
        val backToLevels: () -> Unit = { navController.popBackStack(LevelsRoute, inclusive = false) }
        levelsScreen(
            onLessonClick = navController::navigateToReading,
            onParentClick = navController::navigateToParentPanel,
        )
        readingScreen(onBack = backToLevels, onRateLesson = navController::navigateToLessonRating)
        val backToParentPanel: () -> Unit = { navController.popBackStack(ParentPanelRoute, inclusive = false) }
        parentPanelScreen(
            onBack = backToLevels,
            onOpenSettings = navController::navigateToSettings,
            onOpenPrivacy = navController::navigateToPrivacy,
        )
        lessonRatingScreen(onDone = backToLevels)
        settingsScreen(onBack = backToParentPanel)
        privacyScreen(onBack = backToParentPanel)
    }
}
