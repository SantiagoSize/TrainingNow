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

    /**
     * Traspasa las rutinas del invitado a la cuenta recién iniciada (login o registro).
     *
     * IMPORTANTE: las cuentas reales viven solo en el backend (API), nunca se guardan en la
     * tabla local "users" de Room. Si se reasignaba ownerId/creatorId de una rutina a ese id
     * sin que existiera esa fila local, la FOREIGN KEY de "routines" contra "users" reventaba
     * con SQLiteConstraintException sin capturar, y esto tumbaba toda la app apenas alguien
     * iniciaba sesión (con cuenta nueva o existente) después de haber creado una rutina como
     * invitado. Por eso ahora: (1) se asegura una fila local mínima con ese id antes de migrar,
     * y (2) todo queda protegido con try/catch por si pasa cualquier otra cosa inesperada.
     */
    suspend fun migrarRutinasA(context: Context, userId: Int) {
        if (userId <= 0) return
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val guestId = prefs.getInt(KEY_ID, 0)
            if (guestId <= 0 || guestId == userId) return

            val db = AppDatabase.getInstance(context)
            val userDao = db.userDao()
            val routineDao = db.routineDao()

            // Espejo local mínimo: solo para que la FOREIGN KEY de "routines" no falle.
            // No se usa para mostrar datos del usuario en ninguna pantalla (esos vienen del backend).
            if (userDao.getUserById(userId) == null) {
                userDao.insertUser(
                    UserEntity(
                        id = userId,
                        role = "USER",
                        name = "Usuario",
                        lastName = "",
                        email = "sync_$userId@local.tn",
                        phone = "",
                        password = ""
                    )
                )
            }

            routineDao.getRoutinesByOwnerOnce(guestId).forEach { rutina ->
                routineDao.updateRoutine(rutina.copy(ownerId = userId, creatorId = userId))
            }
            // También se traspasa el historial de entrenamientos ya realizados como invitado
            // (antes solo se migraban las rutinas, no las sesiones ya completadas).
            db.workoutDao().reasignarUsuario(guestId, userId)
        } catch (e: Exception) {
            android.util.Log.w("GuestSession", "No se pudieron migrar las rutinas del invitado: ${e.message}")
        }
    }

    /**
     * Olvida al invitado actual (borra su fila local y el id en caché) para que la próxima
     * vez que se use la app sin cuenta arranque un invitado nuevo, limpio. Se llama al cerrar
     * sesión: así el modo invitado no se queda arrastrando los entrenamientos personalizados
     * que se acaban de transferir a la cuenta con [migrarRutinasA] (el borrado de la fila
     * cascadea y elimina también cualquier rutina/sesión que le hubiera quedado).
     */
    suspend fun reiniciar(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val guestId = prefs.getInt(KEY_ID, 0)
            prefs.edit().remove(KEY_ID).apply()
            if (guestId > 0) {
                val userDao = AppDatabase.getInstance(context).userDao()
                userDao.getUserById(guestId)?.let { userDao.deleteUser(it) }
            }
        } catch (e: Exception) {
            android.util.Log.w("GuestSession", "No se pudo reiniciar el invitado: ${e.message}")
        }
    }
}
