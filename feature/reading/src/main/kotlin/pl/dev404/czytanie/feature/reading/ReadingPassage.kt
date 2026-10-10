package pl.dev404.czytanie.feature.reading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.launch
import pl.dev404.czytanie.core.domain.model.ReadingUnit
import pl.dev404.czytanie.core.domain.model.Word
import pl.dev404.czytanie.core.domain.pacing.ReadingState
import pl.dev404.czytanie.core.ui.theme.CzytanieThemeExtras

/** Arrow size relative to the font size of the passage. */
private const val PASSAGE_ARROW_HEIGHT_EM = 0.5f
private const val PASSAGE_ARROW_WIDTH_EM = 0.65f

/** Gap between the baseline of a line and the tip of the arrow, relative to the font size. */
private const val PASSAGE_ARROW_GAP_EM = 0.3f

/** The current line is kept this far down the visible area, so the next lines are in sight. */
private const val CURRENT_LINE_POSITION = 3

/**
 * A story's paragraph (czytanka), wrapped over several lines. Works like [ReadingWord]: the
 * current unit is highlighted and the arrow sits under it, in the free space of the tall line.
 * When the arrow moves to another line it jumps there instead of gliding across the text, and
 * the paragraph scrolls so the current line stays in view.
 */
@Composable
internal fun ReadingPassage(
    word: Word,
    readingState: ReadingState,
    style: TextStyle,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    units: List<ReadingUnit> = word.units,
) {
    val colors = CzytanieThemeExtras.readingColors
    var layout by remember(word, style) { mutableStateOf<TextLayoutResult?>(null) }
    val arrowX = remember(word, style) { Animatable(Float.NaN) }
    var arrowLine by remember(word, style) { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    val target = readingState.arrowTarget()
    val fontSizePx = with(LocalDensity.current) { style.fontSize.toPx() }

    BoxWithConstraints(modifier) {
        val viewportPx = constraints.maxHeight
        LaunchedEffect(layout, target) {
            val textLayout = layout ?: return@LaunchedEffect
            val unit = units[target.index]
            val line = textLayout.getLineForOffset(unit.startIndex)
            launch {
                val top = textLayout.getLineTop(line).toInt()
                val wanted = if (viewportPx == Int.MAX_VALUE) 0 else top - viewportPx / CURRENT_LINE_POSITION
                scroll.animateScrollTo(wanted.coerceAtLeast(0))
            }
            val x = textLayout.centerOf(unit)
            if (arrowX.value.isNaN() || target.moveMillis == 0 || line != arrowLine) {
                arrowLine = line
                arrowX.snapTo(x)
            } else {
                arrowX.animateTo(x, tween(target.moveMillis, easing = FastOutSlowInEasing))
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().verticalScroll(scroll).testTag(READING_WORD_TAG).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onTap,
            ),
        ) {
            Text(
                text = word.highlighted(readingState, colors, units),
                style = style,
                softWrap = true,
                onTextLayout = { layout = it },
                modifier = Modifier.fillMaxWidth(),
            )
            if (readingState != ReadingState.WordDone) {
                Canvas(Modifier.matchParentSize().testTag(READING_ARROW_TAG)) {
                    val x = arrowX.value
                    val textLayout = layout
                    if (x.isNaN() || textLayout == null) return@Canvas
                    val halfWidth = fontSizePx * PASSAGE_ARROW_WIDTH_EM / 2
                    val y = textLayout.getLineBaseline(arrowLine) + fontSizePx * PASSAGE_ARROW_GAP_EM
                    val path = Path().apply {
                        moveTo(x, y)
                        lineTo(x + halfWidth, y + fontSizePx * PASSAGE_ARROW_HEIGHT_EM)
                        lineTo(x - halfWidth, y + fontSizePx * PASSAGE_ARROW_HEIGHT_EM)
                        close()
                    }
                    drawPath(path, colors.arrow)
                }
            }
        }
    }
}
