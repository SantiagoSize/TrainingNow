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
    exportSchema = false // Lo dejamos en false para evitar configurar rutas extras por ahora
)
abstract class AppDatabase : RoomDatabase() {

    // 1. Exponer todos los DAOs de tu sistema
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
                    // 2. Aquí está la magia del profe: Callback para datos iniciales
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Lanzamos una corutina para no bloquear la UI
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                prepopulateUsers(database.userDao())
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

        // --- Funciones de llenado de datos (Seeding) ---

        private suspend fun prepopulateUsers(dao: UserDao) {
            // Verificamos si ya hay usuarios usando count() (Debemos agregarlo al DAO)
            if (dao.count() == 0) {
                val users = listOf(
                    UserEntity(
                        role = "TRAINER",
                        name = "Santiago",
                        lastName = "Serrano",
                        email = "admin@trainingnow.com",
                        phone = "+56912345678",
                        specializations = "Hipertrofia, Fuerza"
                    ),
                    UserEntity(
                        role = "USER",
                        name = "Cliente",
                        lastName = "Prueba",
                        email = "cliente@gmail.com",
                        phone = "+56987654321",
                        height = 175f,
                        weight = 70f
                    )
                )
                users.forEach { dao.insertUser(it) }
            }
        }

        private suspend fun prepopulateExercises(dao: ExerciseDao) {
            // Llenamos la biblioteca si está vacía
            // Nota: Necesitarás agregar un método count() en ExerciseDao similar al de User
            // Por ahora insertamos directo una lista pequeña
            val exercises = listOf(
                ExerciseEntity(name = "Press Banca", category = "Pectorales", description = "Acostado en banco plano...", videoUrl = "youtube.com/xyz"),
                ExerciseEntity(name = "Sentadilla", category = "Piernas", description = "Barra en la espalda...", videoUrl = "youtube.com/abc"),
                ExerciseEntity(name = "Dominadas", category = "Espalda", description = "Colgado de la barra...", videoUrl = "youtube.com/def")
            )
            dao.insertExercises(exercises)
        }
    }
}