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
     * Obtiene solo las rutinas propias del usuario (sin globales).
     */
    @Query("SELECT * FROM routines WHERE ownerId = :userId ORDER BY scheduledTime DESC")
    fun getUserOwnRoutines(userId: Int): Flow<List<RoutineEntity>>

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
     * Obtiene todos los "días" de una rutina por su nombre (misma cabecera).
     * Cada fila es un día con su dayInfo (ej. "Lunes - Empuje").
     * Usado para: Lista de días → Vista de detalle de ejercicios.
     */
    @Query("""
        SELECT * FROM routines 
        WHERE name = :routineName AND (ownerId = :userId OR ownerId IS NULL)
        ORDER BY id ASC
    """)
    fun getDaysForRoutineName(routineName: String, userId: Int): Flow<List<RoutineEntity>>

    /**
     * Versión síncrona para construir RoutineWithDays.
     */
    @Query("""
        SELECT * FROM routines 
        WHERE name = :routineName AND (ownerId = :userId OR ownerId IS NULL)
        ORDER BY id ASC
    """)
    suspend fun getDaysForRoutineNameSync(routineName: String, userId: Int): List<RoutineEntity>

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

    // ==================== ROUTINE-EXERCISE CROSSREF - CRUD ====================

    /**
     * Inserta una relación rutina-ejercicio.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(crossRef: RoutineExerciseEntity)

    /**
     * Inserta múltiples relaciones rutina-ejercicio.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    /**
     * Elimina un ejercicio de una rutina.
     */
    @Query("DELETE FROM routine_exercise WHERE routineId = :routineId AND exerciseId = :exerciseId")
    suspend fun removeExerciseFromRoutine(routineId: Int, exerciseId: Int)

    /**
     * Elimina todos los ejercicios de una rutina.
     */
    @Query("DELETE FROM routine_exercise WHERE routineId = :routineId")
    suspend fun clearRoutineExercises(routineId: Int)

    /**
     * Actualiza el orden de un ejercicio en la rutina.
     */
    @Query("UPDATE routine_exercise SET `order` = :newOrder WHERE routineId = :routineId AND exerciseId = :exerciseId")
    suspend fun updateExerciseOrder(routineId: Int, exerciseId: Int, newOrder: Int)

    // ==================== ROUTINE-EXERCISE - QUERIES ====================

    /**
     * Obtiene los ejercicios de una rutina con detalles completos.
     */
    @Transaction
    @Query("""
        SELECT exercises.* FROM exercises 
        INNER JOIN routine_exercise ON exercises.id = routine_exercise.exerciseId 
        WHERE routine_exercise.routineId = :routineId 
        ORDER BY routine_exercise.`order` ASC
    """)
    fun getExercisesForRoutine(routineId: Int): Flow<List<ExerciseEntity>>

    /**
     * Obtiene los ejercicios de una rutina de forma síncrona.
     */
    @Transaction
    @Query("""
        SELECT exercises.* FROM exercises 
        INNER JOIN routine_exercise ON exercises.id = routine_exercise.exerciseId 
        WHERE routine_exercise.routineId = :routineId 
        ORDER BY routine_exercise.`order` ASC
    """)
    suspend fun getExercisesForRoutineSync(routineId: Int): List<ExerciseEntity>

    /**
     * Cuenta ejercicios en una rutina.
     */
    @Query("SELECT COUNT(*) FROM routine_exercise WHERE routineId = :routineId")
    suspend fun countExercisesInRoutine(routineId: Int): Int

    /**
     * Verifica si un ejercicio está en una rutina.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM routine_exercise WHERE routineId = :routineId AND exerciseId = :exerciseId)")
    suspend fun isExerciseInRoutine(routineId: Int, exerciseId: Int): Boolean

    /**
     * Obtiene las rutinas que contienen un ejercicio específico.
     */
    @Query("""
        SELECT routines.* FROM routines
        INNER JOIN routine_exercise ON routines.id = routine_exercise.routineId
        WHERE routine_exercise.exerciseId = :exerciseId
    """)
    fun getRoutinesContainingExercise(exerciseId: Int): Flow<List<RoutineEntity>>

    /**
     * Obtiene el detalle de la relación rutina-ejercicio.
     */
    @Query("SELECT * FROM routine_exercise WHERE routineId = :routineId ORDER BY `order` ASC")
    suspend fun getRoutineExerciseDetails(routineId: Int): List<RoutineExerciseEntity>

    // ==================== ESTADÍSTICAS ====================

    /**
     * Obtiene estadísticas de rutinas de un usuario.
     */
    @Query("""
        SELECT 
            COUNT(*) as totalRoutines,
            SUM(CASE WHEN ownerId IS NULL THEN 1 ELSE 0 END) as globalRoutines,
            SUM(CASE WHEN ownerId = :userId THEN 1 ELSE 0 END) as personalRoutines
        FROM routines 
        WHERE ownerId = :userId OR ownerId IS NULL
    """)
    suspend fun getRoutineStats(userId: Int): RoutineStats
}

/**
 * Clase de datos para estadísticas de rutinas.
 */
data class RoutineStats(
    val totalRoutines: Int,
    val globalRoutines: Int,
    val personalRoutines: Int
)