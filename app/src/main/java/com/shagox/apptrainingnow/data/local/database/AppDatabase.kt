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
import com.shagox.apptrainingnow.data.local.notification.NotificationDao
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
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
        MessageEntity::class,
        NotificationEntity::class
    ],
    version = 3, // <--- VERSIÓN 3 (Importante)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao

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
                                prepopulateUsers(database.userDao())
                                prepopulateExercises(database.exerciseDao())
                                // 👇 AQUÍ AGREGAMOS LA LLAMADA A RUTINAS
                                prepopulateRoutines(database.routineDao())
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
                    // ID 1
                    UserEntity(role = "ADMIN", name = "Super", lastName = "Admin", email = "santiago@admin.tn", phone = "000", password = "admin", specializations = "Gestión Total"),
                    // ID 2
                    UserEntity(role = "TRAINER", name = "Santiago", lastName = "Coach", email = "santiago@coach.tn", phone = "+56912345678", password = "coach", specializations = "Hipertrofia"),
                    // ID 3
                    UserEntity(role = "USER", name = "Cliente", lastName = "Prueba", email = "cliente@gmail.com", phone = "+56987654321", password = "123", height = 175f, weight = 70f)
                )
                users.forEach { dao.insertUser(it) }
            }
        }

        private suspend fun prepopulateExercises(dao: ExerciseDao) {
            val exercises = listOf(
                ExerciseEntity(name = "Press Banca", category = "Pectorales", description = "Acostado...", videoUrl = "youtube.com"), // ID 1
                ExerciseEntity(name = "Sentadilla", category = "Piernas", description = "Barra...", videoUrl = "youtube.com"), // ID 2
                ExerciseEntity(name = "Dominadas", category = "Espalda", description = "Agarre...", videoUrl = "youtube.com"), // ID 3
                ExerciseEntity(name = "Curl de Bíceps", category = "Brazos", description = "Mancuernas...", videoUrl = "youtube.com"), // ID 4
                ExerciseEntity(name = "Press Militar", category = "Hombros", description = "Sentado...", videoUrl = "youtube.com") // ID 5
            )
            dao.insertExercises(exercises)
        }

        // 👇 ESTA ES LA MAGIA QUE TE FALTABA
        private suspend fun prepopulateRoutines(dao: RoutineDao) {

            // --- 1. RUTINA GLOBAL (Para todos) ---
            val rutinaGlobal = RoutineEntity(
                name = "Básicos para Todos",
                dayInfo = "Cualquier día",
                ownerId = null, // <--- NULL significa que es pública
                creatorId = 1   // Creada por Admin
            )
            // Insertamos y obtenemos el ID (Será ID 1)
            val globalId = dao.insertRoutine(rutinaGlobal).toInt()

            // Le ponemos Sentadilla (2) y Dominadas (3)
            dao.insertRoutineExercise(RoutineExerciseEntity(routineId = globalId, exerciseId = 2, order = 1))
            dao.insertRoutineExercise(RoutineExerciseEntity(routineId = globalId, exerciseId = 3, order = 2))


            // --- 2. RUTINA PRIVADA (Para Cliente Prueba) ---
            val rutinaCliente = RoutineEntity(
                name = "Pecho y Hombros",
                dayInfo = "Lunes 26",
                ownerId = 3,    // <--- Solo para el usuario ID 3
                creatorId = 2,  // Creada por Coach ID 2
                scheduledTime = System.currentTimeMillis() + 86400000 // Mañana
            )
            // Insertamos y obtenemos el ID (Será ID 2)
            val clienteId = dao.insertRoutine(rutinaCliente).toInt()

            // Le ponemos Press Banca (1) y Press Militar (5)
            dao.insertRoutineExercise(RoutineExerciseEntity(routineId = clienteId, exerciseId = 1, order = 1))
            dao.insertRoutineExercise(RoutineExerciseEntity(routineId = clienteId, exerciseId = 5, order = 2))
        }
    }
}