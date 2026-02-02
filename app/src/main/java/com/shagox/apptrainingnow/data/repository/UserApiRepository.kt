package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.UserApi
import com.shagox.apptrainingnow.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

/**
 * Repositorio de usuarios que usa la API trainingnowapi (Spring Boot).
 * Sigue el mismo patrón que UINavegacion: Repository que llama a la API y devuelve Result/Flow.
 *
 * Sustituye a UserRepository (Room) cuando la app use solo la API como fuente de datos.
 */
class UserApiRepository(
    private val api: UserApi = RemoteModule.userApi()
) : IUserRepository {

    /** Flujo de todos los usuarios (para admin). Obtiene de la API y emite una vez. */
    override fun getAllUsers(): Flow<List<UserEntity>> = flow {
        val list = api.getUsers().map { it.toEntity() }
        emit(list)
    }

    /** IDs de todos los usuarios. */
    override suspend fun getAllUserIds(): List<Int> = api.getUsers().map { it.id }

    /** IDs de usuarios por rol. */
    override suspend fun getUserIdsByRole(role: String): List<Int> =
        api.getUsers().filter { it.role == role }.map { it.id }

    /** Login contra la API: POST /api/users/login. */
    override suspend fun login(email: String, passwordInput: String): Result<UserEntity> {
        return try {
            val trimmedEmail = email.trim()
            val trimmedPassword = passwordInput.trim()
            val response = api.login(
                mapOf(
                    "email" to trimmedEmail,
                    "password" to trimmedPassword
                )
            )
            when {
                response.isSuccessful && response.body() != null ->
                    Result.success(response.body()!!.toEntity())
                response.code() == 401 ->
                    Result.failure(Exception("Credenciales incorrectas"))
                else ->
                    Result.failure(Exception(response.message() ?: "Error de login"))
            }
        } catch (e: Exception) {
            Result.failure(translateException(e))
        }
    }

    /** Crear usuario: POST /api/users. */
    override suspend fun insertUser(user: UserEntity) {
        val dto = user.toDto()
        val response = api.createUser(dto)
        if (!response.isSuccessful) {
            when (response.code()) {
                409 -> throw Exception("El email ya existe")
                else -> throw HttpException(response)
            }
        }
    }

    /** Actualizar usuario: PUT /api/users/{id}. */
    override suspend fun updateUser(user: UserEntity) {
        val response = api.updateUser(user.id, user.toDto())
        if (!response.isSuccessful) throw HttpException(response)
    }

    override fun determineRoleByEmail(email: String): String = when {
        email.endsWith("@admin.tn") -> "ADMIN"
        email.endsWith("@coach.tn") -> "TRAINER"
        else -> "USER"
    }

    override suspend fun searchTrainers(query: String): List<UserEntity> =
        api.searchTrainers(query.ifBlank { "" }).map { it.toEntity() }

    /** Flujo de todos los entrenadores. */
    override fun getAllTrainers(): Flow<List<UserEntity>> = flow {
        emit(api.getTrainers().map { it.toEntity() })
    }

    override suspend fun getUserById(userId: Int): UserEntity? = try {
        api.getUserById(userId).toEntity()
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    } catch (e: Exception) {
        throw translateException(e)
    }

    /** Actualizar foto de perfil: PUT /api/users/{id}. La API puede no tener el campo; se envía el usuario completo. */
    override suspend fun updateProfilePhoto(userId: Int, photoUrl: String?): UserEntity? {
        val user = getUserById(userId) ?: return null
        val updated = user.copy(profilePhotoUrl = photoUrl)
        updateUser(updated)
        return getUserById(userId)
    }

    /** Suspender usuario. La API actual puede no tener este endpoint; se lanza si no está soportado. */
    override suspend fun suspendUser(userId: Int, untilMillis: Long, reason: String) {
        val user = getUserById(userId) ?: throw Exception("Usuario no encontrado")
        // Si la API añade campos suspendedUntil/suspendReason, actualizar usuario:
        val updated = user.copy(
            suspendedUntil = untilMillis,
            suspendReason = reason
        )
        updateUser(updated)
    }

    override suspend fun clearSuspension(userId: Int) {
        val user = getUserById(userId) ?: return
        val updated = user.copy(suspendedUntil = null, suspendReason = null)
        updateUser(updated)
    }

    override suspend fun banUser(userId: Int, reason: String) {
        val user = getUserById(userId) ?: throw Exception("Usuario no encontrado")
        val updated = user.copy(isBanned = true, banReason = reason)
        updateUser(updated)
    }

    override suspend fun unbanUser(userId: Int) {
        val user = getUserById(userId) ?: return
        val updated = user.copy(isBanned = false, banReason = null)
        updateUser(updated)
    }

    /** Eliminar usuario: DELETE /api/users/{id}. */
    override suspend fun deleteUserById(userId: Int) {
        val response = api.deleteUser(userId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    private fun translateException(e: Exception): Exception = when (e) {
        is HttpException -> Exception(e.message() ?: "Error de red (${e.code()})")
        else -> Exception(e.message ?: "Error de conexión. ¿Está la API en marcha?")
    }
}

/** Convierte UserEntity a UserDto para enviar a la API. */
private fun UserEntity.toDto(): UserDto = UserDto(
    id = id,
    role = role,
    name = name,
    lastName = lastName,
    email = email,
    phone = phone,
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
