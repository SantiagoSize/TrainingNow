package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreenVm(
    vm: AuthViewModel,
    onLoginOkNavigateHome: () -> Unit,
    onGoRegister: () -> Unit
) {
    val state by vm.login.collectAsState()

    // Efecto para navegar cuando el login es exitoso
    LaunchedEffect(state.success) {
        if (state.success) {
            onLoginOkNavigateHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.shagox.apptrainingnow.ui.theme.NegroFondo)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "TRAINING NOW",
            style = MaterialTheme.typography.headlineLarge,
            color = VerdeTN //
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo de Email
        OutlinedTextField(
            value = state.email,
            onValueChange = { vm.onLoginEmailChange(it) }, //
            label = { Text("Correo Electrónico") },
            isError = state.emailError != null,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        state.emailError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de Contraseña
        OutlinedTextField(
            value = state.pass,
            onValueChange = { vm.onLoginPassChange(it) }, //
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de Acción
        Button(
            onClick = { vm.submitLogin() }, //
            enabled = state.canSubmit && !state.isSubmitting, //
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN) //
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("Iniciar Sesión", color = Color.Black)
            }
        }

        // Error Global
        state.errorMsg?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        TextButton(onClick = onGoRegister) {
            Text("¿No tienes cuenta? Regístrate aquí", color = Color.White)
        }
    }
}