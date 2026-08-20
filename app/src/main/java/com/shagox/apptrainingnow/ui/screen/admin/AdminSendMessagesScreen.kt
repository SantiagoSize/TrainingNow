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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Segmento rápido de destinatarios: agrega de una vez a todos los usuarios de ese rol. */
private enum class SegmentoDestinatario(val etiqueta: String, val role: String, val icono: ImageVector) {
    ENTRENADORES("Entrenadores", "TRAINER", Icons.Filled.FitnessCenter),
    ADMINS("Admins", "ADMIN", Icons.Filled.AdminPanelSettings),
    USUARIOS("Usuarios", "USER", Icons.Filled.Group)
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

/**
 * Enviar un mensaje interno (vía el chat existente) a uno o varios destinatarios. Además de
 * agregar correos individuales (resaltados en verde apenas coinciden con una cuenta real), el
 * admin puede usar los botones de segmento para agregar de una vez a todos los Entrenadores,
 * Admins o Usuarios. El mensaje enviado lleva automáticamente fecha/hora y una firma formal
 * con el nombre del administrador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSendMessagesScreen(
    userRepository: IUserRepository,
    chatRepository: ChatRepository,
    adminId: Int,
    adminName: String = "Administración",
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val todosLosUsuarios by userRepository.getAllUsers().collectAsState(initial = emptyList())

    var correoInput by remember { mutableStateOf("") }
    var destinatarios by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var mensaje by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusEsError by remember { mutableStateOf(false) }

    // Coincidencia en vivo: apenas lo escrito en el campo calza con un correo real, se resalta
    // en verde con un ✓ antes de que el admin toque "Agregar".
    val coincidenciaEnVivo = remember(correoInput, todosLosUsuarios) {
        val q = correoInput.trim()
        if (q.isBlank() || !EMAIL_REGEX.matches(q)) null
        else todosLosUsuarios.firstOrNull { it.email.equals(q, ignoreCase = true) }
    }

    fun agregarUsuarios(nuevos: List<UserEntity>, mensajeSiVacio: String? = null) {
        val aAgregar = nuevos.filter { candidato -> destinatarios.none { it.id == candidato.id } }
        destinatarios = destinatarios + aAgregar
        statusEsError = false
        statusMessage = when {
            aAgregar.isNotEmpty() -> "${aAgregar.size} agregado(s)"
            mensajeSiVacio != null -> mensajeSiVacio
            else -> "Ya estaban en la lista"
        }
    }

    fun agregarDestinatarios() {
        val correos = correoInput.split(",", ";", "\n", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (correos.isEmpty()) return

        val noEncontrados = mutableListOf<String>()
        val nuevos = mutableListOf<UserEntity>()
        correos.forEach { correo ->
            val usuario = todosLosUsuarios.firstOrNull { it.email.equals(correo, ignoreCase = true) }
            if (usuario == null) {
                noEncontrados += correo
            } else if (destinatarios.none { it.id == usuario.id } && nuevos.none { it.id == usuario.id }) {
                nuevos += usuario
            }
        }
        destinatarios = destinatarios + nuevos
        correoInput = ""
        statusEsError = noEncontrados.isNotEmpty()
        statusMessage = when {
            noEncontrados.isNotEmpty() -> "No se encontró cuenta para: ${noEncontrados.joinToString(", ")}"
            nuevos.isEmpty() -> "Ese correo ya está en la lista"
            else -> null
        }
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
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Enviar Mensajes",
                                color = TextoPrincipal,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Por segmento o a correos individuales",
                                color = GrisTexto,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                Text("Enviar por segmento", color = GrisTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SegmentoDestinatario.entries.forEach { segmento ->
                        SegmentoButton(
                            segmento = segmento,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val delRol = todosLosUsuarios.filter { it.role == segmento.role && it.id != adminId }
                                agregarUsuarios(delRol, mensajeSiVacio = "No hay ${segmento.etiqueta.lowercase()} registrados")
                            }
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Agregar por correo individual", color = GrisTexto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = correoInput,
                        onValueChange = { correoInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("correo@ejemplo.com, otro@ejemplo.com") },
                        singleLine = true,
                        trailingIcon = {
                            if (coincidenciaEnVivo != null) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Cuenta encontrada", tint = VerdeTN)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal,
                            focusedBorderColor = VerdeTN,
                            unfocusedBorderColor = if (coincidenciaEnVivo != null) VerdeTN else GrisBorde,
                            cursorColor = VerdeTN
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { agregarDestinatarios() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(VerdeTN)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Agregar", tint = TextoSobreVerde)
                    }
                }
                if (coincidenciaEnVivo != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "✓ ${coincidenciaEnVivo.name} ${coincidenciaEnVivo.lastName} · ${coincidenciaEnVivo.role}",
                        color = VerdeTN,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (destinatarios.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Destinatarios (${destinatarios.size})",
                        color = GrisTexto,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(destinatarios, key = { it.id }) { usuario ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GrisFondo, RoundedCornerShape(10.dp))
                                    .border(1.dp, GrisBorde, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${usuario.name} ${usuario.lastName}",
                                        color = TextoPrincipal,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text("${usuario.email} · ${usuario.role}", color = GrisTexto, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { destinatarios = destinatarios.filterNot { it.id == usuario.id } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = GrisTexto)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = mensaje,
                    onValueChange = { mensaje = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    label = { Text("Mensaje") },
                    placeholder = { Text("Se le agrega automáticamente fecha, hora y tu firma") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisBorde,
                        focusedLabelColor = VerdeTN,
                        cursorColor = VerdeTN
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                statusMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = if (statusEsError) Color(0xFFFF6B6B) else VerdeTN, fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (destinatarios.isEmpty()) {
                            statusEsError = true
                            statusMessage = "Agrega al menos un destinatario"
                            return@Button
                        }
                        if (mensaje.isBlank()) {
                            statusEsError = true
                            statusMessage = "Escribe un mensaje"
                            return@Button
                        }
                        isSending = true
                        statusMessage = null
                        val formatoFirma = SimpleDateFormat(
                            "dd/MM/yyyy 'a las' HH:mm",
                            Locale.Builder().setLanguage("es").setRegion("CL").build()
                        )
                        val mensajeFormal = buildString {
                            append(mensaje.trim())
                            append("\n\n— Enviado el ${formatoFirma.format(Date())}")
                            append("\nAdministración TrainingNow!")
                            append("\n${adminName.ifBlank { "Administrador" }}")
                        }
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                destinatarios.forEach { destinatario ->
                                    chatRepository.asegurarUsuarioLocal(destinatario)
                                    chatRepository.sendMessage(
                                        MessageEntity(
                                            senderId = adminId,
                                            receiverId = destinatario.id,
                                            content = mensajeFormal
                                        )
                                    )
                                }
                            }
                            isSending = false
                            statusEsError = false
                            statusMessage = "Mensaje enviado a ${destinatarios.size} destinatario(s)"
                            mensaje = ""
                            destinatarios = emptyList()
                            onSuccess()
                        }
                    },
                    enabled = !isSending,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = TextoSobreVerde)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar")
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentoButton(
    segmento: SegmentoDestinatario,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(segmento.icono, contentDescription = null, tint = VerdeTN, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            text = segmento.etiqueta,
            color = TextoPrincipal,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
