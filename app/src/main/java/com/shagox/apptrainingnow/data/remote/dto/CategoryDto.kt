package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Categoría de la biblioteca — contrato con TrainNow-Biblioteca (8082). Puede tener 0 ejercicios. */
data class CategoryDto(
    val id: Long? = null,
    val name: String = "",
    @SerializedName("exerciseCount") val exerciseCount: Int = 0
)
