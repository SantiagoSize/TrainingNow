package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.user.UserDao
import com.shagox.apptrainingnow.data.local.user.UserEntity

class UserRepository(private val userDao: UserDao) {

    // Login: Verifica email y contraseña
    suspend fun login(email: String, passwordInput: String): Result<UserEntity> {
        return try {
            // Ahora sí encontrará esta función porque la agregamos en el Paso 2
            val user = userDao.getUserByEmail(email)

            // Ahora sí encontrará .password porque lo agregamos en el Paso 1
            if (user != null && user.password == passwordInput) {
                Result.success(user)
            } else {
                Result.failure(Exception("Credenciales incorrectas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    // Helper para roles
    fun determineRoleByEmail(email: String): String {
        return when {
            email.endsWith("@admin.tn") -> "ADMIN"
            email.endsWith("@coach.tn") -> "TRAINER"
            else -> "USER"
        }
    }
}