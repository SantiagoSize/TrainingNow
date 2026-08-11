package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.repository.PasswordResetRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeAcento
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Ajustes de la app: avance mensual, notificaciones, apariencia
 * e información sobre Training Now.
 */
@Composable
fun SettingsScreen(
    temaClaro: Boolean,
    onCambiarTema: (Boolean) -> Unit,
    onVerAvanceMensual: () -> Unit,
    onVerNotificaciones: () -> Unit,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var mostrarInfo by remember { mutableStateOf(false) }
    var mostrarComoFunciona by remember { mutableStateOf(false) }
    val loginState by authViewModel.loginState.collectAsState()
    val loggedUser = loginState.loggedUser

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeaderTN(
            subtitle = "Tus",
            title = "AJUSTES",
            actionIcon = Icons.Filled.Settings,
            onActionClick = {}
        )

        // ==================== TU PROGRESO ====================
        SeccionTitulo("TU PROGRESO")
        OpcionAjuste(
            icono = Icons.Filled.CalendarToday,
            titulo = "Mi avance mensual",
            descripcion = "Días entrenados, calendario y rachas",
            onClick = onVerAvanceMensual
        )
        OpcionAjuste(
            icono = Icons.Filled.Notifications,
            titulo = "Notificaciones",
            descripcion = "Avisos de rutinas y mensajes",
            onClick = onVerNotificaciones
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ==================== APARIENCIA ====================
        SeccionTitulo("APARIENCIA")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OpcionTema(
                icono = Icons.Filled.DarkMode,
                etiqueta = "Nocturno",
                seleccionado = !temaClaro,
                onClick = { onCambiarTema(false) },
                modifier = Modifier.weight(1f)
            )
            OpcionTema(
                icono = Icons.Filled.LightMode,
                etiqueta = "Día",
                seleccionado = temaClaro,
                onClick = { onCambiarTema(true) },
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "Elige cómo quieres ver la app. El modo nocturno cuida la vista al entrenar de noche.",
            color = GrisTexto,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ==================== INFORMACIÓN ====================
        SeccionTitulo("INFORMACIÓN")
        OpcionAjuste(
            icono = Icons.Filled.Info,
            titulo = "Cómo funciona Training Now",
            descripcion = "Guía rápida para sacarle partido",
            onClick = { mostrarComoFunciona = !mostrarComoFunciona }
        )
        if (mostrarComoFunciona) {
            TarjetaTexto(
                listOf(
                    "1. Crea tu rutina" to "Ponle nombre (ej. Hipertrofia) y define los 7 días. Cada día lleva el nombre de la sesión (ej. Pecho y Tríceps) y hasta 10 ejercicios. Un día sin ejercicios cuenta como descanso.",
                    "2. Entrena y marca" to "Al abrir tu rutina verás la semana. Marca los ejercicios que completes: el día se pone verde y queda guardado.",
                    "3. Recibe recordatorios" to "Mantén presionada la campana dentro de la rutina para elegir la hora de cada día. La notificación te lleva directo al entrenamiento.",
                    "4. Revisa tu avance" to "En Mi avance mensual verás el calendario del mes, cuántos días entrenaste y si vas bien o necesitas mejorar.",
                    "5. Explora la biblioteca" to "Más de 50 ejercicios con pasos, consejos, errores comunes y series recomendadas."
                )
            )
        }

        OpcionAjuste(
            icono = Icons.Filled.Info,
            titulo = "Acerca de Training Now",
            descripcion = "Versión, equipo y contacto",
            onClick = { mostrarInfo = !mostrarInfo }
        )
        if (mostrarInfo) {
            TarjetaTexto(
                listOf(
                    "Training Now!" to "Aplicación de entrenamiento personal que conecta a usuarios con entrenadores. Organiza tus rutinas semanales, sigue tu progreso y recibe recordatorios en el momento justo.",
                    "Versión" to "1.0.0",
                    "Tecnología" to "App Android en Kotlin con Jetpack Compose y arquitectura de microservicios en Spring Boot.",
                    "Privacidad" to "Tus datos de entrenamiento se guardan en tu cuenta. Sin cuenta, quedan solo en este teléfono.",
                    "Contacto" to "contacsanser@gmail.com"
                )
            )
        }

        // ==================== CUENTA ====================
        if (loggedUser != null) {
            Spacer(modifier = Modifier.height(20.dp))
            SeccionTitulo("CUENTA")
            CuentaAcciones(authViewModel = authViewModel)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Training Now!  ·  Tu entrenamiento, tu ritmo",
            color = GrisTexto,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )
    }
}

/**
 * Botones de Cerrar sesión y Borrar cuenta, al final de Ajustes ("la tuerca").
 * Borrar cuenta exige 2 confirmaciones para evitar eliminaciones accidentales:
 * 1) "¿Estás seguro?" Sí/No. 2) Contraseña + confirmar contraseña + checkbox de responsabilidad.
 */
@Composable
private fun CuentaAcciones(authViewModel: AuthViewModel) {
    var mostrarConfirmacion1 by remember { mutableStateOf(false) }
    var mostrarConfirmacion2 by remember { mutableStateOf(false) }
    var mostrarCambiarPassword by remember { mutableStateOf(false) }
    val loginState by authViewModel.loginState.collectAsState()
    val email = loginState.loggedUser?.email.orEmpty()

    OpcionAjuste(
        icono = Icons.Filled.Lock,
        titulo = "Cambiar contraseña",
        descripcion = "Recibirás un código de verificación por correo",
        onClick = { mostrarCambiarPassword = true }
    )
    Spacer(modifier = Modifier.height(10.dp))

    if (mostrarCambiarPassword && email.isNotBlank()) {
        ChangePasswordDialog(
            email = email,
            onDismiss = { mostrarCambiarPassword = false }
        )
    }

    Button(
        onClick = { authViewModel.logout() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GrisFondo, contentColor = TextoPrincipal),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("CERRAR SESIÓN", fontWeight = FontWeight.SemiBold)
    }

    Button(
        onClick = { mostrarConfirmacion1 = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("BORRAR CUENTA", fontWeight = FontWeight.SemiBold)
    }

    if (mostrarConfirmacion1) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion1 = false },
            containerColor = GrisFondo,
            title = { Text("¿Eliminar tu cuenta?", color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Se eliminará tu cuenta, tus rutinas y tu progreso guardado. Esta acción no se puede deshacer.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion1 = false
                    mostrarConfirmacion2 = true
                }) {
                    Text("Sí", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion1 = false }) {
                    Text("No", color = VerdeTN)
                }
            }
        )
    }

    if (mostrarConfirmacion2) {
        ConfirmarBorradoCuentaDialog(
            authViewModel = authViewModel,
            onDismiss = { mostrarConfirmacion2 = false }
        )
    }
}

