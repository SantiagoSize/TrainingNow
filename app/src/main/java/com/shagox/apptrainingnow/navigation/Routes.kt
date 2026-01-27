package com.shagox.apptrainingnow.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
    
    /** Mis rutinas (para usuarios) */
    data object UserRoutines : Route("user_routines", "Rutinas", Icons.Filled.DateRange)

    // ==================== PANTALLAS DE COACH ====================
    
    /** Lista de clientes del entrenador */
    data object CoachClients : Route("coach_clients", "Clientes", Icons.Filled.People)
    
    /** Gestión de rutinas del entrenador */
    data object CoachRoutines : Route("coach_routines", "Rutinas", Icons.Filled.FitnessCenter)
    
    /** Chats del entrenador con clientes */
    data object CoachChats : Route("coach_chats", "Mensajes", Icons.AutoMirrored.Filled.Chat)
    
    /** Detalle de un cliente */
    data object ClientDetail : Route("client_detail/{clientId}", "Cliente", Icons.Filled.Person) {
        fun createRoute(clientId: Int): String = "client_detail/$clientId"
    }
    
    /** Crear/editar rutina */
    data object CreateRoutine : Route("create_routine?clientId={clientId}", "Nueva Rutina", Icons.Filled.Edit) {
        fun createRoute(clientId: Int? = null): String = 
            if (clientId != null) "create_routine?clientId=$clientId" else "create_routine"
    }
    
    /** Crear objetivo para cliente */
    data object CreateGoal : Route("create_goal/{clientId}", "Nuevo Objetivo", Icons.Filled.Edit) {
        fun createRoute(clientId: Int): String = "create_goal/$clientId"
    }

    // ==================== PANTALLAS COMPARTIDAS ====================
    
    /** Notificaciones */
    data object Notifications : Route("notifications", "Alertas", Icons.Filled.Notifications)
    
    /** Perfil de usuario */
    data object Profile : Route("profile", "Perfil", Icons.Filled.Person)

    // ==================== AUTENTICACIÓN ====================
    
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

    companion object {
        /**
         * Rutas de la barra de navegación inferior para usuarios.
         */
        val userBottomNavRoutes = listOf(
            UserChats,
            Library,
            UserRoutines,
            Notifications,
            Profile
        )

        /**
         * Rutas de la barra de navegación inferior para entrenadores.
         */
        val coachBottomNavRoutes = listOf(
            CoachClients,
            CoachRoutines,
            CoachChats,
            Notifications,
            Profile
        )

        /**
         * Obtiene las rutas de navegación según el rol.
         */
        fun getBottomNavRoutes(role: String): List<Route> {
            return when (role.uppercase()) {
                "TRAINER", "COACH" -> coachBottomNavRoutes
                "ADMIN" -> coachBottomNavRoutes // Admin ve la misma interfaz que coach
                else -> userBottomNavRoutes
            }
        }
    }
}