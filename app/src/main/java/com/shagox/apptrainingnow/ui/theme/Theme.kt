package com.shagox.apptrainingnow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VerdeTN,       // Tu verde neón para botones y cosas importantes
    background = NegroFondo, // Fondo total de la app
    surface = GrisFondo,     // Color para tarjetas (Cards) o menús
    onPrimary = NegroFondo,  // Color del texto encima del verde (negro resalta más)
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = VerdeTN,           // Mantenemos el verde neón para botones
    onPrimary = Color.Black,     // Texto negro sobre botones verdes
    background = Color(0xFFF5F5F5), // Un gris muy clarito (casi blanco)
    surface = Color.White,       // Tarjetas blancas
    onBackground = NegroFondo,   // Texto negro sobre fondo claro
    onSurface = NegroFondo
)

@Composable
fun AppTrainingNowTheme(
    darkTheme: Boolean = true, // Forzamos dark theme para usar nuestros colores
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}