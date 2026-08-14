package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Mensaje de chat — contrato con TrainNow-Comunicaciones (8084). */
data class MessageDto(
    val id: Int = 0,
    @SerializedName("senderId") val senderId: Int = 0,
    @SerializedName("receiverId") val receiverId: Int = 0,
    val content: String = "",
    val timestamp: Long? = null,
    @SerializedName("isRead") val isRead: Boolean = false,
    /** URL relativa del adjunto (ej. "/uploads/chat/xxx.jpg"). Null si es solo texto. */
    val attachmentUrl: String? = null,
    /** "IMAGE" o "VIDEO". Null si es solo texto. */
    val attachmentType: String? = null
)
