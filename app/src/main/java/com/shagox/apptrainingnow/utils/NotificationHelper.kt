package com.shagox.apptrainingnow.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shagox.apptrainingnow.MainActivity

/**
 * Muestra notificaciones push del sistema (barra de estado) cuando se envían
 * notificaciones desde la app (ej: admin envía a todos, o rutina asignada).
 */
object NotificationHelper {

    private const val CHANNEL_ID = "trainingnow_notifications"
    private const val CHANNEL_NAME = "TrainingNow"
    private const val BASE_NOTIFICATION_ID = 3000

    /**
     * Crea el canal de notificaciones si no existe (Android O+).
     */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de la app TrainingNow"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación push en el teléfono (barra de estado).
     * Al pulsar abre la app (MainActivity).
     */
    fun showPush(context: Context, title: String, message: String, notificationId: Int = BASE_NOTIFICATION_ID) {
        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            notificationId,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        manager.notify(notificationId, notification)
    }

    /**
     * Genera un ID único para no sobrescribir notificaciones (usa título+mensaje).
     */
    fun uniqueId(title: String, message: String): Int {
        return BASE_NOTIFICATION_ID + (title.hashCode().and(0x7FFF)) + (message.hashCode().and(0x7FFF)).shr(1)
    }
}
