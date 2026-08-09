package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.progress.BodyMeasurementEntity
import com.shagox.apptrainingnow.data.local.progress.GoalEntity
import com.shagox.apptrainingnow.data.local.progress.GoalStatus
import com.shagox.apptrainingnow.data.local.progress.PersonalRecordEntity
import com.shagox.apptrainingnow.data.local.progress.ProgressDao
import com.shagox.apptrainingnow.data.local.progress.RecordType
import com.shagox.apptrainingnow.data.local.progress.WeightProgress
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestión de progreso del usuario.
 * 
 * Maneja:
 * - Medidas corporales
 * - Objetivos de entrenamiento
 * - Récords personales
 * - Análisis de progreso
 */
class ProgressRepository(private val progressDao: ProgressDao) {

    // ==================== MEDIDAS CORPORALES ====================

    /**
     * Registra una nueva medición corporal.
     */
    suspend fun addMeasurement(measurement: BodyMeasurementEntity): Result<Long> {
        return try {
            val id = progressDao.insertMeasurement(measurement)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el historial de mediciones del usuario.
     */
    fun getMeasurementHistory(userId: Int): Flow<List<BodyMeasurementEntity>> =
        progressDao.getMeasurementsForUser(userId)

    /**
     * Obtiene la última medición del usuario.
     */
    suspend fun getLatestMeasurement(userId: Int): BodyMeasurementEntity? =
        progressDao.getLatestMeasurement(userId)

    /**
     * Observa la última medición en tiempo real.
     */
    fun observeLatestMeasurement(userId: Int): Flow<BodyMeasurementEntity?> =
        progressDao.observeLatestMeasurement(userId)

    /**
     * Obtiene mediciones en un rango de fechas.
     */
    fun getMeasurementsInRange(
        userId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<BodyMeasurementEntity>> =
        progressDao.getMeasurementsInDateRange(userId, startDate, endDate)

    /**
     * Obtiene el progreso de peso (inicial vs actual).
     */
    suspend fun getWeightProgress(userId: Int): WeightProgress? =
        progressDao.getWeightProgress(userId)

    /**
     * Elimina una medición.
     */
    suspend fun deleteMeasurement(measurementId: Int): Result<Unit> {
        return try {
            progressDao.deleteMeasurementById(measurementId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== OBJETIVOS ====================

    /**
     * Crea un nuevo objetivo.
     */
    suspend fun createGoal(goal: GoalEntity): Result<Long> {
        return try {
            val id = progressDao.insertGoal(goal)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza un objetivo existente.
     */
    suspend fun updateGoal(goal: GoalEntity): Result<Unit> {
        return try {
            progressDao.updateGoal(goal)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los objetivos del usuario.
     */
    fun getAllGoals(userId: Int): Flow<List<GoalEntity>> =
        progressDao.getGoalsForUser(userId)

    /**
     * Obtiene solo objetivos activos.
     */
    fun getActiveGoals(userId: Int): Flow<List<GoalEntity>> =
        progressDao.getActiveGoals(userId)

    /**
     * Obtiene un objetivo específico.
     */
    suspend fun getGoal(goalId: Int): GoalEntity? =
        progressDao.getGoalById(goalId)

    /**
     * Observa un objetivo en tiempo real.
     */
    fun observeGoal(goalId: Int): Flow<GoalEntity?> =
        progressDao.observeGoal(goalId)

    /**
     * Actualiza el progreso de un objetivo.
     */
    suspend fun updateGoalProgress(
        goalId: Int,
        currentValue: Double
    ): Result<Unit> {
        return try {
            val goal = progressDao.getGoalById(goalId)
                ?: return Result.failure(Exception("Objetivo no encontrado"))

            val progress = if (goal.targetValue != null && goal.startValue != null) {
                val totalChange = goal.targetValue - goal.startValue
                val currentChange = currentValue - goal.startValue
                if (totalChange != 0.0) (currentChange / totalChange * 100).coerceIn(0.0, 100.0)
                else 0.0
            } else {
                0.0
            }

            progressDao.updateGoalProgress(goalId, currentValue, progress)

            // Marcar como completado si alcanzó el 100%
            if (progress >= 100.0) {
                progressDao.completeGoal(goalId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marca un objetivo como completado.
     */
    suspend fun completeGoal(goalId: Int): Result<Unit> {
        return try {
            progressDao.completeGoal(goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el estado de un objetivo.
     */
    suspend fun updateGoalStatus(goalId: Int, status: GoalStatus): Result<Unit> {
        return try {
            progressDao.updateGoalStatus(goalId, status.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un objetivo.
     */
    suspend fun deleteGoal(goalId: Int): Result<Unit> {
        return try {
            progressDao.deleteGoalById(goalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cuenta objetivos activos.
     */
    suspend fun countActiveGoals(userId: Int): Int =
        progressDao.countActiveGoals(userId)

    /**
     * Cuenta objetivos completados.
     */
    suspend fun countCompletedGoals(userId: Int): Int =
        progressDao.countCompletedGoals(userId)

    /**
     * Obtiene objetivos próximos a vencer.
     */
    fun getUpcomingDeadlines(userId: Int, daysAhead: Int = 7): Flow<List<GoalEntity>> {
        val deadline = System.currentTimeMillis() + (daysAhead * 24 * 60 * 60 * 1000L)
        return progressDao.getUpcomingDeadlines(userId, deadline)
    }

    // ==================== OBJETIVOS DEL ENTRENADOR ====================

    /**
     * Obtiene objetivos creados por el entrenador para sus clientes.
     */
    fun getGoalsCreatedByTrainer(trainerId: Int): Flow<List<GoalEntity>> =
        progressDao.getGoalsCreatedByTrainer(trainerId)

    /**
     * Obtiene objetivos de un cliente específico creados por el entrenador.
     */
    fun getClientGoals(clientId: Int, trainerId: Int): Flow<List<GoalEntity>> =
        progressDao.getClientGoalsByTrainer(clientId, trainerId)

    /**
     * El entrenador añade feedback a un objetivo.
     */
    suspend fun addTrainerFeedback(goalId: Int, feedback: String): Result<Unit> {
        return try {
            progressDao.addTrainerFeedback(goalId, feedback)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== RÉCORDS PERSONALES ====================

    /**
     * Registra un nuevo récord personal.
     */
    suspend fun addPersonalRecord(record: PersonalRecordEntity): Result<Long> {
        return try {
            // Invalidar récords anteriores del mismo tipo
            val existingRecords = progressDao.getCurrentRecord(
                record.userId,
                record.exerciseId,
                record.recordType
            )

            val id = progressDao.insertPersonalRecord(record)

            // Marcar el récord anterior como no actual
            if (existingRecords != null && existingRecords.id != id.toInt()) {
                progressDao.invalidatePreviousRecords(
                    record.userId,
                    record.exerciseId,
                    record.recordType,
                    id.toInt()
                )
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene todos los récords actuales del usuario.
     */
    fun getCurrentRecords(userId: Int): Flow<List<PersonalRecordEntity>> =
        progressDao.getCurrentRecords(userId)

    /**
     * Obtiene el historial de récords de un ejercicio.
     */
    fun getRecordHistory(userId: Int, exerciseId: Int): Flow<List<PersonalRecordEntity>> =
        progressDao.getRecordHistoryForExercise(userId, exerciseId)

    /**
     * Obtiene el récord actual de un ejercicio.
     */
    suspend fun getCurrentRecord(
        userId: Int,
        exerciseId: Int,
        recordType: RecordType = RecordType.MAX_WEIGHT
    ): PersonalRecordEntity? =
        progressDao.getCurrentRecord(userId, exerciseId, recordType.name)

    /**
     * Obtiene récords recientes (últimos N días).
     */
    fun getRecentRecords(userId: Int, days: Int = 30): Flow<List<PersonalRecordEntity>> {
        val sinceDate = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return progressDao.getRecentRecords(userId, sinceDate)
    }

    /**
     * Cuenta el total de récords del usuario.
     */
    suspend fun countRecords(userId: Int): Int =
        progressDao.countCurrentRecords(userId)

    /**
     * Verifica si un peso es un nuevo récord.
     */
    suspend fun isNewRecord(
        userId: Int,
        exerciseId: Int,
        weightKg: Double,
        reps: Int = 1
    ): Boolean {
        val currentRecord = progressDao.getCurrentRecord(
            userId,
            exerciseId,
            RecordType.MAX_WEIGHT.name
        )

        return currentRecord == null || 
               weightKg > (currentRecord.value) ||
               (weightKg == currentRecord.value && (reps > (currentRecord.reps ?: 0)))
    }
}
