package com.shagox.apptrainingnow.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shagox.apptrainingnow.data.local.chat.ChatDao
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.local.exercise.ExerciseDao
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineDao
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.local.routine.RoutineExerciseEntity
import com.shagox.apptrainingnow.data.local.user.UserDao
import com.shagox.apptrainingnow.data.local.user.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "training_now.db"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                // 1. Creamos Usuarios
                                prepopulateUsers(database.userDao())
                                // 2. Creamos Ejercicios (¡AHORA SÍ SE EJECUTA!)
                                prepopulateExercises(database.exerciseDao())
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateUsers(dao: UserDao) {
            if (dao.count() == 0) {
                val users = listOf(
                    UserEntity(
                        role = "ADMIN",
                        name = "Super",
                        lastName = "Admin",
                        email = "santiago@admin.tn",
                        phone = "000",
                        password = "admin",
                        specializations = "Gestión Total"
                    ),
                    UserEntity(
                        role = "TRAINER",
                        name = "Santiago",
                        lastName = "Coach",
                        email = "santiago@coach.tn",
                        phone = "+56912345678",
                        password = "coach",
                        specializations = "Hipertrofia"
                    ),
                    UserEntity(
                        role = "USER",
                        name = "Cliente",
                        lastName = "Prueba",
                        email = "cliente@gmail.com",
                        phone = "+56987654321",
                        password = "123",
                        height = 175f,
                        weight = 70f
                    )
                )
                users.forEach { dao.insertUser(it) }
            }
        }

        private suspend fun prepopulateExercises(dao: ExerciseDao) {
            val exercises = listOf(
                ExerciseEntity(name = "Press Banca", category = "Pectorales", description = "Acostado en banco plano...", videoUrl = "youtube.com"),
                ExerciseEntity(name = "Sentadilla", category = "Piernas", description = "Barra trasnuca...", videoUrl = "youtube.com"),
                ExerciseEntity(name = "Dominadas", category = "Espalda", description = "Agarre prono...", videoUrl = "youtube.com"),
                ExerciseEntity(name = "Curl de Bíceps", category = "Brazos", description = "Con mancuernas...", videoUrl = "youtube.com")
            )
            dao.insertExercises(exercises)
        }
    }
}