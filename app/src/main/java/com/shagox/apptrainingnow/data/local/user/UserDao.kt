package com.shagox.apptrainingnow.data.local.user

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar usuarios de la aplicación.
 * 
 * Proporciona operaciones CRUD completas y queries especializadas para:
 * - Autenticación y gestión de sesiones
 * - Búsqueda y filtrado de usuarios
 * - Gestión de roles (ADMIN, TRAINER, USER)
 * - Estadísticas de usuarios
 */
@Dao
interface UserDao {

    // ==================== OPERACIONES CRUD ====================

    /**
     * Inserta o actualiza un usuario.
     * @return ID del usuario insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    /**
     * Inserta múltiples usuarios de una vez.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    /**
     * Actualiza un usuario existente.
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Elimina un usuario.
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)

    /**
     * Elimina un usuario por ID.
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)

    // ==================== QUERIES DE AUTENTICACIÓN ====================

    /**
     * Busca usuario por email para login.
     */
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    /**
     * Verifica si existe un email (para registro).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE LOWER(email) = LOWER(:email))")
    suspend fun emailExists(email: String): Boolean

    /**
     * Verifica si existe un teléfono (para registro).
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE phone = :phone)")
    suspend fun phoneExists(phone: String): Boolean

    // ==================== QUERIES GENERALES ====================

    /**
     * Obtiene un usuario por ID.
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    /**
     * Obtiene un usuario por ID como Flow.
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUser(userId: Int): Flow<UserEntity?>

    /**
     * Obtiene todos los usuarios.
     */
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    /**
     * Cuenta el total de usuarios.
     */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    /**
     * Obtiene usuarios por rol.
     */
    @Query("SELECT * FROM users WHERE role = :role ORDER BY name ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    // ==================== QUERIES DE ENTRENADORES ====================

    /**
     * Obtiene todos los entrenadores.
     */
    @Query("SELECT * FROM users WHERE role = 'TRAINER' ORDER BY name ASC")
    fun getAllTrainers(): Flow<List<UserEntity>>

    /**
     * Obtiene todos los entrenadores de forma síncrona.
     */
    @Query("SELECT * FROM users WHERE role = 'TRAINER' ORDER BY name ASC")
    suspend fun getAllTrainersSync(): List<UserEntity>

    /**
     * Busca entrenadores por nombre, apellido o especialización.
     */
    @Query("""
        SELECT * FROM users 
        WHERE role = 'TRAINER' 
        AND (
            LOWER(name) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(lastName) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(specializations) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY name ASC
    """)
    suspend fun searchTrainers(query: String): List<UserEntity>

    /**
     * Busca entrenadores por especialización específica.
     */
    @Query("""
        SELECT * FROM users 
        WHERE role = 'TRAINER' 
        AND LOWER(specializations) LIKE '%' || LOWER(:specialization) || '%'
        ORDER BY name ASC
    """)
    fun getTrainersBySpecialization(specialization: String): Flow<List<UserEntity>>

    /**
     * Cuenta el total de entrenadores.
     */
    @Query("SELECT COUNT(*) FROM users WHERE role = 'TRAINER'")
    suspend fun countTrainers(): Int

    // ==================== QUERIES DE CLIENTES ====================

    /**
     * Obtiene todos los clientes (usuarios normales).
     */
    @Query("SELECT * FROM users WHERE role = 'USER' ORDER BY name ASC")
    fun getAllClients(): Flow<List<UserEntity>>

    /**
     * Busca clientes por nombre, apellido o email.
     */
    @Query("""
        SELECT * FROM users 
        WHERE role = 'USER' 
        AND (
            LOWER(name) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(lastName) LIKE '%' || LOWER(:query) || '%'
            OR LOWER(email) LIKE '%' || LOWER(:query) || '%'
        )
        ORDER BY name ASC
    """)
    suspend fun searchClients(query: String): List<UserEntity>

    /**
     * Cuenta el total de clientes.
     */
    @Query("SELECT COUNT(*) FROM users WHERE role = 'USER'")
    suspend fun countClients(): Int

    // ==================== OPERACIONES DE ACTUALIZACIÓN PARCIAL ====================

    /**
     * Actualiza la foto de perfil.
     */
    @Query("UPDATE users SET profilePhotoUrl = :photoUrl WHERE id = :userId")
    suspend fun updateProfilePhoto(userId: Int, photoUrl: String?)

    /**
     * Actualiza el peso del usuario.
     */
    @Query("UPDATE users SET weight = :weight WHERE id = :userId")
    suspend fun updateWeight(userId: Int, weight: Float)

    /**
     * Actualiza la altura del usuario.
     */
    @Query("UPDATE users SET height = :height WHERE id = :userId")
    suspend fun updateHeight(userId: Int, height: Float)

    /**
     * Actualiza la contraseña.
     */
    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: Int, newPassword: String)

    /**
     * Actualiza las especializaciones del entrenador.
     */
    @Query("UPDATE users SET specializations = :specializations WHERE id = :userId AND role = 'TRAINER'")
    suspend fun updateSpecializations(userId: Int, specializations: String)

    /**
     * Actualiza datos físicos del usuario.
     */
    @Query("""
        UPDATE users 
        SET weight = :weight, height = :height, gender = :gender 
        WHERE id = :userId
    """)
    suspend fun updatePhysicalData(userId: Int, weight: Float?, height: Float?, gender: String?)

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtiene estadísticas generales de usuarios.
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN role = 'USER' THEN 1 ELSE 0 END) as clients,
            SUM(CASE WHEN role = 'TRAINER' THEN 1 ELSE 0 END) as trainers,
            SUM(CASE WHEN role = 'ADMIN' THEN 1 ELSE 0 END) as admins
        FROM users
    """)
    suspend fun getUserStats(): UserStats
}

/**
 * Clase de datos para estadísticas de usuarios.
 */
data class UserStats(
    val total: Int,
    val clients: Int,
    val trainers: Int,
    val admins: Int
)