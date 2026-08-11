package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.remote.RemoteModule
import com.shagox.apptrainingnow.data.remote.UserApi
import com.shagox.apptrainingnow.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    }.catch { emit(emptyList()) }

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
                response.isSuccessful && response.body() != null -> {
                    RemoteModule.authToken = response.body()!!.token
                    Result.success(response.body()!!.toEntity())
                }
                response.code() == 401 ->
                    Result.failure(Exception("Credenciales incorrectas"))
                response.code() == 403 ->
                    Result.failure(Exception(extractErrorMessage(response.errorBody()?.string())
                        ?: "Tu cuenta tiene una restricción activa"))
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

    /** El registro público siempre crea USER; los roles de staff los asigna el backend vía admin. */
    override fun determineRoleByEmail(email: String): String = "USER"

    /** Crear usuario con privilegios: POST /api/users/admin-create (validado por el backend). */
    override suspend fun insertUserByAdmin(adminId: Int, user: UserEntity) {
        val response = api.createUserByAdmin(user.toDto()) // el token JWT identifica al admin
        if (!response.isSuccessful) {
            when (response.code()) {
                403 -> throw Exception("Solo un administrador puede crear este usuario")
                409 -> throw Exception("El email ya existe")
                400 -> throw Exception("Datos inválidos: revisa rol, dominio del correo y especialidad")
                else -> throw HttpException(response)
            }
        }
    }

    override suspend fun searchTrainers(query: String): List<UserEntity> =
        api.searchTrainers(query.ifBlank { "" }).map { it.toEntity() }

    /** Flujo de todos los entrenadores. */
    override fun getAllTrainers(): Flow<List<UserEntity>> = flow {
        emit(api.getTrainers().map { it.toEntity() })
    }.catch { emit(emptyList()) }

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

    /** Suspender: PATCH /api/users/{id}/suspend (token de admin). */
    override suspend fun suspendUser(userId: Int, untilMillis: Long, reason: String) {
        val response = api.suspendUser(userId, mapOf("untilMillis" to untilMillis, "reason" to reason))
        if (!response.isSuccessful) throw Exception(sanctionError(response.code()))
    }

    /** Levantar suspensión: PATCH /api/users/{id}/unsuspend (token de admin). */
    override suspend fun clearSuspension(userId: Int) {
        val response = api.unsuspendUser(userId)
        if (!response.isSuccessful) throw Exception(sanctionError(response.code()))
    }

    /** Banear: PATCH /api/users/{id}/ban (token de admin). Bloquea el login del usuario. */
    override suspend fun banUser(userId: Int, reason: String) {
        val response = api.banUser(userId, mapOf("reason" to reason))
        if (!response.isSuccessful) throw Exception(sanctionError(response.code()))
    }

    /** Levantar baneo: PATCH /api/users/{id}/unban (token de admin). */
    override suspend fun unbanUser(userId: Int) {
        val response = api.unbanUser(userId)
        if (!response.isSuccessful) throw Exception(sanctionError(response.code()))
    }

    /** Eliminar usuario: DELETE /api/users/{id}. */
    override suspend fun deleteUserById(userId: Int) {
        val response = api.deleteUser(userId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    /** Usuarios normales con toda su información (incluye createdAt). */
    override suspend fun getAllClientsInfo(): List<UserDto> = try {
        api.getClients()
    } catch (e: Exception) {
        throw translateException(e)
    }

    private fun sanctionError(code: Int): String = when (code) {
        403 -> "Solo un administrador con sesión activa puede aplicar sanciones"
        404 -> "Usuario no encontrado"
        else -> "Error del servidor ($code)"
    }

    /** Extrae "error" del cuerpo JSON {"error": "..."}. */
    private fun extractErrorMessage(body: String?): String? {
        if (body == null) return null
        return Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
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
