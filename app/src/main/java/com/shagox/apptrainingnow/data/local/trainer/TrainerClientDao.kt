package com.shagox.apptrainingnow.data.local.trainer

import androidx.room.*
import com.shagox.apptrainingnow.data.local.user.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar las relaciones Entrenador-Cliente.
 * 
 * Proporciona operaciones CRUD completas y queries especializadas para:
 * - Gestión de clientes por parte del entrenador
 * - Seguimiento del estado de suscripciones
 * - Estadísticas de la cartera de clientes
 */
@Dao
interface TrainerClientDao {

    // ==================== OPERACIONES CRUD ====================

    /**
     * Inserta o actualiza una relación entrenador-cliente.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainerClient(trainerClient: TrainerClientEntity)

    /**
     * Inserta múltiples relaciones de una vez.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainerClients(trainerClients: List<TrainerClientEntity>)

    /**
     * Actualiza una relación existente.
     */
    @Update
    suspend fun updateTrainerClient(trainerClient: TrainerClientEntity)

    /**
     * Elimina una relación específica.
     */
    @Delete
    suspend fun deleteTrainerClient(trainerClient: TrainerClientEntity)

    /**
     * Elimina la relación por IDs.
     */
    @Query("DELETE FROM trainer_clients WHERE trainerId = :trainerId AND clientId = :clientId")
    suspend fun deleteByIds(trainerId: Int, clientId: Int)

    // ==================== QUERIES DEL ENTRENADOR ====================

    /**
     * Obtiene todos los clientes de un entrenador con datos completos del usuario.
     * Incluye solo relaciones activas o pendientes por defecto.
     */
    @Transaction
    @Query("""
        SELECT users.* FROM users
        INNER JOIN trainer_clients ON users.id = trainer_clients.clientId
        WHERE trainer_clients.trainerId = :trainerId
        AND trainer_clients.status IN ('ACTIVE', 'PENDING')
        ORDER BY trainer_clients.lastInteractionDate DESC
    """)
    fun getClientsForTrainer(trainerId: Int): Flow<List<UserEntity>>

    /**
     * Obtiene todos los clientes de un entrenador con todos los estados.
     */
    @Transaction
    @Query("""
        SELECT users.* FROM users
        INNER JOIN trainer_clients ON users.id = trainer_clients.clientId
        WHERE trainer_clients.trainerId = :trainerId
        ORDER BY trainer_clients.status ASC, trainer_clients.lastInteractionDate DESC
    """)
    fun getAllClientsForTrainer(trainerId: Int): Flow<List<UserEntity>>

    /**
     * Obtiene los clientes de un entrenador filtrados por estado.
     */
    @Transaction
    @Query("""
        SELECT users.* FROM users
        INNER JOIN trainer_clients ON users.id = trainer_clients.clientId
        WHERE trainer_clients.trainerId = :trainerId
        AND trainer_clients.status = :status
        ORDER BY trainer_clients.lastInteractionDate DESC
    """)
    fun getClientsByStatus(trainerId: Int, status: String): Flow<List<UserEntity>>

    /**
     * Busca clientes del entrenador por nombre, apellido o email.
     */
    @Transaction
    @Query("""
        SELECT users.* FROM users
        INNER JOIN trainer_clients ON users.id = trainer_clients.clientId
        WHERE trainer_clients.trainerId = :trainerId
        AND trainer_clients.status = 'ACTIVE'
        AND (
            users.name LIKE '%' || :query || '%'
            OR users.lastName LIKE '%' || :query || '%'
            OR users.email LIKE '%' || :query || '%'
        )
        ORDER BY users.name ASC
    """)
    suspend fun searchClients(trainerId: Int, query: String): List<UserEntity>

    /**
     * Obtiene la relación específica entre un entrenador y cliente.
     */
    @Query("SELECT * FROM trainer_clients WHERE trainerId = :trainerId AND clientId = :clientId")
    suspend fun getTrainerClient(trainerId: Int, clientId: Int): TrainerClientEntity?

    /**
     * Obtiene la relación como Flow para observar cambios.
     */
    @Query("SELECT * FROM trainer_clients WHERE trainerId = :trainerId AND clientId = :clientId")
    fun observeTrainerClient(trainerId: Int, clientId: Int): Flow<TrainerClientEntity?>

    // ==================== QUERIES DEL CLIENTE ====================

