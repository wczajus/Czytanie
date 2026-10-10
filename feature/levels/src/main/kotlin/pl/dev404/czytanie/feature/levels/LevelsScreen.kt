package pl.dev404.czytanie.feature.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.model.Level
import pl.dev404.czytanie.core.ui.theme.CzytanieTheme

@Composable
internal fun LevelsScreenRoute(
    onLessonClick: (LessonId) -> Unit,
    onParentClick: () -> Unit,
    viewModel: LevelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LevelsScreen(state = state, onLessonClick = onLessonClick, onParentClick = onParentClick)
}

/** Levels and their lessons; a star marks a lesson the child has read well. */
@Composable
internal fun LevelsScreen(
    state: LevelsUiState,
    onLessonClick: (LessonId) -> Unit,
    modifier: Modifier = Modifier,
    onParentClick: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state) {
            LevelsUiState.Loading -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }

            is LevelsUiState.Ready -> LazyColumn(
                contentPadding = PaddingValues(ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(SectionSpacing),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.levels_title),
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onParentClick) { Text(stringResource(R.string.levels_for_parent)) }
                    }
                }
                items(state.levels, key = { it.id }) { level ->
                    LevelSection(level = level, passedLessons = state.passedLessons, onLessonClick = onLessonClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelSection(
    level: Level,
    passedLessons: Set<LessonId>,
    onLessonClick: (LessonId) -> Unit,
) = Column {
    Text(
        // A story (czytanka) is known by its title, not by a level number.
        text = if (level.id >= Level.FIRST_STORY_LEVEL) {
            level.name
        } else {
            stringResource(R.string.levels_level_title, level.id, level.name)
        },
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = ButtonSpacing),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(ButtonSpacing),
        verticalArrangement = Arrangement.spacedBy(ButtonSpacing),
        modifier = Modifier.fillMaxWidth(),
    ) {
        level.lessons.forEachIndexed { index, lessonId ->
            FilledTonalButton(
                onClick = { onLessonClick(lessonId) },
                modifier = Modifier.sizeIn(minWidth = LessonButtonSize, minHeight = LessonButtonSize),
            ) {
                val passed = lessonId in passedLessons
                val label = stringResource(R.string.levels_lesson, index + 1)
                val passedDescription = stringResource(R.string.levels_lesson_passed, index + 1)
                Text(
                    text = if (passed) "$label ★" else label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = if (passed) Modifier.semantics { contentDescription = passedDescription } else Modifier,
                )
            }
        }
    }
}

private val ScreenPadding = 24.dp
private val SectionSpacing = 24.dp
private val ButtonSpacing = 12.dp
private val LessonButtonSize = 72.dp

@Preview
@Composable
private fun LevelsScreenPreview() {
    CzytanieTheme {
        LevelsScreen(
            state = LevelsUiState.Ready(
                listOf(
                    Level(1, "Samogłoski", listOf(LessonId("l1"))),
                    Level(2, "Sylaby otwarte", listOf(LessonId("l2a"), LessonId("l2b"))),
                ),
            ),
            onLessonClick = {},
        )
    }
}
