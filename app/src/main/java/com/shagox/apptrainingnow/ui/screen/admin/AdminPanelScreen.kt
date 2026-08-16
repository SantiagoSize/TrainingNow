package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal

/**
 * Panel principal de administración.
 * Opciones: Biblioteca, Entrenamiento Global, Rutinas Globales, Enviar Mensajes,
 * Actividad, Gestión de Usuarios. Crear/editar categorías ahora se hace desde
 * dentro de Biblioteca, no tiene entrada propia aquí.
 * Ajustes ya no vive aquí: se accede desde su propia pestaña en la barra inferior.
 */
@Composable
fun AdminPanelScreen(
    onBiblioteca: () -> Unit,
    onEntrenamientoGlobal: () -> Unit,
    onRutinasGlobales: () -> Unit,
    onEnviarMensajes: () -> Unit,
    onVerActividad: () -> Unit,
    onVerReportes: () -> Unit,
    onGestionUsuarios: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Panel de",
            title = "ADMINISTRACIÓN",
            actionIcon = Icons.Filled.Person,
            onActionClick = onGestionUsuarios,
            actionTint = Color.White,
            actionBackgroundColor = VerdeTN
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                AdminPanelCard(
                    icon = Icons.Filled.FitnessCenter,
                    title = "Biblioteca",
                    subtitle = "Categorías y ejercicios",
                    onClick = onBiblioteca
                )
            }
            item {
                AdminPanelCard(
                    icon = Icons.Filled.FitnessCenter,
                    title = "Entrenamiento Global",
                    subtitle = "Publicar una rutina recomendada para todos",
                    onClick = onEntrenamientoGlobal
                )
            }
            item {
                AdminPanelCard(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "Rutinas Globales",
                    subtitle = "Ver y editar las rutinas ya publicadas",
                    onClick = onRutinasGlobales
                )
            }
            item {
                AdminPanelCard(
                    icon = Icons.AutoMirrored.Filled.Send,
                    title = "Enviar Mensajes",
                    subtitle = "A uno o varios usuarios por su correo",
                    onClick = onEnviarMensajes
                )
            }
            item {
                AdminPanelCard(
                    icon = Icons.Filled.Schedule,
                    title = "Actividad",
                    subtitle = "Registro de lo que hace el personal",
                    onClick = onVerActividad
                )
            }
            item {
                AdminPanelCard(
                    icon = Icons.Filled.Flag,
                    title = "Reportes",
                    subtitle = "Usuarios reportados pendientes de revisión",
                    onClick = onVerReportes
                )
            }
        }
    }
}

@Composable
private fun AdminPanelCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VerdeTN.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = VerdeTN,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = GrisTexto,
                    fontSize = 13.sp
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = GrisTexto,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
