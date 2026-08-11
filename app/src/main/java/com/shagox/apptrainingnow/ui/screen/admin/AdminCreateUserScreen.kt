package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.user.UserEntity
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Código requerido para crear cuentas de Admin o Entrenador. */

/**
 * Crear usuario (admin): usuarios, entrenadores o admins.
 * La autorización real la valida el backend (X-Admin-Id debe ser un ADMIN activo).
 * Reglas: staff (ADMIN/TRAINER) requiere correo @trainingnow.com; entrenador exige especialidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("USER") }
    var specialty by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Usuario") },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto
            )
            val textFieldShape = RoundedCornerShape(12.dp)

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
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Teléfono") },
                singleLine = true,
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Contraseña") },
                singleLine = true,
                colors = textFieldColors,
                shape = textFieldShape
            )
            Spacer(Modifier.height(16.dp))
            Text("Rol", color = GrisTexto)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = role == "USER",
                    onClick = { role = "USER" },
                    label = { Text("Usuario") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
                FilterChip(
                    selected = role == "TRAINER",
                    onClick = { role = "TRAINER" },
                    label = { Text("Entrenador") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
                FilterChip(
                    selected = role == "ADMIN",
                    onClick = { role = "ADMIN" },
                    label = { Text("Admin") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdeTN,
                        selectedLabelColor = TextoSobreVerde
                    )
                )
            }
            if (role == "ADMIN" || role == "TRAINER") {
                Spacer(Modifier.height(12.dp))
                Text(
                    "El personal (Admin/Entrenador) debe usar correo @trainingnow.com",
                    color = GrisTexto,
                    fontSize = 12.sp
                )
            }
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
            message?.let { Text(it, color = VerdeTN, modifier = Modifier.padding(vertical = 8.dp)) }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (name.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
                        message = "Nombre, apellido, email y contraseña obligatorios"
                        return@Button
                    }
                    val finalRole = role
                    if ((finalRole == "ADMIN" || finalRole == "TRAINER") &&
                        !email.trim().lowercase().endsWith("@trainingnow.com")
                    ) {
                        message = "El personal debe usar correo @trainingnow.com"
                        return@Button
                    }
                    if (finalRole == "USER" && email.trim().lowercase().endsWith("@trainingnow.com")) {
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
                                        email = email.trim(),
                                        phone = phone.trim(),
                                        password = password,
                                        specializations = specialty.trim().takeIf { it.isNotBlank() }
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
}
