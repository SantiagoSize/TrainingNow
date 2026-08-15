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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.user.UserEntity
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todos los usuarios") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VerdeTN,
                    titleContentColor = NegroFondo,
                    navigationIconContentColor = NegroFondo
                )
            )
        },
        containerColor = NegroFondo
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por nombre, correo o ID") },
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
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto,
                    cursorColor = VerdeTN
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeTN,
                            selectedLabelColor = TextoSobreVerde,
                            containerColor = GrisFondo,
                            labelColor = Color.White
                        )
                    )
                }
            }
            Text(
                text = "${filteredUsers.size} usuario(s)",
                color = GrisTexto,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
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
                    contentPadding = PaddingValues(16.dp),
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
                    color = Color.White
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
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
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
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(VerdeTN.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${user.name.first()}${user.lastName.first()}",
                    color = VerdeTN,
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
                Text(text = user.email, color = GrisTexto, fontSize = 13.sp)
                Text(
                    text = "ID: ${user.id} • ${user.role}",
                    color = GrisTexto,
                    fontSize = 12.sp
                )
                if (user.isBanned) {
                    Text(text = "Baneado: ${user.banReason ?: ""}", color = Color(0xFFE57373), fontSize = 12.sp)
                } else if (isSuspended) {
                    Text(
                        text = "Suspendido hasta ${dateFormat.format(Date(suspendedUntil))}: ${user.suspendReason ?: ""}",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp
                    )
                }
            }
            Box {
                IconButton(onClick = { menuAbierto = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Acciones rápidas", tint = GrisTexto)
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
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
