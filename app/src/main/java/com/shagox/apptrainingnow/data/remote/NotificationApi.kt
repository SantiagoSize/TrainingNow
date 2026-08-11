package com.shagox.apptrainingnow.data.remote

import com.shagox.apptrainingnow.data.remote.dto.NotificationDto
import retrofit2.Response
import retrofit2.http.*

/**
 * API de notificaciones — microservicio tn-comunicaciones (puerto 8084).
 */
interface NotificationApi {

    @GET("api/notifications/{id}")
    suspend fun getNotificationById(@Path("id") id: Int): NotificationDto

    @GET("api/notifications/user/{userId}")
    suspend fun getNotificationsByUser(@Path("userId") userId: Int): List<NotificationDto>

    @POST("api/notifications")
    suspend fun createNotification(@Body notification: NotificationDto): Response<NotificationDto>

    @PUT("api/notifications/{id}")
    suspend fun updateNotification(@Path("id") id: Int, @Body notification: NotificationDto): Response<NotificationDto>

    @PATCH("api/notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Int): Response<NotificationDto>

    @DELETE("api/notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<Unit>
}
