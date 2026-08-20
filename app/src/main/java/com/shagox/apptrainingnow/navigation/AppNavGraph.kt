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
import kotlinx.coroutines.flow.first
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
import com.shagox.apptrainingnow.utils.NotificationHelper
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
import com.shagox.apptrainingnow.ui.screen.admin.AdminGlobalRoutinesScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminPanelScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminSanctionScreen
import com.shagox.apptrainingnow.ui.screen.admin.AdminSendMessagesScreen
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
    // Nombre a mostrar del actor logueado, usado para el registro de actividad (auditoría).
    val actorDisplayName = "${loggedUser?.name.orEmpty()} ${loggedUser?.lastName.orEmpty()}".trim()

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
    // Estado de sanción del usuario logueado (baneo/suspensión), refrescado junto al
    // heartbeat: si el admin sanciona a alguien mientras usa la app, en máximo 20s se le
    // bloquea la pantalla sin esperar a que vuelva a iniciar sesión.
    var sancionActiva by remember { mutableStateOf<com.shagox.apptrainingnow.data.local.user.UserEntity?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId <= 0) {
            sancionActiva = null
            return@LaunchedEffect
        }
        while (true) {
            userRepository.heartbeat(currentUserId)
            try {
                val u = userRepository.getUserById(currentUserId)
                val suspendido = u?.suspendedUntil != null && u.suspendedUntil > System.currentTimeMillis()
                sancionActiva = if (u != null && (u.isBanned || suspendido)) u else null
            } catch (e: Exception) {
                // Sin conexión: no se toca el estado de sanción ya conocido.
            }
            kotlinx.coroutines.delay(20_000L)
        }
    }

    // Push local real (barra de estado) para notificaciones (rutina asignada, etc.) y
    // mensajes de chat nuevos. No hay servidor de push (FCM): se sondea cada 20s mientras
    // la app está abierta y logueada. La primera vuelta solo establece la línea base (no
    // dispara push de cosas viejas); desde la segunda vuelta, todo lo nuevo sí notifica.
    LaunchedEffect(currentUserId) {
        if (currentUserId <= 0) return@LaunchedEffect
        val prefs = context.getSharedPreferences("push_notify_prefs", android.content.Context.MODE_PRIVATE)
        var primeraVuelta = true
        while (true) {
            try {
                // --- Notificaciones (rutina asignada, sistema, etc.) ---
                val notifKey = "notif_seen_ids_$currentUserId"
                val vistos = prefs.getStringSet(notifKey, emptySet())
                    ?.mapNotNull { it.toIntOrNull() }?.toMutableSet() ?: mutableSetOf()
                val notifs = notificationRepository?.getUserNotifications(currentUserId)?.first() ?: emptyList()
                if (primeraVuelta) {
                    vistos.addAll(notifs.map { it.id })
                } else {
                    for (n in notifs) {
                        if (n.id !in vistos) {
                            vistos.add(n.id)
                            NotificationHelper.showPush(
                                context,
                                title = n.title,
                                message = n.message,
                                notificationId = NotificationHelper.uniqueId("notif_${n.id}", n.title)
                            )
                        }
                    }
                }
                prefs.edit().putStringSet(notifKey, vistos.map { it.toString() }.toSet()).apply()

                // --- Mensajes de chat nuevos ---
                val resumen = chatRepository.obtenerResumenConversaciones(currentUserId)
                val chatAbiertoConId = navController.currentBackStackEntry
                    ?.takeIf { it.destination.route == Route.ChatDetail.path }
                    ?.arguments?.getInt("otherId")
                for (r in resumen) {
                    val ts = r.lastTimestamp ?: continue
                    if (r.unreadCount <= 0 || r.contactId == chatAbiertoConId) continue
                    val key = "chat_seen_ts_${currentUserId}_${r.contactId}"
                    if (primeraVuelta) {
                        prefs.edit().putLong(key, ts).apply()
                        continue
                    }
                    val visto = prefs.getLong(key, 0L)
                    if (ts > visto) {
                        prefs.edit().putLong(key, ts).apply()
                        val contacto = try { userRepository.getUserById(r.contactId) } catch (e: Exception) { null }
                        val nombre = contacto?.let { "${it.name} ${it.lastName}".trim() } ?: "Nuevo mensaje"
                        NotificationHelper.showPush(
                            context,
                            title = nombre,
                            message = r.lastMessage ?: "Te envió un mensaje",
                            notificationId = NotificationHelper.uniqueId("chat_${r.contactId}", ts.toString())
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppNavGraph", "Error en sondeo de notificaciones push", e)
            }
            primeraVuelta = false
            kotlinx.coroutines.delay(20_000L)
        }
    }

    // Aviso al ver el avance mensual sin cuenta
    var mostrarAvisoAvance by remember { mutableStateOf(false) }

    // Ocultar barra en Welcome, detalle y subpantallas de admin
    val hideBottomBarRoutes = listOf(
        Route.Welcome.path,
        Route.AdminCreateCategory.path,
        Route.AdminMessages.path,
        Route.AdminGlobalRoutines.path,
        Route.AdminActivityLog.path,
        Route.AdminUserList.path,
        Route.AdminCreateUser.path,
        Route.AdminSanctions.path,
        Route.AdminExercises.path
    )
    val shouldHideBottomBar = currentRoute in hideBottomBarRoutes ||
            currentRoute?.startsWith("chat_detail") == true ||
            currentRoute?.startsWith("client_detail") == true ||
            currentRoute?.startsWith("create_goal") == true ||
            currentRoute?.startsWith("admin_library_category") == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Siempre emitir algo para evitar restricciones indefinidas en measure
            if (shouldHideBottomBar) {
                Box(modifier = Modifier.fillMaxWidth().height(0.dp))
            } else {
                BottomNavigationBarTN(
                    navController = navController,
                    userRole = userRole
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
                    ChatBlockedScreen(onGoToProfile = {
                        // Sin popUpTo/launchSingleTop esto apilaba Profile ENCIMA de UserChats
                        // en el back stack (a diferencia del resto de tabs, que usan ese patrón
                        // en BottomNavigationBarTN). Si el usuario no se registraba y volvía a
                        // tocar "Chat", el tab quedaba con estado corrupto y no reaccionaba más.
                        navController.navigate(Route.Profile.path) {
                            popUpTo(Route.UserChats.path) { inclusive = true }
                            launchSingleTop = true
                        }
                    })
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
                    userRepository = userRepository,
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
                    },
                    onEditRoutine = { routine ->
                        navController.navigate(Route.CreateRoutine.createRoute(editRoutineId = routine.id))
                    },
                    onDeleteRoutine = { routine ->
                        CoroutineScope(Dispatchers.IO).launch {
                            routineRepository.deleteRoutine(routine)
                        }
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

            composable(Route.CoachPublicProfile.path) {
                com.shagox.apptrainingnow.ui.screen.coach.CoachPublicProfileScreen(
                    userRepository = userRepository,
                    currentUserId = currentUserId,
                    onBack = { navController.popBackStack() }
                )
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
                        },
                        routineRepository = routineRepository,
                        userRepository = userRepository,
                        trainerId = currentUserId
                    )
                } else {
                    LoadingScreen()
                }
            }

            // Nota: se quitó el tab "Mensajes" (Route.CoachChats) del entrenador — se
            // chatea con clientes desde el ícono de chat en cada tarjeta de "Mis Clientes".

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
                    onEntrenamientoGlobal = { navController.navigate(Route.CreateRoutine.createRoute(isGlobal = true)) },
                    onRutinasGlobales = { navController.navigate(Route.AdminGlobalRoutines.path) },
                    onEnviarMensajes = { navController.navigate(Route.AdminMessages.path) },
                    onVerActividad = { navController.navigate(Route.AdminActivityLog.path) },
                    onGestionUsuarios = { navController.navigate(Route.AdminUserManagement.path) }
                )
            }

            composable(Route.AdminActivityLog.path) {
                com.shagox.apptrainingnow.ui.screen.admin.AdminActivityLogScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.AdminGlobalRoutines.path) {
                AdminGlobalRoutinesScreen(
                    routineRepository = routineRepository,
                    onBack = { navController.popBackStack() },
                    onEditRoutine = { routineId ->
                        navController.navigate(Route.CreateRoutine.createRoute(isGlobal = true, editRoutineId = routineId))
                    },
                    actorId = currentUserId,
                    actorName = actorDisplayName,
                    actorRole = userRole
                )
            }

            composable(Route.AdminUserManagement.path) {
                AdminUserManagementScreen(
                    onBack = { navController.popBackStack() },
                    onVerUsuarios = { navController.navigate(Route.AdminUserList.path) },
                    onCrearUsuario = { navController.navigate(Route.AdminCreateUser.path) },
                    onSuspenderBanearEliminar = { navController.navigate(Route.AdminSanctions.path) },
                    userRepository = userRepository
                )
            }

            composable(Route.AdminCreateCategory.path) {
                if (exerciseRepository != null) {
                    AdminCreateCategoryScreen(
                        exerciseRepository = exerciseRepository,
                        onBack = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() },
                        actorId = currentUserId,
                        actorName = actorDisplayName,
                        actorRole = userRole
                    )
                } else {
                    Text("Biblioteca no disponible", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }

            composable(Route.AdminMessages.path) {
                AdminSendMessagesScreen(
                    userRepository = userRepository,
                    chatRepository = chatRepository,
                    adminId = currentUserId,
                    adminName = actorDisplayName,
                    onBack = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }

            composable(Route.AdminUserList.path) {
                AdminUserListScreen(
                    userRepository = userRepository,
                    onBack = { navController.popBackStack() },
                    actorId = currentUserId,
                    actorName = actorDisplayName,
                    actorRole = userRole
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
                    com.shagox.apptrainingnow.ui.screen.admin.AdminLibraryScreen(
                        exerciseRepository = exerciseRepository,
                        onBack = { navController.popBackStack() },
                        onCategoryClick = { categoryName ->
                            navController.navigate(Route.AdminLibraryCategory.createRoute(categoryName))
                        },
                        onCreateCategory = { navController.navigate(Route.AdminCreateCategory.path) },
                        actorId = currentUserId,
                        actorName = actorDisplayName,
                        actorRole = userRole
                    )
                }
            }

            composable(
                route = Route.AdminLibraryCategory.path,
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                if (exerciseRepository != null) {
                    com.shagox.apptrainingnow.ui.screen.admin.AdminLibraryCategoryScreen(
                        categoryName = categoryName,
                        exerciseRepository = exerciseRepository,
                        onBack = { navController.popBackStack() },
                        actorId = currentUserId,
                        actorName = actorDisplayName,
                        actorRole = userRole
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
                    onSuccess = { navController.popBackStack() },
                    actorId = currentUserId,
                    actorName = actorDisplayName,
                    actorRole = userRole
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
                        routineRepository = routineRepository,
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
                    },
                    navArgument("global") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("editRoutineId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val clientIdStr = backStackEntry.arguments?.getString("clientId")
                val clientId = clientIdStr?.toIntOrNull()
                val esGlobal = backStackEntry.arguments?.getBoolean("global") ?: false
                val editRoutineId = backStackEntry.arguments?.getString("editRoutineId")?.toIntOrNull()
                val esEntrenadorSinCliente = (userRole == "TRAINER" || userRole == "COACH") && clientId == null && !esGlobal
                var clientDisplayName by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(clientId) {
                    if (clientId != null) {
                        clientDisplayName = withContext(Dispatchers.IO) {
                            userRepository.getUserById(clientId)?.name
                        }
                    }
                }
                // Modo edición: precarga nombre y días de la rutina propia que se va a editar.
                var initialRoutineName by remember(editRoutineId) { mutableStateOf<String?>(null) }
                var initialDays by remember(editRoutineId) {
                    mutableStateOf<List<com.shagox.apptrainingnow.data.repository.DayRoutineInput>?>(null)
                }
                LaunchedEffect(editRoutineId) {
                    if (editRoutineId != null) {
                        routineRepository.getRoutineWithDays(editRoutineId, effectiveUserId).first()?.let { conDias ->
                            initialRoutineName = conDias.header.name
                            initialDays = conDias.days.map { dia ->
                                com.shagox.apptrainingnow.data.repository.DayRoutineInput(
                                    dayLabel = dia.dayLabel,
                                    activityName = dia.activityName,
                                    exerciseNames = dia.exercises.map { it.name }
                                )
                            }
                        }
                    }
                }
                // Si es edición, espera a que carguen nombre/días antes de componer la pantalla:
                // CreateRoutineScreen solo lee initialRoutineName/initialDays una vez (remember),
                // así que mostrarla antes de que termine la carga async dejaría el formulario vacío.
                if (editRoutineId != null && initialRoutineName == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                    return@composable
                }
                CreateRoutineScreen(
                    initialRoutineName = initialRoutineName,
                    initialDays = initialDays,
                    onBack = { navController.popBackStack() },
                    onSaveRoutine = { name, days, targetEmailOrId, esPlantilla ->
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
                            } else if (esGlobal) {
                                // Edición de una rutina global existente: se borra (cascada limpia
                                // días/ejercicios) y se recrea con saveGlobalRoutine para que
                                // conserve ownerId = null. Antes esta rama no distinguía edición
                                // de creación y, si se reutilizaba el flujo de "editar rutina
                                // propia" (savePersonalRoutine), la rutina dejaba de ser global
                                // y pasaba a pertenecer al admin que la editó.
                                if (editRoutineId != null) {
                                    routineRepository.getRoutineById(editRoutineId)?.let { existente ->
                                        routineRepository.deleteRoutine(existente)
                                    }
                                }
                                routineRepository.saveGlobalRoutine(currentUserId, name, days)
                                com.shagox.apptrainingnow.data.repository.AuditLogRepository().log(
                                    actorId = currentUserId,
                                    actorName = actorDisplayName,
                                    actorRole = userRole,
                                    action = if (editRoutineId != null) "ROUTINE_GLOBAL_UPDATED" else "ROUTINE_GLOBAL_CREATED",
                                    targetType = "ROUTINE",
                                    targetName = name
                                )
                            } else if (esEntrenadorSinCliente && esPlantilla) {
                                routineRepository.saveAsTemplate(currentUserId, name, days)
                            } else if (esEntrenadorSinCliente && targetEmailOrId != null) {
                                val trainerId = currentUserId
                                val targetUserId = targetEmailOrId.toIntOrNull()
                                    ?: userRepository.getAllUsers().first()
                                        .firstOrNull { it.email.equals(targetEmailOrId, ignoreCase = true) }?.id
                                if (targetUserId == null || targetUserId <= 0) {
                                    withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context, "No se encontró un usuario con ese correo", android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    routineRepository.shareRoutineWithUser(trainerId, targetUserId, name, days)
                                    val trainer = userRepository.getUserById(trainerId)
                                    val trainerName = trainer?.name ?: "Tu entrenador"
                                    notificationRepository?.saveNotification(
                                        NotificationEntity(
                                            userId = targetUserId,
                                            title = "$trainerName te compartió una rutina",
                                            message = "\"$name\": ¿quieres aceptarla y empezar a entrenarla?",
                                            type = NotificationType.ROUTINE_ASSIGNED.name,
                                            senderId = trainerId,
                                            actionType = NotificationAction.ACCEPT_DECLINE_ROUTINE.name,
                                            actionData = name
                                        )
                                    )
                                }
                            } else if (editRoutineId != null) {
                                // Edición: no existe un "update" multi-día, se borra la rutina
                                // anterior (cascada limpia días/ejercicios) y se recrea con los
                                // datos editados, conservando el mismo dueño.
                                routineRepository.getRoutineById(editRoutineId)?.let { existente ->
                                    routineRepository.deleteRoutine(existente)
                                }
                                routineRepository.savePersonalRoutine(effectiveUserId, name, days)
                            } else {
                                routineRepository.savePersonalRoutine(effectiveUserId, name, days)
                            }
                            withContext(Dispatchers.Main) { navController.popBackStack() }
                        }
                    },
                    clientDisplayName = clientDisplayName,
                    esEntrenadorSinCliente = esEntrenadorSinCliente
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

        // Bloqueo total: cuenta baneada o suspendida. Se muestra en un Dialog sin botón de
        // cerrar ni click-outside para que sea imposible seguir usando la app; la única
        // acción disponible es cerrar sesión. El motivo es obligatorio al banear/suspender
        // (ver AdminSanctionsScreen), así que siempre hay algo que mostrar acá.
        val sancion = sancionActiva
        if (sancion != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { },
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NegroFondo)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (sancion.isBanned) "Cuenta baneada" else "Cuenta suspendida",
                            color = TextoPrincipal,
                            fontSize = 22.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!sancion.isBanned && sancion.suspendedUntil != null) {
                            Text(
                                text = "Hasta el " + java.text.SimpleDateFormat(
                                    "dd-MM-yyyy HH:mm",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(sancion.suspendedUntil)),
                                color = GrisTexto,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                        Text(
                            text = "Motivo: " + ((if (sancion.isBanned) sancion.banReason else sancion.suspendReason)
                                ?.takeIf { it.isNotBlank() } ?: "No especificado"),
                            color = TextoPrincipal,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Si crees que esto es un error, contacta a soporte de TrainingNow.",
                            color = GrisTexto,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Button(
                            onClick = { authViewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN)
                        ) {
                            Text("Cerrar sesión", color = TextoSobreVerde)
                        }
                    }
                }
            }
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
