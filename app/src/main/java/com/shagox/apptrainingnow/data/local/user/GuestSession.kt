package com.shagox.apptrainingnow.data.local.user

import android.content.Context
import com.shagox.apptrainingnow.data.local.database.AppDatabase

/**
 * Usuario local "Invitado" para quien usa la app sin cuenta.
 *
 * Las rutinas requieren un usuario existente (clave foránea), por lo que se
 * crea una vez en Room y su id se guarda en caché. Al registrarse, sus
 * rutinas pueden migrarse con [migrarRutinasA].
 */
object GuestSession {

    private const val PREFS = "guest_session"
    private const val KEY_ID = "guest_user_id"
    const val EMAIL_INVITADO = "invitado@local.tn"

    /** Devuelve el id del usuario invitado, creándolo la primera vez. */
    suspend fun obtenerGuestId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val guardado = prefs.getInt(KEY_ID, 0)
        val dao = AppDatabase.getInstance(context).userDao()

        if (guardado > 0 && dao.getUserById(guardado) != null) return guardado

        val existente = dao.getUserByEmail(EMAIL_INVITADO)
        val id = existente?.id ?: dao.insertUser(
            UserEntity(
                role = "USER",
                name = "Invitado",
                lastName = "",
                email = EMAIL_INVITADO,
                phone = "",
                password = ""
            )
        ).toInt()

        prefs.edit().putInt(KEY_ID, id).apply()
        return id
    }

    /** Traspasa las rutinas del invitado a la cuenta recién iniciada. */
    suspend fun migrarRutinasA(context: Context, userId: Int) {
        if (userId <= 0) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val guestId = prefs.getInt(KEY_ID, 0)
        if (guestId <= 0 || guestId == userId) return

        val dao = AppDatabase.getInstance(context).routineDao()
        dao.getRoutinesByOwnerOnce(guestId).forEach { rutina ->
            dao.updateRoutine(rutina.copy(ownerId = userId, creatorId = userId))
        }
    }
}
