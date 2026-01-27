package com.shagox.apptrainingnow

import android.app.Application
import android.util.Log
import com.shagox.apptrainingnow.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrainingNowApplication : Application() {
    companion object {
        private const val TAG = "TrainingNowApp"
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate iniciado")
        
        // Inicializar la base de datos en background ANTES de que MainActivity se cree
        applicationScope.launch {
            try {
                Log.d(TAG, "Pre-inicializando base de datos...")
                AppDatabase.getInstance(this@TrainingNowApplication)
                Log.d(TAG, "Base de datos pre-inicializada")
            } catch (e: Exception) {
                Log.e(TAG, "Error al pre-inicializar base de datos", e)
            }
        }
        
        // Capturar excepciones no manejadas
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "EXCEPCIÓN NO CAPTURADA", exception)
            Log.e(TAG, "Thread: ${thread.name}")
            exception.printStackTrace()
            // Llamar al handler por defecto
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        Log.d(TAG, "Application onCreate completado")
    }
}
