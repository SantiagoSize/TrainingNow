package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO relación entrenador-cliente (API trainingnowapi).
 */
data class TrainerClientDto(
    @SerializedName("trainerId") val trainerId: Int = 0,
    @SerializedName("clientId") val clientId: Int = 0,
    val status: String = "PENDING",
    @SerializedName("startDate") val startDate: Long? = null,
    @SerializedName("endDate") val endDate: Long? = null,
    @SerializedName("trainerNotes") val trainerNotes: String? = null,
    @SerializedName("clientGoals") val clientGoals: String? = null,
    @SerializedName("sessionPrice") val sessionPrice: Double? = null,
    @SerializedName("sessionsPerWeek") val sessionsPerWeek: Int = 3,
    @SerializedName("lastInteractionDate") val lastInteractionDate: Long? = null,
    @SerializedName("createdAt") val createdAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null
)
