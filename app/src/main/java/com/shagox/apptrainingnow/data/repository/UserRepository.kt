package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.user.UserDao
import com.shagox.apptrainingnow.data.local.user.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class UserRepository(private val userDao: UserDao) : IUserRepository {

    /** Flujo de todos los usuarios (para admin). */
    override fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    /** IDs de todos los usuarios (suspend, para notificaciones). */
    override suspend fun getAllUserIds(): List<Int> = userDao.getAllUsers().first().map { it.id }

    /** IDs de usuarios por rol. */
    override suspend fun getUserIdsByRole(role: String): List<Int> = userDao.getUsersByRole(role).first().map { it.id }

    // Login: Verifica email y contraseña
    override suspend fun login(email: String, passwordInput: String): Result<UserEntity> {
        return try {
            // Aplicar trim a ambos parámetros para evitar problemas con espacios
            val trimmedEmail = email.trim()
            val trimmedPassword = passwordInput.trim()
            
            // Buscar usuario por email (también con trim)
            val user = userDao.getUserByEmail(trimmedEmail)

            if (user == null || user.password.trim() != trimmedPassword)
                return Result.failure(Exception("Credenciales incorrectas"))
            if (user.isBanned)
                return Result.failure(Exception("Cuenta baneada. Motivo: ${user.banReason ?: "No indicado"}"))
            if (user.suspendedUntil != null && user.suspendedUntil > System.currentTimeMillis())
                return Result.failure(Exception("Cuenta suspendida. Motivo: ${user.suspendReason ?: "No indicado"}"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    override suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    // Helper para roles
    override fun determineRoleByEmail(email: String): String {
        return when {
            email.endsWith("@admin.tn") -> "ADMIN"
            email.endsWith("@coach.tn") -> "TRAINER"
            else -> "USER"
        }
    }

    // Buscar entrenadores
    override suspend fun searchTrainers(query: String): List<UserEntity> {
        return userDao.searchTrainers(query)
    }

    // Obtener todos los entrenadores
    override fun getAllTrainers() = userDao.getAllTrainers()

    // Obtener usuario por ID
    override suspend fun getUserById(userId: Int): UserEntity? {
        return userDao.getUserById(userId)
    }

    /** Actualiza la foto de perfil y devuelve el usuario actualizado, o null si no existe. */
    override suspend fun updateProfilePhoto(userId: Int, photoUrl: String?): UserEntity? {
        userDao.updateProfilePhoto(userId, photoUrl)
        return userDao.getUserById(userId)
    }

    /** Suspender usuario hasta una fecha (motivo obligatorio). */
    override suspend fun suspendUser(userId: Int, untilMillis: Long, reason: String) {
        userDao.suspendUser(userId, untilMillis, reason)
    }

    /** Levantar suspensión. */
    override suspend fun clearSuspension(userId: Int) {
        userDao.clearSuspension(userId)
    }

    /** Banear usuario (motivo obligatorio). */
    override suspend fun banUser(userId: Int, reason: String) {
        userDao.banUser(userId, reason)
    }

    /** Desbanear. */
    override suspend fun unbanUser(userId: Int) {
        userDao.unbanUser(userId)
    }

    /** Eliminar usuario por ID (admin). */
    override suspend fun deleteUserById(userId: Int) {
        userDao.deleteUserById(userId)
    }
}