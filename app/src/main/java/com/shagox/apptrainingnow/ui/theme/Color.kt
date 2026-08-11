package com.shagox.apptrainingnow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ==================== PALETA FIJA ====================

/** Verde neón de la marca: igual en ambos temas. */
val VerdeTN = Color(0xFF22FF5F)

/** Verde más oscuro, legible sobre fondos claros. */
val VerdeTNOscuro = Color(0xFF00A63C)

// --- Modo oscuro ---
private val NegroOscuro = Color(0xFF000000)
private val GrisSuperficieOscuro = Color(0xFF121212)
private val GrisTextoOscuro = Color(0xFF9E9E9E)
private val GrisBordeOscuro = Color(0xFF2C2C2C)

// --- Modo claro ---
private val BlancoFondo = Color(0xFFF7F7F7)
private val GrisSuperficieClaro = Color(0xFFFFFFFF)
private val GrisTextoClaro = Color(0xFF6B6B6B)
private val GrisBordeClaro = Color(0xFFE0E0E0)

// ==================== COLORES ADAPTATIVOS ====================
// Cambian automáticamente según el tema elegido por el usuario.

/** Fondo principal de las pantallas. */
val NegroFondo: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) BlancoFondo else NegroOscuro

/** Superficie de tarjetas y contenedores. */
val GrisFondo: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) GrisSuperficieClaro else GrisSuperficieOscuro

/** Texto secundario. */
val GrisTexto: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) GrisTextoClaro else GrisTextoOscuro

/** Bordes y separadores. */
val GrisBorde: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) GrisBordeClaro else GrisBordeOscuro

/** Verde de acento legible en el tema activo. */
val VerdeAcento: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) VerdeTNOscuro else VerdeTN

/** Color de texto principal (blanco en oscuro, casi negro en claro). */
val TextoPrincipal: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) Color(0xFF121212) else Color.White

/** Texto legible encima del verde de acento (negro en oscuro, blanco en claro). */
val TextoSobreVerde: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) Color.White else Color(0xFF000000)

/** Superficie algo más elevada que [GrisFondo] (paneles internos, diálogos). */
val SuperficieElevada: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalTemaClaro.current) Color(0xFFEFEFEF) else Color(0xFF1F1F1F)

// ==================== COMPATIBILIDAD ====================
// Valores fijos para usos fuera de composables (previews, constantes).
val NegroFondoFijo = NegroOscuro
val GrisFondoFijo = GrisSuperficieOscuro

// Colores predeterminados de Material (se mantienen por compatibilidad)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
