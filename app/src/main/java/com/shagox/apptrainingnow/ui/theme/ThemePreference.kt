package com.shagox.apptrainingnow.ui.theme

import android.content.Context
import androidx.compose.runtime.compositionLocalOf

/** Indica si la app se está mostrando en modo claro. Por defecto: oscuro. */
val LocalTemaClaro = compositionLocalOf { false }

/**
 * Guarda y lee la preferencia de tema (claro / oscuro) del usuario.
 */
object ThemePreference {

    private const val PREFS = "app_prefs"
    private const val KEY_TEMA_CLARO = "tema_claro"

    fun esTemaClaro(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_TEMA_CLARO, false)

    fun guardar(context: Context, claro: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TEMA_CLARO, claro)
            .apply()
    }
}
