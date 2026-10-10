package pl.dev404.czytanie.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// High-contrast palette: near-black on warm paper, no pure white to reduce glare.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF1D4E89),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5E3F7),
    onPrimaryContainer = Color(0xFF0B2545),
    secondary = Color(0xFF8A4B00),
    onSecondary = Color(0xFFFFFFFF),
    // Tonal buttons: a warm neutral that matches the paper, instead of Material's default lilac.
    secondaryContainer = Color(0xFFE9E1D4),
    onSecondaryContainer = Color(0xFF1B1B1B),
    background = Color(0xFFFBF8F2),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFBF8F2),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFECE7DD),
    onSurfaceVariant = Color(0xFF45413A),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7F0),
    onPrimary = Color(0xFF0B2545),
    primaryContainer = Color(0xFF1D4E89),
    onPrimaryContainer = Color(0xFFD5E3F7),
    secondary = Color(0xFFFFB866),
    onSecondary = Color(0xFF3D2000),
    secondaryContainer = Color(0xFF37332C),
    onSecondaryContainer = Color(0xFFF2EFE9),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2EFE9),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF2EFE9),
    surfaceVariant = Color(0xFF2B2925),
    onSurfaceVariant = Color(0xFFD0CABF),
)

/** Colours of the word being read: the current unit stands out, read units fade back. */
@Immutable
data class ReadingColors(
    val upcoming: Color,
    val current: Color,
    val read: Color,
    val arrow: Color,
)

internal val LightReadingColors = ReadingColors(
    upcoming = Color(0xFF1B1B1B),
    current = Color(0xFFB3261E),
    read = Color(0xFF8C8C8C),
    arrow = Color(0xFFB3261E),
)

internal val DarkReadingColors = ReadingColors(
    upcoming = Color(0xFFF2EFE9),
    current = Color(0xFFFF8A80),
    read = Color(0xFF7A7A7A),
    arrow = Color(0xFFFF8A80),
)

val LocalReadingColors = staticCompositionLocalOf { LightReadingColors }
