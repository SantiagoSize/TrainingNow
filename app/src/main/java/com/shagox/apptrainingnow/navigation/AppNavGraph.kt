package com.shagox.apptrainingnow.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
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
import com.shagox.apptrainingnow.data.local.notification.NotificationAction
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationType
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.data.repository.INotificationRepository
import com.shagox.apptrainingnow.data.repository.ProgressRepository
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.data.repository.TrainerRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.components.BottomNavigationBarTN
import com.shagox.apptrainingnow.ui.screen.ChatScreen
import com.shagox.apptrainingnow.ui.screen.CreateRoutineScreen
import com.shagox.apptrainingnow.ui.screen.ExerciseDetailScreen
import com.shagox.apptrainingnow.ui.screen.LibraryCategoryScreen
import com.shagox.apptrainingnow.ui.screen.LibraryScreen
import com.shagox.apptrainingnow.ui.screen.NotificationsScreen
import com.shagox.apptrainingnow.ui.screen.ProfileScreen
import com.shagox.apptrainingnow.ui.screen.RoutineActiveScreen
import com.shagox.apptrainingnow.ui.screen.UserChatsScreen
import com.shagox.apptrainingnow.ui.screen.UserRoutinesScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminChatsScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminCreateCategoryScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminCreateUserScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminPanelScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminSanctionScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminSendNotificationScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminUserListScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminUserManagementScreen
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
    userRepository: IUserRepository,
    chatRepository: ChatRepository,
    routineRepository: RoutineRepository,
    trainerRepository: TrainerRepository? = null,
    progressRepository: ProgressRepository? = null,
    notificationRepository: INotificationRepository? = null,
    exerciseRepository: IExerciseRepository? = null,
    workoutRepository: com.shagox.apptrainingnow.data.repository.WorkoutRepository? = null,
    temaClaro: Boolean = false,
    onCambiarTema: (Boolean) -> Unit = {}
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    
    val loginState by authViewModel.loginState.collectAsState()
    val loggedUser = loginState.loggedUser
    val userRole = loggedUser?.role ?: "USER"
    val currentUserId = loggedUser?.id ?: 0

    val context = LocalContext.current
    val safeStartDestination = startDestination.ifBlank { Route.Welcome.path }

    // Id efectivo para guardar datos locales: el usuario logueado o el invitado
    var guestId by remember { mutableStateOf(0) }
    LaunchedEffect(currentUserId) {
        if (currentUserId <= 0) {
            guestId = com.shagox.apptrainingnow.data.local.user.GuestSession.obtenerGuestId(context)
        } else {
            // Al iniciar sesión, las rutinas creadas como invitado pasan a la cuenta
            com.shagox.apptrainingnow.data.local.user.GuestSession.migrarRutinasA(context, currentUserId)
        }
    }
    val effectiveUserId = if (currentUserId > 0) currentUserId else guestId

    // "Heartbeat" de presencia: mientras haya una cuenta logueada y la app esté en primer
    // plano (esta pantalla compuesta viva), se avisa cada 20s al backend "sigo conectado".
    // Así el chat puede mostrar "Conectado"/"Desconectado" del otro usuario.
    LaunchedEffect(currentUserId) {
        if (currentUserId <= 0) return@LaunchedEffect
        while (true) {
            userRepository.heartbeat(currentUserId)
            kotlinx.coroutines.delay(20_000L)
        }
    }

    // Aviso al ver el avance mensual sin cuenta
    var mostrarAvisoAvance by remember { mutableStateOf(false) }

    // Ocultar barra en Welcome, detalle y subpantallas de admin
    val hideBottomBarRoutes = listOf(
        Route.Welcome.path,
        Route.AdminCreateCategory.path,
        Route.AdminSendNotification.path,
        Route.AdminUserList.path,
        Route.AdminCreateUser.path,
        Route.AdminSanctions.path,
        Route.AdminExercises.path
    )
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

        // Transiciones: las pantallas entran deslizándose con un leve zoom y desvanecido
        val duracion = 320
        val easing = FastOutSlowInEasing

        NavHost(
            navController = navController,
            startDestination = safeStartDestination,
            modifier = Modifier.padding(padding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 5 },
                    animationSpec = tween(duracion, easing = easing)
                ) + fadeIn(animationSpec = tween(duracion)) +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(duracion, easing = easing))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 6 },
                    animationSpec = tween(duracion, easing = easing)
                ) + fadeOut(animationSpec = tween(duracion / 2))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 5 },
                    animationSpec = tween(duracion, easing = easing)
                ) + fadeIn(animationSpec = tween(duracion))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 4 },
                    animationSpec = tween(duracion, easing = easing)
                ) + fadeOut(animationSpec = tween(duracion / 2)) +
                        scaleOut(targetScale = 0.96f, animationSpec = tween(duracion))
            }
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
                // Leer estado de login aquí para que al volver de Perfil con sesión iniciada se muestre el chat
                val chatLoginState by authViewModel.loginState.collectAsState()
                val chatUser = chatLoginState.loggedUser
                if (chatUser == null) {
                    ChatBlockedScreen(onGoToProfile = { navController.navigate(Route.Profile.path) })
                } else {
                    UserChatsScreen(
                        userRepository = userRepository,
                        chatRepository = chatRepository,
                        currentUserId = chatUser.id,
                        onNavigateToChat = { trainerId ->
                            navController.navigate(Route.ChatDetail.createRoute(trainerId))
                        }
                    )
                }
            }

            composable(Route.Library.path) {
                if (exerciseRepository != null) {
                    LibraryScreen(
                        exerciseRepository = exerciseRepository,
                        onExerciseClick = { exerciseId ->
                            navController.navigate(Route.ExerciseDetail.createRoute(exerciseId))
                        },
                        onCategoryClick = { categoryName ->
                            navController.navigate(Route.LibraryCategory.createRoute(categoryName))
                        }
                    )
                } else {
                    Text("Biblioteca no disponible", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }

            composable(
                route = Route.LibraryCategory.path,
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                if (exerciseRepository != null) {
                    LibraryCategoryScreen(
                        categoryName = categoryName,
                        exerciseRepository = exerciseRepository,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    Text("Biblioteca no disponible", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }

            composable(
                route = Route.ExerciseDetail.path,
                arguments = listOf(navArgument("exerciseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: 0
                if (exerciseRepository != null) {
                    ExerciseDetailScreen(
                        exerciseId = exerciseId,
                        exerciseRepository = exerciseRepository,
                        onClose = { navController.popBackStack() }
                    )
                } else {
                    Text("Detalle no disponible", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }

            composable(Route.UserRoutines.path) {
                // Sincronizar rutinas (asignadas por el entrenador y públicas) desde el backend
                androidx.compose.runtime.LaunchedEffect(currentUserId) {
                    routineRepository.syncRoutinesFromBackend(currentUserId)
                }
                UserRoutinesScreen(
                    routineRepository = routineRepository,
                    userId = effectiveUserId,
                    isLoggedIn = currentUserId > 0,
                    onCrearCuenta = {
                        // Mismo comportamiento que la barra inferior: se puede volver a Rutinas
                        navController.navigate(Route.Profile.path) {
                            popUpTo(safeStartDestination) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    avisoYaMostrado = context
                        .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        .getBoolean("aviso_cuenta_mostrado", false),
                    onAvisoMostrado = {
                        context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("aviso_cuenta_mostrado", true)
                            .apply()
                    },
                    onCreateRoutine = {
                        // Se permite crear sin cuenta; el aviso solo aparece la primera vez
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
                    exerciseRepository = exerciseRepository,
                    workoutRepository = workoutRepository,
                    userId = effectiveUserId,
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
                            trainerId = currentUserId,
                            notificationRepository = notificationRepository
                        )
                    )
                    
                    CoachClientsScreen(
                        viewModel = coachViewModel,
                        onVerUsuarios = { navController.navigate(Route.CoachUsers.path) },
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
                            trainerId = currentUserId,
                            notificationRepository = notificationRepository
                        )
                    )
                    
                    CoachRoutinesScreen(
                        viewModel = coachViewModel,
                        onCreateRoutine = {
                            navController.navigate(Route.CreateRoutine.createRoute())
                        },
                        onRoutineClick = { routineId ->
                            navController.navigate(Route.RoutineActive.createRoute(routineId))
                        }
                    )
                } else {
                    LoadingScreen()
                }
            }

            composable(Route.CoachChats.path) {
                // Chats del entrenador: SUS clientes (no la lista de otros entrenadores).
                if (trainerRepository != null) {
                    com.shagox.apptrainingnow.ui.screen.CoachChatsScreen(
                        trainerRepository = trainerRepository,
                        chatRepository = chatRepository,
                        currentUserId = currentUserId,
                        onNavigateToChat = { clientId ->
                            navController.navigate(Route.ChatDetail.createRoute(clientId))
                        }
                    )
                } else {
                    LoadingScreen()
                }
            }

            // ==================== PANTALLAS DE ADMIN ====================

            composable(Route.AdminChats.path) {
                AdminChatsScreen(
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    currentUserId = currentUserId,
                    onNavigateToChat = { otherId ->
                        navController.navigate(Route.ChatDetail.createRoute(otherId))
                    }
                )
            }

            composable(Route.AdminPanel.path) {
                AdminPanelScreen(
                    onBiblioteca = { navController.navigate(Route.AdminExercises.path) },
                    onNuevaCategoria = { navController.navigate(Route.AdminCreateCategory.path) },
                    onEntrenamientoGlobal = { navController.navigate(Route.CreateRoutine.createRoute()) },
                    onEnviarNotificacion = { navController.navigate(Route.AdminSendNotification.path) },
                    onGestionUsuarios = { navController.navigate(Route.AdminUserManagement.path) }
                )
            }

            composable(Route.AdminUserManagement.path) {
                AdminUserManagementScreen(
                    onBack = { navController.popBackStack() },
                    onVerUsuarios = { navController.navigate(Route.AdminUserList.path) },
                    onCrearUsuario = { navController.navigate(Route.AdminCreateUser.path) },
                    onSuspenderBanearEliminar = { navController.navigate(Route.AdminSanctions.path) }
                )
            }

            composable(Route.AdminCreateCategory.path) {
                if (exerciseRepository != null) {
                    AdminCreateCategoryScreen(
                        exerciseRepository = exerciseRepository,
                        onBack = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() }
                    )
                } else {
                    Text("Biblioteca no disponible", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }

            composable(Route.AdminSendNotification.path) {
                if (notificationRepository != null) {
                    AdminSendNotificationScreen(
                        userRepository = userRepository,
                        notificationRepository = notificationRepository,
                        adminId = currentUserId,
                        onBack = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() }
                    )
                } else {
                    Text("Notificaciones no disponibles", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }

            composable(Route.AdminUserList.path) {
                AdminUserListScreen(
                    userRepository = userRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.Settings.path) {
                com.shagox.apptrainingnow.ui.screen.SettingsScreen(
                    temaClaro = temaClaro,
                    onCambiarTema = onCambiarTema,
                    onVerAvanceMensual = {
                        if (currentUserId > 0) {
                            navController.navigate(Route.MonthlyReport.path)
                        } else {
                            mostrarAvisoAvance = true
                        }
                    },
                    onVerNotificaciones = { navController.navigate(Route.Notifications.path) },
                    authViewModel = authViewModel
                )
            }

            composable(Route.MonthlyReport.path) {
                com.shagox.apptrainingnow.ui.screen.MonthlyReportScreen(
                    userId = effectiveUserId,
                    esInvitado = currentUserId <= 0,
                    workoutRepository = workoutRepository,
                    exerciseRepository = exerciseRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.CoachUsers.path) {
                com.shagox.apptrainingnow.ui.screen.coach.CoachUsersScreen(
                    userRepository = userRepository,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.AdminExercises.path) {
                if (exerciseRepository != null) {
                    com.shagox.apptrainingnow.ui.screen.admin.AdminExerciseManagerScreen(
                        exerciseRepository = exerciseRepository,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Route.AdminCreateUser.path) {
                AdminCreateUserScreen(
                    userRepository = userRepository,
                    adminId = currentUserId,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(Route.AdminSanctions.path) {
                AdminSanctionScreen(
                    userRepository = userRepository,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
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
                            trainerId = currentUserId,
                            notificationRepository = notificationRepository
                        )
                    )
                    
                    ClientDetailScreen(
                        viewModel = coachViewModel,
                        clientId = clientId,
                        onBack = { navController.popBackStack() },
                        onChatClick = {
                            navController.navigate(Route.ChatDetail.createRoute(clientId))
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
                // Sincronizar la conversación desde TrainNow-Comunicaciones al abrir el chat
                androidx.compose.runtime.LaunchedEffect(currentUserId, otherId) {
                    chatRepository.syncConversation(currentUserId, otherId)
                }
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
                        userId = currentUserId,
                        onBack = if (navController.previousBackStackEntry != null) {
                            { navController.popBackStack() }
                        } else null
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
                ProfileScreen(
                    authViewModel = authViewModel
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
                var clientDisplayName by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(clientId) {
                    if (clientId != null) {
                        clientDisplayName = withContext(Dispatchers.IO) {
                            userRepository.getUserById(clientId)?.name
                        }
                    }
                }
                CreateRoutineScreen(
                    onBack = { navController.popBackStack() },
                    onSaveRoutine = { name, days ->
                        CoroutineScope(Dispatchers.IO).launch {
                            if (clientId != null) {
                                val trainerId = currentUserId
                                val firstRoutineId = routineRepository.saveRoutineForClient(trainerId, clientId, name, days)
                                val trainer = userRepository.getUserById(trainerId)
                                val trainerName = trainer?.name ?: "Tu entrenador"
                                notificationRepository?.saveNotification(
                                    NotificationEntity(
                                        userId = clientId,
                                        title = "Nueva rutina asignada",
                                        message = "$trainerName te ha asignado la rutina \"$name\".",
                                        type = NotificationType.ROUTINE_ASSIGNED.name,
                                        senderId = trainerId,
                                        actionType = NotificationAction.OPEN_ROUTINE.name,
                                        actionData = firstRoutineId.toInt().toString()
                                    )
                                )
                            } else {
                                routineRepository.savePersonalRoutine(effectiveUserId, name, days)
                            }
                            withContext(Dispatchers.Main) { navController.popBackStack() }
                        }
                    },
                    clientDisplayName = clientDisplayName
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

        // Aviso: el avance mensual sin cuenta solo vive en este teléfono
        if (mostrarAvisoAvance) {
            com.shagox.apptrainingnow.ui.components.CuentaRecomendadaDialogTN(
                titulo = "Guarda tu avance",
                mensaje = "Sin cuenta puedes ver tu avance, pero se guarda solo en este teléfono y se pierde al desinstalar la app. Crea tu cuenta o inicia sesión para conservarlo.",
                textoNegativo = "VER IGUAL",
                onCrearCuenta = {
                    mostrarAvisoAvance = false
                    navController.navigate(Route.Profile.path) {
                        popUpTo(safeStartDestination) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onContinuar = {
                    mostrarAvisoAvance = false
                    navController.navigate(Route.MonthlyReport.path)
                },
                onDismiss = { mostrarAvisoAvance = false }
            )
        }
    }
}

/**
 * Pantalla que bloquea el chat cuando el usuario no ha iniciado sesión.
 * Diseño: candado, "Inicia sesión para continuar", "Esta función requiere una cuenta", botón "Ir a Mi Perfil".
 */
@Composable
private fun ChatBlockedScreen(onGoToProfile: () -> Unit) {
    val amarilloCandado = Color(0xFFFFC107) // Amarillo/dorado como en la imagen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = amarilloCandado,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Inicia sesión para continuar",
                color = TextoPrincipal,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Esta función requiere una cuenta",
                color = GrisTexto,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onGoToProfile,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde),
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Text(text = "Ir a Mi Perfil")
            }
        }
    }
}
/**
 * Indicador de carga a pantalla completa, mientras se preparan los repositorios.
 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = VerdeTN)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando...",
                color = GrisTexto,
                fontSize = 14.sp
            )
        }
    }
}
