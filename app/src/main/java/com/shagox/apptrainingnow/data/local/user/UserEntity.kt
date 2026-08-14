package com.shagox.apptrainingnow.data.local.user

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "ADMIN", "TRAINER", "USER"
    val name: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
    val profilePhotoUrl: String? = null,

    // Específicos de Usuario
    val birthDate: Long? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val gender: String? = null,

    // Específicos de Entrenador
    val specializations: String? = null,
    /** Descripción/bio libre que el entrenador puede escribir para su perfil público. */
    val bio: String? = null,

    // Sanciones (admin): motivo y tiempo de suspensión / baneo
    val suspendedUntil: Long? = null,
    val suspendReason: String? = null,
    val isBanned: Boolean = false,
    val banReason: String? = null,

    /** Último "heartbeat" (epoch millis) recibido del backend para este usuario.
     *  Se usa para mostrar "Conectado"/"Desconectado" en el chat. */
    val lastActiveAt: Long? = null
)