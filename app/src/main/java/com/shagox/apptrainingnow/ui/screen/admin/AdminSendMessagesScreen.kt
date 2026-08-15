package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.chat.MessageEntity
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.data.repository.ChatRepository
import com.shagox.apptrainingnow.data.repository.IUserRepository
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enviar un mensaje interno (vía el chat existente) a uno o varios usuarios,
 * identificados por su correo. El admin escribe los correos, la pantalla resuelve
 * cada uno contra la base de usuarios y manda una copia del mensaje a cada uno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSendMessagesScreen(
    userRepository: IUserRepository,
    chatRepository: ChatRepository,
    adminId: Int,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enviar Mensajes") },
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
            Text("Destinatarios (por correo)", color = GrisTexto, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = correoInput,
                    onValueChange = { correoInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("correo@ejemplo.com, otro@ejemplo.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisTexto,
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

            if (destinatarios.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(destinatarios, key = { it.id }) { usuario ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GrisFondo, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${usuario.name} ${usuario.lastName}",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(usuario.email, color = GrisTexto, fontSize = 12.sp)
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto,
                    focusedLabelColor = VerdeTN,
                    cursorColor = VerdeTN
                ),
                shape = RoundedCornerShape(12.dp)
            )

            statusMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = if (statusEsError) Color(0xFFFF6B6B) else VerdeTN, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
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
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            destinatarios.forEach { destinatario ->
                                chatRepository.asegurarUsuarioLocal(destinatario)
                                chatRepository.sendMessage(
                                    MessageEntity(
                                        senderId = adminId,
                                        receiverId = destinatario.id,
                                        content = mensaje.trim()
                                    )
                                )
                            }
                        }
                        isSending = false
                        statusEsError = false
                        statusMessage = "Mensaje enviado a ${destinatarios.size} usuario(s)"
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
