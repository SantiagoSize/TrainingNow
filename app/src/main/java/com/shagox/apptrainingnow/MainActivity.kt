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
        val startDestination = sharedPreferences.getString("active_routine_route", null)
            ?: sharedPreferences.getBoolean("has_seen_welcome", false).let { seen ->
                if (seen) Route.UserRoutines.path else Route.Welcome.path
            }
        setContent {
            AppTrainingNowTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(app.userRepository)
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
                    workoutRepository = app.workoutRepository
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
