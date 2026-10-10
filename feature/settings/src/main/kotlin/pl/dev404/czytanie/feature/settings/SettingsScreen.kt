package pl.dev404.czytanie.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToLong
import pl.dev404.czytanie.core.domain.pacing.PacingConfig
import pl.dev404.czytanie.core.domain.settings.ReadingSettings
import pl.dev404.czytanie.core.domain.settings.WordFontSize
import pl.dev404.czytanie.core.ui.theme.readingWordStyle

@Composable
internal fun SettingsScreenRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    SettingsScreen(settings = settings, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
internal fun SettingsScreen(
    settings: ReadingSettings?,
    onEvent: (SettingsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onBack) { Text(stringResource(R.string.settings_back)) }
            }
            if (settings != null) {
                TempoSetting(settings.tempoMillis, onEvent)
                PhonemesSetting(settings.phonemesEnabled, onEvent)
                FontSizeSetting(settings.fontSize, onEvent)
            }
        }
    }
}

/** Slower is on the left; the value is stored when the parent lets go of the slider. */
@Composable
private fun TempoSetting(
    tempoMillis: Long,
    onEvent: (SettingsEvent) -> Unit,
) {
    val range = PacingConfig.TEMPO_RANGE_MILLIS
    // The slider runs from slow to fast, so its value is the reversed tempo.
    var position by remember(tempoMillis) { mutableFloatStateOf((range.last + range.first - tempoMillis).toFloat()) }
    val shownTempo = range.last + range.first - position.roundToLong()
    Column {
        Text(stringResource(R.string.settings_tempo), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.settings_tempo_value, shownTempo / MILLIS_PER_SECOND),
            style = MaterialTheme.typography.bodyLarge,
        )
        Slider(
            value = position,
            onValueChange = { position = it },
            onValueChangeFinished = { onEvent(SettingsEvent.TempoChanged(shownTempo)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = ((range.last - range.first) / TEMPO_STEP_MILLIS - 1).toInt(),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_tempo_slower), modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.settings_tempo_faster),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PhonemesSetting(
    enabled: Boolean,
    onEvent: (SettingsEvent) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_phonemes), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.settings_phonemes_hint), style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = enabled, onCheckedChange = { onEvent(SettingsEvent.PhonemesToggled(it)) })
    }
}

@Composable
private fun FontSizeSetting(
    fontSize: WordFontSize,
    onEvent: (SettingsEvent) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_font_size),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            WordFontSize.entries.forEachIndexed { index, size ->
                SegmentedButton(
                    selected = size == fontSize,
                    onClick = { onEvent(SettingsEvent.FontSizeChosen(size)) },
                    shape = SegmentedButtonDefaults.itemShape(index, WordFontSize.entries.size),
                ) { Text(stringResource(size.label)) }
            }
        }
        Text(text = stringResource(R.string.settings_preview_word), style = readingWordStyle(fontSize))
    }
}

private val WordFontSize.label: Int
    get() = when (this) {
        WordFontSize.Medium -> R.string.settings_font_medium
        WordFontSize.Large -> R.string.settings_font_large
        WordFontSize.ExtraLarge -> R.string.settings_font_extra_large
    }

private const val MILLIS_PER_SECOND = 1000f
private const val TEMPO_STEP_MILLIS = 100L
