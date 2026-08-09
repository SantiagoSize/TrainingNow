package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de notificación según la API trainingnowapi.
 */
data class NotificationDto(
    val id: Int = 0,
    @SerializedName("userId") val userId: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: String = "SYSTEM",
    val date: Long? = null,
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("actionType") val actionType: String? = null,
    @SerializedName("actionData") val actionData: String? = null,
    val priority: String = "NORMAL",
    @SerializedName("expiresAt") val expiresAt: Long? = null,
    @SerializedName("senderId") val senderId: Int? = null,
    @SerializedName("iconUrl") val iconUrl: String? = null,
    @SerializedName("createdAt") val createdAt: Long? = null
)
