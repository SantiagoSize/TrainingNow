package com.shagox.apptrainingnow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.VerdeTN

private val RojoEliminar = Color(0xFFE53935)

/**
 * Lista de acciones sobre un contacto de chat: ver perfil (opcional, se omite cuando este
 * contenido ya está embebido dentro del propio diálogo de perfil), silenciar/reactivar,
 * bloquear/desbloquear y eliminar el historial de la conversación. Se reutiliza tanto en
 * [MenuOpcionesContactoDialog] (long-press de 2 s en la cabecera del chat) como dentro de
 * [PerfilContactoDialog].
 */
@Composable
fun OpcionesContactoContenido(
    bloqueado: Boolean,
    silenciado: Boolean,
    onVerPerfil: (() -> Unit)? = null,
    onBloquear: () -> Unit,
    onDesbloquear: () -> Unit,
    onSilenciar: () -> Unit,
    onDesilenciar: () -> Unit,
    onEliminarConversacion: () -> Unit,
    onReportar: (() -> Unit)? = null,
    onEliminarDeMisChats: (() -> Unit)? = null
) {
    var confirmandoEliminar by remember { mutableStateOf(false) }
    var confirmandoEliminarDeMisChats by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (onVerPerfil != null) {
            OpcionContacto(
                icono = Icons.Default.Person,
                texto = "Ver perfil",
                tint = VerdeTN,
                onClick = onVerPerfil
            )
        }
        OpcionContacto(
            icono = if (silenciado) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
            texto = if (silenciado) "Reactivar notificaciones" else "Silenciar",
            tint = TextoPrincipal,
            onClick = { if (silenciado) onDesilenciar() else onSilenciar() }
        )
        OpcionContacto(
            icono = Icons.Default.Block,
            texto = if (bloqueado) "Desbloquear" else "Bloquear",
            tint = if (bloqueado) TextoPrincipal else RojoEliminar,
            onClick = { if (bloqueado) onDesbloquear() else onBloquear() }
        )
        if (onReportar != null) {
            OpcionContacto(
                icono = Icons.Default.Flag,
                texto = "Reportar",
                tint = RojoEliminar,
                onClick = onReportar
            )
        }

        if (!confirmandoEliminar) {
            OpcionContacto(
                icono = Icons.Default.DeleteForever,
                texto = "Eliminar conversación",
                tint = RojoEliminar,
                onClick = { confirmandoEliminar = true }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "¿Eliminar todo el historial de este chat? No se puede deshacer.",
                    color = GrisTexto,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { confirmandoEliminar = false }) {
                        Text("Cancelar", color = GrisTexto)
                    }
                    TextButton(onClick = {
                        confirmandoEliminar = false
                        onEliminarConversacion()
                    }) {
                        Text("Eliminar", color = RojoEliminar, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }

        if (onEliminarDeMisChats != null) {
            if (!confirmandoEliminarDeMisChats) {
                OpcionContacto(
                    icono = Icons.Default.DeleteForever,
                    texto = "Eliminar de mis chats",
                    tint = RojoEliminar,
                    onClick = { confirmandoEliminarDeMisChats = true }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "¿Quitar este contacto de Mis chats? Se borra la conversación y deja de aparecer en la lista.",
                        color = GrisTexto,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { confirmandoEliminarDeMisChats = false }) {
                            Text("Cancelar", color = GrisTexto)
                        }
                        TextButton(onClick = {
                            confirmandoEliminarDeMisChats = false
                            onEliminarDeMisChats()
                        }) {
                            Text("Eliminar", color = RojoEliminar, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OpcionContacto(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = tint)
        Text(text = texto, color = tint, fontSize = 15.sp)
    }
}

/**
 * Diálogo standalone con las opciones de contacto, disparado al mantener presionada
 * (2 s) la zona de foto+nombre en la cabecera del chat.
 */
@Composable
fun MenuOpcionesContactoDialog(
    onDismiss: () -> Unit,
    bloqueado: Boolean,
    silenciado: Boolean,
    onVerPerfil: () -> Unit,
    onBloquear: () -> Unit,
    onDesbloquear: () -> Unit,
    onSilenciar: () -> Unit,
    onDesilenciar: () -> Unit,
    onEliminarConversacion: () -> Unit,
    onReportar: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrisFondo,
        title = { Text("Opciones del contacto", color = TextoPrincipal, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OpcionesContactoContenido(
                    bloqueado = bloqueado,
                    silenciado = silenciado,
                    onVerPerfil = {
                        onDismiss()
                        onVerPerfil()
                    },
                    onBloquear = { onBloquear(); onDismiss() },
                    onDesbloquear = { onDesbloquear(); onDismiss() },
                    onSilenciar = { onSilenciar(); onDismiss() },
                    onDesilenciar = { onDesilenciar(); onDismiss() },
                    onEliminarConversacion = { onEliminarConversacion(); onDismiss() },
                    onReportar = onReportar?.let { { onDismiss(); it() } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = VerdeTN)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
