package com.shagox.apptrainingnow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Define todas las rutas de navegación de la aplicación.
 * Organizado por rol de usuario y funcionalidad.
 */
sealed class Route(val path: String, val title: String, val icon: ImageVector) {
    
    // ==================== PANTALLAS DE USUARIO ====================
    
    /** Chat con entrenadores (para usuarios) */
    data object UserChats : Route("user_chats", "Chats", Icons.AutoMirrored.Filled.Chat)
    
    /** Biblioteca de ejercicios */
    data object Library : Route("library", "Biblioteca", Icons.Filled.Search)
    
    /** Mis rutinas (para usuarios) — DateRange (calendario) del set base para que siempre se dibuje */
    data object UserRoutines : Route("user_routines", "Rutinas", Icons.Filled.DateRange)

    // ==================== PANTALLAS DE ADMIN ====================

    /** Chat del admin (posición 1 en barra) */
    data object AdminChats : Route("admin_chats", "Chat", Icons.AutoMirrored.Filled.Chat)

    /** Gestión de usuarios (posición 2 en barra) */
    data object AdminUserManagement : Route("admin_users", "Usuarios", Icons.Filled.People)

    /** Panel de administración - icono 6x3 puntitos (posición 3 en barra) */
    data object AdminPanel : Route("admin_panel", "Panel", Icons.Filled.Apps)

    /** Crear categoría de ejercicios (admin) */
    data object AdminCreateCategory : Route("admin_create_category", "Nueva Categoría", Icons.AutoMirrored.Filled.Label)

    /** Biblioteca del admin: grilla de categorías (misma vista que el usuario) + crear categoría */
    data object AdminExercises : Route("admin_exercises", "Biblioteca", Icons.Filled.FitnessCenter)

    /** Ejercicios de una categoría, vistos por el admin: crear/editar/eliminar ejercicios */
    data object AdminLibraryCategory : Route("admin_library_category/{categoryName}", "Ejercicios", Icons.Filled.FitnessCenter) {
        fun createRoute(categoryName: String): String = "admin_library_category/$categoryName"
    }

    /** Enviar mensajes a uno o varios usuarios por correo (admin) */
    data object AdminMessages : Route("admin_messages", "Enviar Mensajes", Icons.AutoMirrored.Filled.Send)

    /** Ver y editar las rutinas globales ya publicadas (admin) */
    data object AdminGlobalRoutines : Route("admin_global_routines", "Rutinas Globales", Icons.AutoMirrored.Filled.List)

    /** Registro de actividad administrativa: quién hizo qué y cuándo (admin) */
    data object AdminActivityLog : Route("admin_activity_log", "Actividad", Icons.Filled.Schedule)

    /** Lista de todos los usuarios (admin) */
    data object AdminUserList : Route("admin_user_list", "Usuarios", Icons.Filled.People)

    /** Crear usuario (admin) */
    data object AdminCreateUser : Route("admin_create_user", "Crear Usuario", Icons.Filled.PersonAdd)

    /** Suspender / Banear / Eliminar cuenta (admin) */
    data object AdminSanctions : Route("admin_sanctions", "Sanciones", Icons.Filled.Block)

    // ==================== PANTALLAS DE COACH ====================
    
    /** Lista de clientes del entrenador */
    data object CoachClients : Route("coach_clients", "Clientes", Icons.Filled.People)
    
    /** Gestión de rutinas del entrenador */
    data object CoachRoutines : Route("coach_routines", "Rutinas", Icons.Filled.FitnessCenter)
    
    /** Chats del entrenador con clientes */
    data object CoachChats : Route("coach_chats", "Mensajes", Icons.AutoMirrored.Filled.Chat)
    
    /** Lista de todos los usuarios normales (solo lectura, para el entrenador) */
    data object CoachUsers : Route("coach_users", "Usuarios", Icons.Filled.People)

    /** Perfil público del entrenador: cómo lo ven los usuarios en "Mis chats" (imagen, bio). */
    data object CoachPublicProfile : Route("coach_public_profile", "Mi Perfil", Icons.Filled.Campaign)

    /** Detalle de un cliente */
    data object ClientDetail : Route("client_detail/{clientId}", "Cliente", Icons.Filled.Person) {
        fun createRoute(clientId: Int): String = "client_detail/$clientId"
    }
    
