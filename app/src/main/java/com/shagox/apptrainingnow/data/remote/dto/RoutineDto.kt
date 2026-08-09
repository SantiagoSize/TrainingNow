package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de rutina según la API trainingnowapi.
 */
data class RoutineDto(
    val id: Int = 0,
    @SerializedName("ownerId") val ownerId: Int? = null,
    @SerializedName("creatorId") val creatorId: Int = 0,
    val name: String = "",
    @SerializedName("dayInfo") val dayInfo: String? = null,
    @SerializedName("creationDate") val creationDate: Long? = null,
    @SerializedName("scheduledTime") val scheduledTime: Long? = null
)
