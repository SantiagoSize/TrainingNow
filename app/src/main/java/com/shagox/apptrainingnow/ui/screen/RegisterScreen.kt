package com.shagox.apptrainingnow.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel
import com.shagox.apptrainingnow.utils.ComposeFileProvider

@Composable
fun RegisterScreen(
    vm: AuthViewModel,
    onRegistered: () -> Unit,
    onGoLogin: () -> Unit
) {
    val state by vm.register.collectAsState()
    val context = LocalContext.current
    
    // Estado para guardar el URI de la imagen capturada
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var hasImage by remember { mutableStateOf(false) }

    // 1. Launcher para capturar la foto usando TakePicture con URI
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            hasImage = true
        }
    }

    // 2. Launcher para pedir el permiso de cámara
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Si el usuario acepta, intentamos lanzar la cámara inmediatamente
            try {
                val permissionCheckResult = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                )
                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                    val uri = ComposeFileProvider.getImageUri(context)
                    imageUri = uri
                    cameraLauncher.launch(uri)
                }
            } catch (e: Exception) {
                // Si hay cualquier error, simplemente lo ignoramos para no crashear la app
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            onRegistered()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium, color = VerdeTN)

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTÓN DE FOTO PROTEGIDO ---
        Button(
            onClick = {
                try {
                    // Verificamos si el permiso ya existe
                    val permissionCheckResult = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    )
                    
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        // Si hay permiso, generamos URI y lanzamos la cámara directamente
                        try {
                            val uri = ComposeFileProvider.getImageUri(context)
                            imageUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            // Si hay error al generar URI o lanzar cámara, lo ignoramos
                            e.printStackTrace()
                        }
                    } else {
                        // Si no hay permiso, lo pedimos
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                } catch (e: Exception) {
                    // Si hay cualquier error, lo ignoramos para no crashear la app
                    e.printStackTrace()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text(
                text = if (hasImage) "Foto Capturada ✓" else "Tomar Foto de Perfil",
                color = Color.White
            )
        }
        
        // Mostrar mensaje si la foto fue capturada
        if (hasImage) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Foto de perfil lista",
                color = VerdeTN,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nombre
        OutlinedTextField(
            value = state.name,
            onValueChange = { vm.onNameChange(it) },
            label = { Text("Nombre Completo") },
            isError = state.nameError != null,
            modifier = Modifier.fillMaxWidth()
        )
        state.nameError?.let { Text(it, color = Color.Red) }

        // Teléfono
        OutlinedTextField(
            value = state.phone,
            onValueChange = { vm.onPhoneChange(it) },
            label = { Text("Teléfono") },
            isError = state.phoneError != null,
            modifier = Modifier.fillMaxWidth()
        )
        state.phoneError?.let { Text(it, color = Color.Red) }

        // Email
        OutlinedTextField(
            value = state.email,
            onValueChange = { vm.onRegisterEmailChange(it) },
            label = { Text("Email") },
            isError = state.emailError != null,
            modifier = Modifier.fillMaxWidth()
        )
        state.emailError?.let { Text(it, color = Color.Red) }

        // Contraseña
        OutlinedTextField(
            value = state.pass,
            onValueChange = { vm.onRegisterPassChange(it) },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            isError = state.passError != null,
            modifier = Modifier.fillMaxWidth()
        )
        state.passError?.let { Text(it, color = Color.Red) }

        // Confirmar Contraseña
        OutlinedTextField(
            value = state.confirm,
            onValueChange = { vm.onConfirmChange(it) },
            label = { Text("Confirmar Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            isError = state.confirmError != null,
            modifier = Modifier.fillMaxWidth()
        )
        state.confirmError?.let { Text(it, color = Color.Red) }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { vm.submitRegister() },
            enabled = state.canSubmit && !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN)
        ) {
            Text("Registrarse", color = Color.Black)
        }

        state.errorMsg?.let { Text(it, color = Color.Red) }

        TextButton(onClick = onGoLogin) {
            Text("Ya tengo cuenta, iniciar sesión", color = Color.White)
        }
    }
}