    /** Crear/editar rutina. isGlobal=true solo lo usa el admin desde "Entrenamiento Global".
     *  editRoutineId != null: abre la pantalla precargada con esa rutina propia para editarla. */
    data object CreateRoutine : Route("create_routine?clientId={clientId}&global={global}&editRoutineId={editRoutineId}", "Nueva Rutina", Icons.Filled.Edit) {
        fun createRoute(clientId: Int? = null, isGlobal: Boolean = false, editRoutineId: Int? = null): String {
            var route = "create_routine"
            val params = mutableListOf<String>()
            if (clientId != null) params.add("clientId=$clientId")
            if (isGlobal) params.add("global=true")
            if (editRoutineId != null) params.add("editRoutineId=$editRoutineId")
            if (params.isNotEmpty()) route += "?" + params.joinToString("&")
            return route
        }
    }
    
    /** Crear objetivo para cliente */
    data object CreateGoal : Route("create_goal/{clientId}", "Nuevo Objetivo", Icons.Filled.Edit) {
        fun createRoute(clientId: Int): String = "create_goal/$clientId"
    }

    // ==================== PANTALLAS COMPARTIDAS ====================
    
    /** Notificaciones */
    data object Notifications : Route("notifications", "Alertas", Icons.Filled.Notifications)
    
    /** Reporte mensual de entrenamiento */
    data object MonthlyReport : Route("monthly_report", "Mi avance", Icons.Filled.DateRange)

    /** Ajustes de la app (usuario) */
    data object Settings : Route("settings", "Ajustes", Icons.Filled.Settings)

    /** Perfil de usuario */
    data object Profile : Route("profile", "Perfil", Icons.Filled.Person)

    // ==================== AUTENTICACIÓN ====================

    /** Pantalla de bienvenida (solo primera vez que se abre la app) */
    data object Welcome : Route("welcome", "Bienvenida", Icons.Filled.Person)
    
    /** Pantalla de login */
    data object Login : Route("login", "Login", Icons.Filled.Person)
    
    /** Pantalla de registro */
    data object Register : Route("register", "Registro", Icons.Filled.Person)
    
    /** Pantalla de inicio (legacy) */
    data object Home : Route("home", "Inicio", Icons.Filled.Person)

    // ==================== CHAT ====================
    
    /** Chat individual */
    data object ChatDetail : Route("chat_detail/{otherId}", "Chat", Icons.AutoMirrored.Filled.Chat) {
        fun createRoute(otherId: Int): String = "chat_detail/$otherId"
    }

    /** Pantalla de rutina activa (encima, con botón rojo para salir) */
    data object RoutineActive : Route("routine_active/{routineId}", "Rutina activa", Icons.Filled.DateRange) {
        fun createRoute(routineId: Int): String = "routine_active/$routineId"
    }

    /** Ejercicios de una categoría (ej. Pectorales -> lista Press de banca, etc.) */
    data object LibraryCategory : Route("library_category/{categoryName}", "Ejercicios", Icons.Filled.FitnessCenter) {
        fun createRoute(categoryName: String): String = "library_category/$categoryName"
    }

    /** Detalle de un ejercicio: video, descripción, CERRAR */
    data object ExerciseDetail : Route("exercise_detail/{exerciseId}", "Ejercicio", Icons.Filled.FitnessCenter) {
        fun createRoute(exerciseId: Int): String = "exercise_detail/$exerciseId"
    }

