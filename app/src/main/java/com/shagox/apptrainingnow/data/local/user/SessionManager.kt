package com.shagox.apptrainingnow.data.local.user

import android.content.Context
import com.google.gson.Gson

/**
 * Guarda la sesión (token JWT + datos del usuario logueado) en SharedPreferences para que
 * sobreviva a cerrar la app o apagar el teléfono.
 *
 * Antes de esto, el usuario logueado y el token vivían SOLO en memoria (en AuthViewModel y
 * en RemoteModule.authToken respectivamente): apenas Android mataba el proceso de la app
 * (al cerrarla, o por falta de memoria), la sesión se perdía por completo aunque el usuario
 * nunca hubiera tocado "Cerrar sesión". Con esto, al volver a abrir la app se restaura la
 * sesión guardada en vez de mostrar el login de nuevo.
 */
object SessionManager {
    private const val PREFS = "session"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_USER_JSON = "user_json"
    private val gson = Gson()

    /** Guarda el token JWT y una copia completa del usuario logueado. */
    fun guardar(context: Context, token: String?, user: UserEntity) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_JSON, gson.toJson(user))
            .apply()
    }

    /** Actualiza solo los datos del usuario guardado (ej. tras editar perfil), sin tocar el token. */
    fun actualizarUsuario(context: Context, user: UserEntity) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER_JSON, gson.toJson(user))
            .apply()
    }

    /** Token JWT guardado, o null si no hay sesión. Se restaura apenas arranca la app. */
    fun cargarToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)

    /** Copia del usuario guardado, o null si no hay sesión (o si el JSON quedó corrupto). */
    fun cargarUsuario(context: Context): UserEntity? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER_JSON, null) ?: return null
        return try {
            gson.fromJson(json, UserEntity::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** Borra la sesión guardada (logout o cuenta eliminada). */
    fun limpiar(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
