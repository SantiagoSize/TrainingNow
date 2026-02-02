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
    val specializations: String? = null
)