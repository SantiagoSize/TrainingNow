package com.shagox.apptrainingnow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shagox.apptrainingnow.navigation.Route
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde

private val INDICADOR_SIZE = 56.dp

/**
 * Barra de navegación inferior de TrainingNow.
 *
 * El indicador verde se **desplaza** horizontalmente hasta el ítem activo
 * (en lugar de encenderse y apagarse), con un movimiento suave y estable.
 */
@Composable
fun BottomNavigationBarTN(
    navController: NavController,
    userRole: String
) {
    val navContext = androidx.compose.ui.platform.LocalContext.current
    val role = userRole.takeIf { it.isNotBlank() } ?: "USER"
    val items = remember(role) { Route.getBottomNavRoutes(role) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (items.isEmpty()) return

    val rutaActual = currentDestination?.route

    // 1) Coincidencia directa con un ítem de la barra
    val indiceDirecto = items.indexOfFirst { route ->
        currentDestination?.hierarchy?.any { it.route == route.path } == true
    }

    // 2) Subpantallas: se resuelven a su sección según el rol actual
    val indicePorSeccion = Route.seccionesCandidatas(rutaActual)
        .firstNotNullOfOrNull { seccion ->
            items.indexOfFirst { it.path == seccion }.takeIf { it >= 0 }
        } ?: -1

    // 3) Si nada coincide, se conserva la última sección activa
    var ultimoIndiceValido by remember { mutableIntStateOf(0) }
    val indiceResuelto = when {
        indiceDirecto >= 0 -> indiceDirecto
        indicePorSeccion >= 0 -> indicePorSeccion
        else -> ultimoIndiceValido
    }
    LaunchedEffect(indiceResuelto) { ultimoIndiceValido = indiceResuelto }
    val selectedIndex = indiceResuelto

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        color = TextoSobreVerde,
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(GrisBorde)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp, bottom = 28.dp)
            ) {
                val anchoCelda = maxWidth / items.size
                // Posición animada del indicador: se desliza hasta el ítem activo
                val offsetIndicador by animateDpAsState(
                    targetValue = anchoCelda * selectedIndex + (anchoCelda - INDICADOR_SIZE) / 2,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "indicadorOffset"
                )

                // Indicador deslizante
                Box(
                    modifier = Modifier
                        .offset(x = offsetIndicador)
                        .align(Alignment.CenterStart)
                        .size(INDICADOR_SIZE)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = VerdeTN,
                            spotColor = VerdeTN
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(VerdeTN)
                )

                // Íconos: ocupan todo el alto para centrarse igual que el indicador
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, route ->
                        // Cada ítem ocupa exactamente una celda: coincide con el indicador
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            NavBarItem(
                                isSelected = index == selectedIndex,
                                iconVector = route.icon,
                                contentDescription = route.title,
                                onClick = {
                                    // Si el tab tocado es el de "Rutina" y hay una rutina activa
                                    // guardada (mismo mecanismo que usa MainActivity para retomarla
                                    // al reabrir la app), se vuelve DIRECTO a esa rutina en vez de a
                                    // la lista "Mis rutinas". Así "solo se elige una": cambiar de tab
                                    // no la descarta, solo la flecha del header (RoutineActiveScreen)
                                    // la abandona de verdad.
                                    val destino = if (route.path == com.shagox.apptrainingnow.navigation.Route.UserRoutines.path) {
                                        val prefs = navContext.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                        prefs.getString("active_routine_route", null) ?: route.path
                                    } else {
                                        route.path
                                    }
                                    // Ancla del popUpTo: el primer tab del rol actual, NO la ruta de
                                    // lanzamiento global de la app (que puede no pertenecer a este rol,
                                    // ej. "user_routines" no existe en la barra del admin y dejaba la
                                    // pila mal anclada tras el primer cambio de pestaña).
                                    navController.navigate(destino) {
                                        popUpTo(items.first().path) { saveState = true }
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
}

/**
 * Ícono de la barra. El fondo lo pinta el indicador deslizante;
 * aquí solo se anima el color para evitar movimientos superpuestos.
 */
@Composable
private fun NavBarItem(
    isSelected: Boolean,
    iconVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    val colorSobreVerde = TextoSobreVerde
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) colorSobreVerde else GrisTexto,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconColor"
    )
    Box(
        modifier = Modifier
            .size(INDICADOR_SIZE)
            .clip(RoundedCornerShape(18.dp))
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
            modifier = Modifier.size(27.dp),
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
        userRole = "USER"
    )
}
