package com.shagox.apptrainingnow.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.shagox.apptrainingnow.navigation.Route
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN

/**
 * Barra de navegación inferior personalizada para TrainingNow.
 * Muestra diferentes opciones según el rol del usuario.
 * 
 * @param navController Controlador de navegación
 * @param userRole Rol del usuario: "USER", "TRAINER", "ADMIN"
 */
@Composable
fun BottomNavigationBarTN(navController: NavController, userRole: String) {
    // Obtener las rutas según el rol del usuario
    val items = Route.getBottomNavRoutes(userRole)

    NavigationBar(
        containerColor = NegroFondo,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            // CORRECCIÓN LÍNEA 39: Cambiado de 'routes' a 'path'
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.path } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // CORRECCIÓN LÍNEA 43: Cambiado de 'route' a 'path'
                    navController.navigate(screen.path) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) VerdeTN else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = screen.icon, contentDescription = screen.title, tint = Color.White)
                    }
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 10.sp,
                        color = if (isSelected) VerdeTN else Color.Gray
                    )
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }
    }
}