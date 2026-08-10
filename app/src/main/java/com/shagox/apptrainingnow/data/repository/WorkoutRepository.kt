package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.workout.ExerciseLogEntity
import com.shagox.apptrainingnow.data.local.workout.WorkoutDao
import com.shagox.apptrainingnow.data.local.workout.WorkoutSessionEntity
import com.shagox.apptrainingnow.data.local.workout.WorkoutSessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para gestión de sesiones de entrenamiento.
 * 
 * Maneja:
 * - Inicio y finalización de entrenamientos
 * - Registro de ejercicios realizados
 * - Historial de entrenamientos
 * - Estadísticas de rendimiento
 */
class WorkoutRepository(private val workoutDao: WorkoutDao) {

    // ==================== SESIONES DE ENTRENAMIENTO ====================

    /**
     * Inicia una nueva sesión de entrenamiento.
     * @return ID de la sesión creada
     */
    /**
     * Reporte mensual calculado desde la base de datos local.
     * Se usa para usuarios sin cuenta (invitados), que no existen en el backend.
     * @param mes formato yyyy-MM
     */
    suspend fun reporteMensualLocal(userId: Int, mes: String): com.shagox.apptrainingnow.data.remote.dto.MonthlyReportDto {
        val formato = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        val partes = mes.split("-")
        if (partes.size == 2) {
            cal.set(java.util.Calendar.YEAR, partes[0].toIntOrNull() ?: cal.get(java.util.Calendar.YEAR))
            cal.set(java.util.Calendar.MONTH, (partes[1].toIntOrNull() ?: 1) - 1)
        }
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val inicio = cal.timeInMillis
        cal.add(java.util.Calendar.MONTH, 1)
        val fin = cal.timeInMillis

        val sesiones = workoutDao.getRecentSessions(userId, 200)
            .filter { it.startTime in inicio until fin && it.status == WorkoutSessionStatus.COMPLETED.name }

        // Un registro por día entrenado
        val porDia = sesiones.groupBy { formato.format(java.util.Date(it.startTime)) }
        val dias = porDia.entries.sortedBy { it.key }.map { (fecha, lista) ->
            com.shagox.apptrainingnow.data.remote.dto.AttendanceDayDto(
                userId = userId,
                date = fecha,
                status = "TRAINED",
                exercisesCompleted = lista.size,
                createdAt = lista.first().startTime
            )
        }

        // Rachas de días consecutivos
        var mejorRacha = 0
        var rachaActual = 0
        var anterior: java.util.Calendar? = null
        dias.forEach { dia ->
            val actual = java.util.Calendar.getInstance().apply { time = formato.parse(dia.date) ?: java.util.Date() }
            val consecutivo = anterior?.let {
                val siguiente = (it.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
                siguiente.get(java.util.Calendar.DAY_OF_YEAR) == actual.get(java.util.Calendar.DAY_OF_YEAR)
            } ?: false
            rachaActual = if (consecutivo) rachaActual + 1 else 1
            mejorRacha = maxOf(mejorRacha, rachaActual)
            anterior = actual
        }

        val entrenados = dias.size
        return com.shagox.apptrainingnow.data.remote.dto.MonthlyReportDto(
            month = mes,
            daysTrained = entrenados,
            daysMissed = 0,
            daysRest = 0,
            totalExercises = dias.sumOf { it.exercisesCompleted },
            totalMinutes = 0,
            adherencePercent = if (entrenados == 0) 0 else 100,
            longestStreak = mejorRacha,
            currentStreak = rachaActual,
            days = dias
        )
    }

    /**
     * Registra un día de entrenamiento ya terminado (marcar todos los ejercicios).
     * Queda guardado en la base de datos con el nombre de la rutina y la sesión del día,
     * de modo que el conteo de días entrenados sobrevive al cierre de la app.
     *
     * Es idempotente por día: si ya hay una sesión completada hoy para esa rutina, no duplica.
     */
    suspend fun registrarDiaCompletado(
        userId: Int,
        routineId: Int,
        nombreRutina: String,
        nombreSesion: String,
        ejercicios: Int
    ): Result<Long> {
        return try {
            val inicioDia = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            val finDia = inicioDia + 24L * 60 * 60 * 1000

            val yaRegistrado = workoutDao.getRecentSessions(userId, 20).any { sesion ->
                sesion.routineId == routineId &&
                    sesion.status == WorkoutSessionStatus.COMPLETED.name &&
                    sesion.startTime in inicioDia until finDia
            }
            if (yaRegistrado) return Result.success(0L)

            val ahora = System.currentTimeMillis()
            val id = workoutDao.insertSession(
                WorkoutSessionEntity(
                    userId = userId,
                    routineId = routineId,
                    status = WorkoutSessionStatus.COMPLETED.name,
                    startTime = ahora,
                    endTime = ahora,
                    notes = "$nombreRutina · $nombreSesion ($ejercicios ejercicios)"
                )
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startWorkout(
        userId: Int,
        routineId: Int? = null,
        mood: String? = null
    ): Result<Long> {
        return try {
            // Verificar si hay una sesión en progreso
            val existingSession = workoutDao.getInProgressSession(userId)
            if (existingSession != null) {
                return Result.failure(Exception("Ya tienes una sesión de entrenamiento en progreso"))
            }

            val session = WorkoutSessionEntity(
                userId = userId,
                routineId = routineId,
                status = WorkoutSessionStatus.IN_PROGRESS.name,
                mood = mood
            )
            val sessionId = workoutDao.insertSession(session)
            Result.success(sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Finaliza una sesión de entrenamiento.
     */
    suspend fun completeWorkout(
        sessionId: Int,
        rating: Int? = null,
        notes: String? = null,
        caloriesBurned: Int? = null
    ): Result<Unit> {
        return try {
            val session = workoutDao.getSessionById(sessionId)
                ?: return Result.failure(Exception("Sesión no encontrada"))

            val duration = ((System.currentTimeMillis() - session.startTime) / 60000).toInt()
            
            workoutDao.completeSession(
                sessionId = sessionId,
                duration = duration,
                rating = rating,
                notes = notes
            )
            
            // Actualizar calorías si se proporcionan
            if (caloriesBurned != null) {
                workoutDao.updateSession(
                    session.copy(
                        caloriesBurned = caloriesBurned,
                        status = WorkoutSessionStatus.COMPLETED.name,
                        endTime = System.currentTimeMillis(),
                        totalDurationMinutes = duration
                    )
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Abandona una sesión de entrenamiento.
     */
    suspend fun abandonWorkout(sessionId: Int): Result<Unit> {
        return try {
            workoutDao.abandonSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene la sesión en progreso del usuario (si existe).
     */
    suspend fun getInProgressSession(userId: Int): WorkoutSessionEntity? =
        workoutDao.getInProgressSession(userId)

    /**
     * Obtiene una sesión por ID.
     */
    suspend fun getSession(sessionId: Int): WorkoutSessionEntity? =
        workoutDao.getSessionById(sessionId)

    /**
     * Observa una sesión en tiempo real.
     */
    fun observeSession(sessionId: Int): Flow<WorkoutSessionEntity?> =
        workoutDao.observeSession(sessionId)

    // ==================== HISTORIAL ====================

    /**
     * Obtiene el historial de entrenamientos del usuario.
     */
    fun getWorkoutHistory(userId: Int): Flow<List<WorkoutSessionEntity>> =
        workoutDao.getSessionsForUser(userId)

    /**
     * Obtiene entrenamientos en un rango de fechas.
     */
    fun getWorkoutsInDateRange(
        userId: Int,
        startDate: Long,
        endDate: Long
    ): Flow<List<WorkoutSessionEntity>> =
        workoutDao.getSessionsInDateRange(userId, startDate, endDate)

    /**
     * Obtiene los últimos N entrenamientos.
     */
    suspend fun getRecentWorkouts(userId: Int, limit: Int = 10): List<WorkoutSessionEntity> =
        workoutDao.getRecentSessions(userId, limit)

    /**
     * Obtiene entrenamientos de una rutina específica.
     */
    fun getWorkoutsForRoutine(routineId: Int): Flow<List<WorkoutSessionEntity>> =
        workoutDao.getSessionsForRoutine(routineId)

    // ==================== REGISTRO DE EJERCICIOS ====================

    /**
     * Registra un ejercicio realizado en la sesión.
     */
    suspend fun logExercise(
        sessionId: Int,
        exerciseId: Int,
        orderInSession: Int,
        plannedSets: Int = 3,
        plannedReps: Int = 12,
        completedSets: Int = 0,
        actualReps: String? = null,
        weightKg: Double? = null,
        rpe: Int? = null,
        notes: String? = null,
        isPersonalRecord: Boolean = false
    ): Result<Long> {
        return try {
            val log = ExerciseLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                orderInSession = orderInSession,
                plannedSets = plannedSets,
                plannedReps = plannedReps,
                completedSets = completedSets,
                actualReps = actualReps,
                weightKg = weightKg,
                rpe = rpe,
                notes = notes,
                isPersonalRecord = isPersonalRecord
            )
            val logId = workoutDao.insertExerciseLog(log)
            Result.success(logId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza un registro de ejercicio.
     */
    suspend fun updateExerciseLog(log: ExerciseLogEntity): Result<Unit> {
        return try {
            workoutDao.updateExerciseLog(log)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los registros de una sesión.
     */
    fun getExerciseLogsForSession(sessionId: Int): Flow<List<ExerciseLogEntity>> =
        workoutDao.getLogsForSession(sessionId)

    /**
     * Obtiene el historial de un ejercicio específico.
     */
    fun getExerciseHistory(userId: Int, exerciseId: Int): Flow<List<ExerciseLogEntity>> =
        workoutDao.getExerciseHistory(userId, exerciseId)

    /**
     * Obtiene el último registro de un ejercicio (para sugerir pesos).
     */
    suspend fun getLastExerciseLog(userId: Int, exerciseId: Int): ExerciseLogEntity? =
        workoutDao.getLastExerciseLog(userId, exerciseId)

    /**
     * Obtiene el peso máximo registrado para un ejercicio.
     */
    suspend fun getMaxWeight(userId: Int, exerciseId: Int): Double? =
        workoutDao.getMaxWeightForExercise(userId, exerciseId)

    // ==================== ESTADÍSTICAS ====================

    /**
     * Cuenta sesiones completadas.
     */
    suspend fun countCompletedWorkouts(userId: Int): Int =
        workoutDao.countCompletedSessions(userId)

    /**
     * Observa el conteo de sesiones completadas.
     */
    fun observeCompletedWorkoutCount(userId: Int): Flow<Int> =
        workoutDao.observeCompletedSessionCount(userId)

    /**
     * Obtiene el total de minutos entrenados.
     */
    suspend fun getTotalTrainingMinutes(userId: Int): Int =
        workoutDao.getTotalTrainingMinutes(userId)

    /**
     * Obtiene la duración promedio de sesiones.
     */
    suspend fun getAverageSessionDuration(userId: Int): Double =
        workoutDao.getAverageSessionDuration(userId)

    /**
     * Obtiene sesiones de la semana actual.
     */
    suspend fun getWeeklySessionCount(userId: Int): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return workoutDao.getWeeklySessionCount(userId, calendar.timeInMillis)
    }

    /**
     * Calcula estadísticas de rendimiento para un ejercicio.
     */
    suspend fun getExerciseStats(userId: Int, exerciseId: Int): ExerciseStats {
        val maxWeight = workoutDao.getMaxWeightForExercise(userId, exerciseId) ?: 0.0
        val timesPerformed = workoutDao.countExercisePerformed(userId, exerciseId)
        val lastLog = workoutDao.getLastExerciseLog(userId, exerciseId)
        
        return ExerciseStats(
            maxWeightKg = maxWeight,
            timesPerformed = timesPerformed,
            lastWeightKg = lastLog?.weightKg,
            lastReps = lastLog?.actualReps,
            lastPerformedDate = lastLog?.createdAt
        )
    }
}

/**
 * Estadísticas de un ejercicio.
 */
data class ExerciseStats(
    val maxWeightKg: Double,
    val timesPerformed: Int,
    val lastWeightKg: Double?,
    val lastReps: String?,
    val lastPerformedDate: Long?
)
