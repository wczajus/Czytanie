package pl.dev404.czytanie.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pl.dev404.czytanie.core.domain.settings.WordFontSize
import pl.dev404.czytanie.core.ui.R

/**
 * Andika: a school-style typeface with a single-storey "a" and clearly distinct b/d/p/q.
 * Used for every piece of text, so what the child sees on buttons matches the words they read.
 */
val Andika = FontFamily(
    Font(R.font.andika_regular, FontWeight.Normal),
    Font(R.font.andika_bold, FontWeight.Bold),
)

private val base = Typography()

val CzytanieTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Andika),
    displayMedium = base.displayMedium.copy(fontFamily = Andika),
    displaySmall = base.displaySmall.copy(fontFamily = Andika),
    headlineLarge = base.headlineLarge.copy(fontFamily = Andika),
    headlineMedium = base.headlineMedium.copy(fontFamily = Andika),
    headlineSmall = base.headlineSmall.copy(fontFamily = Andika),
    titleLarge = base.titleLarge.copy(fontFamily = Andika),
    titleMedium = base.titleMedium.copy(fontFamily = Andika),
    titleSmall = base.titleSmall.copy(fontFamily = Andika),
    bodyLarge = base.bodyLarge.copy(fontFamily = Andika, fontSize = 18.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Andika, fontSize = 16.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Andika),
    labelLarge = base.labelLarge.copy(fontFamily = Andika, fontSize = 18.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Andika),
    labelSmall = base.labelSmall.copy(fontFamily = Andika),
)

/**
 * Style of the word on the reading screen. Generous letter spacing keeps graphemes apart,
 * so the arrow clearly sits under one unit.
 */
fun readingWordStyle(size: WordFontSize): TextStyle = TextStyle(
    fontFamily = Andika,
    fontWeight = FontWeight.Normal,
    fontSize = when (size) {
        WordFontSize.Medium -> 56.sp
        WordFontSize.Large -> 72.sp
        WordFontSize.ExtraLarge -> 96.sp
    },
    letterSpacing = 0.08.em,
)
