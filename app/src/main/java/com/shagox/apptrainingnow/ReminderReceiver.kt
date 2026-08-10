package com.shagox.apptrainingnow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationType
import com.shagox.apptrainingnow.utils.ReminderHelper
import kotlinx.coroutines.runBlocking
import java.util.Calendar

/** Datos de la sesión que toca entrenar, para armar la notificación. */
private data class SesionHoy(
    val routineId: Int,
    val routineName: String,
    val actividad: String,
    val ejercicios: Int
)

/**
 * Recibe las alarmas de recordatorio y muestra la notificación del entrenamiento.
 *
 * Puede venir de dos orígenes:
 * - Alarma de un día concreto (con hora propia): trae el id del día.
 * - Alarma general diaria: se busca la sesión que corresponde a hoy.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TrainingNowApplication ?: return
        val userId = ReminderHelper.getUserId(context)
        if (userId <= 0) return

        val dayIdDeAlarma = intent.getIntExtra(ReminderHelper.EXTRA_DAY_ID, 0)

        val sesion: SesionHoy? = runBlocking {
            try {
                val routineDao = app.database.routineDao()

                if (dayIdDeAlarma > 0) {
                    // Alarma de un día con hora propia
                    val dia = routineDao.getDayById(dayIdDeAlarma)
                    val rutina = dia?.let { routineDao.getRoutineById(it.routineId) }
                    val cantidad = dia?.let { routineDao.countExercisesInDay(it.id) } ?: 0
                    if (dia != null && rutina != null && cantidad > 0) {
                        SesionHoy(
                            routineId = rutina.id,
                            routineName = rutina.name,
                            actividad = dia.activityName.ifBlank { "Entrenamiento" },
                            ejercicios = cantidad
                        )
                    } else null
                } else {
                    // Alarma general: se busca el día de hoy
                    val indiceHoy = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
                    val rutinas = routineDao.getRoutinesByOwnerOnce(userId)
                        .ifEmpty { routineDao.getGlobalRoutinesOnce() }

                    rutinas.firstNotNullOfOrNull { rutina ->
                        routineDao.getDaysOfRoutine(rutina.id)
                            .firstOrNull { it.dayOrder == indiceHoy }
                            ?.let { dia ->
                                val cantidad = routineDao.countExercisesInDay(dia.id)
                                if (cantidad > 0) {
                                    SesionHoy(
                                        routineId = rutina.id,
                                        routineName = rutina.name,
                                        actividad = dia.activityName.ifBlank { "Entrenamiento" },
                                        ejercicios = cantidad
                                    )
                                } else null
                            }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }

        val titulo = if (sesion != null) "Hoy toca: ${sesion.actividad}" else "Recordatorio de entrenamiento"
        val mensaje = if (sesion != null) {
            "${sesion.routineName} · ${sesion.ejercicios} ejercicios. Toca para empezar."
        } else {
            "Es hora de tu rutina. ¡A entrenar!"
        }

        // Guardar también en la bandeja de notificaciones de la app
        runBlocking {
            try {
                app.database.notificationDao().insertNotification(
                    NotificationEntity(
                        userId = userId,
                        title = titulo,
                        message = mensaje,
                        type = NotificationType.REMINDER.name
                    )
                )
            } catch (_: Exception) {
                // Sin notificación en la bandeja si falla; la del sistema igual se muestra
            }
        }

        // Canal de notificación
        val channelId = "routine_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorio de rutina",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Recordatorios de entrenamiento" }
            notificationManager.createNotificationChannel(channel)
        }

        // Al tocar la notificación se abre directamente la rutina
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (sesion != null) {
                putExtra(EXTRA_ROUTINE_ID, sesion.routineId)
                putExtra(EXTRA_ROUTINE_NAME, sesion.routineName)
            }
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            dayIdDeAlarma,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + dayIdDeAlarma, notification)

        // Las alarmas por día se repiten solas cada semana; solo se reprograma la general
        if (dayIdDeAlarma == 0 && ReminderHelper.isEnabled(context)) {
            ReminderHelper.schedule(context, userId)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_ROUTINE_NAME = "extra_routine_name"
    }
}
