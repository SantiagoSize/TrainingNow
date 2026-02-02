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

/**
 * Recibe la alarma del recordatorio diario: muestra notificación en el teléfono
 * e inserta la notificación en la BD para que aparezca en el apartado de Notificaciones.
 * Vuelve a programar la alarma para el día siguiente.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TrainingNowApplication ?: return
        val userId = ReminderHelper.getUserId(context)
        if (userId <= 0) return

        // Insertar en BD para que aparezca en la pantalla de Notificaciones
        runBlocking {
            app.database.notificationDao().insertNotification(
                NotificationEntity(
                    userId = userId,
                    title = "Recordatorio de entrenamiento",
                    message = "Es hora de tu rutina. ¡A entrenar!",
                    type = NotificationType.REMINDER.name
                )
            )
        }

        // Canal y notificación del sistema
        val channelId = "routine_reminder"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorio de rutina",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Recordatorios diarios de entrenamiento" }
            notificationManager.createNotificationChannel(channel)
        }

        val openApp = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pendingOpen = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Recordatorio de entrenamiento")
            .setContentText("Es hora de tu rutina. ¡A entrenar!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)

        // Reprogramar para mañana
        if (ReminderHelper.isEnabled(context)) {
            ReminderHelper.schedule(context, userId)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
