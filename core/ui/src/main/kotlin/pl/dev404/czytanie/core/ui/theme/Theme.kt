package pl.dev404.czytanie.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * App theme. Dynamic colour is deliberately off: the reading colours must keep the same
 * contrast on every device.
 */
@Composable
fun CzytanieTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalReadingColors provides if (darkTheme) DarkReadingColors else LightReadingColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = CzytanieTypography,
            content = content,
        )
    }
}

object CzytanieThemeExtras {
    val readingColors: ReadingColors
        @Composable
        @ReadOnlyComposable
        get() = LocalReadingColors.current
}
