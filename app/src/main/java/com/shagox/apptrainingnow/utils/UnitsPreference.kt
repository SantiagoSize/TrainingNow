package com.shagox.apptrainingnow.utils

import android.content.Context

/**
 * Guarda y lee la preferencia de unidades de medida (métrico kg/cm vs imperial lb/in).
 * Mismo patrón que ThemePreference: SharedPreferences local al teléfono.
 *
 * IMPORTANTE: el peso y la altura del usuario siempre se guardan en kg/cm en la base de
 * datos (formato canónico, sin cambios), tanto local como en el backend. Esta preferencia
 * SOLO afecta cómo se muestran/escriben los valores en la pantalla; la conversión ocurre
 * al mostrar (kg→lb, cm→in) y al guardar (lb→kg, in→cm).
 */
object UnitsPreference {

    private const val PREFS = "app_prefs"
    private const val KEY_IMPERIAL = "unidades_imperiales"

    /** true = libras/pulgadas (imperial), false = kilos/centímetros (métrico, por defecto). */
    fun esImperial(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_IMPERIAL, false)

    fun guardar(context: Context, imperial: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IMPERIAL, imperial)
            .apply()
    }

    // ==================== Conversión de peso ====================

    fun kgALibras(kg: Double): Double = kg * 2.2046226218
    fun librasAKg(libras: Double): Double = libras / 2.2046226218

    // ==================== Conversión de altura/longitud ====================

    fun cmAPulgadas(cm: Double): Double = cm / 2.54
    fun pulgadasACm(pulgadas: Double): Double = pulgadas * 2.54

    /** Etiqueta corta de la unidad de peso actual: "kg" o "lb". */
    fun etiquetaPeso(context: Context): String = if (esImperial(context)) "lb" else "kg"

    /** Etiqueta corta de la unidad de longitud actual: "cm" o "in". */
    fun etiquetaLongitud(context: Context): String = if (esImperial(context)) "in" else "cm"
}
