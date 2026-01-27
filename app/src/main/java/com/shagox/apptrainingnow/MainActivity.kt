package com.shagox.apptrainingnow

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.shagox.apptrainingnow.data.local.database.AppDatabase
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.UserRepository
import com.shagox.apptrainingnow.navigation.AppNavGraph
import com.shagox.apptrainingnow.ui.theme.AppTrainingNowTheme
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Activity principal de TrainingNow.
 * 
 * Inicializa la base de datos y todos los repositorios necesarios,
 * luego configura la navegación principal de la aplicación.
 */
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTrainingNowTheme {
                var isInitialized by remember { mutableStateOf(false) }
                var userRepository by remember { mutableStateOf<UserRepository?>(null) }
                var chatRepository by remember { mutableStateOf<ChatRepository?>(null) }
                var trainerRepository by remember { mutableStateOf<TrainerRepository?>(null) }
                var progressRepository by remember { mutableStateOf<ProgressRepository?>(null) }
                var factory by remember { mutableStateOf<AuthViewModelFactory?>(null) }
                var error by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        try {
                            Log.d(TAG, "Obteniendo base de datos...")
                            // Esperar un poco para que Application termine de inicializar
                            kotlinx.coroutines.delay(100)
                            val database = AppDatabase.getInstance(this@MainActivity)
                            Log.d(TAG, "Base de datos obtenida")
                            
                            // Crear todos los repositorios
                            val userRepo = UserRepository(database.userDao())
                            val chatRepo = ChatRepository(database.chatDao())
                            val trainerRepo = TrainerRepository(
                                trainerClientDao = database.trainerClientDao(),
                                routineDao = database.routineDao()
                            )
                            val progressRepo = ProgressRepository(database.progressDao())
                            val fact = AuthViewModelFactory(userRepo)
                            
                            Log.d(TAG, "Repositorios creados exitosamente")
                            
                            withContext(Dispatchers.Main) {
                                userRepository = userRepo
                                chatRepository = chatRepo
                                trainerRepository = trainerRepo
                                progressRepository = progressRepo
                                factory = fact
                                isInitialized = true
                                Log.d(TAG, "Inicialización completada")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al inicializar", e)
                            e.printStackTrace()
                            withContext(Dispatchers.Main) {
                                error = e.message ?: "Error desconocido"
                            }
                        }
                    }
                }

                when {
                    error != null -> {
                        ErrorScreen(error = error!!)
                    }
                    !isInitialized || userRepository == null || chatRepository == null || factory == null -> {
                        LoadingScreen()
                    }
                    else -> {
                        val navController = rememberNavController()
                        val authViewModel: AuthViewModel = viewModel(factory = factory!!)

                        AppNavGraph(
                            navController = navController,
                            authViewModel = authViewModel,
                            userRepository = userRepository!!,
                            chatRepository = chatRepository!!,
                            trainerRepository = trainerRepository,
                            progressRepository = progressRepository
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.shagox.apptrainingnow.ui.theme.NegroFondo),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = VerdeTN)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Error al inicializar",
                color = Color.Red,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}