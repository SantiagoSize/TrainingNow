package com.shagox.apptrainingnow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.VerdeTN

/**
 * Perfil de un contacto del chat (nombre, especialización si es entrenador, email y la
 * descripción que haya escrito). Se abre con un toque en la cabecera del chat, o
 * manteniendo presionada su tarjeta (3 s) desde la lista de contactos.
 *
 * Si se proveen los callbacks de acciones (bloquear/silenciar/eliminar), el diálogo
 * también muestra ahí mismo el menú de opciones sobre el contacto, para poder
 * gestionarlo sin salir del perfil.
 */
@Composable
fun PerfilContactoDialog(
    usuario: UserEntity,
    onDismiss: () -> Unit,
    bloqueado: Boolean = false,
    silenciado: Boolean = false,
    onBloquear: (() -> Unit)? = null,
    onDesbloquear: (() -> Unit)? = null,
    onSilenciar: (() -> Unit)? = null,
    onDesilenciar: (() -> Unit)? = null,
    onEliminarConversacion: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrisFondo,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(GrisTexto.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    val foto = usuario.profilePhotoUrl
                    if (foto != null && foto.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(foto).build(),
                            contentDescription = "Foto de ${usuario.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = usuario.name.firstOrNull()?.toString() ?: "U",
                            color = TextoPrincipal,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${usuario.name} ${usuario.lastName}",
                    color = TextoPrincipal,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (usuario.role == "TRAINER" && !usuario.specializations.isNullOrBlank()) {
                    Text(usuario.specializations, color = VerdeTN, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(usuario.email, color = GrisTexto, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Descripción",
                    color = GrisTexto,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = usuario.bio?.takeIf { it.isNotBlank() } ?: "Todavía no ha escrito una descripción.",
                    color = TextoPrincipal,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (onEliminarConversacion != null && onBloquear != null && onDesbloquear != null &&
                    onSilenciar != null && onDesilenciar != null
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = GrisTexto.copy(alpha = 0.2f))
                    OpcionesContactoContenido(
                        bloqueado = bloqueado,
                        silenciado = silenciado,
                        onBloquear = onBloquear,
                        onDesbloquear = onDesbloquear,
                        onSilenciar = onSilenciar,
                        onDesilenciar = onDesilenciar,
                        onEliminarConversacion = {
                            onEliminarConversacion()
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = VerdeTN)
            }
        }
    )
}
