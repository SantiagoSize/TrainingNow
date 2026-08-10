package com.shagox.apptrainingnow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde

/**
 * Botón "Volver" con estilo Training Now: círculo verde con la flecha
 * y etiqueta en píldora oscura. Reemplaza la flecha suelta anterior.
 *
 * @param text Texto de la píldora (ej. "Biblioteca")
 * @param onClick Acción al pulsar
 * @param showLabel Si false solo muestra el círculo con la flecha
 */
@Composable
fun BackButtonTN(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = TextoPrincipal,
    showLabel: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(GrisFondo)
                .border(1.dp, VerdeTN.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onClick)
                .padding(start = 4.dp, end = if (showLabel) 16.dp else 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VerdeTN),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = TextoSobreVerde,
                    modifier = Modifier.size(19.dp)
                )
            }
            if (showLabel) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ==================== VISTA PREVIA ====================

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewBackButtonTN() {
    BackButtonTN(text = "Biblioteca", onClick = { })
}
