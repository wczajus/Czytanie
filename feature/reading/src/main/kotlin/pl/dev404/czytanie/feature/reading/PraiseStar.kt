package pl.dev404.czytanie.feature.reading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.cos
import kotlin.math.sin
import pl.dev404.czytanie.core.ui.theme.CzytanieThemeExtras

/** A gently scaling star: praise for finishing, the same for every child and every lesson. */
@Composable
internal fun PraiseStar(modifier: Modifier = Modifier) {
    val scale = remember { Animatable(PRAISE_START_SCALE) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
    val color = CzytanieThemeExtras.readingColors.current
    Canvas(modifier.graphicsLayer(scaleX = scale.value, scaleY = scale.value)) {
        val center = Offset(size.width / 2, size.height / 2)
        val outer = size.minDimension / 2
        val inner = outer * STAR_INNER_RATIO
        val path = Path()
        repeat(STAR_POINTS * 2) { i ->
            val radius = if (i % 2 == 0) outer else inner
            val angle = Math.PI * i / STAR_POINTS - Math.PI / 2
            val point = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color)
    }
}

private const val PRAISE_START_SCALE = 0.3f
private const val STAR_POINTS = 5
private const val STAR_INNER_RATIO = 0.45f
