package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Ejercicio de la biblioteca — contrato con TrainNow-Biblioteca (8082). */
data class ExerciseDto(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val description: String? = null,
    @SerializedName("videoUrl") val videoUrl: String? = null,
    /** URL pública o data URI comprimido (JPEG base64). */
    @SerializedName("imageUrl") val imageUrl: String? = null,
    val muscles: String? = null,
    val difficulty: String? = "PRINCIPIANTE",
    val equipment: String? = null,
    val alternatives: String? = null,
    val instructions: String? = null,
    val tips: String? = null,
    @SerializedName("commonMistakes") val commonMistakes: String? = null,
    @SerializedName("recommendedSets") val recommendedSets: Int? = null,
    @SerializedName("recommendedReps") val recommendedReps: String? = null,
    @SerializedName("restSeconds") val restSeconds: Int? = null,
    @SerializedName("isSystemDefault") val isSystemDefault: Boolean = true
)
