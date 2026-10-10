package pl.dev404.czytanie.feature.reading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import pl.dev404.czytanie.core.domain.model.ReadingUnit
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.pacing.ReadingState
import pl.dev404.czytanie.core.domain.pacing.currentIndex
import pl.dev404.czytanie.core.ui.theme.CzytanieThemeExtras
import pl.dev404.czytanie.core.ui.theme.ReadingColors

internal const val READING_WORD_TAG = "reading_word"
internal const val READING_ARROW_TAG = "reading_arrow"

/** Share of a step spent gliding to the next unit; the arrow then rests under it while it sounds. */
private const val ARROW_MOVE_SHARE = 0.3f
private const val ARROW_MOVE_MAX_MILLIS = 300

/**
 * The word being read, with the arrow under the current unit. The current unit is highlighted
 * and the units already read fade back. Colour is the only difference between them: a bolder
 * weight would change the word's width and make the arrow jump.
 *
 * The domain only says which unit is current and how long the step is; the movement lives here.
 */
@Composable
internal fun ReadingWord(
    word: Word,
    readingState: ReadingState,
    style: TextStyle,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CzytanieThemeExtras.readingColors
    var layout by remember(word, style) { mutableStateOf<TextLayoutResult?>(null) }
    val arrowX = remember(word, style) { Animatable(Float.NaN) }
    val target = readingState.arrowTarget()

    LaunchedEffect(layout, target) {
        val textLayout = layout ?: return@LaunchedEffect
        val x = textLayout.centerOf(word.units[target.index])
        if (arrowX.value.isNaN() || target.moveMillis == 0) {
            arrowX.snapTo(x)
        } else {
            arrowX.animateTo(x, tween(target.moveMillis, easing = FastOutSlowInEasing))
        }
    }

    Box(
        // The tag is on the clickable box, which merges the word's text into one node.
        modifier = modifier.testTag(READING_WORD_TAG).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClick = onTap,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = word.highlighted(readingState, colors),
                style = style,
                softWrap = false,
                onTextLayout = { layout = it },
            )
            Spacer(Modifier.height(ArrowHeight + ArrowGap))
        }
        if (readingState != ReadingState.WordDone) {
            Arrow(
                xProvider = { arrowX.value },
                top = { (layout?.size?.height ?: 0).toFloat() },
                colors = colors,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun Arrow(
    xProvider: () -> Float,
    top: () -> Float,
    colors: ReadingColors,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.testTag(READING_ARROW_TAG).padding(top = ArrowGap)) {
        val x = xProvider()
        if (x.isNaN()) return@Canvas
        val halfWidth = ArrowWidth.toPx() / 2
        val y = top()
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x + halfWidth, y + ArrowHeight.toPx())
            lineTo(x - halfWidth, y + ArrowHeight.toPx())
            close()
        }
        drawPath(path, colors.arrow)
    }
}

private val ArrowHeight = 28.dp
private val ArrowWidth = 36.dp
private val ArrowGap = 8.dp

/** Where the arrow should be and how long it may take to get there. */
private data class ArrowTarget(
    val index: Int,
    val moveMillis: Int,
)

private fun ReadingState.arrowTarget(): ArrowTarget = when (this) {
    is ReadingState.Pointing -> ArrowTarget(
        index = index,
        moveMillis = (tempoMillis * ARROW_MOVE_SHARE).toInt().coerceAtMost(ARROW_MOVE_MAX_MILLIS),
    )

    // Before the start and after the end the arrow waits under the first unit.
    else -> ArrowTarget(index = currentIndex ?: 0, moveMillis = 0)
}

/**
 * Horizontal centre of [unit]: between the left edge of its first character and the right edge
 * of its last one, so a digraph like "sz" gets the arrow under its middle.
 */
private fun TextLayoutResult.centerOf(unit: ReadingUnit): Float {
    val first = getBoundingBox(unit.startIndex)
    val last = getBoundingBox(unit.lastIndex)
    return (first.left + last.right) / 2
}

private fun Word.highlighted(
    state: ReadingState,
    colors: ReadingColors,
): AnnotatedString {
    val current = state.currentIndex
    fun colorOf(index: Int) = when {
        current == null -> colors.upcoming
        index < current -> colors.read
        index == current -> colors.current
        else -> colors.upcoming
    }

    // Spaces and punctuation of a sentence (level 7) are never current; they fade once passed.
    fun gapColorBefore(index: Int) = if (current != null && index <= current) colors.read else colors.upcoming
    return buildAnnotatedString {
        var end = 0
        units.forEachIndexed { index, unit ->
            if (unit.startIndex > end) {
                withStyle(SpanStyle(color = gapColorBefore(index))) { append(text.substring(end, unit.startIndex)) }
            }
            withStyle(SpanStyle(color = colorOf(index))) { append(unit.grapheme) }
            end = unit.endIndex
        }
        if (end < text.length) withStyle(SpanStyle(color = gapColorBefore(units.size))) { append(text.substring(end)) }
    }
}
