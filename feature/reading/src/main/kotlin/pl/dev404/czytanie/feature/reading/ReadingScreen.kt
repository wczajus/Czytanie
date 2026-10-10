package pl.dev404.czytanie.feature.reading

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.dev404.czytanie.core.domain.model.LessonId
import pl.dev404.czytanie.core.domain.pacing.ReadingState
import pl.dev404.czytanie.core.ui.theme.readingWordStyle

@Composable
internal fun ReadingScreenRoute(
    onBack: () -> Unit,
    onRateLesson: (LessonId) -> Unit = {},
    viewModel: ReadingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReadingScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack, onRateLesson = onRateLesson)
}

@Composable
internal fun ReadingScreen(
    state: ReadingUiState,
    onEvent: (ReadingEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRateLesson: (LessonId) -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(ScreenPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.size(TopButtonSize)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_reading_back),
                        contentDescription = stringResource(R.string.reading_back),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (state is ReadingUiState.Reading) {
                    ProgressDots(current = state.wordIndex, count = state.wordCount)
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (state) {
                    ReadingUiState.Loading -> CircularProgressIndicator()

                    ReadingUiState.LessonNotFound -> Message(stringResource(R.string.reading_lesson_not_found))

                    is ReadingUiState.LessonFinished -> LessonFinished(
                        onEvent = onEvent,
                        onBack = onBack,
                        onRate = { onRateLesson(state.lessonId) },
                    )

                    is ReadingUiState.Reading -> WordArea(state = state, onEvent = onEvent)
                }
            }
            if (state is ReadingUiState.Reading) {
                Controls(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun WordArea(
    state: ReadingUiState.Reading,
    onEvent: (ReadingEvent) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        state.image?.let { WordImage(path = it, description = state.word.text) }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val maxWidthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            ReadingWord(
                word = state.word,
                readingState = state.readingState,
                style = fittedWordStyle(state.word.text, readingWordStyle(state.fontSize), maxWidthPx),
                onTap = { onEvent(ReadingEvent.WordTapped) },
            )
        }
    }
}

/** Shrinks the chosen size only when a long word would not fit on one line. */
@Composable
private fun fittedWordStyle(
    text: String,
    style: TextStyle,
    maxWidthPx: Int,
): TextStyle {
    val measurer = rememberTextMeasurer()
    return remember(text, style, maxWidthPx) {
        val width = measurer.measure(text, style, softWrap = false, maxLines = 1).size.width
        if (width <= maxWidthPx) style else style.copy(fontSize = style.fontSize * (maxWidthPx * FIT_MARGIN / width))
    }
}

@Composable
private fun Controls(
    state: ReadingUiState.Reading,
    onEvent: (ReadingEvent) -> Unit,
) {
    // Buttons keep their places for the whole lesson, so the child learns where each one is.
    Row(
        modifier = Modifier.fillMaxWidth().wrapContentWidth().widthIn(max = ControlsMaxWidth),
        horizontalArrangement = Arrangement.spacedBy(ButtonSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReadingButton(
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_reading_repeat,
            label = stringResource(R.string.reading_repeat),
            enabled = state.canRepeat,
            onClick = { onEvent(ReadingEvent.Repeat) },
        )
        if (state.isWordDone) {
            ReadingButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_reading_next,
                label = stringResource(R.string.reading_next),
                primary = true,
                enabled = state.canGoNext,
                onClick = { onEvent(ReadingEvent.Next) },
            )
        } else {
            val (icon, label) = when (state.readingState) {
                ReadingState.Idle -> R.drawable.ic_reading_play to R.string.reading_start
                is ReadingState.Paused -> R.drawable.ic_reading_play to R.string.reading_resume
                else -> R.drawable.ic_reading_pause to R.string.reading_pause
            }
            ReadingButton(
                modifier = Modifier.weight(1f),
                icon = icon,
                label = stringResource(label),
                primary = true,
                onClick = { onEvent(ReadingEvent.MainAction) },
            )
        }
        if (state.canPlayWordAudio) {
            ReadingButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_reading_listen,
                label = stringResource(R.string.reading_listen),
                enabled = state.canListen,
                onClick = { onEvent(ReadingEvent.Listen) },
            )
        } else {
            // No Polish voice and no recording: nothing to offer, but the other buttons keep their places.
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ReadingButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val content: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(IconSize))
            Text(text = label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
    val buttonModifier = modifier.heightIn(min = ButtonMinHeight)
    if (primary) {
        Button(onClick, buttonModifier, enabled, contentPadding = ButtonPadding) { content() }
    } else {
        FilledTonalButton(onClick, buttonModifier, enabled, contentPadding = ButtonPadding) { content() }
    }
}

@Composable
private fun ProgressDots(
    current: Int,
    count: Int,
) {
    val description = stringResource(R.string.reading_progress, current + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(DotSpacing),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(count) { index ->
            val color = if (index <= current) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            Box(Modifier.size(DotSize).background(color, CircleShape))
        }
    }
}

@Composable
private fun LessonFinished(
    onEvent: (ReadingEvent) -> Unit,
    onBack: () -> Unit,
    onRate: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PraiseStar(modifier = Modifier.size(PraiseStarSize))
        Text(
            text = stringResource(R.string.reading_lesson_finished_title),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Message(stringResource(R.string.reading_lesson_finished_message))
        Spacer(Modifier.height(ButtonSpacing))
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonSpacing),
            modifier = Modifier.widthIn(max = ControlsMaxWidth),
        ) {
            ReadingButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_reading_repeat,
                label = stringResource(R.string.reading_read_lesson_again),
                onClick = { onEvent(ReadingEvent.ReadLessonAgain) },
            )
            ReadingButton(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_reading_back,
                label = stringResource(R.string.reading_back),
                primary = true,
                onClick = onBack,
            )
        }
        // For the adult next to the child; the rating screen itself is behind the parental gate.
        TextButton(onClick = onRate, modifier = Modifier.padding(top = ButtonSpacing)) {
            Text(stringResource(R.string.reading_rate_lesson), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = ButtonSpacing),
    )
}

/** Pictures are bundled assets; decoding happens off the main thread. */
@Composable
private fun WordImage(
    path: String,
    description: String,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                context.assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            } catch (_: IOException) {
                null
            }
        }
    }
    bitmap?.let {
        Image(bitmap = it, contentDescription = description, modifier = Modifier.size(ImageSize))
    }
}

/** Leaves a little air around a word that had to be shrunk to fit. */
private const val FIT_MARGIN = 0.92f

private val ScreenPadding = 16.dp
private val TopButtonSize = 56.dp
private val ControlsMaxWidth = 560.dp
private val ButtonMinHeight = 88.dp
private val ButtonPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
private val ButtonSpacing = 12.dp
private val IconSize = 36.dp
private val DotSize = 12.dp
private val DotSpacing = 6.dp
private val ImageSize = 200.dp
private val PraiseStarSize = 120.dp
