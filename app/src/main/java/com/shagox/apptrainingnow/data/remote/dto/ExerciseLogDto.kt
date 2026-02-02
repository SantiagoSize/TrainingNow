package com.shagox.apptrainingnow.data.remote.dto

/**
 * DTO de log de ejercicio según workout-service (puerto 8084).
 */
data class ExerciseLogDto(
    val id: Int = 0,
    val sessionId: Int = 0,
    val exerciseId: Int = 0,
    val orderInSession: Int = 0,
    val plannedSets: Int = 3,
    val plannedReps: Int = 12,
    val plannedWeightKg: Double? = null,
    val completedSets: Int = 0,
    val actualReps: String? = null,
    val weightKg: Double? = null,
    val restTimeSeconds: Int = 60,
    val durationSeconds: Int? = null,
    val notes: String? = null,
    val rpe: Int? = null,
    val isPersonalRecord: Boolean = false,
    val formRating: Int? = null,
    val tempo: String? = null,
    val createdAt: Long? = null
)
