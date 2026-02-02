package com.shagox.apptrainingnow.data.remote.dto

/**
 * DTO de sesión de entrenamiento según workout-service (puerto 8084).
 */
data class WorkoutSessionDto(
    val id: Int = 0,
    val userId: Int = 0,
    val routineId: Int? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val status: String = "IN_PROGRESS",
    val totalDurationMinutes: Int? = null,
    val caloriesBurned: Int? = null,
    val notes: String? = null,
    val rating: Int? = null,
    val perceivedDifficulty: Int? = null,
    val mood: String? = null,
    val location: String? = null,
    val createdAt: Long? = null
)
