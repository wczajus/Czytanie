package pl.dev404.czytanie.feature.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** The privacy policy inside the app, as Google Play Families requires. Same text as doc/polityka-prywatnosci.md. */
@Composable
internal fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.privacy_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onBack) { Text(stringResource(R.string.parent_back)) }
            }
            PRIVACY_PARAGRAPHS.forEach { paragraph ->
                Text(text = stringResource(paragraph), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private val PRIVACY_PARAGRAPHS = listOf(
    R.string.privacy_summary,
    R.string.privacy_no_network,
    R.string.privacy_stored_data,
    R.string.privacy_no_third_parties,
    R.string.privacy_tts,
    R.string.privacy_deletion,
    R.string.privacy_contact,
)
