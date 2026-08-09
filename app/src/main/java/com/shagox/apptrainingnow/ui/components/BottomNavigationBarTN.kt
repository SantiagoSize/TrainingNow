package com.shagox.apptrainingnow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.shagox.apptrainingnow.navigation.Route
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN

private const val NAV_ITEM_SIZE_DP = 56

/**
 * Barra de navegación inferior personalizada para TrainingNow.
 * Muestra 5 iconos según el rol; cada slot tiene tamaño fijo para que todos se dibujen.
 *
 * @param navController Controlador de navegación
 * @param userRole Rol del usuario: "USER", "TRAINER", "ADMIN"
 * @param startDestinationRoute Ruta de destino inicial (evita acceder a navController.graph antes de setGraph)
 */
@Composable
fun BottomNavigationBarTN(
    navController: NavController,
    userRole: String,
    startDestinationRoute: String
) {
    val role = userRole.takeIf { it.isNotBlank() } ?: "USER"
    val items = remember(role) {
        Route.getBottomNavRoutes(role)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (items.isEmpty()) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        color = NegroFondo,
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFF2A2A2A))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { route ->
                    val icon = route.icon
                    val path = route.path
                    val title = route.title
                    val isSelected = currentDestination?.hierarchy?.any { it.route == path } == true
                    Box(
                        modifier = Modifier.size(NAV_ITEM_SIZE_DP.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NavBarItem(
                            isSelected = isSelected,
                            iconVector = icon,
                            contentDescription = title,
                            onClick = {
                                navController.navigate(path) {
                                    popUpTo(startDestinationRoute) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    isSelected: Boolean,
    iconVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) VerdeTN else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "bgColor"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color(0xFF808080),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconColor"
    )
    val boxSizeDp = if (isSelected) 58.dp else NAV_ITEM_SIZE_DP.dp
    val elevationDp = if (isSelected) 6.dp else 0.dp

    Box(
        modifier = Modifier
            .size(boxSizeDp)
            .shadow(
                elevation = elevationDp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isSelected) VerdeTN else Color.Transparent,
                spotColor = if (isSelected) VerdeTN else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(28.dp),
            tint = iconColor
        )
    }
}

// ==================== VISTA PREVIA ====================

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewBottomNavigationBarTN_User() {
    val navController = rememberNavController()
    BottomNavigationBarTN(
        navController = navController,
        userRole = "USER",
        startDestinationRoute = Route.UserRoutines.path
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewBottomNavigationBarTN_Coach() {
    val navController = rememberNavController()
    BottomNavigationBarTN(
        navController = navController,
        userRole = "TRAINER",
        startDestinationRoute = Route.CoachClients.path
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewBottomNavigationBarTN_Admin() {
    val navController = rememberNavController()
    BottomNavigationBarTN(
        navController = navController,
        userRole = "ADMIN",
        startDestinationRoute = Route.CoachClients.path
    )
}
