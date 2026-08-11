package com.shagox.apptrainingnow

import android.app.Application
import android.util.Log
import com.shagox.apptrainingnow.data.local.database.AppDatabase
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.ExerciseApiRepository
import com.shagox.apptrainingnow.data.repository.ExerciseRepository
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.data.repository.INotificationRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.data.repository.NotificationApiRepository
import com.shagox.apptrainingnow.data.repository.NotificationRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.UserApiRepository
import com.shagox.apptrainingnow.data.repository.UserRepository
import com.shagox.apptrainingnow.data.repository.WorkoutRepository

class TrainingNowApplication : Application() {
    companion object {
        private const val TAG = "TrainingNowApp"
        /** true = usar API trainingnowapi (Spring Boot); false = usar Room local. */
        private const val USE_API = true
    }

    private var _database: AppDatabase? = null
    private var _userRepository: IUserRepository? = null
    private var _chatRepository: ChatRepository? = null
    private var _routineRepository: RoutineRepository? = null
    private var _trainerRepository: TrainerRepository? = null
    private var _progressRepository: ProgressRepository? = null
    private var _notificationRepository: INotificationRepository? = null
    private var _exerciseRepository: IExerciseRepository? = null
    private var _workoutRepository: WorkoutRepository? = null

    val database: AppDatabase
        get() = _database ?: run {
            AppDatabase.getInstance(this).also { _database = it }
        }

    val userRepository: IUserRepository
        get() = _userRepository ?: if (USE_API) {
            UserApiRepository().also { _userRepository = it }
        } else {
            UserRepository(database.userDao()).also { _userRepository = it }
        }
    val chatRepository: ChatRepository
        get() = _chatRepository ?: ChatRepository(database.chatDao()).also { _chatRepository = it }
    val routineRepository: RoutineRepository
        get() = _routineRepository ?: RoutineRepository(database.routineDao(), database.exerciseDao()).also { _routineRepository = it }
    val trainerRepository: TrainerRepository
        get() = _trainerRepository ?: TrainerRepository(
            trainerClientDao = database.trainerClientDao(),
            routineDao = database.routineDao()
        ).also { _trainerRepository = it }
    val progressRepository: ProgressRepository
        get() = _progressRepository ?: ProgressRepository(database.progressDao()).also { _progressRepository = it }
    val notificationRepository: INotificationRepository
        get() = _notificationRepository ?: if (USE_API) {
            NotificationApiRepository().also { _notificationRepository = it }
        } else {
            NotificationRepository(database.notificationDao()).also { _notificationRepository = it }
        }
    val workoutRepository: WorkoutRepository
        get() = _workoutRepository ?: WorkoutRepository(database.workoutDao()).also { _workoutRepository = it }
    val exerciseRepository: IExerciseRepository
        get() = _exerciseRepository ?: if (USE_API) {
            ExerciseApiRepository().also { _exerciseRepository = it }
        } else {
            ExerciseRepository(database.exerciseDao()).also { _exerciseRepository = it }
        }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "Excepción no capturada", exception)
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)
        }
        // Inicializar la base de datos aquí para no bloquear el primer frame en MainActivity
        _database = AppDatabase.getInstance(this)
        // Repuebla ejercicios y rutinas recomendadas si la base quedó vacía
        AppDatabase.asegurarDatosBase(_database!!)
    }
}
