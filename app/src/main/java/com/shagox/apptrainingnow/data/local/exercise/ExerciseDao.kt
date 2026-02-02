package com.shagox.apptrainingnow.data.local.exercise

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gestionar el catálogo de ejercicios.
 * 
 * Proporciona operaciones CRUD completas y queries especializadas para:
 * - Gestión del catálogo de ejercicios
 * - Búsqueda y filtrado por categoría
 * - Ejercicios del sistema vs personalizados
 */
@Dao
interface ExerciseDao {

    // ==================== OPERACIONES CRUD ====================

    /**
     * Inserta un ejercicio.
     * @return ID del ejercicio insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    /**
     * Inserta múltiples ejercicios.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    /**
     * Actualiza un ejercicio.
     */
    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    /**
     * Elimina un ejercicio.
     */
    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    /**
     * Elimina un ejercicio por ID.
     */
    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Int)

    // ==================== QUERIES GENERALES ====================

    /**
     * Obtiene un ejercicio por ID.
     */
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: Int): ExerciseEntity?

    /**
     * Obtiene un ejercicio como Flow.
     */
    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun observeExercise(exerciseId: Int): Flow<ExerciseEntity?>

    /**
     * Obtiene todos los ejercicios.
     */
    @Query("SELECT * FROM exercises ORDER BY category ASC, name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    /**
     * Obtiene todos los ejercicios de forma síncrona.
     */
    @Query("SELECT * FROM exercises ORDER BY category ASC, name ASC")
    suspend fun getAllExercisesSync(): List<ExerciseEntity>

    /**
     * Cuenta el total de ejercicios.
     */
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    // ==================== QUERIES POR CATEGORÍA ====================

    /**
     * Obtiene ejercicios por categoría.
     */
    @Query("SELECT * FROM exercises WHERE LOWER(category) = LOWER(:category) ORDER BY name ASC")
    fun getExercisesByCategory(category: String): Flow<List<ExerciseEntity>>

    /**
     * Obtiene todas las categorías disponibles.
     */
    @Query("SELECT DISTINCT category FROM exercises ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    /**
     * Cuenta ejercicios por categoría.
     */
    @Query("SELECT COUNT(*) FROM exercises WHERE LOWER(category) = LOWER(:category)")
    suspend fun countByCategory(category: String): Int

    /**
     * Obtiene el número de ejercicios por cada categoría.
     */
    @Query("""
        SELECT category, COUNT(*) as count 
        FROM exercises 
        GROUP BY category 
        ORDER BY count DESC
    """)
    fun getCategoryStats(): Flow<List<CategoryCount>>

    // ==================== BÚSQUEDA ====================

    /**
     * Busca ejercicios por nombre o descripción.
     */
    @Query("""
        SELECT * FROM exercises 
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' 
        OR LOWER(description) LIKE '%' || LOWER(:query) || '%'
        ORDER BY name ASC
    """)
    suspend fun searchExercises(query: String): List<ExerciseEntity>

    /**
     * Busca ejercicios por nombre o descripción como Flow.
     */
    @Query("""
        SELECT * FROM exercises 
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' 
        OR LOWER(description) LIKE '%' || LOWER(:query) || '%'
        ORDER BY name ASC
    """)
    fun searchExercisesFlow(query: String): Flow<List<ExerciseEntity>>

    // ==================== EJERCICIOS DEL SISTEMA VS PERSONALIZADOS ====================

    /**
     * Obtiene solo ejercicios del sistema (predeterminados).
     */
    @Query("SELECT * FROM exercises WHERE isSystemDefault = 1 ORDER BY category ASC, name ASC")
    fun getSystemExercises(): Flow<List<ExerciseEntity>>

    /**
     * Obtiene ejercicios personalizados (creados por usuarios).
     */
    @Query("SELECT * FROM exercises WHERE isSystemDefault = 0 ORDER BY category ASC, name ASC")
    fun getCustomExercises(): Flow<List<ExerciseEntity>>

    // ==================== QUERIES AVANZADAS ====================

    /**
     * Obtiene ejercicios con video disponible.
     */
    @Query("SELECT * FROM exercises WHERE videoUrl IS NOT NULL AND videoUrl != '' ORDER BY name ASC")
    fun getExercisesWithVideo(): Flow<List<ExerciseEntity>>

    /**
     * Obtiene ejercicios aleatorios de una categoría (para sugerencias).
     */
    @Query("SELECT * FROM exercises WHERE LOWER(category) = LOWER(:category) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomExercisesByCategory(category: String, limit: Int = 5): List<ExerciseEntity>

    /**
     * Obtiene los ejercicios más recientes (últimos añadidos).
     */
    @Query("SELECT * FROM exercises ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentExercises(limit: Int = 10): List<ExerciseEntity>

    /**
     * Verifica si existe un ejercicio con el nombre dado.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM exercises WHERE LOWER(name) = LOWER(:name))")
    suspend fun exerciseNameExists(name: String): Boolean

    /**
     * Obtiene un ejercicio por nombre exacto (para crear rutinas).
     */
    @Query("SELECT * FROM exercises WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getExerciseByName(name: String): ExerciseEntity?
}

/**
 * Clase de datos para contar ejercicios por categoría.
 */
data class CategoryCount(
    val category: String,
    val count: Int
)