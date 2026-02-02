package com.shagox.apptrainingnow.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shagox.apptrainingnow.ui.screen.WelcomeScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.NotificationRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.UserRepository
import com.shagox.apptrainingnow.ui.components.BottomNavigationBarTN
import com.shagox.apptrainingnow.ui.screen.ChatScreen
import com.shagox.apptrainingnow.ui.screen.CreateRoutineScreen
import com.shagox.apptrainingnow.ui.screen.NotificationsScreen
import com.shagox.apptrainingnow.ui.screen.ProfileScreen
import com.shagox.apptrainingnow.ui.screen.RoutineActiveScreen
import com.shagox.apptrainingnow.ui.screen.UserChatsScreen
import com.shagox.apptrainingnow.ui.screen.UserRoutinesScreen
import com.shagox.apptrainingnow.ui.screen.coach.ClientDetailScreen
import com.shagox.apptrainingnow.ui.screen.coach.CoachClientsScreen
import com.shagox.apptrainingnow.ui.screen.coach.CoachRoutinesScreen
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel
import com.shagox.apptrainingnow.ui.viewmodel.CoachViewModel
import com.shagox.apptrainingnow.ui.viewmodel.CoachViewModelFactory

/**
 * Grafo de navegación principal de la aplicación.
 * 
 * Gestiona la navegación entre todas las pantallas según el rol del usuario:
 * - USER: Pantallas de cliente (chats con entrenadores, rutinas, biblioteca)
 * - TRAINER: Pantallas de entrenador (gestión de clientes, rutinas, mensajes)
 * - ADMIN: Mismas pantallas que TRAINER con permisos adicionales
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String,
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    routineRepository: RoutineRepository,
    trainerRepository: TrainerRepository? = null,
    progressRepository: ProgressRepository? = null,
    notificationRepository: NotificationRepository? = null
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    
    val loginState by authViewModel.loginState.collectAsState()
    val loggedUser = loginState.loggedUser
    val userRole = loggedUser?.role ?: "USER"
    val currentUserId = loggedUser?.id ?: 0

    val context = LocalContext.current
    val safeStartDestination = startDestination.ifBlank { Route.Welcome.path }

    // Ocultar barra en Welcome y pantallas de detalle; RoutineActive muestra barra para cambiar de pantalla
    val hideBottomBarRoutes = listOf(Route.Welcome.path)
    val shouldHideBottomBar = currentRoute in hideBottomBarRoutes ||
            currentRoute?.startsWith("chat_detail") == true ||
            currentRoute?.startsWith("client_detail") == true ||
            currentRoute?.startsWith("create_goal") == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Siempre emitir algo para evitar restricciones indefinidas en measure
            if (shouldHideBottomBar) {
                Box(modifier = Modifier.fillMaxWidth().height(0.dp))
            } else {
                BottomNavigationBarTN(
                    navController = navController,
                    userRole = userRole,
                    startDestinationRoute = safeStartDestination
                )
            }
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = safeStartDestination,
            modifier = Modifier.padding(padding)
        ) {

            // ==================== BIENVENIDA (solo primera vez) ====================

            composable(Route.Welcome.path) {
                WelcomeScreen(
                    onComenzar = {
                        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("has_seen_welcome", true)
                            .apply()
                        navController.navigate(Route.UserRoutines.path) {
                            popUpTo(Route.Welcome.path) { inclusive = true }
                        }
                    }
                )
            }

            // ==================== PANTALLAS DE USUARIO ====================

            composable(Route.UserChats.path) {
                UserChatsScreen(
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    currentUserId = currentUserId,
                    onNavigateToChat = { trainerId ->
                        navController.navigate(Route.ChatDetail.createRoute(trainerId))
                    }
                )
            }

            composable(Route.Library.path) {
                Text(
                    text = "Biblioteca de Ejercicios",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            composable(Route.UserRoutines.path) {
                UserRoutinesScreen(
                    routineRepository = routineRepository,
                    userId = currentUserId,
                    onCreateRoutine = {
                        navController.navigate(Route.CreateRoutine.createRoute())
                    },
                    onRoutineClick = { routineId ->
                        val route = Route.RoutineActive.createRoute(routineId)
                        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("active_routine_route", route)
                            .apply()
                        navController.navigate(route)
                    }
                )
            }

            // Pantalla de rutina activa: Lista de días → Vista de detalle de ejercicios
            composable(
                route = Route.RoutineActive.path,
                arguments = listOf(navArgument("routineId") { type = NavType.IntType })
            ) { backStackEntry ->
                val routineId = backStackEntry.arguments?.getInt("routineId") ?: 0
                val routine by routineRepository.observeRoutine(routineId).collectAsState(initial = null)
                val routineName = routine?.name ?: "Rutina"
                val prefs = context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                RoutineActiveScreen(
                    routineRepository = routineRepository,
                    userId = currentUserId,
                    routineId = routineId,
                    initialRoutineName = routineName,
                    onBack = {
                        prefs.edit().remove("active_routine_route").apply()
                        navController.popBackStack()
                        if (navController.currentBackStackEntry == null) {
                            navController.navigate(Route.UserRoutines.path) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // ==================== PANTALLAS DE ENTRENADOR ====================

            composable(Route.CoachClients.path) {
                if (trainerRepository != null && progressRepository != null) {
                    val coachViewModel: CoachViewModel = viewModel(
                        factory = CoachViewModelFactory(
                            trainerRepository = trainerRepository,
                            progressRepository = progressRepository,
                            userRepository = userRepository,
                            trainerId = currentUserId
                        )
                    )
                    
                    CoachClientsScreen(
                        viewModel = coachViewModel,
                        onClientClick = { clientId ->
                            coachViewModel.selectClient(
                                coachViewModel.activeClients.value.find { it.id == clientId }
                                    ?: return@CoachClientsScreen
                            )
                            navController.navigate(Route.ClientDetail.createRoute(clientId))
                        },
                        onChatClick = { clientId ->
                            navController.navigate(Route.ChatDetail.createRoute(clientId))
                        }
                    )
                } else {
                    LoadingScreen()
                }
            }

            composable(Route.CoachRoutines.path) {
                if (trainerRepository != null && progressRepository != null) {
                    val coachViewModel: CoachViewModel = viewModel(
                        factory = CoachViewModelFactory(
                            trainerRepository = trainerRepository,
                            progressRepository = progressRepository,
                            userRepository = userRepository,
                            trainerId = currentUserId
                        )
                    )
                    
                    CoachRoutinesScreen(
                        viewModel = coachViewModel,
                        onCreateRoutine = {
                            navController.navigate(Route.CreateRoutine.createRoute())
                        },
                        onRoutineClick = { routineId ->
                            // TODO: Navegar a detalle de rutina
                        }
                    )
                } else {
                    LoadingScreen()
                }
            }

            composable(Route.CoachChats.path) {
                // Chats del entrenador (muestra clientes con mensajes)
                UserChatsScreen(
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    currentUserId = currentUserId,
                    onNavigateToChat = { clientId ->
                        navController.navigate(Route.ChatDetail.createRoute(clientId))
                    }
                )
            }

            // Detalle del cliente (para entrenadores)
            composable(
                route = Route.ClientDetail.path,
                arguments = listOf(navArgument("clientId") { type = NavType.IntType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                
                if (trainerRepository != null && progressRepository != null) {
                    val coachViewModel: CoachViewModel = viewModel(
                        factory = CoachViewModelFactory(
                            trainerRepository = trainerRepository,
                            progressRepository = progressRepository,
                            userRepository = userRepository,
                            trainerId = currentUserId
                        )
                    )
                    
                    ClientDetailScreen(
                        viewModel = coachViewModel,
                        clientId = clientId,
                        onBack = { navController.popBackStack() },
                        onChatClick = {
                            navController.navigate(Route.ChatDetail.createRoute(clientId))
                        },
                        onCreateRoutine = {
                            navController.navigate(Route.CreateRoutine.createRoute(clientId))
                        },
                        onCreateGoal = {
                            navController.navigate(Route.CreateGoal.createRoute(clientId))
                        }
                    )
                } else {
                    LoadingScreen()
                }
            }

            // ==================== PANTALLAS COMPARTIDAS ====================

            composable(
                route = Route.ChatDetail.path,
                arguments = listOf(navArgument("otherId") { type = NavType.IntType })
            ) { backStackEntry ->
                val otherId = backStackEntry.arguments?.getInt("otherId") ?: 0
                ChatScreen(
                    currentUserId = currentUserId,
                    trainerId = otherId,
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.Notifications.path) {
                if (notificationRepository != null) {
                    NotificationsScreen(
                        notificationRepository = notificationRepository,
                        userId = currentUserId
                    )
                } else {
                    Text(
                        text = "Notificaciones",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            composable(Route.Profile.path) {
                ProfileScreen(authViewModel = authViewModel)
            }

            // ==================== CREACIÓN (Para entrenadores) ====================

            composable(
                route = Route.CreateRoutine.path,
                arguments = listOf(
                    navArgument("clientId") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val clientIdStr = backStackEntry.arguments?.getString("clientId")
                val clientId = clientIdStr?.toIntOrNull()
                val ownerId = clientId ?: currentUserId

                CreateRoutineScreen(
                    onBack = { navController.popBackStack() },
                    onSaveRoutine = { name, days ->
                        CoroutineScope(Dispatchers.IO).launch {
                            routineRepository.savePersonalRoutine(ownerId, name, days)
                            withContext(Dispatchers.Main) { navController.popBackStack() }
                        }
                    }
                )
            }

            composable(
                route = Route.CreateGoal.path,
                arguments = listOf(navArgument("clientId") { type = NavType.IntType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getInt("clientId") ?: 0
                Text(
                    text = "Crear Objetivo para Cliente #$clientId",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/**
 * Pantalla de carga mientras se inicializan los repositorios.
 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
