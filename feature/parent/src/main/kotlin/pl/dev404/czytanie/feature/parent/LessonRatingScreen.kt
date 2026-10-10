package pl.dev404.czytanie.feature.parent

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.dev404.czytanie.core.domain.model.ParentRating
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.ui.theme.Andika

@Composable
internal fun LessonRatingScreenRoute(
    onDone: () -> Unit,
    viewModel: LessonRatingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == LessonRatingUiState.Saved) onDone() }
    LessonRatingScreen(state = state, onEvent = viewModel::onEvent, onCancel = onDone)
}

@Composable
internal fun LessonRatingScreen(
    state: LessonRatingUiState,
    onEvent: (LessonRatingEvent) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when (state) {
            LessonRatingUiState.Loading, LessonRatingUiState.Saved ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator()
                }

            LessonRatingUiState.LessonNotFound ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(stringResource(R.string.parent_lesson_not_found))
                }

            is LessonRatingUiState.Rating -> RatingList(state, onEvent, onCancel)
        }
    }
}

@Composable
private fun RatingList(
    state: LessonRatingUiState.Rating,
    onEvent: (LessonRatingEvent) -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(stringResource(R.string.parent_rate_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.parent_rate_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.words, key = { it.id.value }) { word ->
                WordRating(word = word, selected = state.ratings[word.id], onRate = {
                    onEvent(LessonRatingEvent.Rate(word.id, it))
                })
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        ) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.parent_back)) }
            Button(onClick = { onEvent(LessonRatingEvent.Save) }, enabled = state.canSave) {
                Text(stringResource(R.string.parent_rate_save))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordRating(
    word: Word,
    selected: ParentRating?,
    onRate: (ParentRating) -> Unit,
) {
    Column {
        Text(text = word.text, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Andika))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ParentRating.entries.forEach { rating ->
                FilterChip(
                    selected = selected == rating,
                    onClick = { onRate(rating) },
                    label = { Text(stringResource(rating.label)) },
                )
            }
        }
    }
}
