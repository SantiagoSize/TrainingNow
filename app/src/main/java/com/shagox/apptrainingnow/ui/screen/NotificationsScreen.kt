package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.notification.NotificationAction
import com.shagox.apptrainingnow.data.local.notification.NotificationEntity
import com.shagox.apptrainingnow.data.local.notification.NotificationType
import com.shagox.apptrainingnow.data.repository.INotificationRepository
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
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
import java.util.Date
import java.util.Locale

/**
 * Pantalla del apartado de notificaciones.
 * Muestra las notificaciones del usuario (recordatorios, mensajes, sistema, etc.)
 * que llegan al teléfono y se guardan en la app. Las rutinas compartidas por un
 * entrenador ([NotificationAction.ACCEPT_DECLINE_ROUTINE]) muestran Aceptar/Rechazar.
 */
@Composable
fun NotificationsScreen(
    notificationRepository: INotificationRepository,
    userId: Int,
    routineRepository: RoutineRepository? = null,
    /** Si se abre desde Ajustes, muestra el botón para volver. */
    onBack: (() -> Unit)? = null
) {
    val notifications by notificationRepository.getUserNotifications(userId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var procesando by remember { mutableStateOf<Int?>(null) }
    var resueltas by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        if (onBack != null) {
            com.shagox.apptrainingnow.ui.components.BackButtonTN(
                text = "Ajustes",
                onClick = onBack
            )
        }
        ScreenHeaderTN(
            subtitle = "Mis",
            title = "NOTIFICACIONES",
            onActionClick = null
        )

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = GrisTexto,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No tienes notificaciones",
                        color = GrisTexto,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Los recordatorios de rutina aparecerán aquí",
                        color = GrisTexto,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    val esInvitacionRutina = notification.actionType == NotificationAction.ACCEPT_DECLINE_ROUTINE.name
                    NotificationItem(
                        notification = notification,
                        mostrarAcciones = esInvitacionRutina && routineRepository != null &&
                            notification.id !in resueltas,
                        procesando = procesando == notification.id,
                        onAceptar = {
                            val routineName = notification.actionData ?: return@NotificationItem
                            procesando = notification.id
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    routineRepository?.acceptSharedRoutine(userId, routineName)
                                }
                                resueltas = resueltas + notification.id
                                procesando = null
                            }
                        },
                        onRechazar = {
                            val routineName = notification.actionData ?: return@NotificationItem
                            procesando = notification.id
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    routineRepository?.declineSharedRoutine(userId, routineName)
                                }
                                resueltas = resueltas + notification.id
                                procesando = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationEntity,
    mostrarAcciones: Boolean = false,
    procesando: Boolean = false,
    onAceptar: () -> Unit = {},
    onRechazar: () -> Unit = {}
) {
    val isReminder = notification.type == NotificationType.REMINDER.name
    val dateStr = try {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(notification.date))
    } catch (_: Exception) {
        ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, VerdeTN, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = GrisFondo),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = if (isReminder) Icons.Filled.NotificationsActive else Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = VerdeTN,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = notification.title,
                        color = TextoPrincipal,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = notification.message,
                        color = GrisTexto,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = dateStr,
                        color = GrisTexto.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            if (mostrarAcciones) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRechazar,
                        enabled = !procesando,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GrisTexto.copy(alpha = 0.25f), contentColor = TextoPrincipal)
                    ) { Text("Rechazar") }
                    Button(
                        onClick = onAceptar,
                        enabled = !procesando,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
                    ) {
                        if (procesando) CircularProgressIndicator(Modifier.size(18.dp), color = TextoSobreVerde)
                        else Text("Aceptar")
                    }
                }
            }
        }
    }
}
