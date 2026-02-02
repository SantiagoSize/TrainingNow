package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shagox.apptrainingnow.utils.NotificationHelper
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationType
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Segmento de notificación. */
enum class NotificationSegment { ALL, TRAINERS, ADMINS }

/**
 * Enviar notificación segmentada: todos, solo entrenadores o solo admins.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSendNotificationScreen(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    notificationRepository: com.shagox.apptrainingnow.data.repository.INotificationRepository,
    adminId: Int,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf(NotificationSegment.ALL) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviar Notificación") },
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
            Text("Destinatarios", color = GrisTexto)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    NotificationSegment.ALL to "Todos",
                    NotificationSegment.TRAINERS to "Entrenadores",
                    NotificationSegment.ADMINS to "Admins"
                ).forEach { (s, label) ->
                    FilterChip(
                        selected = segment == s,
                        onClick = { segment = s },
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
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text("Mensaje") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto
                ),
                shape = RoundedCornerShape(12.dp)
            )
            statusMessage?.let { Text(it, color = VerdeTN, modifier = Modifier.padding(vertical = 8.dp)) }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isBlank() || message.isBlank()) {
                        statusMessage = "Título y mensaje obligatorios"
                        return@Button
                    }
                    isLoading = true
                    statusMessage = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                val userIds = when (segment) {
                                    NotificationSegment.ALL -> userRepository.getAllUserIds()
                                    NotificationSegment.TRAINERS -> userRepository.getUserIdsByRole("TRAINER")
                                    NotificationSegment.ADMINS -> userRepository.getUserIdsByRole("ADMIN")
                                }
                                userIds.forEach { userId ->
                                    notificationRepository.saveNotification(
                                        NotificationEntity(
                                            userId = userId,
                                            title = title.trim(),
                                            message = message.trim(),
                                            type = NotificationType.SYSTEM.name,
                                            senderId = adminId
                                        )
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    if (userIds.contains(adminId)) {
                                        NotificationHelper.showPush(
                                            context,
                                            title.trim(),
                                            message.trim(),
                                            NotificationHelper.uniqueId(title, message)
                                        )
                                    }
                                    statusMessage = "Enviado a ${userIds.size} destinatarios"
                                    onSuccess()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    statusMessage = e.message ?: "Error"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = NegroFondo)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = NegroFondo)
                else Text("Enviar")
            }
        }
    }
}
