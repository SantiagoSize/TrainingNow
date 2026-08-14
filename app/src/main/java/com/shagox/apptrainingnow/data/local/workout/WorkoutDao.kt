package com.shagox.apptrainingnow.data.local.workout

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar sesiones de entrenamiento y registros de ejercicios.
 * 
 * Proporciona operaciones completas para:
 * - Crear y gestionar sesiones de entrenamiento
 * - Registrar ejercicios realizados
 * - Consultar historial y estadísticas
 * - Análisis de progreso
 */
@Dao
interface WorkoutDao {

    // ==================== WORKOUT SESSIONS - CRUD ====================

    /**
     * Inserta una nueva sesión de entrenamiento.
     * @return ID de la sesión creada
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    /**
     * Actualiza una sesión existente.
     */
    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    /**
     * Elimina una sesión y todos sus registros de ejercicios (CASCADE).
     */
    @Delete
    suspend fun deleteSession(session: WorkoutSessionEntity)

    /**
     * Elimina una sesión por ID.
     */
    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    /**
     * Traspasa TODAS las sesiones de entrenamiento (historial) de un usuario a otro.
     * Se usa al pasar de invitado a cuenta (y viceversa al resetear el invitado), igual que
     * se hace con las rutinas en GuestSession.
     */
    @Query("UPDATE workout_sessions SET userId = :nuevoUserId WHERE userId = :userIdActual")
    suspend fun reasignarUsuario(userIdActual: Int, nuevoUserId: Int)

    // ==================== WORKOUT SESSIONS - QUERIES ====================

