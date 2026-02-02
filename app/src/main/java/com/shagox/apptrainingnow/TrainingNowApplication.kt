package com.shagox.apptrainingnow

import android.app.Application
import android.util.Log
import com.shagox.apptrainingnow.data.local.database.AppDatabase
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.ExerciseRepository
import com.shagox.apptrainingnow.data.repository.NotificationRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.UserRepository

class TrainingNowApplication : Application() {
    companion object {
        private const val TAG = "TrainingNowApp"
    }

    private var _database: AppDatabase? = null
    private var _userRepository: UserRepository? = null
    private var _chatRepository: ChatRepository? = null
    private var _routineRepository: RoutineRepository? = null
    private var _trainerRepository: TrainerRepository? = null
    private var _progressRepository: ProgressRepository? = null
    private var _notificationRepository: NotificationRepository? = null
    private var _exerciseRepository: ExerciseRepository? = null

    val database: AppDatabase
        get() = _database ?: run {
            AppDatabase.getInstance(this).also { _database = it }
        }

    val userRepository: UserRepository
        get() = _userRepository ?: UserRepository(database.userDao()).also { _userRepository = it }
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
    val notificationRepository: NotificationRepository
        get() = _notificationRepository ?: NotificationRepository(database.notificationDao()).also { _notificationRepository = it }
    val exerciseRepository: ExerciseRepository
        get() = _exerciseRepository ?: ExerciseRepository(database.exerciseDao()).also { _exerciseRepository = it }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "Excepción no capturada", exception)
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)
        }
        // Inicializar la base de datos aquí para no bloquear el primer frame en MainActivity
        _database = AppDatabase.getInstance(this)
    }
}
