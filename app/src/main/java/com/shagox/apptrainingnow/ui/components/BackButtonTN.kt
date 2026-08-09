package com.shagox.apptrainingnow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Botón "Volver" reutilizable para cualquier pantalla.
 * Muestra icono chevron izquierda + texto (ej. "Volver a Mis Rutinas").
 *
 * @param text Texto del botón (ej. "Volver a Mis Rutinas")
 * @param onClick Acción al pulsar
 * @param modifier Modifier opcional
 * @param textColor Color del texto (por defecto blanco)
 */
@Composable
fun BackButtonTN(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

// ==================== VISTA PREVIA ====================

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewBackButtonTN() {
    BackButtonTN(
        text = "Volver a Mis Rutinas",
        onClick = { }
    )
}
