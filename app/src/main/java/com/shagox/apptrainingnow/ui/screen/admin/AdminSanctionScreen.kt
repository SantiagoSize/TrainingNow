package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import java.util.*

/** Acción de sanción. */
enum class SanctionAction { SUSPEND, BAN, DELETE, LIFT }

/** Motivos típicos para agilizar la sanción (el campo de texto sigue siendo editable). */
private val MOTIVOS_FRECUENTES = listOf(
    "Acoso o lenguaje ofensivo en el chat",
    "Cuenta bot / registro automatizado",
    "Suplantación de otro usuario o entrenador",
    "Publicidad o venta no autorizada",
    "Contenido inapropiado en foto o publicación",
    "Cuenta creada para evadir un baneo anterior",
    "Incumplimiento reiterado de los Términos y Condiciones"
)

/** Color de acento por rol (mismo criterio que AdminUserListScreen). */
private fun colorDeRolSancion(role: String): Color = when (role) {
    "ADMIN" -> Color(0xFFFFC107)
    "TRAINER" -> VerdeTN
    else -> Color(0xFF64B5F6)
}

private fun etiquetaDeRolSancion(role: String): String = when (role) {
    "ADMIN" -> "Admin"
    "TRAINER" -> "Entrenador"
    else -> "Usuario"
}

