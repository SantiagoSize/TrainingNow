package com.shagox.apptrainingnow.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.compositionLocalOf

/** Indica si la app se está mostrando en modo claro. Valor de respaldo antes de leer la preferencia real. */
val LocalTemaClaro = compositionLocalOf { false }

/**
 * Guarda y lee la preferencia de tema (claro / oscuro) del usuario.
 * Si el usuario nunca eligió un tema manualmente (recién instalada la app),
 * se usa el tema del sistema operativo del teléfono como valor inicial.
 */
object ThemePreference {

    private const val PREFS = "app_prefs"
    private const val KEY_TEMA_CLARO = "tema_claro"

    fun esTemaClaro(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_TEMA_CLARO)) {
            return prefs.getBoolean(KEY_TEMA_CLARO, false)
        }
        // Sin preferencia guardada todavía: heredar el tema claro/oscuro del sistema.
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode != Configuration.UI_MODE_NIGHT_YES
    }

    fun guardar(context: Context, claro: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TEMA_CLARO, claro)
            .apply()
    }
}
