package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.notification.NotificationDao
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val notificationDao: NotificationDao) : INotificationRepository {

    override suspend fun saveNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    override fun getUserNotifications(userId: Int): Flow<List<NotificationEntity>> {
        return notificationDao.getNotificationsForUser(userId)
    }
}