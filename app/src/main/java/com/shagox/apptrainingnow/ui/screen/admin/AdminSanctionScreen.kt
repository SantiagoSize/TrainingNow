package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/** Acción de sanción. */
enum class SanctionAction { SUSPEND, BAN, DELETE }

/**
 * Suspender / Banear / Eliminar: obligatorio motivo y tiempo de suspensión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSanctionScreen(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var searchId by remember { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf<SanctionAction?>(null) }
    var reason by remember { mutableStateOf("") }
    var suspendDays by remember { mutableStateOf("7") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suspender / Banear / Eliminar") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text("Buscar usuario por ID", color = GrisTexto)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchId,
                    onValueChange = { searchId = it.filter { c -> c.isDigit() }; searchError = null },
                    modifier = Modifier.weight(1f),
                    label = { Text("ID de usuario") },
                    placeholder = { Text("Ej: 1") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisTexto
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(
                    onClick = {
                        val id = searchId.trim().toIntOrNull()
                        if (id == null) {
                            searchError = "Introduce un ID válido"
                            selectedUser = null
                            return@IconButton
                        }
                        scope.launch {
                            val user = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                userRepository.getUserById(id)
                            }
                            selectedUser = user
                            searchError = if (user == null) "No hay usuario con ID $id" else null
                        }
                    }
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = VerdeTN)
                }
            }
            searchError?.let { Text(it, color = Color(0xFFE57373), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
            selectedUser?.let { user ->
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VerdeTN.copy(alpha = 0.2f))
                        .border(1.dp, VerdeTN, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "${user.name} ${user.lastName} (${user.email}) - ${user.role}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Acción", color = GrisTexto)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    SanctionAction.SUSPEND to "Suspender",
                    SanctionAction.BAN to "Banear",
                    SanctionAction.DELETE to "Eliminar"
                ).forEach { (a, label) ->
                    FilterChip(
                        selected = action == a,
                        onClick = { action = a },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VerdeTN,
                            selectedLabelColor = NegroFondo
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Motivo (obligatorio)") },
                placeholder = { Text("Indica el motivo de la sanción") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto
                ),
                shape = RoundedCornerShape(12.dp)
            )
            if (action == SanctionAction.SUSPEND) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = suspendDays,
                    onValueChange = { suspendDays = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Días de suspensión") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisTexto
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            message?.let { Text(it, color = VerdeTN, modifier = Modifier.padding(vertical = 8.dp)) }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (selectedUser == null) {
                        message = "Busca un usuario por ID primero"
                        return@Button
                    }
                    if (reason.isBlank()) {
                        message = "El motivo es obligatorio"
                        return@Button
                    }
                    when (action) {
                        SanctionAction.DELETE -> showConfirmDelete = true
                        else -> applySanction(
                            userRepository = userRepository,
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
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = NegroFondo)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = NegroFondo)
                else Text("Aplicar")
            }
        }
    }
    if (showConfirmDelete && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Eliminar cuenta", color = Color.White) },
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
                                    withContext(Dispatchers.Main) {
                                        showConfirmDelete = false
                                        message = "Usuario eliminado"
                                        onSuccess()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
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

private fun applySanction(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
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
    scope.launch {
        withContext(Dispatchers.IO) {
            try {
                when (action) {
                    SanctionAction.SUSPEND -> {
                        val until = System.currentTimeMillis() + suspendDays * 24L * 60 * 60 * 1000
                        userRepository.suspendUser(user.id, until, reason)
                    }
                    SanctionAction.BAN -> userRepository.banUser(user.id, reason)
                    SanctionAction.DELETE -> userRepository.deleteUserById(user.id)
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
