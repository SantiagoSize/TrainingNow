package com.shagox.apptrainingnow.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val EsquemaOscuro = darkColorScheme(
    primary = VerdeTN,
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onPrimary = Color(0xFF000000),
    onBackground = Color.White,
    onSurface = Color.White
)

private val EsquemaClaro = lightColorScheme(
    primary = VerdeTNOscuro,
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212)
)

/**
 * Tema de Training Now.
 *
 * @param temaClaro true = modo día (fondo blanco), false = modo nocturno (fondo negro).
 * El valor se propaga con [LocalTemaClaro], que usan los colores adaptativos.
 */
@Composable
fun AppTrainingNowTheme(
    temaClaro: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTemaClaro provides temaClaro) {
        MaterialTheme(
            colorScheme = if (temaClaro) EsquemaClaro else EsquemaOscuro,
            typography = Typography,
            content = content
        )
    }
}
