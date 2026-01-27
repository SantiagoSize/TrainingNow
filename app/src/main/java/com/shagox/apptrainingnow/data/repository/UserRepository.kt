package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.user.UserDao
import com.shagox.apptrainingnow.data.local.user.UserEntity

class UserRepository(private val userDao: UserDao) {

    // Login: Verifica email y contraseña
    suspend fun login(email: String, passwordInput: String): Result<UserEntity> {
        return try {
            // Aplicar trim a ambos parámetros para evitar problemas con espacios
            val trimmedEmail = email.trim()
            val trimmedPassword = passwordInput.trim()
            
            // Buscar usuario por email (también con trim)
            val user = userDao.getUserByEmail(trimmedEmail)

            // Comparar contraseñas (ambas ya con trim)
            if (user != null && user.password.trim() == trimmedPassword) {
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

    // Buscar entrenadores
    suspend fun searchTrainers(query: String): List<UserEntity> {
        return userDao.searchTrainers(query)
    }

    // Obtener todos los entrenadores
    fun getAllTrainers() = userDao.getAllTrainers()

    // Obtener usuario por ID
    suspend fun getUserById(userId: Int): UserEntity? {
        return userDao.getUserById(userId)
    }
}