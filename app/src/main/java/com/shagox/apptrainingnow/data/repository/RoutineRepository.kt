package com.shagox.apptrainingnow.data.repository

import com.shagox.apptrainingnow.data.domain.MAX_EXERCISES_PER_DAY
import com.shagox.apptrainingnow.data.domain.RoutineDayView
import com.shagox.apptrainingnow.data.domain.RoutineHeader
import com.shagox.apptrainingnow.data.domain.RoutineWithDays
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.local.exercise.ExerciseDao
import com.shagox.apptrainingnow.data.local.routine.RoutineDao
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Datos de un día de la rutina semanal (nombre del día, actividad, ejercicios por nombre).
 */
data class DayRoutineInput(
    val dayLabel: String,
    val activityName: String,
    val exerciseNames: List<String>
)

/**
 * Interfaz del repositorio de rutinas para integración con backend (Spring Boot).
 * CRUD de rutinas, días (RoutineEntity por día) y ejercicios por día.
 * Permisos: Admin/Entrenador → rutinas globales; Usuario → rutinas privadas (ownerId = userId).
 */
interface RoutineRepositoryContract {
    fun getMyRoutines(userId: Int): Flow<List<RoutineEntity>>
    fun getGlobalRoutines(): Flow<List<RoutineEntity>>
    fun observeRoutine(routineId: Int): Flow<RoutineEntity?>
    fun getRoutineWithDays(routineId: Int, userId: Int): Flow<RoutineWithDays?>
    suspend fun getRoutineById(routineId: Int): RoutineEntity?
    suspend fun insertRoutine(routine: RoutineEntity): Long
    suspend fun updateRoutine(routine: RoutineEntity)
    suspend fun deleteRoutine(routine: RoutineEntity)
    suspend fun getExercisesForDay(routineId: Int): List<ExerciseEntity>
    suspend fun addExerciseToDay(routineId: Int, exerciseId: Int)
    suspend fun removeExerciseFromDay(routineId: Int, exerciseId: Int)
    suspend fun canAddExerciseToDay(routineId: Int): Boolean
    suspend fun updateDayActivity(routineId: Int, dayLabel: String, activityName: String)
}

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val exerciseDao: ExerciseDao
) : RoutineRepositoryContract {

    // ==================== JERARQUÍA: Rutina → Días → Ejercicios ====================

    /**
     * Observa la rutina completa: cabecera + lista de días con ejercicios (0-10 por día).
     * Para UI: Lista de días → Vista de detalle de ejercicios.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getRoutineWithDays(routineId: Int, userId: Int): Flow<RoutineWithDays?> {
        return routineDao.observeRoutine(routineId).flatMapLatest { routine ->
            if (routine == null) flowOf(null)
            else flow { emit(buildRoutineWithDays(routine, userId)) }
        }
    }

    private suspend fun buildRoutineWithDays(routine: RoutineEntity, userId: Int): RoutineWithDays {
        val days = routineDao.getDaysForRoutineNameSync(routine.name, userId)
        val dayViews = days.map { day ->
            val exercises = routineDao.getExercisesForRoutineSync(day.id)
            val (label, activity) = parseDayInfo(day.dayInfo)
            RoutineDayView(
                routineId = day.id,
                dayLabel = label,
                activityName = activity,
                exercises = exercises,
                exerciseCount = exercises.size
            )
        }
        return RoutineWithDays(
            header = RoutineHeader(
                id = routine.id,
                name = routine.name,
                ownerId = routine.ownerId,
                creatorId = routine.creatorId
            ),
            days = dayViews
        )
    }

    private fun parseDayInfo(dayInfo: String): Pair<String, String> {
        val idx = dayInfo.indexOf(" - ")
        return if (idx >= 0) {
            dayInfo.substring(0, idx).trim() to dayInfo.substring(idx + 3).trim()
        } else {
            dayInfo.trim() to ""
        }
    }

    /**
     * Indica si se puede añadir otro ejercicio al día (límite 10 por día).
     */
    override suspend fun canAddExerciseToDay(routineId: Int): Boolean {
        return routineDao.countExercisesInRoutine(routineId) < MAX_EXERCISES_PER_DAY
    }

    override suspend fun getRoutineById(routineId: Int): RoutineEntity? =
        routineDao.getRoutineById(routineId)

    override suspend fun getExercisesForDay(routineId: Int): List<ExerciseEntity> =
        routineDao.getExercisesForRoutineSync(routineId)

    override suspend fun addExerciseToDay(routineId: Int, exerciseId: Int) {
        if (!canAddExerciseToDay(routineId)) return
        val count = routineDao.countExercisesInRoutine(routineId)
        routineDao.insertRoutineExercise(
            RoutineExerciseEntity(routineId = routineId, exerciseId = exerciseId, order = count + 1)
        )
    }

    override suspend fun removeExerciseFromDay(routineId: Int, exerciseId: Int) {
        routineDao.removeExerciseFromRoutine(routineId, exerciseId)
    }

    override suspend fun updateDayActivity(routineId: Int, dayLabel: String, activityName: String) {
        val r = routineDao.getRoutineById(routineId) ?: return
        val dayInfo = if (activityName.isNotBlank()) "$dayLabel - $activityName" else dayLabel
        routineDao.updateRoutine(r.copy(dayInfo = dayInfo))
    }

    override suspend fun insertRoutine(routine: RoutineEntity): Long =
        routineDao.insertRoutine(routine)

    override suspend fun updateRoutine(routine: RoutineEntity) {
        routineDao.updateRoutine(routine)
    }

    override suspend fun deleteRoutine(routine: RoutineEntity) {
        routineDao.deleteRoutine(routine)
    }

    // ==================== QUERIES ====================

    override fun getMyRoutines(userId: Int): Flow<List<RoutineEntity>> {
        return routineDao.getMyRoutines(userId)
    }

    override fun getGlobalRoutines(): Flow<List<RoutineEntity>> {
        return routineDao.getGlobalRoutines()
    }

    // Obtener solo rutinas propias del usuario (para sección "MIS RUTINAS")
    fun getUserOwnRoutines(userId: Int): Flow<List<RoutineEntity>> {
        return routineDao.getUserOwnRoutines(userId)
    }

    override fun observeRoutine(routineId: Int): Flow<RoutineEntity?> {
        return routineDao.observeRoutine(routineId)
    }

    // Obtener los ejercicios de una rutina específica (Para cuando le des click)
    fun getExercisesForRoutine(routineId: Int): Flow<List<ExerciseEntity>> {
        return routineDao.getExercisesForRoutine(routineId)
    }

    /**
     * Guarda una rutina personal: 7 días (cajas), cada uno con 0–10 ejercicios.
     * La rutina es solo del usuario (ownerId = creatorId = userId).
     */
    suspend fun savePersonalRoutine(
        userId: Int,
        routineName: String,
        days: List<DayRoutineInput>
    ) {
        require(days.size == 7) { "Se requieren exactamente 7 días" }
        val now = System.currentTimeMillis()
        for (day in days) {
            val dayInfo = if (day.activityName.isNotBlank()) "${day.dayLabel} - ${day.activityName}" else day.dayLabel
            val routine = RoutineEntity(
                ownerId = userId,
                creatorId = userId,
                name = routineName,
                dayInfo = dayInfo,
                creationDate = now,
                scheduledTime = now
            )
            val routineId = routineDao.insertRoutine(routine).toInt()
            val exerciseIds = mutableListOf<Int>()
            val names = day.exerciseNames.map { it.trim() }.filter { it.isNotBlank() }.take(MAX_EXERCISES_PER_DAY)
            for (name in names) {
                val exercise = exerciseDao.getExerciseByName(name)
                val id = if (exercise != null) {
                    exercise.id
                } else {
                    val newExercise = ExerciseEntity(
                        name = name,
                        category = "Personalizado",
                        description = "",
                        videoUrl = "",
                        isSystemDefault = false
                    )
                    exerciseDao.insertExercise(newExercise).toInt()
                }
                exerciseIds.add(id)
            }
            val crossRefs = exerciseIds.mapIndexed { index, exerciseId ->
                RoutineExerciseEntity(routineId = routineId, exerciseId = exerciseId, order = index + 1)
            }
            if (crossRefs.isNotEmpty()) {
                routineDao.insertRoutineExercises(crossRefs)
            }
        }
    }
}