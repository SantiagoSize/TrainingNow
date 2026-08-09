package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.user.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz común para el repositorio de usuarios.
 * Permite usar la API (UserApiRepository) o Room (UserRepository) sin cambiar la UI.
 */
interface IUserRepository {

    fun getAllUsers(): Flow<List<UserEntity>>
    suspend fun getAllUserIds(): List<Int>
    suspend fun getUserIdsByRole(role: String): List<Int>
    suspend fun login(email: String, passwordInput: String): Result<UserEntity>
    suspend fun insertUser(user: UserEntity)

    /** Creación por administrador (roles ADMIN/TRAINER). Por defecto delega en insertUser (modo local). */
    suspend fun insertUserByAdmin(adminId: Int, user: UserEntity) = insertUser(user)

    /** Info completa (DTO) de los usuarios normales, para la vista del entrenador. */
    suspend fun getAllClientsInfo(): List<com.shagox.apptrainingnow.data.remote.dto.UserDto> = emptyList()
    suspend fun updateUser(user: UserEntity)
    fun determineRoleByEmail(email: String): String
    suspend fun searchTrainers(query: String): List<UserEntity>
    fun getAllTrainers(): Flow<List<UserEntity>>
    suspend fun getUserById(userId: Int): UserEntity?
    suspend fun updateProfilePhoto(userId: Int, photoUrl: String?): UserEntity?
    suspend fun suspendUser(userId: Int, untilMillis: Long, reason: String)
    suspend fun clearSuspension(userId: Int)
    suspend fun banUser(userId: Int, reason: String)
    suspend fun unbanUser(userId: Int)
    suspend fun deleteUserById(userId: Int)
}
