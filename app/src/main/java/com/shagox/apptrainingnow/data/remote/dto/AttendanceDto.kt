package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Asistencia de un día — contrato con TrainNow-Rutinas (8083). */
data class AttendanceDayDto(
    val id: Int = 0,
    @SerializedName("userId") val userId: Int = 0,
    /** Formato yyyy-MM-dd */
    val date: String = "",
    /** TRAINED | REST | MISSED */
    val status: String = "TRAINED",
    @SerializedName("routineId") val routineId: Int? = null,
    @SerializedName("exercisesCompleted") val exercisesCompleted: Int = 0,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
    @SerializedName("createdAt") val createdAt: Long? = null
)

/** Reporte mensual de entrenamiento. */
data class MonthlyReportDto(
    val month: String = "",
    @SerializedName("daysTrained") val daysTrained: Int = 0,
    @SerializedName("daysMissed") val daysMissed: Int = 0,
    @SerializedName("daysRest") val daysRest: Int = 0,
    @SerializedName("totalExercises") val totalExercises: Int = 0,
    @SerializedName("totalMinutes") val totalMinutes: Int = 0,
    @SerializedName("adherencePercent") val adherencePercent: Int = 0,
    @SerializedName("longestStreak") val longestStreak: Int = 0,
    @SerializedName("currentStreak") val currentStreak: Int = 0,
    val days: List<AttendanceDayDto> = emptyList()
)