    /**
     * Obtiene todos los entrenadores de un cliente.
     */
    @Transaction
    @Query("""
        SELECT users.* FROM users
        INNER JOIN trainer_clients ON users.id = trainer_clients.trainerId
        WHERE trainer_clients.clientId = :clientId
        AND trainer_clients.status = 'ACTIVE'
        ORDER BY trainer_clients.startDate DESC
    """)
    fun getTrainersForClient(clientId: Int): Flow<List<UserEntity>>

    /**
     * Verifica si un cliente tiene un entrenador activo.
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM trainer_clients 
            WHERE clientId = :clientId 
            AND status = 'ACTIVE'
        )
    """)
    suspend fun hasActiveTrainer(clientId: Int): Boolean

    // ==================== ESTADÍSTICAS DEL ENTRENADOR ====================

    /**
     * Cuenta el total de clientes activos de un entrenador.
     */
    @Query("SELECT COUNT(*) FROM trainer_clients WHERE trainerId = :trainerId AND status = 'ACTIVE'")
    suspend fun countActiveClients(trainerId: Int): Int

    /**
     * Cuenta el total de clientes activos como Flow (para UI reactiva).
     */
    @Query("SELECT COUNT(*) FROM trainer_clients WHERE trainerId = :trainerId AND status = 'ACTIVE'")
    fun observeActiveClientCount(trainerId: Int): Flow<Int>

    /**
     * Cuenta solicitudes pendientes.
     */
    @Query("SELECT COUNT(*) FROM trainer_clients WHERE trainerId = :trainerId AND status = 'PENDING'")
    fun observePendingRequestCount(trainerId: Int): Flow<Int>

    /**
     * Obtiene estadísticas completas del entrenador.
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active,
            SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
            SUM(CASE WHEN status = 'PAUSED' THEN 1 ELSE 0 END) as paused,
            SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled
        FROM trainer_clients 
        WHERE trainerId = :trainerId
    """)
    suspend fun getTrainerStats(trainerId: Int): TrainerStats

    /**
     * Obtiene los clientes con los que no se ha interactuado recientemente.
     */
    @Transaction
    @Query("""
        SELECT users.* FROM users
        INNER JOIN trainer_clients ON users.id = trainer_clients.clientId
        WHERE trainer_clients.trainerId = :trainerId
        AND trainer_clients.status = 'ACTIVE'
        AND trainer_clients.lastInteractionDate < :thresholdTime
        ORDER BY trainer_clients.lastInteractionDate ASC
    """)
    suspend fun getInactiveClients(trainerId: Int, thresholdTime: Long): List<UserEntity>

    // ==================== OPERACIONES DE ESTADO ====================

    /**
     * Actualiza el estado de una relación.
     */
    @Query("""
        UPDATE trainer_clients 
        SET status = :newStatus, updatedAt = :updateTime 
        WHERE trainerId = :trainerId AND clientId = :clientId
    """)
    suspend fun updateStatus(trainerId: Int, clientId: Int, newStatus: String, updateTime: Long = System.currentTimeMillis())

    /**
     * Acepta una solicitud pendiente (cambia PENDING a ACTIVE).
     */
    @Query("""
        UPDATE trainer_clients 
        SET status = 'ACTIVE', startDate = :startTime, updatedAt = :startTime 
        WHERE trainerId = :trainerId AND clientId = :clientId AND status = 'PENDING'
    """)
    suspend fun acceptClientRequest(trainerId: Int, clientId: Int, startTime: Long = System.currentTimeMillis())

    /**
     * Actualiza la fecha de última interacción.
     */
    @Query("""
        UPDATE trainer_clients 
        SET lastInteractionDate = :interactionTime, updatedAt = :interactionTime 
        WHERE trainerId = :trainerId AND clientId = :clientId
    """)
    suspend fun updateLastInteraction(trainerId: Int, clientId: Int, interactionTime: Long = System.currentTimeMillis())

    /**
     * Actualiza las notas del entrenador sobre un cliente.
     */
    @Query("""
        UPDATE trainer_clients 
        SET trainerNotes = :notes, updatedAt = :updateTime 
        WHERE trainerId = :trainerId AND clientId = :clientId
    """)
    suspend fun updateTrainerNotes(trainerId: Int, clientId: Int, notes: String, updateTime: Long = System.currentTimeMillis())
}

/**
 * Clase de datos para las estadísticas del entrenador.
 */
data class TrainerStats(
    val total: Int,
    val active: Int,
    val pending: Int,
    val paused: Int,
    val cancelled: Int
)
