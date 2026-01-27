package com.shagox.apptrainingnow.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.UserRepository
import com.shagox.apptrainingnow.ui.components.BottomNavigationBarTN
import com.shagox.apptrainingnow.ui.screen.ChatScreen
import com.shagox.apptrainingnow.ui.screen.LoginScreenVm
import com.shagox.apptrainingnow.ui.screen.RegisterScreen
import com.shagox.apptrainingnow.ui.screen.UserChatsScreen
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
    userRepository: UserRepository,
    chatRepository: ChatRepository,
    trainerRepository: TrainerRepository? = null,
    progressRepository: ProgressRepository? = null
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    
    // Obtener usuario logueado del AuthViewModel
    val loginState by authViewModel.loginState.collectAsState()
    val loggedUser = loginState.loggedUser
    val userRole = loggedUser?.role ?: "USER"
    val currentUserId = loggedUser?.id ?: 0

    // Rutas donde NO mostrar la barra de navegación
    val hideBottomBarRoutes = listOf(
        Route.Login.path,
        Route.Register.path
    )
    val shouldHideBottomBar = currentRoute in hideBottomBarRoutes ||
            currentRoute?.startsWith("chat_detail") == true ||
            currentRoute?.startsWith("client_detail") == true ||
            currentRoute?.startsWith("create_") == true

    Scaffold(
        bottomBar = {
            if (!shouldHideBottomBar && loggedUser != null) {
                BottomNavigationBarTN(
                    navController = navController,
                    userRole = userRole
                )
            }
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Route.Login.path,
            modifier = Modifier.padding(padding)
        ) {

            // ==================== AUTENTICACIÓN ====================

            composable(Route.Login.path) {
                LoginScreenVm(
                    vm = authViewModel,
                    onLoginOkNavigateHome = {
                        // Navegar según el rol del usuario
                        val destination = when (loggedUser?.role?.uppercase()) {
                            "TRAINER", "ADMIN" -> Route.CoachClients.path
                            else -> Route.UserChats.path
                        }
                        navController.navigate(destination) {
                            popUpTo(Route.Login.path) { inclusive = true }
                        }
                    },
                    onGoRegister = {
                        navController.navigate(Route.Register.path)
                    }
                )
            }

            composable(Route.Register.path) {
                RegisterScreen(
                    vm = authViewModel,
                    onRegistered = {
                        navController.popBackStack()
                    },
                    onGoLogin = {
                        navController.popBackStack()
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
                Text(
                    text = "Mis Rutinas",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
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
                Text(
                    text = "Notificaciones",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            composable(Route.Profile.path) {
                Text(
                    text = "Perfil de ${loggedUser?.name ?: "Usuario"}",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
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
                
                Text(
                    text = if (clientId != null) 
                        "Crear Rutina para Cliente #$clientId" 
                    else 
                        "Crear Rutina Global",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
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