@Composable
private fun ConfirmarBorradoCuentaDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var aceptaResponsabilidad by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val passwordsCoinciden = password.isNotBlank() && password == confirmPassword
    val puedeEliminar = passwordsCoinciden && aceptaResponsabilidad && !enviando

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        containerColor = GrisFondo,
        title = { Text("Última confirmación", color = TextoPrincipal, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Para evitar eliminaciones accidentales, ingresa tu contraseña dos veces y confirma que entiendes que esta acción es irreversible.",
                    color = GrisTexto,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("Contraseña", color = GrisTexto) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Confirmar contraseña", color = GrisTexto) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (password.isNotBlank() && confirmPassword.isNotBlank() && !passwordsCoinciden) {
                    Text("Las contraseñas no coinciden", color = Color(0xFFE53935), fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = aceptaResponsabilidad,
                        onCheckedChange = { aceptaResponsabilidad = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFFE53935),
                            uncheckedColor = GrisTexto,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        "Entiendo que esta acción es irreversible y acepto la responsabilidad de eliminar mi cuenta.",
                        color = TextoPrincipal,
                        fontSize = 13.sp
                    )
                }
                if (error != null) {
                    Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    enviando = true
                    error = null
                    authViewModel.deleteAccount(password) { success, mensajeError ->
                        enviando = false
                        if (success) {
                            onDismiss()
                        } else {
                            error = mensajeError
                        }
                    }
                },
                enabled = puedeEliminar,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White)
            ) {
                if (enviando) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), color = Color.White)
                } else {
                    Text("ELIMINAR CUENTA", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!enviando) onDismiss() }) {
                Text("Cancelar", color = TextoPrincipal)
            }
        }
    )
}

/**
 * Cambiar contraseña desde Ajustes, con el mismo flujo de código por correo (EmailJS) que
 * "Olvidé mi contraseña", pero sin pedir el email (ya se conoce por la sesión activa). Solo se
 * muestran los primeros 3 caracteres del correo para confirmar a dónde llegará el código.
 */
