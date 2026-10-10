package pl.dev404.czytanie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import pl.dev404.czytanie.core.ui.theme.CzytanieTheme
import pl.dev404.czytanie.navigation.CzytanieNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CzytanieTheme {
                CzytanieNavHost()
            }
        }
    }
}
