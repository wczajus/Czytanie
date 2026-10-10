package pl.dev404.czytanie.feature.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import pl.dev404.czytanie.core.domain.progress.Attempt

@Composable
internal fun ParentPanelScreenRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ParentPanelViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ParentPanelScreen(state = state, onBack = onBack, onOpenSettings = onOpenSettings)
}

/** For the parent: plain, dense, no child-oriented decoration. */
@Composable
internal fun ParentPanelScreen(
    state: ParentPanelUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.parent_panel_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onBack) { Text(stringResource(R.string.parent_back)) }
                }
            }
            item {
                Button(onClick = onOpenSettings, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.parent_open_settings))
                }
            }
            item {
                Text(
                    text = stringResource(R.string.parent_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (!state.isLoading && state.history.isEmpty()) {
                item { Text(stringResource(R.string.parent_history_empty), style = MaterialTheme.typography.bodyLarge) }
            }
            items(state.history) { attempt ->
                HistoryRow(attempt)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryRow(attempt: Attempt) {
    val date = remember(attempt.createdAtEpochMillis) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(attempt.createdAtEpochMillis))
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attempt.wordText ?: stringResource(R.string.parent_word_removed),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = date, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = stringResource(attempt.rating.label), style = MaterialTheme.typography.bodyLarge)
    }
}
