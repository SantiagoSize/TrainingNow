package com.shagox.apptrainingnow

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.shagox.apptrainingnow.navigation.AppNavGraph
import com.shagox.apptrainingnow.navigation.Route
import com.shagox.apptrainingnow.ui.theme.AppTrainingNowTheme
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TrainingNowApplication
        // Si se abrió desde la notificación del recordatorio, ir directo a esa rutina
        val routineIdDesdeNotificacion = intent?.getIntExtra(ReminderReceiver.EXTRA_ROUTINE_ID, 0) ?: 0
        if (routineIdDesdeNotificacion > 0) {
            sharedPreferences.edit()
                .putString("active_routine_route", Route.RoutineActive.createRoute(routineIdDesdeNotificacion))
                .apply()
        }

        // La pantalla de inicio depende del rol de la sesión guardada: un admin o entrenador
        // no tiene rutinas propias, así que "active_routine_route" (guardado cada vez que
        // CUALQUIER usuario entra a una rutina, para restaurarla si Android mata el proceso)
        // NO debe usarse si la sesión actual es de personal — si no, un admin podía quedar
        // atrapado para siempre en la última rutina que haya quedado guardada en el teléfono.
        // Se lee la sesión guardada de forma síncrona (SessionManager, SharedPreferences)
        // porque en este punto el AuthViewModel todavía no existe.
        val sesionGuardada = com.shagox.apptrainingnow.data.local.user.SessionManager.cargarUsuario(this)
        val hasSeenWelcome = sharedPreferences.getBoolean("has_seen_welcome", false)
        val startDestination = when {
            // Notificación de recordatorio tocada en este instante: siempre tiene prioridad.
            routineIdDesdeNotificacion > 0 -> Route.RoutineActive.createRoute(routineIdDesdeNotificacion)
            !hasSeenWelcome -> Route.Welcome.path
            sesionGuardada?.role == "ADMIN" -> Route.AdminChats.path
            sesionGuardada?.role == "TRAINER" || sesionGuardada?.role == "COACH" -> Route.CoachClients.path
            else -> sharedPreferences.getString("active_routine_route", null) ?: Route.UserRoutines.path
        }
        setContent {
            // Tema elegido por el usuario (Ajustes); se aplica a toda la app
            var temaClaro by remember {
                mutableStateOf(com.shagox.apptrainingnow.ui.theme.ThemePreference.esTemaClaro(this))
            }
            AppTrainingNowTheme(temaClaro = temaClaro) {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(app.userRepository, app)
                )
                AppNavGraph(
                    navController = navController,
                    authViewModel = authViewModel,
                    startDestination = startDestination,
                    userRepository = app.userRepository,
                    chatRepository = app.chatRepository,
                    routineRepository = app.routineRepository,
                    trainerRepository = app.trainerRepository,
                    progressRepository = app.progressRepository,
                    notificationRepository = app.notificationRepository,
                    exerciseRepository = app.exerciseRepository,
                    workoutRepository = app.workoutRepository,
                    temaClaro = temaClaro,
                    onCambiarTema = { claro ->
                        temaClaro = claro
                        com.shagox.apptrainingnow.ui.theme.ThemePreference.guardar(this, claro)
                    }
                )
            }
        }
    }

    private val sharedPreferences
        get() = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
}

@Composable
fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
