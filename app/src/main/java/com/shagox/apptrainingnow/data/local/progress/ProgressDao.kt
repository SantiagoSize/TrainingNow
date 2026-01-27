package com.shagox.apptrainingnow.data.local.progress

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar el progreso del usuario: medidas corporales, objetivos y récords.
 * 
 * Proporciona operaciones completas para:
 * - Registrar y consultar medidas corporales
 * - Gestionar objetivos de entrenamiento
 * - Seguimiento de récords personales
 * - Análisis de progreso histórico
 */
@Dao
interface ProgressDao {

    // ==================== BODY MEASUREMENTS - CRUD ====================

    /**
     * Inserta una nueva medición corporal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity): Long

    /**
     * Actualiza una medición existente.
     */
    @Update
    suspend fun updateMeasurement(measurement: BodyMeasurementEntity)

    /**
     * Elimina una medición.
     */
    @Delete
    suspend fun deleteMeasurement(measurement: BodyMeasurementEntity)

    /**
     * Elimina una medición por ID.
     */
    @Query("DELETE FROM body_measurements WHERE id = :measurementId")
    suspend fun deleteMeasurementById(measurementId: Int)

    // ==================== BODY MEASUREMENTS - QUERIES ====================

    /**
     * Obtiene una medición por ID.
     */
    @Query("SELECT * FROM body_measurements WHERE id = :measurementId")
    suspend fun getMeasurementById(measurementId: Int): BodyMeasurementEntity?

    /**
     * Obtiene todas las mediciones de un usuario ordenadas por fecha.
     */
    @Query("SELECT * FROM body_measurements WHERE userId = :userId ORDER BY measurementDate DESC")
    fun getMeasurementsForUser(userId: Int): Flow<List<BodyMeasurementEntity>>

    /**
     * Obtiene la última medición de un usuario.
     */
    @Query("SELECT * FROM body_measurements WHERE userId = :userId ORDER BY measurementDate DESC LIMIT 1")
    suspend fun getLatestMeasurement(userId: Int): BodyMeasurementEntity?

    /**
     * Obtiene la última medición como Flow.
     */
    @Query("SELECT * FROM body_measurements WHERE userId = :userId ORDER BY measurementDate DESC LIMIT 1")
    fun observeLatestMeasurement(userId: Int): Flow<BodyMeasurementEntity?>

    /**
     * Obtiene mediciones en un rango de fechas.
     */
    @Query("""
        SELECT * FROM body_measurements 
        WHERE userId = :userId 
        AND measurementDate BETWEEN :startDate AND :endDate 
        ORDER BY measurementDate ASC
    """)
    fun getMeasurementsInDateRange(userId: Int, startDate: Long, endDate: Long): Flow<List<BodyMeasurementEntity>>

    /**
     * Obtiene el historial de peso.
     */
    @Query("""
        SELECT measurementDate, weightKg 
        FROM body_measurements 
        WHERE userId = :userId AND weightKg IS NOT NULL 
        ORDER BY measurementDate ASC
    """)
    fun getWeightHistory(userId: Int): Flow<List<WeightDataPoint>>

    /**
     * Obtiene el progreso de peso (primera vs última medición).
     */
    @Query("""
        SELECT 
            (SELECT weightKg FROM body_measurements WHERE userId = :userId ORDER BY measurementDate ASC LIMIT 1) as startWeight,
            (SELECT weightKg FROM body_measurements WHERE userId = :userId ORDER BY measurementDate DESC LIMIT 1) as currentWeight
    """)
    suspend fun getWeightProgress(userId: Int): WeightProgress?

    // ==================== GOALS - CRUD ====================

    /**
     * Inserta un nuevo objetivo.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    /**
     * Inserta múltiples objetivos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)

    /**
     * Actualiza un objetivo existente.
     */
    @Update
    suspend fun updateGoal(goal: GoalEntity)

