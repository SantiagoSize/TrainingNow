package com.shagox.apptrainingnow.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.shagox.apptrainingnow.data.local.user.UserEntity

/**
 * DTO de usuario según la API trainingnowapi (Spring Boot).
 * Campos opcionales para compatibilidad con versiones futuras (suspensión/baneo).
 */
data class UserDto(
    val id: Int = 0,
    val role: String = "USER",
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    /** Nullable a propósito: Gson puede asignar null aquí si el backend devuelve el campo
     *  como JSON null (ej. cuentas semilla sin teléfono), sin que el valor por defecto proteja. */
    val phone: String? = null,
    val password: String = "",
    @SerializedName("profilePhotoUrl") val profilePhotoUrl: String? = null,
    @SerializedName("birthDate") val birthDate: Long? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val gender: String? = null,
    val specializations: String? = null,
    @SerializedName("suspendedUntil") val suspendedUntil: Long? = null,
    @SerializedName("suspendReason") val suspendReason: String? = null,
    @SerializedName("isBanned") val isBanned: Boolean = false,
    @SerializedName("banReason") val banReason: String? = null,
    @SerializedName("createdAt") val createdAt: Long? = null,
    @SerializedName("updatedAt") val updatedAt: Long? = null,
    /** Token JWT: solo viene en la respuesta del login. */
    val token: String? = null
) {
    /** Convierte el DTO a la entidad usada en la app (Room/UI). */
    fun toEntity(): UserEntity = UserEntity(
        id = id,
        role = role,
        name = name,
        lastName = lastName,
        email = email,
        phone = phone ?: "",
        password = password,
        profilePhotoUrl = profilePhotoUrl,
        birthDate = birthDate,
        height = height,
        weight = weight,
        gender = gender,
        specializations = specializations,
        suspendedUntil = suspendedUntil,
        suspendReason = suspendReason,
        isBanned = isBanned,
        banReason = banReason
    )
}
