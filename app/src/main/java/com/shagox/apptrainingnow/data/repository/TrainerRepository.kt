package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.routine.RoutineDao
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineExerciseEntity
import com.shagox.apptrainingnow.data.local.trainer.TrainerClientDao
import com.shagox.apptrainingnow.data.local.trainer.TrainerClientEntity
import com.shagox.apptrainingnow.data.local.trainer.TrainerClientStatus
import com.shagox.apptrainingnow.data.local.trainer.TrainerStats
import com.shagox.apptrainingnow.data.local.user.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones relacionadas con el entrenador.
 * 
 * Gestiona:
 * - Cartera de clientes
 * - Creación y asignación de rutinas
 * - Seguimiento de interacciones
 * - Estadísticas del entrenador
 */
class TrainerRepository(
    private val trainerClientDao: TrainerClientDao,
    private val routineDao: RoutineDao
) {

    // ==================== GESTIÓN DE CLIENTES ====================

    /**
     * Obtiene todos los clientes activos del entrenador.
     */
    fun getActiveClients(trainerId: Int): Flow<List<UserEntity>> =
        trainerClientDao.getClientsForTrainer(trainerId)

    /**
     * Obtiene todos los clientes del entrenador (todos los estados).
     */
    fun getAllClients(trainerId: Int): Flow<List<UserEntity>> =
        trainerClientDao.getAllClientsForTrainer(trainerId)

    /**
     * Obtiene clientes filtrados por estado.
     */
    fun getClientsByStatus(trainerId: Int, status: TrainerClientStatus): Flow<List<UserEntity>> =
        trainerClientDao.getClientsByStatus(trainerId, status.name)

    /**
     * Busca clientes por nombre, apellido o email.
     */
    suspend fun searchClients(trainerId: Int, query: String): List<UserEntity> =
        trainerClientDao.searchClients(trainerId, query)

    /**
     * Obtiene la relación con un cliente específico.
     */
    suspend fun getClientRelation(trainerId: Int, clientId: Int): TrainerClientEntity? =
        trainerClientDao.getTrainerClient(trainerId, clientId)

    /**
     * Observa cambios en la relación con un cliente.
     */
    fun observeClientRelation(trainerId: Int, clientId: Int): Flow<TrainerClientEntity?> =
        trainerClientDao.observeTrainerClient(trainerId, clientId)

    /**
     * Obtiene clientes con los que no se ha interactuado en X días.
     */
    suspend fun getInactiveClients(trainerId: Int, daysThreshold: Int = 7): List<UserEntity> {
        val thresholdTime = System.currentTimeMillis() - (daysThreshold * 24 * 60 * 60 * 1000L)
        return trainerClientDao.getInactiveClients(trainerId, thresholdTime)
    }

    // ==================== SOLICITUDES DE CLIENTES ====================

    /**
     * Acepta una solicitud de cliente.
     */
    suspend fun acceptClientRequest(trainerId: Int, clientId: Int): Result<Unit> {
        return try {
            trainerClientDao.acceptClientRequest(trainerId, clientId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rechaza una solicitud de cliente.
     */
    suspend fun rejectClientRequest(trainerId: Int, clientId: Int): Result<Unit> {
        return try {
            trainerClientDao.updateStatus(trainerId, clientId, TrainerClientStatus.CANCELLED.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pausa la relación con un cliente.
     */
    suspend fun pauseClient(trainerId: Int, clientId: Int): Result<Unit> {
        return try {
            trainerClientDao.updateStatus(trainerId, clientId, TrainerClientStatus.PAUSED.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reactiva la relación con un cliente.
     */
    suspend fun reactivateClient(trainerId: Int, clientId: Int): Result<Unit> {
        return try {
            trainerClientDao.updateStatus(trainerId, clientId, TrainerClientStatus.ACTIVE.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cuenta solicitudes pendientes.
     */
    fun observePendingRequestCount(trainerId: Int): Flow<Int> =
        trainerClientDao.observePendingRequestCount(trainerId)

    // ==================== NOTAS Y SEGUIMIENTO ====================

    /**
     * Actualiza las notas sobre un cliente.
     */
    suspend fun updateClientNotes(trainerId: Int, clientId: Int, notes: String): Result<Unit> {
        return try {
            trainerClientDao.updateTrainerNotes(trainerId, clientId, notes)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registra una interacción con el cliente (para seguimiento).
     */
    suspend fun recordInteraction(trainerId: Int, clientId: Int): Result<Unit> {
        return try {
            trainerClientDao.updateLastInteraction(trainerId, clientId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtiene estadísticas generales del entrenador.
     */
    suspend fun getTrainerStats(trainerId: Int): TrainerStats =
        trainerClientDao.getTrainerStats(trainerId)

    /**
     * Cuenta clientes activos.
     */
    suspend fun countActiveClients(trainerId: Int): Int =
        trainerClientDao.countActiveClients(trainerId)

    /**
     * Observa el conteo de clientes activos (reactivo).
     */
    fun observeActiveClientCount(trainerId: Int): Flow<Int> =
        trainerClientDao.observeActiveClientCount(trainerId)

    // ==================== GESTIÓN DE RUTINAS ====================

    /**
     * Obtiene todas las rutinas creadas por el entrenador.
     */
    fun getMyCreatedRoutines(trainerId: Int): Flow<List<RoutineEntity>> =
        routineDao.getRoutinesByCreator(trainerId)

    /**
     * Obtiene las rutinas asignadas a un cliente específico.
     */
    fun getClientRoutines(trainerId: Int, clientId: Int): Flow<List<RoutineEntity>> =
        routineDao.getRoutinesForClient(trainerId, clientId)

    /**
     * Crea una nueva rutina para un cliente.
     */
    suspend fun createRoutineForClient(
        trainerId: Int,
        clientId: Int,
        name: String,
        dayInfo: String,
        exerciseIds: List<Int>,
        scheduledTime: Long? = null
    ): Result<Long> {
        return try {
            val routine = RoutineEntity(
                ownerId = clientId,
                creatorId = trainerId,
                name = name,
                dayInfo = dayInfo,
                scheduledTime = scheduledTime ?: System.currentTimeMillis()
            )
            val routineId = routineDao.insertRoutine(routine)
            
            // Agregar ejercicios a la rutina
            exerciseIds.forEachIndexed { index, exerciseId ->
                routineDao.insertRoutineExercise(
                    RoutineExerciseEntity(
                        routineId = routineId.toInt(),
                        exerciseId = exerciseId,
                        order = index + 1
                    )
                )
            }
            
            // Actualizar última interacción con el cliente
            trainerClientDao.updateLastInteraction(trainerId, clientId)
            
            Result.success(routineId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea una rutina global (pública).
     */
    suspend fun createGlobalRoutine(
        trainerId: Int,
        name: String,
        dayInfo: String,
        exerciseIds: List<Int>
    ): Result<Long> {
        return try {
            val routine = RoutineEntity(
                ownerId = null, // Pública
                creatorId = trainerId,
                name = name,
                dayInfo = dayInfo
            )
            val routineId = routineDao.insertRoutine(routine)
            
            exerciseIds.forEachIndexed { index, exerciseId ->
                routineDao.insertRoutineExercise(
                    RoutineExerciseEntity(
                        routineId = routineId.toInt(),
                        exerciseId = exerciseId,
                        order = index + 1
                    )
                )
            }
            
            Result.success(routineId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza una rutina existente.
     */
    suspend fun updateRoutine(routine: RoutineEntity): Result<Unit> {
        return try {
            routineDao.updateRoutine(routine)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina una rutina.
     */
    suspend fun deleteRoutine(routineId: Int): Result<Unit> {
        return try {
            routineDao.deleteRoutineById(routineId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cuenta rutinas creadas por el entrenador.
     */
    suspend fun countMyRoutines(trainerId: Int): Int =
        routineDao.countRoutinesByCreator(trainerId)
}
