package com.shagox.apptrainingnow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.repository.ReportRepository
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.launch

/** Motivos frecuentes para agilizar el reporte (el campo de texto sigue siendo editable). */
private val MOTIVOS_REPORTE = listOf(
    "Acoso o lenguaje ofensivo en el chat",
    "Parece una cuenta bot",
    "Se hace pasar por otra persona",
    "Publicidad o venta no autorizada",
    "Contenido inapropiado en foto o publicación",
    "Otro"
)

/**
 * Reporta a [reportedName] ante el equipo de TrainingNow. El motivo es obligatorio; el
 * detalle es opcional. Se usa desde el menú de opciones de un contacto en el chat.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ReportUserDialog(
    reporterId: Int,
    reporterName: String,
    reportedId: Int,
    reportedName: String,
    onDismiss: () -> Unit,
    onEnviado: () -> Unit
) {
    val reportRepository = remember { ReportRepository() }
    var motivo by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!enviando) onDismiss() },
        containerColor = GrisFondo,
        title = { Text("Reportar a $reportedName", color = TextoPrincipal, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Motivo", color = GrisTexto, fontSize = 12.sp)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MOTIVOS_REPORTE.forEach { opcion ->
                        AssistChip(
                            onClick = { motivo = opcion },
                            label = { Text(opcion, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (motivo == opcion) VerdeTN else GrisFondo,
                                labelColor = if (motivo == opcion) Color.Black else TextoPrincipal
                            )
                        )
                    }
                }
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Motivo (obligatorio)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisTexto
                    )
                )
                OutlinedTextField(
                    value = detalle,
                    onValueChange = { detalle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Detalle (opcional)") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal,
                        focusedBorderColor = VerdeTN,
                        unfocusedBorderColor = GrisTexto
                    )
                )
                error?.let { Text(it, color = Color(0xFFE57373), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !enviando,
                onClick = {
                    if (motivo.isBlank()) {
                        error = "Indica un motivo"
                        return@TextButton
                    }
                    enviando = true
                    error = null
                    scope.launch {
                        val ok = reportRepository.crear(
                            reporterId = reporterId,
                            reporterName = reporterName,
                            reportedId = reportedId,
                            reportedName = reportedName,
                            reason = motivo.trim(),
                            details = detalle.trim().ifBlank { null }
                        )
                        enviando = false
                        if (ok) onEnviado() else error = "No se pudo enviar el reporte. Revisa tu conexión."
                    }
                }
            ) {
                if (enviando) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VerdeTN)
                else Text("Enviar", color = VerdeTN)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !enviando) {
                Text("Cancelar", color = GrisTexto)
            }
        }
    )
}
