package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import kotlinx.coroutines.flow.Flow

interface INotificationRepository {
    suspend fun saveNotification(notification: NotificationEntity)
    fun getUserNotifications(userId: Int): Flow<List<NotificationEntity>>
}
