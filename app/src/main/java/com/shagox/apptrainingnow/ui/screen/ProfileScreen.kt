package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.R
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.viewmodel.AuthViewModel

/**
 * Pantalla Perfil: pestaña para iniciar sesión y registrarse en la misma página.
 * Si el usuario está logueado, muestra sus datos y el botón Cerrar sesión.
 */
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val loginState by authViewModel.loginState.collectAsState()
    val registerState by authViewModel.register.collectAsState()
    val loggedUser = loginState.loggedUser
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Iniciar sesión, 1 = Registrarse

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Tu",
            title = "PERFIL",
            actionIcon = Icons.Filled.Person,
            onActionClick = {}  // Botón verde con icono siempre visible (diseño)
        )

        if (loggedUser != null) {
            // Usuario logueado: mostrar datos y Cerrar sesión
            ProfileLoggedContent(
                userName = loggedUser.name,
                userEmail = loggedUser.email,
                onLogout = { authViewModel.logout() }
            )
        } else {
            // No logueado: control segmentado (bordes redondeados en los extremos, integrado con fondo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NegroFondo)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selectedTab == 0) VerdeTN else NegroFondo)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "INICIAR SESIÓN",
                        color = if (selectedTab == 0) androidx.compose.ui.graphics.Color.White else GrisTexto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selectedTab == 1) VerdeTN else NegroFondo)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "REGISTRARSE",
                        color = if (selectedTab == 1) androidx.compose.ui.graphics.Color.White else GrisTexto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (selectedTab == 0) {
                LoginTabContent(
                    loginState = loginState,
                    onEmailChange = { authViewModel.onLoginEmailChange(it) },
                    onPassChange = { authViewModel.onLoginPassChange(it) },
                    onSubmit = { authViewModel.submitLogin() }
                )
            } else {
                RegisterTabContent(
                    registerState = registerState,
                    onNameChange = { authViewModel.onNameChange(it) },
                    onEmailChange = { authViewModel.onRegisterEmailChange(it) },
                    onPhoneChange = { authViewModel.onPhoneChange(it) },
                    onPassChange = { authViewModel.onRegisterPassChange(it) },
                    onConfirmChange = { authViewModel.onConfirmChange(it) },
                    onSubmit = { authViewModel.submitRegister() }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "TrainingNow v1.0",
                color = GrisTexto,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileLoggedContent(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GrisFondo)
                .padding(20.dp)
        ) {
            Column {
                Text("Nombre", color = GrisTexto, fontSize = 12.sp)
                Text(userName, color = androidx.compose.ui.graphics.Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Correo", color = GrisTexto, fontSize = 12.sp)
                Text(userEmail, color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cerrar sesión", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LoginTabContent(
    loginState: com.shagox.apptrainingnow.ui.viewmodel.LoginUiState,
    onEmailChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        // Logo arriba (logo_2, sin texto)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_2),
                contentDescription = "Logo Training NOW!",
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(100.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = loginState.email,
            onValueChange = onEmailChange,
            label = { Text("Correo Electrónico", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto,
                cursorColor = VerdeTN,
                focusedLabelColor = VerdeTN,
                unfocusedLabelColor = GrisTexto,
                focusedContainerColor = GrisFondo,
                unfocusedContainerColor = GrisFondo
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (loginState.emailError != null) {
            Text(loginState.emailError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = loginState.pass,
            onValueChange = onPassChange,
            label = { Text("Contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = androidx.compose.ui.graphics.Color.White,
                unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto,
                cursorColor = VerdeTN,
                focusedLabelColor = VerdeTN,
                unfocusedLabelColor = GrisTexto,
                focusedContainerColor = GrisFondo,
                unfocusedContainerColor = GrisFondo
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState.canSubmit && !loginState.isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = GrisFondo, contentColor = androidx.compose.ui.graphics.Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (loginState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), color = VerdeTN)
            } else {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.SemiBold)
            }
        }
        if (loginState.errorMsg != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(androidx.compose.ui.graphics.Color(0xFF8B0000))  // Fondo rojo oscuro/granate
                    .padding(12.dp)
            ) {
                Text(
                    loginState.errorMsg,
                    color = androidx.compose.ui.graphics.Color(0xFFE53935),  // Texto rojo vibrante
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun RegisterTabContent(
    registerState: com.shagox.apptrainingnow.ui.viewmodel.RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPassChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = androidx.compose.ui.graphics.Color.White,
        unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto,
        focusedContainerColor = GrisFondo,
        unfocusedContainerColor = GrisFondo
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        // Logo arriba (logo_2, sin texto)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.logo_2),
                contentDescription = "Logo Training NOW!",
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(100.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = registerState.name,
            onValueChange = onNameChange,
            label = { Text("Nombre", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.nameError != null) Text(registerState.nameError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.email,
            onValueChange = onEmailChange,
            label = { Text("Correo", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.emailError != null) Text(registerState.emailError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.phone,
            onValueChange = onPhoneChange,
            label = { Text("Teléfono", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.phoneError != null) Text(registerState.phoneError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.pass,
            onValueChange = onPassChange,
            label = { Text("Contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.passError != null) Text(registerState.passError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = registerState.confirm,
            onValueChange = onConfirmChange,
            label = { Text("Confirmar contraseña", color = GrisTexto) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            shape = RoundedCornerShape(12.dp)
        )
        if (registerState.confirmError != null) Text(registerState.confirmError, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 12.sp)
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = registerState.canSubmit && !registerState.isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = NegroFondo),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (registerState.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), color = NegroFondo)
            } else {
                Text("REGISTRARSE", fontWeight = FontWeight.SemiBold)
            }
        }
        if (registerState.errorMsg != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(registerState.errorMsg, color = androidx.compose.ui.graphics.Color(0xFFE53935), fontSize = 14.sp)
        }
        if (registerState.success) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Registro exitoso. Inicia sesión.", color = VerdeTN, fontSize = 14.sp)
        }
    }
}
