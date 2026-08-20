package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Lista de todos los usuarios (admin).
 */

/** Filtro de rol para la lista de usuarios. */
private enum class RoleFilter { TODOS, ADMIN, USUARIOS, ENTRENADOR }

/** Color de acento por rol, para el avatar, el badge y el borde de la tarjeta. */
private fun colorDeRol(role: String): Color = when (role) {
    "ADMIN" -> Color(0xFFFFC107)
    "TRAINER" -> VerdeTN
    else -> Color(0xFF64B5F6)
}

private fun etiquetaDeRol(role: String): String = when (role) {
    "ADMIN" -> "Admin"
    "TRAINER" -> "Entrenador"
    else -> "Usuario"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserListScreen(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    onBack: () -> Unit,
    actorId: Int = 0,
    actorName: String = "",
    actorRole: String = "ADMIN"
) {
    val auditLogRepository = remember { com.shagox.apptrainingnow.data.repository.AuditLogRepository() }
    val users by userRepository.getAllUsers().collectAsState(initial = emptyList())
    var roleFilter by remember { mutableStateOf(RoleFilter.TODOS) }
    var busqueda by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val filteredUsers = remember(users, roleFilter, busqueda) {
        val porRol = when (roleFilter) {
            RoleFilter.TODOS -> users
            RoleFilter.ADMIN -> users.filter { it.role == "ADMIN" }
            RoleFilter.USUARIOS -> users.filter { it.role == "USER" }
            RoleFilter.ENTRENADOR -> users.filter { it.role == "TRAINER" }
        }
        val texto = busqueda.trim()
        if (texto.isBlank()) porRol
        else porRol.filter {
            "${it.name} ${it.lastName}".contains(texto, ignoreCase = true) ||
                    it.email.contains(texto, ignoreCase = true) ||
                    it.id.toString() == texto
        }
    }

    var usuarioParaAccion by remember { mutableStateOf<UserEntity?>(null) }
    var accionRapida by remember { mutableStateOf<SanctionAction?>(null) }
    var motivoRapido by remember { mutableStateOf("") }
    var diasSuspension by remember { mutableStateOf("7") }
    var isProcesando by remember { mutableStateOf(false) }
    var errorAccion by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(NegroFondo)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Cabecera con degradado (misma estética que el resto del panel admin) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(VerdeTN.copy(alpha = 0.20f), NegroFondo))
                    )
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp)
            ) {
                Column {
                    BackButtonTN(text = "Volver", onClick = onBack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(VerdeTN.copy(alpha = 0.2f))
                                .border(1.dp, VerdeTN.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Group,
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Todos los usuarios",
                                color = TextoPrincipal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${filteredUsers.size} de ${users.size} usuario(s)",
                                color = GrisTexto,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ===== Búsqueda =====
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp),
                placeholder = { Text("Buscar por nombre, correo o ID", color = GrisTexto) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = GrisTexto) },
                trailingIcon = {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = GrisTexto)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextoPrincipal,
                    unfocusedTextColor = TextoPrincipal,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisBorde,
                    cursorColor = VerdeTN
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // ===== Filtros por rol =====
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    RoleFilter.TODOS to "Todos",
                    RoleFilter.ADMIN to "Admin",
                    RoleFilter.USUARIOS to "Usuarios",
                    RoleFilter.ENTRENADOR to "Entrenador"
                ).forEach { (filter, label) ->
                    FilterChip(
                        selected = roleFilter == filter,
                        onClick = { roleFilter = filter },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeTN,
                            selectedLabelColor = TextoSobreVerde,
                            containerColor = GrisFondo,
                            labelColor = TextoPrincipal
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = roleFilter == filter,
                            borderColor = GrisBorde,
                            selectedBorderColor = VerdeTN
                        )
                    )
                }
            }

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (users.isEmpty()) "No hay usuarios" else "No hay usuarios con este filtro o búsqueda",
                        color = GrisTexto
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        UserListCard(
                            user = user,
                            onAccionRapida = { accion ->
                                errorAccion = null
                                usuarioParaAccion = user
                                accionRapida = accion
                                motivoRapido = ""
                                if (accion == SanctionAction.LIFT) {
                                    isProcesando = true
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            try {
                                                userRepository.unbanUser(user.id)
                                                userRepository.clearSuspension(user.id)
                                                auditLogRepository.log(
                                                    actorId = actorId,
                                                    actorName = actorName,
                                                    actorRole = actorRole,
                                                    action = "USER_RESTRICTION_LIFTED",
                                                    targetType = "USER",
                                                    targetId = user.id,
                                                    targetName = "${user.name} ${user.lastName}"
                                                )
                                            } catch (_: Exception) { }
                                        }
                                        isProcesando = false
                                        usuarioParaAccion = null
                                        accionRapida = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para Banear / Suspender (requieren motivo) y confirmación de Eliminar
    val usuarioActivo = usuarioParaAccion
    val accionActiva = accionRapida
    if (usuarioActivo != null && accionActiva != null && accionActiva != SanctionAction.LIFT) {
        AlertDialog(
            onDismissRequest = { if (!isProcesando) { usuarioParaAccion = null; accionRapida = null } },
            containerColor = GrisFondo,
            title = {
                Text(
                    when (accionActiva) {
                        SanctionAction.BAN -> "Banear a ${usuarioActivo.name}"
                        SanctionAction.SUSPEND -> "Suspender a ${usuarioActivo.name}"
                        SanctionAction.DELETE -> "Eliminar a ${usuarioActivo.name}"
                        SanctionAction.LIFT -> ""
                    },
                    color = TextoPrincipal
                )
            },
            text = {
                Column {
                    if (accionActiva == SanctionAction.DELETE) {
                        Text(
                            "Esta acción no se puede deshacer. Se eliminará la cuenta y sus datos.",
                            color = GrisTexto,
                            fontSize = 13.sp
                        )
                    } else {
                        OutlinedTextField(
                            value = motivoRapido,
                            onValueChange = { motivoRapido = it },
                            label = { Text("Motivo") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextoPrincipal,
                                unfocusedTextColor = TextoPrincipal,
                                focusedBorderColor = VerdeTN,
                                unfocusedBorderColor = GrisTexto
                            )
                        )
                        if (accionActiva == SanctionAction.SUSPEND) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = diasSuspension,
                                onValueChange = { diasSuspension = it.filter { c -> c.isDigit() } },
                                label = { Text("Días de suspensión") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextoPrincipal,
                                    unfocusedTextColor = TextoPrincipal,
                                    focusedBorderColor = VerdeTN,
                                    unfocusedBorderColor = GrisTexto
                                )
                            )
                        }
                    }
                    errorAccion?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Color(0xFFE57373), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isProcesando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (accionActiva == SanctionAction.DELETE) Color(0xFFE57373) else VerdeTN,
                        contentColor = if (accionActiva == SanctionAction.DELETE) Color.White else TextoSobreVerde
                    ),
                    onClick = {
                        if (accionActiva != SanctionAction.DELETE && motivoRapido.isBlank()) {
                            errorAccion = "El motivo es obligatorio"
                            return@Button
                        }
                        isProcesando = true
                        errorAccion = null
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    val nombreObjetivo = "${usuarioActivo.name} ${usuarioActivo.lastName}"
                                    when (accionActiva) {
                                        SanctionAction.BAN -> {
                                            userRepository.banUser(usuarioActivo.id, motivoRapido.trim())
                                            auditLogRepository.log(
                                                actorId = actorId, actorName = actorName, actorRole = actorRole,
                                                action = "USER_BANNED", targetType = "USER",
                                                targetId = usuarioActivo.id, targetName = nombreObjetivo,
                                                details = "Motivo: ${motivoRapido.trim()}"
                                            )
                                        }
                                        SanctionAction.SUSPEND -> {
                                            val dias = diasSuspension.toIntOrNull() ?: 7
                                            val until = System.currentTimeMillis() + dias * 24L * 60 * 60 * 1000
                                            userRepository.suspendUser(usuarioActivo.id, until, motivoRapido.trim())
                                            auditLogRepository.log(
                                                actorId = actorId, actorName = actorName, actorRole = actorRole,
                                                action = "USER_SUSPENDED", targetType = "USER",
                                                targetId = usuarioActivo.id, targetName = nombreObjetivo,
                                                details = "Motivo: ${motivoRapido.trim()} · $dias día(s)"
                                            )
                                        }
                                        SanctionAction.DELETE -> {
                                            userRepository.deleteUserById(usuarioActivo.id)
                                            auditLogRepository.log(
                                                actorId = actorId, actorName = actorName, actorRole = actorRole,
                                                action = "USER_DELETED", targetType = "USER",
                                                targetId = usuarioActivo.id, targetName = nombreObjetivo
                                            )
                                        }
                                        SanctionAction.LIFT -> { }
                                    }
                                    withContext(Dispatchers.Main) {
                                        isProcesando = false
                                        usuarioParaAccion = null
                                        accionRapida = null
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isProcesando = false
                                        errorAccion = e.message ?: "Error al aplicar la acción"
                                    }
                                }
                            }
                        }
                    }
                ) {
                    if (isProcesando) CircularProgressIndicator(Modifier.size(20.dp), color = TextoSobreVerde)
                    else Text(if (accionActiva == SanctionAction.DELETE) "Eliminar" else "Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isProcesando) { usuarioParaAccion = null; accionRapida = null } }) {
                    Text("Cancelar", color = GrisTexto)
                }
            }
        )
    }
}

