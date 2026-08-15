package com.shagox.apptrainingnow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Icono de "ojito" para alternar mostrar/ocultar una contraseña. Se usa como
 * trailingIcon en todos los OutlinedTextField de contraseña de la app: login,
 * registro, cambio de contraseña (Perfil y Ajustes), recuperación de contraseña
 * y creación de usuarios desde el panel de administrador.
 */
@Composable
fun IconoOjoContrasena(
    visible: Boolean,
    onToggle: () -> Unit,
    tint: Color
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
            tint = tint
        )
    }
}