    /**
     * Elimina un objetivo.
     */
    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    /**
     * Elimina un objetivo por ID.
     */
    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Int)

    // ==================== GOALS - QUERIES ====================

    /**
     * Obtiene un objetivo por ID.
     */
    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Int): GoalEntity?

    /**
     * Obtiene un objetivo como Flow.
     */
    @Query("SELECT * FROM goals WHERE id = :goalId")
    fun observeGoal(goalId: Int): Flow<GoalEntity?>

    /**
     * Obtiene todos los objetivos de un usuario.
     */
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY priority DESC, targetDate ASC")
    fun getGoalsForUser(userId: Int): Flow<List<GoalEntity>>

    /**
     * Obtiene objetivos activos de un usuario.
     */
    @Query("SELECT * FROM goals WHERE userId = :userId AND status = 'ACTIVE' ORDER BY priority DESC, targetDate ASC")
    fun getActiveGoals(userId: Int): Flow<List<GoalEntity>>

    /**
     * Obtiene objetivos por estado.
     */
    @Query("SELECT * FROM goals WHERE userId = :userId AND status = :status ORDER BY updatedAt DESC")
    fun getGoalsByStatus(userId: Int, status: String): Flow<List<GoalEntity>>

    /**
     * Obtiene objetivos por categoría.
     */
    @Query("SELECT * FROM goals WHERE userId = :userId AND category = :category ORDER BY status ASC, targetDate ASC")
    fun getGoalsByCategory(userId: Int, category: String): Flow<List<GoalEntity>>

    /**
     * Obtiene objetivos creados por un entrenador para sus clientes.
     */
    @Query("SELECT * FROM goals WHERE createdByTrainerId = :trainerId ORDER BY userId, updatedAt DESC")
    fun getGoalsCreatedByTrainer(trainerId: Int): Flow<List<GoalEntity>>

    /**
     * Obtiene objetivos de un cliente específico creados por el entrenador.
     */
    @Query("""
        SELECT * FROM goals 
        WHERE userId = :clientId AND createdByTrainerId = :trainerId 
        ORDER BY status ASC, priority DESC
    """)
    fun getClientGoalsByTrainer(clientId: Int, trainerId: Int): Flow<List<GoalEntity>>

    /**
     * Cuenta objetivos activos.
     */
    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND status = 'ACTIVE'")
    suspend fun countActiveGoals(userId: Int): Int

    /**
     * Cuenta objetivos completados.
     */
    @Query("SELECT COUNT(*) FROM goals WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun countCompletedGoals(userId: Int): Int

    /**
     * Obtiene objetivos próximos a vencer (dentro de X días).
     */
    @Query("""
        SELECT * FROM goals 
        WHERE userId = :userId 
        AND status = 'ACTIVE'
        AND targetDate IS NOT NULL
        AND targetDate <= :deadlineThreshold
        ORDER BY targetDate ASC
    """)
    fun getUpcomingDeadlines(userId: Int, deadlineThreshold: Long): Flow<List<GoalEntity>>

    // ==================== GOALS - OPERACIONES DE ESTADO ====================

    /**
     * Actualiza el estado de un objetivo.
     */
    @Query("UPDATE goals SET status = :status, updatedAt = :updateTime WHERE id = :goalId")
    suspend fun updateGoalStatus(goalId: Int, status: String, updateTime: Long = System.currentTimeMillis())

    /**
     * Marca un objetivo como completado.
     */
    @Query("""
        UPDATE goals 
        SET status = 'COMPLETED', 
            completedDate = :completedTime, 
            progressPercentage = 100.0,
            updatedAt = :completedTime 
        WHERE id = :goalId
    """)
    suspend fun completeGoal(goalId: Int, completedTime: Long = System.currentTimeMillis())

    /**
     * Actualiza el progreso de un objetivo.
     */
    @Query("""
        UPDATE goals 
        SET currentValue = :currentValue, 
            progressPercentage = :progress,
            updatedAt = :updateTime 
        WHERE id = :goalId
    """)
    suspend fun updateGoalProgress(
        goalId: Int, 
        currentValue: Double, 
        progress: Double, 
        updateTime: Long = System.currentTimeMillis()
    )

    /**
     * Añade feedback del entrenador a un objetivo.
     */
    @Query("UPDATE goals SET trainerFeedback = :feedback, updatedAt = :updateTime WHERE id = :goalId")
    suspend fun addTrainerFeedback(goalId: Int, feedback: String, updateTime: Long = System.currentTimeMillis())

    // ==================== PERSONAL RECORDS - CRUD ====================

    /**
     * Inserta un nuevo récord personal.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonalRecord(record: PersonalRecordEntity): Long

    /**
     * Actualiza un récord.
     */
    @Update
    suspend fun updatePersonalRecord(record: PersonalRecordEntity)

    /**
     * Elimina un récord.
     */
    @Delete
    suspend fun deletePersonalRecord(record: PersonalRecordEntity)

    // ==================== PERSONAL RECORDS - QUERIES ====================

    /**
     * Obtiene todos los récords actuales de un usuario.
     */
    @Query("SELECT * FROM personal_records WHERE userId = :userId AND isCurrentRecord = 1 ORDER BY achievedAt DESC")
    fun getCurrentRecords(userId: Int): Flow<List<PersonalRecordEntity>>

    /**
     * Obtiene el historial de récords de un ejercicio.
     */
    @Query("""
        SELECT * FROM personal_records 
        WHERE userId = :userId AND exerciseId = :exerciseId 
        ORDER BY achievedAt DESC
    """)
    fun getRecordHistoryForExercise(userId: Int, exerciseId: Int): Flow<List<PersonalRecordEntity>>

    /**
     * Obtiene el récord actual de un ejercicio.
     */
    @Query("""
        SELECT * FROM personal_records 
        WHERE userId = :userId AND exerciseId = :exerciseId AND recordType = :recordType AND isCurrentRecord = 1 
        LIMIT 1
    """)
    suspend fun getCurrentRecord(userId: Int, exerciseId: Int, recordType: String): PersonalRecordEntity?

    /**
     * Obtiene récords recientes (últimos N días).
     */
    @Query("""
        SELECT * FROM personal_records 
        WHERE userId = :userId AND achievedAt >= :sinceDate 
        ORDER BY achievedAt DESC
    """)
    fun getRecentRecords(userId: Int, sinceDate: Long): Flow<List<PersonalRecordEntity>>

    /**
     * Cuenta el total de récords de un usuario.
     */
    @Query("SELECT COUNT(*) FROM personal_records WHERE userId = :userId AND isCurrentRecord = 1")
    suspend fun countCurrentRecords(userId: Int): Int

    /**
     * Marca récords anteriores como no actuales cuando hay uno nuevo.
     */
    @Query("""
        UPDATE personal_records 
        SET isCurrentRecord = 0 
        WHERE userId = :userId AND exerciseId = :exerciseId AND recordType = :recordType AND id != :newRecordId
    """)
    suspend fun invalidatePreviousRecords(userId: Int, exerciseId: Int, recordType: String, newRecordId: Int)
}

/**
 * Clase de datos para el historial de peso.
 */
data class WeightDataPoint(
    val measurementDate: Long,
    val weightKg: Double?
)

/**
 * Clase de datos para el progreso de peso.
 */
data class WeightProgress(
    val startWeight: Double?,
    val currentWeight: Double?
)
