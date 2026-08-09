package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO de ejercicio según la API trainingnowapi.
 */
data class ExerciseDto(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val description: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    @SerializedName("isSystemDefault") val isSystemDefault: Boolean = true
)
