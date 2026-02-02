package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de ejercicio dentro de una rutina (API trainingnowapi).
 */
data class RoutineExerciseDto(
    @SerializedName("routineId") val routineId: Int = 0,
    @SerializedName("exerciseId") val exerciseId: Int = 0,
    val order: Int = 0
)
