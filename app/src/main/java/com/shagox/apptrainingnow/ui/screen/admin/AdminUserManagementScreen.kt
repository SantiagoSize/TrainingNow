package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

/**
 * Gestión de usuarios: Ver todos, Crear usuario, Suspender/Banear/Eliminar.
 */
@Composable
fun AdminUserManagementScreen(
    onBack: () -> Unit,
    onVerUsuarios: () -> Unit,
    onCrearUsuario: () -> Unit,
    onSuspenderBanearEliminar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Gestión de",
            title = "USUARIOS",
            actionIcon = Icons.Filled.People,
            onActionClick = null,
            actionTint = Color.White,
            actionBackgroundColor = VerdeTN
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                AdminUserCard(
                    icon = Icons.Filled.People,
                    title = "Ver todos los usuarios",
                    subtitle = "Lista completa de usuarios",
                    onClick = onVerUsuarios
                )
            }
            item {
                AdminUserCard(
                    icon = Icons.Filled.PersonAdd,
                    title = "Crear Usuario",
                    subtitle = "Usuarios, entrenadores o admins",
                    onClick = onCrearUsuario
                )
            }
            item {
                AdminUserCard(
                    icon = Icons.Filled.Block,
                    title = "Suspender / Banear / Eliminar",
                    subtitle = "Indicar motivo y tiempo de suspensión",
                    onClick = onSuspenderBanearEliminar
                )
            }
        }
    }
}

@Composable
private fun AdminUserCard(
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
                Icon(icon, contentDescription = null, tint = VerdeTN, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(text = subtitle, color = GrisTexto, fontSize = 13.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrisTexto, modifier = Modifier.size(24.dp))
        }
    }
}
