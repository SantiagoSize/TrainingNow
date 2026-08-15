package com.shagox.apptrainingnow.data.local.routine

import androidx.room.*
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar rutinas de entrenamiento.
 * 
 * Proporciona operaciones CRUD completas y queries especializadas para:
 * - Gestión de rutinas personales y globales
 * - Relación rutina-ejercicios
 * - Rutinas por entrenador y cliente
 * - Programación y calendario
 */
@Dao
interface RoutineDao {

    // ==================== RUTINAS - CRUD ====================

    /**
     * Inserta una rutina y devuelve su ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    /**
     * Inserta múltiples rutinas.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<RoutineEntity>)

    /**
     * Actualiza una rutina.
     */
    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    /**
     * Elimina una rutina.
     */
    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    /**
     * Elimina una rutina por ID.
     */
    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutineById(routineId: Int)

    /**
     * Elimina rutinas globales (ownerId nulo) cuyo nombre coincida con planes
     * predeterminados descontinuados, aunque ya existan otras rutinas en la tabla.
     * Limpieza de residuos que la siembra normal (solo corre con la tabla vacía) no toca.
     */
    @Query("DELETE FROM routines WHERE ownerId IS NULL AND name LIKE '%Full Body%Principiante%'")
    suspend fun deleteRutinasDescontinuadas()

    // ==================== RUTINAS - QUERIES GENERALES ====================

    /**
     * Obtiene una rutina por ID.
     */
    @Query("SELECT * FROM routines WHERE id = :routineId")
    suspend fun getRoutineById(routineId: Int): RoutineEntity?

    /**
     * Obtiene una rutina como Flow.
     */
    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun observeRoutine(routineId: Int): Flow<RoutineEntity?>

