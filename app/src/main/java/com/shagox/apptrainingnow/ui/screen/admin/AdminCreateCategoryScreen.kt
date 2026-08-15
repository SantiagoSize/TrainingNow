package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Crear una categoría nueva: solo pide el nombre. La categoría queda vacía (0 ejercicios);
 * los ejercicios se agregan después desde dentro de la categoría, en Biblioteca.
 */
@Composable
fun AdminCreateCategoryScreen(
    exerciseRepository: IExerciseRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    actorId: Int = 0,
    actorName: String = "",
    actorRole: String = "ADMIN"
) {
    val auditLogRepository = remember { com.shagox.apptrainingnow.data.repository.AuditLogRepository() }
    var categoryName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        BackButtonTN(text = "Panel", onClick = onBack)
        ScreenHeaderTN(
            subtitle = "Crear",
            title = "NUEVA CATEGORÍA"
        )

        Text(
            "Elige un nombre claro, ej. Pectorales, Piernas, Cardio. Podrás agregarle ejercicios apenas la crees.",
            color = GrisTexto,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it; message = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre de la categoría") },
            placeholder = { Text("Ej: Pectorales") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextoPrincipal,
                unfocusedTextColor = TextoPrincipal,
                focusedBorderColor = VerdeTN,
                unfocusedBorderColor = GrisTexto,
                focusedLabelColor = VerdeTN,
                cursorColor = VerdeTN
            ),
            shape = RoundedCornerShape(12.dp)
        )
        message?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(msg, color = Color(0xFFE57373), fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (categoryName.isBlank()) {
                    message = "Indica el nombre de la categoría"
                    return@Button
                }
                isLoading = true
                message = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            exerciseRepository.createCategory(categoryName.trim())
                            auditLogRepository.log(
                                actorId = actorId,
                                actorName = actorName,
                                actorRole = actorRole,
                                action = "CATEGORY_CREATED",
                                targetType = "CATEGORY",
                                targetName = categoryName.trim()
                            )
                        }
                        onSuccess()
                    } catch (e: Exception) {
                        message = e.message ?: "Error al crear la categoría"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = TextoSobreVerde)
            else Text("Crear categoría")
        }
    }
}