    /**
     * Obtiene una sesión por ID.
     */
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Int): WorkoutSessionEntity?

    /**
     * Obtiene una sesión por ID como Flow.
     */
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Int): Flow<WorkoutSessionEntity?>

    /**
     * Obtiene todas las sesiones de un usuario ordenadas por fecha.
     */
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getSessionsForUser(userId: Int): Flow<List<WorkoutSessionEntity>>

    /**
     * Obtiene sesiones en un rango de fechas.
     */
    @Query("""
        SELECT * FROM workout_sessions 
        WHERE userId = :userId 
        AND startTime BETWEEN :startDate AND :endDate 
        ORDER BY startTime DESC
    """)
    fun getSessionsInDateRange(userId: Int, startDate: Long, endDate: Long): Flow<List<WorkoutSessionEntity>>

    /**
     * Obtiene las últimas N sesiones de un usuario.
     */
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(userId: Int, limit: Int = 10): List<WorkoutSessionEntity>

    /**
     * Obtiene sesiones por estado.
     */
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND status = :status ORDER BY startTime DESC")
    fun getSessionsByStatus(userId: Int, status: String): Flow<List<WorkoutSessionEntity>>

    /**
     * Obtiene la sesión en progreso (si existe).
     */
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getInProgressSession(userId: Int): WorkoutSessionEntity?

    /**
     * Obtiene sesiones de una rutina específica.
     */
    @Query("SELECT * FROM workout_sessions WHERE routineId = :routineId ORDER BY startTime DESC")
    fun getSessionsForRoutine(routineId: Int): Flow<List<WorkoutSessionEntity>>

    // ==================== WORKOUT SESSIONS - ESTADÍSTICAS ====================

    /**
     * Cuenta el total de sesiones completadas de un usuario.
     */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun countCompletedSessions(userId: Int): Int

    /**
     * Cuenta sesiones completadas como Flow.
     */
    @Query("SELECT COUNT(*) FROM workout_sessions WHERE userId = :userId AND status = 'COMPLETED'")
    fun observeCompletedSessionCount(userId: Int): Flow<Int>

    /**
     * Obtiene el total de minutos entrenados.
     */
    @Query("""
        SELECT COALESCE(SUM(totalDurationMinutes), 0) 
        FROM workout_sessions 
        WHERE userId = :userId AND status = 'COMPLETED'
    """)
    suspend fun getTotalTrainingMinutes(userId: Int): Int

    /**
     * Obtiene el promedio de duración de sesiones.
     */
    @Query("""
        SELECT COALESCE(AVG(totalDurationMinutes), 0) 
        FROM workout_sessions 
        WHERE userId = :userId AND status = 'COMPLETED'
    """)
    suspend fun getAverageSessionDuration(userId: Int): Double

    /**
     * Obtiene la racha actual de días entrenando consecutivamente.
     */
    @Query("""
        SELECT COUNT(DISTINCT date(startTime/1000, 'unixepoch')) 
        FROM workout_sessions 
        WHERE userId = :userId 
        AND status = 'COMPLETED'
        AND startTime >= :sinceDate
    """)
    suspend fun getTrainingDaysSince(userId: Int, sinceDate: Long): Int

    /**
     * Obtiene estadísticas semanales (sesiones por semana).
     */
    @Query("""
        SELECT COUNT(*) FROM workout_sessions 
        WHERE userId = :userId 
        AND status = 'COMPLETED'
        AND startTime >= :weekStartTime
    """)
    suspend fun getWeeklySessionCount(userId: Int, weekStartTime: Long): Int

    // ==================== WORKOUT SESSIONS - OPERACIONES DE ESTADO ====================

    /**
     * Finaliza una sesión.
     */
    @Query("""
        UPDATE workout_sessions 
        SET status = 'COMPLETED', 
            endTime = :endTime, 
            totalDurationMinutes = :duration,
            rating = :rating,
            notes = :notes
        WHERE id = :sessionId
    """)
    suspend fun completeSession(
        sessionId: Int, 
        endTime: Long = System.currentTimeMillis(),
        duration: Int,
        rating: Int? = null,
        notes: String? = null
    )

    /**
     * Abandona una sesión.
     */
    @Query("""
        UPDATE workout_sessions 
        SET status = 'ABANDONED', endTime = :endTime 
        WHERE id = :sessionId
    """)
    suspend fun abandonSession(sessionId: Int, endTime: Long = System.currentTimeMillis())

    // ==================== EXERCISE LOGS - CRUD ====================

    /**
     * Inserta un registro de ejercicio.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(log: ExerciseLogEntity): Long

    /**
     * Inserta múltiples registros.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLogs(logs: List<ExerciseLogEntity>)

    /**
     * Actualiza un registro de ejercicio.
     */
    @Update
    suspend fun updateExerciseLog(log: ExerciseLogEntity)

    /**
     * Elimina un registro de ejercicio.
     */
    @Delete
    suspend fun deleteExerciseLog(log: ExerciseLogEntity)

    // ==================== EXERCISE LOGS - QUERIES ====================

    /**
     * Obtiene todos los registros de una sesión.
     */
    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY orderInSession ASC")
    fun getLogsForSession(sessionId: Int): Flow<List<ExerciseLogEntity>>

    /**
     * Obtiene los registros de una sesión de forma síncrona.
     */
    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY orderInSession ASC")
    suspend fun getLogsForSessionSync(sessionId: Int): List<ExerciseLogEntity>

    /**
     * Obtiene el historial de un ejercicio específico para un usuario.
     */
    @Query("""
        SELECT el.* FROM exercise_logs el
        INNER JOIN workout_sessions ws ON el.sessionId = ws.id
        WHERE ws.userId = :userId AND el.exerciseId = :exerciseId
        ORDER BY ws.startTime DESC
    """)
    fun getExerciseHistory(userId: Int, exerciseId: Int): Flow<List<ExerciseLogEntity>>

    /**
     * Obtiene el último registro de un ejercicio (para sugerir pesos).
     */
    @Query("""
        SELECT el.* FROM exercise_logs el
        INNER JOIN workout_sessions ws ON el.sessionId = ws.id
        WHERE ws.userId = :userId AND el.exerciseId = :exerciseId
        ORDER BY ws.startTime DESC
        LIMIT 1
    """)
    suspend fun getLastExerciseLog(userId: Int, exerciseId: Int): ExerciseLogEntity?

    /**
     * Obtiene el mejor peso registrado para un ejercicio.
     */
    @Query("""
        SELECT MAX(weightKg) FROM exercise_logs el
        INNER JOIN workout_sessions ws ON el.sessionId = ws.id
        WHERE ws.userId = :userId AND el.exerciseId = :exerciseId
    """)
    suspend fun getMaxWeightForExercise(userId: Int, exerciseId: Int): Double?

    /**
     * Cuenta cuántas veces se ha realizado un ejercicio.
     */
    @Query("""
        SELECT COUNT(*) FROM exercise_logs el
        INNER JOIN workout_sessions ws ON el.sessionId = ws.id
        WHERE ws.userId = :userId AND el.exerciseId = :exerciseId
    """)
    suspend fun countExercisePerformed(userId: Int, exerciseId: Int): Int

    /**
     * Obtiene ejercicios con récord personal marcado.
     */
    @Query("""
        SELECT el.* FROM exercise_logs el
        INNER JOIN workout_sessions ws ON el.sessionId = ws.id
        WHERE ws.userId = :userId AND el.isPersonalRecord = 1
        ORDER BY ws.startTime DESC
    """)
    fun getPersonalRecordLogs(userId: Int): Flow<List<ExerciseLogEntity>>

    /**
     * Calcula el volumen total de un ejercicio en una fecha.
     */
    @Query("""
        SELECT COALESCE(SUM(weightKg * completedSets * plannedReps), 0) 
        FROM exercise_logs el
        INNER JOIN workout_sessions ws ON el.sessionId = ws.id
        WHERE ws.userId = :userId 
        AND el.exerciseId = :exerciseId
        AND ws.startTime BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalVolumeForExercise(
        userId: Int, 
        exerciseId: Int, 
        startDate: Long, 
        endDate: Long
    ): Double
}