    /**
     * Obtiene todas las rutinas.
     */
    @Query("SELECT * FROM routines ORDER BY creationDate DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    /**
     * Cuenta el total de rutinas.
     */
    @Query("SELECT COUNT(*) FROM routines")
    suspend fun count(): Int

    // ==================== RUTINAS - QUERIES POR USUARIO ====================

    /**
     * Obtiene las rutinas del usuario + las globales (públicas).
     */
    @Query("SELECT * FROM routines WHERE ownerId = :userId OR ownerId IS NULL ORDER BY scheduledTime DESC")
    fun getMyRoutines(userId: Int): Flow<List<RoutineEntity>>

    /**
     * Obtiene solo las rutinas propias del usuario (sin globales). No incluye rutinas
     * compartidas por un entrenador que todavía están pendientes de aceptación.
     */
    @Query("SELECT * FROM routines WHERE ownerId = :userId AND pendingShare = 0 ORDER BY scheduledTime DESC")
    fun getUserOwnRoutines(userId: Int): Flow<List<RoutineEntity>>

    /** Rutinas que un entrenador compartió con el usuario y todavía no acepta ni rechaza. */
    @Query("SELECT * FROM routines WHERE ownerId = :userId AND pendingShare = 1 ORDER BY creationDate DESC")
    fun getPendingSharedRoutines(userId: Int): Flow<List<RoutineEntity>>

    /** Plantillas reutilizables del entrenador (ownerId null, isTemplate = true). */
    @Query("SELECT * FROM routines WHERE creatorId = :trainerId AND isTemplate = 1 ORDER BY creationDate DESC")
    fun getTemplates(trainerId: Int): Flow<List<RoutineEntity>>

    /** Rutinas del usuario (consulta puntual, para sincronización con el backend). */
    @Query("SELECT * FROM routines WHERE ownerId = :userId")
    suspend fun getRoutinesByOwnerOnce(userId: Int): List<RoutineEntity>

    /** Rutinas públicas (consulta puntual, para sincronización con el backend). */
    @Query("SELECT * FROM routines WHERE ownerId IS NULL")
    suspend fun getGlobalRoutinesOnce(): List<RoutineEntity>

    /**
     * Obtiene solo las rutinas globales (públicas).
     */
    @Query("SELECT * FROM routines WHERE ownerId IS NULL ORDER BY name ASC")
    fun getGlobalRoutines(): Flow<List<RoutineEntity>>

    // ==================== RUTINAS - QUERIES POR ENTRENADOR ====================

    /**
     * Obtiene todas las rutinas creadas por un entrenador.
     */
    @Query("SELECT * FROM routines WHERE creatorId = :trainerId ORDER BY creationDate DESC")
    fun getRoutinesByCreator(trainerId: Int): Flow<List<RoutineEntity>>

    /**
     * Obtiene rutinas asignadas a un cliente específico por el entrenador.
     */
    @Query("SELECT * FROM routines WHERE creatorId = :trainerId AND ownerId = :clientId ORDER BY scheduledTime DESC")
    fun getRoutinesForClient(trainerId: Int, clientId: Int): Flow<List<RoutineEntity>>

    /**
     * Cuenta rutinas creadas por un entrenador.
     */
    @Query("SELECT COUNT(*) FROM routines WHERE creatorId = :trainerId")
    suspend fun countRoutinesByCreator(trainerId: Int): Int

    /**
     * Obtiene clientes únicos que tienen rutinas de este entrenador.
     */
    @Query("""
        SELECT DISTINCT ownerId FROM routines 
        WHERE creatorId = :trainerId AND ownerId IS NOT NULL
    """)
    suspend fun getClientsWithRoutines(trainerId: Int): List<Int>

    // ==================== RUTINAS - PROGRAMACIÓN/CALENDARIO ====================

    /**
     * Obtiene rutinas programadas para hoy.
     */
    @Query("""
        SELECT * FROM routines 
        WHERE (ownerId = :userId OR ownerId IS NULL)
        AND date(scheduledTime/1000, 'unixepoch') = date(:todayTimestamp/1000, 'unixepoch')
        ORDER BY scheduledTime ASC
    """)
    fun getRoutinesForToday(userId: Int, todayTimestamp: Long): Flow<List<RoutineEntity>>

    /**
     * Obtiene rutinas en un rango de fechas.
     */
    @Query("""
        SELECT * FROM routines 
        WHERE (ownerId = :userId OR ownerId IS NULL)
        AND scheduledTime BETWEEN :startDate AND :endDate
        ORDER BY scheduledTime ASC
    """)
    fun getRoutinesInDateRange(userId: Int, startDate: Long, endDate: Long): Flow<List<RoutineEntity>>

    /**
     * Obtiene rutinas programadas para los próximos N días.
     */
    @Query("""
        SELECT * FROM routines 
        WHERE (ownerId = :userId OR ownerId IS NULL)
        AND scheduledTime BETWEEN :now AND :futureDate
        ORDER BY scheduledTime ASC
    """)
    fun getUpcomingRoutines(userId: Int, now: Long, futureDate: Long): Flow<List<RoutineEntity>>

    // ==================== RUTINAS - BÚSQUEDA ====================

    /**
     * Busca rutinas por nombre.
     */
    @Query("""
        SELECT * FROM routines 
        WHERE (ownerId = :userId OR ownerId IS NULL)
        AND LOWER(name) LIKE '%' || LOWER(:query) || '%'
        ORDER BY name ASC
    """)
    suspend fun searchRoutines(userId: Int, query: String): List<RoutineEntity>

    /**
     * Busca rutinas por día.
     */
    @Query("""
        SELECT * FROM routines 
        WHERE (ownerId = :userId OR ownerId IS NULL)
        AND LOWER(dayInfo) LIKE '%' || LOWER(:day) || '%'
        ORDER BY name ASC
    """)
    fun getRoutinesByDay(userId: Int, day: String): Flow<List<RoutineEntity>>

    // ==================== DÍAS DE LA RUTINA ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: RoutineDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<RoutineDayEntity>)

    @Update
    suspend fun updateDay(day: RoutineDayEntity)

    @Query("SELECT * FROM routine_days WHERE routineId = :routineId ORDER BY dayOrder ASC")
    suspend fun getDaysOfRoutine(routineId: Int): List<RoutineDayEntity>

    @Query("SELECT * FROM routine_days WHERE routineId = :routineId ORDER BY dayOrder ASC")
    fun observeDaysOfRoutine(routineId: Int): Flow<List<RoutineDayEntity>>

    @Query("SELECT * FROM routine_days WHERE id = :dayId")
    suspend fun getDayById(dayId: Int): RoutineDayEntity?

    /** Fija la hora del recordatorio de un día concreto. */
    @Query("UPDATE routine_days SET reminderHour = :hora, reminderMinute = :minuto WHERE id = :dayId")
    suspend fun updateDayReminder(dayId: Int, hora: Int?, minuto: Int?)

    /** Todos los días con recordatorio propio de las rutinas de un usuario. */
    @Query("""
        SELECT routine_days.* FROM routine_days
        INNER JOIN routines ON routines.id = routine_days.routineId
        WHERE (routines.ownerId = :userId OR routines.ownerId IS NULL)
          AND routine_days.reminderHour IS NOT NULL
    """)
    suspend fun getDaysWithReminder(userId: Int): List<RoutineDayEntity>

    @Query("DELETE FROM routine_days WHERE routineId = :routineId")
    suspend fun deleteDaysOfRoutine(routineId: Int)

    // ==================== EJERCICIOS DEL DÍA ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(crossRef: RoutineExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercise WHERE dayId = :dayId AND exerciseId = :exerciseId")
    suspend fun removeExerciseFromDay(dayId: Int, exerciseId: Int)

    @Query("DELETE FROM routine_exercise WHERE dayId = :dayId")
    suspend fun clearDayExercises(dayId: Int)

    @Query("UPDATE routine_exercise SET `order` = :newOrder WHERE dayId = :dayId AND exerciseId = :exerciseId")
    suspend fun updateExerciseOrder(dayId: Int, exerciseId: Int, newOrder: Int)

    /** Ejercicios de un día concreto. */
    @Transaction
    @Query("""
        SELECT exercises.* FROM exercises
        INNER JOIN routine_exercise ON exercises.id = routine_exercise.exerciseId
        WHERE routine_exercise.dayId = :dayId
        ORDER BY routine_exercise.`order` ASC
    """)
    fun getExercisesForDay(dayId: Int): Flow<List<ExerciseEntity>>

    @Transaction
    @Query("""
        SELECT exercises.* FROM exercises
        INNER JOIN routine_exercise ON exercises.id = routine_exercise.exerciseId
        WHERE routine_exercise.dayId = :dayId
        ORDER BY routine_exercise.`order` ASC
    """)
    suspend fun getExercisesForDaySync(dayId: Int): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM routine_exercise WHERE dayId = :dayId")
    suspend fun countExercisesInDay(dayId: Int): Int

    @Query("SELECT EXISTS(SELECT 1 FROM routine_exercise WHERE dayId = :dayId AND exerciseId = :exerciseId)")
    suspend fun isExerciseInDay(dayId: Int, exerciseId: Int): Boolean
}
