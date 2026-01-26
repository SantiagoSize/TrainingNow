package com.shagox.apptrainingnow.data.local.routine

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    // Insertar Rutina y devolver su ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    // 👇 ESTO FALTABA (Singular): Para insertar ejercicio por ejercicio en el Seeding
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercise(crossRef: RoutineExerciseEntity)

    // (Opcional) Para insertar varios de golpe
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineExercises(exercises: List<RoutineExerciseEntity>)

    // 👇 ESTO FALTABA (Vital): Une la tabla Rutinas con Ejercicios
    // Cambiamos "SELECT *" por "SELECT exercises.*"
    @Transaction
    @Query("""
        SELECT exercises.* FROM exercises 
        INNER JOIN routine_exercise ON exercises.id = routine_exercise.exerciseId 
        WHERE routine_exercise.routineId = :routineId 
        ORDER BY routine_exercise.`order` ASC
    """)
    fun getExercisesForRoutine(routineId: Int): Flow<List<ExerciseEntity>>

    // Traer mis rutinas + las públicas (Globales)
    @Query("SELECT * FROM routines WHERE ownerId = :userId OR ownerId IS NULL")
    fun getMyRoutines(userId: Int): Flow<List<RoutineEntity>>
}