@Composable
private fun ChangePasswordDialog(
    email: String,
    onDismiss: () -> Unit
) {
    val repository = remember { PasswordResetRepository() }
    val scope = rememberCoroutineScope()

    var step by remember { mutableIntStateOf(1) }
    var code by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )

    fun run(block: suspend () -> Result<Unit>, onOk: () -> Unit) {
        loading = true
        error = null
        scope.launch {
            block().fold(
                onSuccess = {
                    loading = false
                    onOk()
                },
                onFailure = {
                    loading = false
                    error = it.message
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = GrisFondo,
        title = {
            Text(
                text = when {
                    success -> "¡Listo!"
                    step == 1 -> "Cambiar contraseña"
                    step == 2 -> "Revisa tu correo"
                    else -> "Nueva contraseña"
                },
                color = VerdeTN,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    success -> {
                        Text("Tu contraseña fue actualizada.", color = TextoPrincipal)
                    }
                    step == 1 -> {
                        Text(
                            "Te enviaremos un código de verificación a ${maskEmail(email)}",
                            color = GrisTexto, fontSize = 13.sp
                        )
                    }
                    step == 2 -> {
                        Text(
                            "Ingresa el código enviado a ${maskEmail(email)} (expira en 10 minutos).",
                            color = GrisTexto, fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                            label = { Text("Código de 6 dígitos") }, singleLine = true,
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = {
                            run({ repository.requestCode(email) }) { error = null }
                        }, enabled = !loading) {
                            Text("Reenviar código", color = VerdeTN, fontSize = 12.sp)
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = newPass, onValueChange = { newPass = it },
                            label = { Text("Nueva contraseña") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPass, onValueChange = { confirmPass = it },
                            label = { Text("Confirmar contraseña") }, singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = fieldColors, modifier = Modifier.fillMaxWidth()
                        )
                        if (confirmPass.isNotEmpty() && newPass != confirmPass) {
                            Text(
                                "Las contraseñas no coinciden",
                                color = Color(0xFFE53935), fontSize = 12.sp
                            )
                        }
                    }
                }
                if (error != null) {
                    Text(error!!, color = Color(0xFFE53935), fontSize = 13.sp)
                }
                if (loading) {
                    CircularProgressIndicator(color = VerdeTN, modifier = Modifier.size(24.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        success -> onDismiss()
                        step == 1 -> run({ repository.requestCode(email) }) { step = 2 }
                        step == 2 -> run({ repository.verifyCode(email, code) }) { step = 3 }
                        else -> run({ repository.confirmReset(email, code, newPass) }) { success = true }
                    }
                },
                enabled = !loading && when {
                    success -> true
                    step == 1 -> true
                    step == 2 -> code.length == 6
                    else -> newPass.length >= 6 && newPass == confirmPass
                },
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
            ) {
                Text(
                    when {
                        success -> "ENTENDIDO"
                        step == 1 -> "ENVIAR CÓDIGO"
                        step == 2 -> "SIGUIENTE"
                        else -> "CAMBIAR CONTRASEÑA"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!success) {
                Button(
                    onClick = onDismiss,
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = TextoPrincipal)
                ) { Text("Cancelar") }
            }
        }
    )
}

/** Enmascara el correo mostrando solo los primeros 3 caracteres, ej: "usu•••@gmail.com". */
private fun maskEmail(email: String): String {
    val atIndex = email.indexOf('@')
    if (atIndex <= 0) return email
    val local = email.substring(0, atIndex)
    val domain = email.substring(atIndex)
    val visible = local.take(3)
    val hiddenLength = (local.length - visible.length).coerceAtLeast(3)
    return "$visible${"•".repeat(hiddenLength)}$domain"
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(
        text = texto,
        color = VerdeAcento,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
    )
}

@Composable
private fun OpcionAjuste(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GrisFondo)
            .border(1.dp, GrisBorde, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(VerdeAcento.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = VerdeAcento, modifier = Modifier.size(21.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = TextoPrincipal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(descripcion, color = GrisTexto, fontSize = 12.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = VerdeAcento,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun OpcionTema(
    icono: ImageVector,
    etiqueta: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (seleccionado) VerdeAcento.copy(alpha = 0.18f) else GrisFondo)
            .border(
                width = if (seleccionado) 2.dp else 1.dp,
                color = if (seleccionado) VerdeAcento else GrisBorde,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = if (seleccionado) VerdeAcento else GrisTexto,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            etiqueta,
            color = if (seleccionado) VerdeAcento else GrisTexto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Tarjeta desplegable con pares título / texto. */
@Composable
private fun TarjetaTexto(bloques: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeAcento.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        bloques.forEach { (titulo, texto) ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(VerdeAcento)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(titulo, color = VerdeAcento, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = texto,
                    color = TextoPrincipal.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}