    companion object {
        /**
         * Rutas de la barra de navegación inferior para usuarios.
         */
        val userBottomNavRoutes = listOf(
            UserChats,
            Library,
            UserRoutines,
            Settings,
            Profile
        )

        /**
         * Rutas de la barra de navegación inferior para entrenadores.
         */
        val coachBottomNavRoutes = listOf(
            CoachClients,
            CoachRoutines,
            Settings,
            Profile
        )

        /**
         * Barra admin: 1=Chat, 2=Usuarios, 3=Panel, 4=Ajustes, 5=Perfil.
         * Notificaciones no tiene tab propia: se accede desde Ajustes (evita duplicar
         * la misma entrada dos veces en la barra).
         */
        val adminBottomNavRoutes = listOf(
            AdminChats,           // 1 - Chat
            AdminUserManagement, // 2 - Gestión usuarios
            AdminPanel,          // 3 - Panel administración (cuadrícula puntitos)
            Settings,            // 4 - Ajustes (tema, notificaciones, cuenta)
            Profile              // 5 - Perfil
        )

        /**
         * Obtiene las rutas de navegación según el rol.
         * Se construye una lista nueva en cada llamada para evitar referencias
         * nulas por inicialización diferida del companion object.
         */
/**
         * Devuelve, para una ruta cualquiera, las secciones de la barra a las que
         * pertenece, en orden de preferencia. La barra marca la primera que exista
         * en el rol actual, de modo que las subpantallas nunca "saltan" de sección.
         *
         * Ejemplo: estando en "library_category/Pectorales" la barra sigue marcando
         * Biblioteca; en "client_detail/3" sigue marcando Clientes.
         */
        fun seccionesCandidatas(ruta: String?): List<String> {
            if (ruta.isNullOrBlank()) return emptyList()
            return when {
                // ===== Biblioteca =====
                ruta.startsWith("library_category") ||
                        ruta.startsWith("exercise_detail") -> listOf(Library.path, AdminPanel.path)

                // ===== Rutinas (usuario o entrenador) =====
                ruta.startsWith("routine_active") ||
                        ruta.startsWith("create_routine") ->
                    listOf(UserRoutines.path, CoachRoutines.path, AdminPanel.path)

                // ===== Chats =====
                // El entrenador ya no tiene tab de "Mensajes": ahora se chatea desde el ícono
                // de chat en cada tarjeta de "Mis Clientes", así que ese chat resalta ese tab.
                ruta.startsWith("chat_detail") ->
                    listOf(UserChats.path, CoachClients.path, AdminChats.path)

                // ===== Clientes del entrenador =====
                ruta.startsWith("client_detail") ||
                        ruta.startsWith("create_goal") ||
                        ruta == CoachUsers.path -> listOf(CoachClients.path)

                // ===== Panel de administración =====
                ruta == AdminCreateCategory.path ||
                        ruta == AdminExercises.path ||
                        ruta.startsWith("admin_library_category") ||
                        ruta == AdminGlobalRoutines.path ||
                        ruta == AdminActivityLog.path ||
                        ruta == AdminMessages.path -> listOf(AdminPanel.path)

                // ===== Gestión de usuarios (admin) =====
                ruta == AdminUserList.path ||
                        ruta == AdminCreateUser.path ||
                        ruta == AdminSanctions.path -> listOf(AdminUserManagement.path)

                // ===== Perfil =====
                ruta == MonthlyReport.path -> listOf(Settings.path, Profile.path)

                ruta == Notifications.path -> listOf(Notifications.path, Settings.path)

                ruta == Login.path ||
                        ruta == Register.path ||
                        ruta == Home.path -> listOf(Profile.path)

                else -> emptyList()
            }
        }

        fun getBottomNavRoutes(role: String): List<Route> {
            return when (role.uppercase()) {
                "ADMIN" -> listOf(
                    AdminChats,
                    AdminUserManagement,
                    AdminPanel,
                    Settings,
                    Profile
                )
                "TRAINER", "COACH" -> listOf(
                    CoachClients,
                    CoachRoutines,
                    CoachPublicProfile,
                    // Sin tab de "Mensajes": ahora se chatea desde el ícono de chat en cada
                    // tarjeta de "Mis Clientes" (ver CoachClientsScreen.ClientCard).
                    Settings,
                    Profile
                )
                else -> listOf(
                    UserChats,
                    Library,
                    UserRoutines,
                    Settings,
                    Profile
                )
            }
        }
    }
}

/**
 * Permite que una pantalla "protegida" (por ahora, [com.shagox.apptrainingnow.ui.screen.RoutineActiveScreen])
 * intercepte la navegación de la bottom nav bar antes de que descarte su entrada del back stack.
 *
 * Sin esto, tocar un tab de [com.shagox.apptrainingnow.ui.components.BottomNavigationBarTN] hace
 * un popUpTo que saca la pantalla activa del back stack SIN pasar por ningún callback suyo — se
 * saltaba por completo el diálogo de "¿salir de la rutina?" (solo la flecha del header lo mostraba).
 * La pantalla protegida registra aquí un interceptor mientras está en composición; la bottom nav,
 * si hay uno registrado, le delega la navegación en vez de ejecutarla directo.
 */
object RoutineExitGuard {
    var interceptor: ((onConfirmado: () -> Unit) -> Unit)? = null
}