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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.VerdeAcento

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
    modifier: Modifier = Modifier
) {
    var mostrarInfo by remember { mutableStateOf(false) }
    var mostrarComoFunciona by remember { mutableStateOf(false) }

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
