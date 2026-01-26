package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.notification.NotificationDao
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) {

    // Guardar una notificación (ej: cuando el sistema te avisa algo)
    suspend fun saveNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    // Leer las notificaciones de un usuario
    fun getUserNotifications(userId: Int): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsForUser(userId)
    }
}