/**
 * Suspender / Banear / Eliminar: obligatorio motivo y tiempo de suspensión.
 * Busca por nombre, correo o ID (antes solo por ID exacto).
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AdminSanctionScreen(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    actorId: Int = 0,
    actorName: String = "",
    actorRole: String = "ADMIN"
) {
    val auditLogRepository = remember { com.shagox.apptrainingnow.data.repository.AuditLogRepository() }
    val users by userRepository.getAllUsers().collectAsState(initial = emptyList())
    var busqueda by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var action by remember { mutableStateOf<SanctionAction?>(null) }
    var reason by remember { mutableStateOf("") }
    var suspendDays by remember { mutableStateOf("7") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val resultados = remember(users, busqueda, selectedUser) {
        val texto = busqueda.trim()
        if (selectedUser != null || texto.isBlank()) emptyList()
        else users.filter {
            "${it.name} ${it.lastName}".contains(texto, ignoreCase = true) ||
                    it.email.contains(texto, ignoreCase = true) ||
                    it.id.toString() == texto
        }.take(6)
    }

    Box(modifier = Modifier.fillMaxSize().background(NegroFondo)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Cabecera con degradado (misma estética que el resto del panel admin) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(VerdeTN.copy(alpha = 0.20f), NegroFondo)))
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
                                imageVector = Icons.Filled.GppBad,
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Suspender / Banear / Eliminar",
                            color = TextoPrincipal,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp)
            ) {
                Text("Buscar usuario", color = GrisTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                if (selectedUser == null) {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nombre, correo o ID") },
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
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (resultados.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(GrisFondo)
                                .border(1.dp, GrisBorde, RoundedCornerShape(14.dp))
                        ) {
                            resultados.forEachIndexed { index, user ->
                                ResultadoBusquedaRow(
                                    user = user,
                                    onClick = {
                                        selectedUser = user
                                        busqueda = ""
                                        message = null
                                    }
                                )
                                if (index < resultados.lastIndex) {
                                    HorizontalDivider(color = GrisBorde.copy(alpha = 0.5f))
                                }
                            }
                        }
                    } else if (busqueda.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Sin resultados para \"$busqueda\"", color = GrisTexto, fontSize = 12.sp)
                    }
                } else {
                    val user = selectedUser!!
                    val colorRol = colorDeRolSancion(user.role)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GrisFondo)
                            .border(1.dp, colorRol.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colorRol.copy(alpha = 0.18f))
                                .border(1.dp, colorRol.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${user.name.first()}${user.lastName.first()}",
                                color = colorRol,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${user.name} ${user.lastName}", color = TextoPrincipal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(user.email, color = GrisTexto, fontSize = 12.sp)
                            Spacer(Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colorRol.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(etiquetaDeRolSancion(user.role), color = colorRol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { selectedUser = null; action = null; reason = ""; message = null }) {
                            Text("Cambiar", color = VerdeTN, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Acción", color = GrisTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        SanctionAction.SUSPEND to "Suspender",
                        SanctionAction.BAN to "Banear",
                        SanctionAction.DELETE to "Eliminar",
                        SanctionAction.LIFT to "Levantar"
                    ).forEach { (a, label) ->
                        FilterChip(
                            selected = action == a,
                            onClick = { action = a },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (a == SanctionAction.DELETE) Color(0xFFE57373) else VerdeTN,
                                selectedLabelColor = if (a == SanctionAction.DELETE) Color.White else TextoSobreVerde,
                                containerColor = GrisFondo,
                                labelColor = TextoPrincipal
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = action == a,
                                borderColor = GrisBorde,
                                selectedBorderColor = if (a == SanctionAction.DELETE) Color(0xFFE57373) else VerdeTN
                            )
                        )
                    }
                }

                if (action == SanctionAction.DELETE && selectedUser != null) {
                    Spacer(Modifier.height(12.dp))
                    val user = selectedUser!!
                    val aviso = when (user.role) {
                        "ADMIN" -> "Solo se puede eliminar si queda al menos otro administrador activo."
                        "TRAINER" -> "Se liberarán sus clientes asignados (dejan de tener entrenador); sus rutinas ya creadas no se borran."
                        else -> null
                    }
                    aviso?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFB74D).copy(alpha = 0.12f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.GppBad, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                            Text(it, color = Color(0xFFFFB74D), fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }

                if (action == SanctionAction.SUSPEND || action == SanctionAction.BAN) {
                    Spacer(Modifier.height(16.dp))
                    Text("Motivos frecuentes", color = GrisTexto, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MOTIVOS_FRECUENTES.forEach { motivo ->
                            AssistChip(
                                onClick = { reason = motivo },
                                label = { Text(motivo, fontSize = 12.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = GrisFondo,
                                    labelColor = TextoPrincipal
                                )
                            )
                        }
                    }
                }
                if (action != null && action != SanctionAction.LIFT) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Motivo (obligatorio)") },
                        placeholder = { Text("Indica el motivo") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal,
                            focusedBorderColor = VerdeTN,
                            unfocusedBorderColor = GrisBorde
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                if (action == SanctionAction.SUSPEND) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = suspendDays,
                        onValueChange = { suspendDays = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Días de suspensión") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal,
                            focusedBorderColor = VerdeTN,
                            unfocusedBorderColor = GrisBorde
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                message?.let {
                    Text(it, color = if (it == "Hecho" || it == "Usuario eliminado") VerdeTN else Color(0xFFE57373), modifier = Modifier.padding(vertical = 8.dp))
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (selectedUser == null) {
                            message = "Busca y selecciona un usuario primero"
                            return@Button
                        }
                        if (action == null) {
                            message = "Elige una acción"
                            return@Button
                        }
                        if (reason.isBlank() && action != SanctionAction.LIFT) {
                            message = "El motivo es obligatorio"
                            return@Button
                        }
                        when (action) {
                            SanctionAction.DELETE -> showConfirmDelete = true
                            else -> applySanction(
                                userRepository = userRepository,
                                auditLogRepository = auditLogRepository,
                                actorId = actorId,
                                actorName = actorName,
                                actorRole = actorRole,
                                user = selectedUser!!,
                                action = action!!,
                                reason = reason.trim(),
                                suspendDays = suspendDays.toIntOrNull() ?: 7,
                                scope = scope,
                                onSuccess = { message = "Hecho"; onSuccess() },
                                onError = { message = it },
                                isLoading = { isLoading = it }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action == SanctionAction.DELETE) Color(0xFFE57373) else VerdeTN,
                        contentColor = if (action == SanctionAction.DELETE) Color.White else TextoSobreVerde
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = TextoSobreVerde)
                    else Text("Aplicar", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (showConfirmDelete && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Eliminar cuenta", color = TextoPrincipal) },
            text = {
                Text(
                    "¿Eliminar a ${selectedUser!!.name} ${selectedUser!!.lastName}? Motivo: $reason. No se puede deshacer.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                val userToDelete = selectedUser!!
                TextButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    userRepository.deleteUserById(userToDelete.id)
                                    auditLogRepository.log(
                                        actorId = actorId, actorName = actorName, actorRole = actorRole,
                                        action = "USER_DELETED", targetType = "USER",
                                        targetId = userToDelete.id, targetName = "${userToDelete.name} ${userToDelete.lastName}",
                                        details = "Motivo: $reason"
                                    )
                                    withContext(Dispatchers.Main) {
                                        showConfirmDelete = false
                                        message = "Usuario eliminado"
                                        onSuccess()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        showConfirmDelete = false
                                        message = e.message ?: "Error"
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Eliminar", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDelete = false }) {
                    Text("Cancelar", color = VerdeTN)
                }
            },
            containerColor = GrisFondo
        )
    }
}

/** Fila de resultado de búsqueda: avatar con inicial, nombre, correo y badge de rol. */
@Composable
private fun ResultadoBusquedaRow(user: UserEntity, onClick: () -> Unit) {
    val colorRol = colorDeRolSancion(user.role)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colorRol.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("${user.name.first()}${user.lastName.first()}", color = colorRol, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("${user.name} ${user.lastName}", color = TextoPrincipal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(user.email, color = GrisTexto, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colorRol.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(etiquetaDeRolSancion(user.role), color = colorRol, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun applySanction(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    auditLogRepository: com.shagox.apptrainingnow.data.repository.AuditLogRepository,
    actorId: Int,
    actorName: String,
    actorRole: String,
    user: UserEntity,
    action: SanctionAction,
    reason: String,
    suspendDays: Int,
    scope: kotlinx.coroutines.CoroutineScope,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    isLoading: (Boolean) -> Unit
) {
    isLoading(true)
    val nombreObjetivo = "${user.name} ${user.lastName}"
    scope.launch {
        withContext(Dispatchers.IO) {
            try {
                when (action) {
                    SanctionAction.SUSPEND -> {
                        val until = System.currentTimeMillis() + suspendDays * 24L * 60 * 60 * 1000
                        userRepository.suspendUser(user.id, until, reason)
                        auditLogRepository.log(
                            actorId = actorId, actorName = actorName, actorRole = actorRole,
                            action = "USER_SUSPENDED", targetType = "USER",
                            targetId = user.id, targetName = nombreObjetivo,
                            details = "Motivo: $reason · $suspendDays día(s)"
                        )
                    }
                    SanctionAction.BAN -> {
                        userRepository.banUser(user.id, reason)
                        auditLogRepository.log(
                            actorId = actorId, actorName = actorName, actorRole = actorRole,
                            action = "USER_BANNED", targetType = "USER",
                            targetId = user.id, targetName = nombreObjetivo,
                            details = "Motivo: $reason"
                        )
                    }
                    SanctionAction.DELETE -> {
                        userRepository.deleteUserById(user.id)
                        auditLogRepository.log(
                            actorId = actorId, actorName = actorName, actorRole = actorRole,
                            action = "USER_DELETED", targetType = "USER",
                            targetId = user.id, targetName = nombreObjetivo,
                            details = "Motivo: $reason"
                        )
                    }
                    SanctionAction.LIFT -> {
                        // Levanta ambas restricciones: baneo y suspensión
                        userRepository.unbanUser(user.id)
                        userRepository.clearSuspension(user.id)
                        auditLogRepository.log(
                            actorId = actorId, actorName = actorName, actorRole = actorRole,
                            action = "USER_RESTRICTION_LIFTED", targetType = "USER",
                            targetId = user.id, targetName = nombreObjetivo
                        )
                    }
                }
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Error") }
            } finally {
                isLoading(false)
            }
        }
    }
}