@Composable
private fun UserListCard(
    user: UserEntity,
    onAccionRapida: (SanctionAction) -> Unit
) {
    val suspendedUntil = user.suspendedUntil
    val isSuspended = suspendedUntil != null && suspendedUntil > System.currentTimeMillis()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var menuAbierto by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val colorRol = colorDeRol(user.role)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GrisFondo)
            .border(1.dp, colorRol.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colorRol.copy(alpha = 0.18f))
                    .border(1.dp, colorRol.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${user.name.first()}${user.lastName.first()}",
                    color = colorRol,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${user.name} ${user.lastName}",
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = user.email, color = GrisTexto, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colorRol.copy(alpha = 0.15f))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(etiquetaDeRol(user.role), color = colorRol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("ID ${user.id}", color = GrisTexto, fontSize = 11.sp)
                }
                if (user.isBanned) {
                    Spacer(modifier = Modifier.height(6.dp))
                    EstadoBadge(
                        icono = Icons.Filled.Block,
                        texto = "Baneado: ${user.banReason ?: ""}",
                        color = Color(0xFFE57373)
                    )
                } else if (isSuspended) {
                    Spacer(modifier = Modifier.height(6.dp))
                    EstadoBadge(
                        icono = Icons.Filled.Warning,
                        texto = "Suspendido hasta ${dateFormat.format(Date(suspendedUntil))}: ${user.suspendReason ?: ""}",
                        color = Color(0xFFFFB74D)
                    )
                }
            }
            Box {
                IconButton(onClick = { menuAbierto = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Acciones", tint = GrisTexto)
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    DropdownMenuItem(
                        text = { Text("Copiar correo") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            clipboard.setText(AnnotatedString(user.email))
                            android.widget.Toast.makeText(context, "Correo copiado", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copiar ID") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuAbierto = false
                            clipboard.setText(AnnotatedString(user.id.toString()))
                            android.widget.Toast.makeText(context, "ID copiado", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(color = GrisBorde.copy(alpha = 0.5f))
                    if (user.isBanned || isSuspended) {
                        DropdownMenuItem(
                            text = { Text("Levantar restricción") },
                            onClick = { menuAbierto = false; onAccionRapida(SanctionAction.LIFT) }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Suspender") },
                            onClick = { menuAbierto = false; onAccionRapida(SanctionAction.SUSPEND) }
                        )
                        DropdownMenuItem(
                            text = { Text("Banear") },
                            onClick = { menuAbierto = false; onAccionRapida(SanctionAction.BAN) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Eliminar cuenta", color = Color(0xFFE57373)) },
                        onClick = { menuAbierto = false; onAccionRapida(SanctionAction.DELETE) }
                    )
                }
            }
        }
    }
}

/** Badge compacto para el estado de sanción (baneado/suspendido) de una tarjeta de usuario. */
@Composable
private fun EstadoBadge(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Text(texto, color = color, fontSize = 11.sp, lineHeight = 14.sp)
    }
}
