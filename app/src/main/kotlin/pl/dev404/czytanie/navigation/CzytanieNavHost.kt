package pl.dev404.czytanie.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import pl.dev404.czytanie.feature.levels.LevelsRoute
import pl.dev404.czytanie.feature.levels.levelsScreen
import pl.dev404.czytanie.feature.reading.navigateToReading
import pl.dev404.czytanie.feature.reading.readingScreen

/** The only place that knows about all features; features never depend on each other. */
@Composable
fun CzytanieNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = LevelsRoute,
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
    ) {
        levelsScreen(onLessonClick = navController::navigateToReading)
        // Popping to the list (not just once) keeps a quick double tap from emptying the back stack.
        readingScreen(onBack = { navController.popBackStack(LevelsRoute, inclusive = false) })
    }
}
