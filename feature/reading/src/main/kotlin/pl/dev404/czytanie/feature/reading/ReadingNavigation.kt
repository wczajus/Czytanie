package pl.dev404.czytanie.feature.reading

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pl.dev404.czytanie.core.domain.model.LessonId

/** Reading one lesson word by word. [lessonId] is also the key in the ViewModel's SavedStateHandle. */
@Serializable
data class ReadingRoute(val lessonId: String) {
    companion object {
        internal const val LESSON_ID_KEY = "lessonId"
    }
}

fun NavController.navigateToReading(lessonId: LessonId) = navigate(ReadingRoute(lessonId.value))

fun NavGraphBuilder.readingScreen(onBack: () -> Unit) {
    composable<ReadingRoute> { ReadingScreenRoute(onBack = onBack) }
}
