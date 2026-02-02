package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.UserApi
import com.shagox.apptrainingnow.data.remote.dto.NotificationDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class NotificationApiRepository(
    private val api: UserApi = RemoteModule.userApi()
) : INotificationRepository {

    override suspend fun saveNotification(notification: NotificationEntity) {
        val response = api.createNotification(notification.toDto())
        if (!response.isSuccessful) throw HttpException(response)
    }

    override fun getUserNotifications(userId: Int): Flow<List<NotificationEntity>> = flow {
        emit(api.getNotificationsByUser(userId).map { it.toEntity() })
    }

    private fun NotificationDto.toEntity(): NotificationEntity = NotificationEntity(
        id = id,
        userId = userId,
        title = title,
        message = message,
        type = type,
        date = date ?: System.currentTimeMillis(),
        isRead = isRead,
        actionType = actionType,
        actionData = actionData,
        priority = priority,
        expiresAt = expiresAt,
        senderId = senderId,
        iconUrl = iconUrl,
        createdAt = createdAt ?: System.currentTimeMillis()
    )

    private fun NotificationEntity.toDto(): NotificationDto = NotificationDto(
        id = id,
        userId = userId,
        title = title,
        message = message,
        type = type,
        date = date,
        isRead = isRead,
        actionType = actionType,
        actionData = actionData,
        priority = priority,
        expiresAt = expiresAt,
        senderId = senderId,
        iconUrl = iconUrl,
        createdAt = createdAt
    )
}
