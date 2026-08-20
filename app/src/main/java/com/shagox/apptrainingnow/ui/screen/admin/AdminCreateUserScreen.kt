package com.shagox.apptrainingnow.ui.screen.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.domain.validation.validateConfirm
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.components.IconoOjoContrasena
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.screen.TERMINOS_Y_CONDICIONES_TN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.utils.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Crear usuario (admin): usuarios, entrenadores o admins.
 * La autorización real la valida el backend (X-Admin-Id debe ser un ADMIN activo).
 * Reglas: staff (ADMIN/TRAINER) usa siempre @trainingnow.com (fijo, no editable) y no tiene
 * altura/peso (esos datos son de seguimiento físico, solo aplican a USER). Requiere aceptar
 * los Términos y Condiciones antes de crear la cuenta.
 */
@Composable
fun AdminCreateUserScreen(
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository,
    adminId: Int,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") }
    var specialty by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var birthDateStr by remember { mutableStateOf("") }
    var photoDataUri by remember { mutableStateOf<String?>(null) }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsInfo by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val dataUri = withContext(Dispatchers.IO) {
                    ImageCompressor.compressToDataUri(context, uri)
                }
                if (dataUri != null) photoDataUri = dataUri
            }
        }
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        focusedLabelColor = VerdeTN,
        cursorColor = VerdeTN
    )
    val textFieldShape = RoundedCornerShape(12.dp)
    val esStaff = role == "ADMIN" || role == "TRAINER"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        BackButtonTN(text = "Usuarios", onClick = onBack)
        ScreenHeaderTN(subtitle = "Crear", title = "NUEVO USUARIO")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(GrisTexto.copy(alpha = 0.15f))
                        .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoDataUri != null) {
                        AsyncImage(
                            model = photoDataUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = GrisTexto, modifier = Modifier.size(48.dp))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(VerdeTN),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Cambiar foto", tint = TextoSobreVerde, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") },
                singleLine = true,
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Apellido") },
                singleLine = true,
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(16.dp))
            Text("Rol", color = GrisTexto, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = role == "USER",
                    onClick = { role = "USER"; usernameError = null },
                    label = { Text("Usuario") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
                FilterChip(
                    selected = role == "TRAINER",
                    onClick = { role = "TRAINER"; usernameError = null },
                    label = { Text("Entrenador") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
                FilterChip(
                    selected = role == "ADMIN",
                    onClick = { role = "ADMIN"; usernameError = null },
                    label = { Text("Admin") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            if (esStaff) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { input ->
                        if (input.contains('@')) {
                            usernameError = "No puedes escribir \"@\": el dominio @trainingnow.com ya está incluido"
                        } else {
                            usernameError = null
                            email = input.filter { c -> !c.isWhitespace() }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre de usuario") },
                    placeholder = { Text("Ej: juan.perez") },
                    singleLine = true,
                    isError = usernameError != null,
                    suffix = { Text("@trainingnow.com", color = GrisTexto) },
                    colors = textFieldColors,
                    shape = textFieldShape
                )
                if (usernameError != null) {
                    Text(usernameError!!, color = androidx.compose.ui.graphics.Color(0xFFE57373), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    placeholder = { Text("correo@ejemplo.com") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = textFieldShape
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() }.take(16) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Teléfono") },
                placeholder = { Text("Solo números") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(12.dp))
            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconoOjoContrasena(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible }, tint = GrisTexto)
                },
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(12.dp))
            var confirmPasswordVisible by remember { mutableStateOf(false) }
            val confirmError = if (confirmPassword.isNotEmpty()) validateConfirm(password, confirmPassword) else null
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirmar contraseña") },
                singleLine = true,
                isError = confirmError != null,
                supportingText = confirmError?.let { { Text(it, color = androidx.compose.ui.graphics.Color(0xFFE57373)) } },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconoOjoContrasena(visible = confirmPasswordVisible, onToggle = { confirmPasswordVisible = !confirmPasswordVisible }, tint = GrisTexto)
                },
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(16.dp))
            Text("Género", color = GrisTexto, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = gender == "F",
                    onClick = { gender = "F" },
                    label = { Text("Mujer") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
                FilterChip(
                    selected = gender == "M",
                    onClick = { gender = "M" },
                    label = { Text("Hombre") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
                FilterChip(
                    selected = gender == "N",
                    onClick = { gender = "N" },
                    label = { Text("No especificar") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
            }
            if (!esStaff) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Altura (cm)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = textFieldShape
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Peso (kg)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors,
                        shape = textFieldShape
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = birthDateStr,
                onValueChange = { birthDateStr = autoFormatearFechaAdmin(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Fecha de nacimiento") },
                placeholder = { Text("dd/MM/aaaa") },
                singleLine = true,
                isError = birthDateStr.length == 10 && parseBirthDateAdmin(birthDateStr) == null,
                supportingText = {
                    if (birthDateStr.length == 10 && parseBirthDateAdmin(birthDateStr) == null) {
                        Text("Fecha inválida", color = androidx.compose.ui.graphics.Color(0xFFE53935))
                    }
                },
                colors = textFieldColors,
                shape = textFieldShape
            )
            if (role == "TRAINER") {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Especialidad (obligatoria)") },
                    placeholder = { Text("Ej: Fuerza, CrossFit, Running") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = textFieldShape
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = VerdeTN,
                        uncheckedColor = GrisTexto,
                        checkmarkColor = TextoSobreVerde
                    )
                )
                Text(
                    text = "Acepto los Términos y Condiciones",
                    color = TextoPrincipal,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showTermsInfo = true }) {
                    Icon(Icons.Filled.Info, contentDescription = "Ver Términos y Condiciones", tint = VerdeTN)
                }
            }
            message?.let { Text(it, color = VerdeTN, modifier = Modifier.padding(vertical = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (name.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
                        message = "Nombre, apellido, email y contraseña obligatorios"
                        return@Button
                    }
                    if (password != confirmPassword) {
                        message = "Las contraseñas no coinciden"
                        return@Button
                    }
                    if (usernameError != null) {
                        message = "Corrige el nombre de usuario antes de continuar"
                        return@Button
                    }
                    if (!termsAccepted) {
                        message = "Debes aceptar los Términos y Condiciones"
                        return@Button
                    }
                    val finalRole = role
                    val finalEmail = if (finalRole == "ADMIN" || finalRole == "TRAINER") {
                        "${email.trim().lowercase()}@trainingnow.com"
                    } else {
                        email.trim()
                    }
                    if (finalRole == "USER" && finalEmail.lowercase().endsWith("@trainingnow.com")) {
                        message = "El dominio @trainingnow.com es exclusivo del personal"
                        return@Button
                    }
                    if (finalRole == "TRAINER" && specialty.isBlank()) {
                        message = "La especialidad es obligatoria para entrenadores"
                        return@Button
                    }
                    isLoading = true
                    message = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                userRepository.insertUserByAdmin(
                                    adminId,
                                    UserEntity(
                                        role = finalRole,
                                        name = name.trim(),
                                        lastName = lastName.trim(),
                                        email = finalEmail,
                                        phone = phone.trim(),
                                        password = password,
                                        specializations = specialty.trim().takeIf { it.isNotBlank() },
                                        gender = gender,
                                        height = if (esStaff) null else height.toFloatOrNull(),
                                        weight = if (esStaff) null else weight.toFloatOrNull(),
                                        birthDate = parseBirthDateAdmin(birthDateStr),
                                        profilePhotoUrl = photoDataUri
                                    )
                                )
                                withContext(Dispatchers.Main) {
                                    message = "Usuario creado"
                                    onSuccess()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    message = e.message ?: "Error"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = TextoSobreVerde)
                else Text("Crear usuario")
            }
        }
    }

    if (showTermsInfo) {
        AlertDialog(
            onDismissRequest = { showTermsInfo = false },
            containerColor = GrisFondo,
            title = { Text("Términos y Condiciones", color = TextoPrincipal, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = TERMINOS_Y_CONDICIONES_TN, color = TextoPrincipal, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsInfo = false }) {
                    Text("CERRAR", color = VerdeTN, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

/** Agrega las barras "/" automáticamente mientras se escribe dd/MM/aaaa. */
private fun autoFormatearFechaAdmin(texto: String): String {
    val digitos = texto.filter { it.isDigit() }.take(8)
    return buildString {
        digitos.forEachIndexed { index, c ->
            if (index == 2 || index == 4) append('/')
            append(c)
        }
    }
}

/** Convierte "dd/MM/aaaa" a millis, o null si el texto está incompleto o es inválido. */
private fun parseBirthDateAdmin(texto: String): Long? {
    if (texto.length != 10) return null
    return try {
        val millis = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .apply { isLenient = false }
            .parse(texto)?.time ?: return null
        // Rechaza fechas futuras y edades fuera de un rango humano razonable (0-120 años),
        // no solo fechas calendario imposibles (isLenient ya cubre eso).
        val ahora = System.currentTimeMillis()
        if (millis > ahora) return null
        val edadAnios = (ahora - millis) / (365.25 * 24 * 60 * 60 * 1000)
        if (edadAnios !in 0.0..120.0) return null
        millis
    } catch (_: Exception) {
        null
    }
}
