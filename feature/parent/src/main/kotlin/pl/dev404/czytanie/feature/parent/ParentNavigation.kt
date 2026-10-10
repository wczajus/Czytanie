package pl.dev404.czytanie.feature.parent

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.ui.parent.ParentGated

/** The parent's area behind the parental gate: history and the way to settings. */
@Serializable
data object ParentPanelRoute

/** The parent rates the words of a lesson the child has just read. Also behind the gate. */
@Serializable
data class LessonRatingRoute(val lessonId: String) {
    companion object {
        internal const val LESSON_ID_KEY = "lessonId"
    }
}

fun NavController.navigateToParentPanel() = navigate(ParentPanelRoute)

fun NavController.navigateToLessonRating(lessonId: LessonId) = navigate(LessonRatingRoute(lessonId.value))

fun NavGraphBuilder.parentPanelScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    composable<ParentPanelRoute> {
        ParentGated(onCancel = onBack) { ParentPanelScreenRoute(onBack = onBack, onOpenSettings = onOpenSettings) }
    }
}

fun NavGraphBuilder.lessonRatingScreen(onDone: () -> Unit) {
    composable<LessonRatingRoute> {
        ParentGated(onCancel = onDone) { LessonRatingScreenRoute(onDone = onDone) }
    }
}
