package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.local.workout.ExerciseLogEntity
import com.shagox.apptrainingnow.data.local.workout.WorkoutDao
import com.shagox.apptrainingnow.data.local.workout.WorkoutSessionEntity
import com.shagox.apptrainingnow.data.local.workout.WorkoutSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
     * Registra como completado el día de rutina cuya fecha (dentro de la semana) es [inicioDia].
     * Antes se estampaba siempre con la hora real de "ahora", así que marcar el checklist de
     * cualquier día (ej. Martes) quedaba registrado bajo el día real del sistema (ej. Lunes) y
     * la franja semanal mostraba verde el día equivocado. Ahora se guarda bajo la fecha del
     * propio día seleccionado.
     *
     * Es idempotente por día: si ya hay una sesión completada ese día para esa rutina, no duplica.
     */
    suspend fun registrarDiaCompletado(
        userId: Int,
        routineId: Int,
        nombreRutina: String,
        nombreSesion: String,
        ejercicios: Int,
        inicioDia: Long = inicioDeHoy()
    ): Result<Long> {
        return try {
            val finDia = inicioDia + 24L * 60 * 60 * 1000

            val yaRegistrado = workoutDao.getRecentSessions(userId, 20).any { sesion ->
                sesion.routineId == routineId &&
                    sesion.status == WorkoutSessionStatus.COMPLETED.name &&
                    sesion.startTime in inicioDia until finDia
            }
            if (yaRegistrado) return Result.success(0L)

            // La marca de tiempo queda dentro de la ventana del día seleccionado (mediodía,
            // para evitar bordes por redondeo), no la hora real de "ahora".
            val marcaTiempo = inicioDia + 12L * 60 * 60 * 1000
            val id = workoutDao.insertSession(
                WorkoutSessionEntity(
                    userId = userId,
                    routineId = routineId,
                    status = WorkoutSessionStatus.COMPLETED.name,
                    startTime = marcaTiempo,
                    endTime = marcaTiempo,
                    notes = "$nombreRutina · $nombreSesion ($ejercicios ejercicios)"
                )
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Revierte el registro de completado de un día (al desmarcar algún ejercicio ya marcado).
     * Sin esto, un día quedaba verde para siempre aunque luego se desmarcaran sus ejercicios.
     */
    suspend fun desregistrarDiaCompletado(
        userId: Int,
        routineId: Int,
        inicioDia: Long
    ) {
        val finDia = inicioDia + 24L * 60 * 60 * 1000
        workoutDao.getRecentSessions(userId, 20)
            .filter { sesion ->
                sesion.routineId == routineId &&
                    sesion.status == WorkoutSessionStatus.COMPLETED.name &&
                    sesion.startTime in inicioDia until finDia
            }
            .forEach { workoutDao.deleteSessionById(it.id) }
    }

    /**
     * Obtiene (o crea si no existe) la sesión de entrenamiento del día [inicioDia] para esta
     * rutina. A diferencia de [registrarDiaCompletado] (que solo crea una sesión COMPLETED
     * cuando se marca TODO el día), esto permite ir guardando series sueltas de un ejercicio
     * aunque el día no esté terminado todavía. Idempotente: si ya existe una sesión ese día
     * para esa rutina (completada o no), la reutiliza en vez de crear otra.
     */
    suspend fun obtenerOCrearSesionDelDia(
        userId: Int,
        routineId: Int,
        inicioDia: Long
    ): Int {
        val finDia = inicioDia + 24L * 60 * 60 * 1000
        val existente = workoutDao.getRecentSessions(userId, 50).firstOrNull { sesion ->
            sesion.routineId == routineId && sesion.startTime in inicioDia until finDia
        }
        if (existente != null) return existente.id

        val marcaTiempo = inicioDia + 12L * 60 * 60 * 1000 // mediodía del día, evita bordes por redondeo
        val id = workoutDao.insertSession(
            WorkoutSessionEntity(
                userId = userId,
                routineId = routineId,
                status = WorkoutSessionStatus.IN_PROGRESS.name,
                startTime = marcaTiempo
            )
        )
        return id.toInt()
    }

    /**
     * Agrega una serie (repeticiones + carga) de un ejercicio dentro de una sesión. Cada
     * serie queda como su propia fila; el número de serie ("Serie 1", "Serie 2"...) se
     * calcula automáticamente según cuántas ya había guardadas para ese ejercicio.
     * @param cargaKg carga SIEMPRE en kg (formato canónico); la UI convierte antes de llamar
     * si el usuario tiene elegido libras (ver UnitsPreference).
     */
    suspend fun agregarSerie(sessionId: Int, exerciseId: Int, reps: Int, cargaKg: Double?): Long {
        val yaGuardadas = workoutDao.getLogsForSessionSync(sessionId).count { it.exerciseId == exerciseId }
        val log = ExerciseLogEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            orderInSession = yaGuardadas, // reutilizado como índice de serie DENTRO de este ejercicio
            completedSets = 1,
            actualReps = reps.toString(),
            weightKg = cargaKg
        )
        return workoutDao.insertExerciseLog(log)
    }

    /** Borra una serie ya registrada (para poder corregir un error de tipeo). */
    suspend fun borrarSerie(log: ExerciseLogEntity) {
        workoutDao.deleteExerciseLog(log)
    }

    /** Series ya registradas de un ejercicio puntual dentro de una sesión, en orden. */
    fun getSeriesDeEjercicio(sessionId: Int, exerciseId: Int): Flow<List<ExerciseLogEntity>> =
        workoutDao.getLogsForSession(sessionId).map { lista ->
            lista.filter { it.exerciseId == exerciseId }.sortedBy { it.orderInSession }
        }

    /**
     * Detalle de series registradas en un día concreto (usado por el detalle del día del
     * calendario mensual en Ajustes): agrupa por ejercicio todas las series guardadas en
     * cualquier sesión de ese día, sin importar la rutina.
     */
    suspend fun obtenerDetalleDelDia(userId: Int, inicioDia: Long): Map<Int, List<ExerciseLogEntity>> {
        val finDia = inicioDia + 24L * 60 * 60 * 1000
        val sesiones = workoutDao.getRecentSessions(userId, 100).filter { it.startTime in inicioDia until finDia }
        val logs = sesiones.flatMap { workoutDao.getLogsForSessionSync(it.id) }
        return logs.groupBy { it.exerciseId }.mapValues { (_, lista) -> lista.sortedBy { it.orderInSession } }
    }

    private fun inicioDeHoy(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

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
