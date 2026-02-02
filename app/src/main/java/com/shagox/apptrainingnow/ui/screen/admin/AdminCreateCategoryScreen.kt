package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla para crear una nueva categoría de ejercicios.
 * Inserta un ejercicio placeholder con la categoría para que aparezca en la biblioteca.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateCategoryScreen(
    exerciseRepository: com.shagox.apptrainingnow.data.repository.IExerciseRepository,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var exerciseName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Categoría") },
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
                .padding(20.dp)
        ) {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre de la categoría") },
                placeholder = { Text("Ej: Pectorales") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto,
                    focusedLabelColor = VerdeTN,
                    cursorColor = VerdeTN
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = exerciseName,
                onValueChange = { exerciseName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del primer ejercicio (opcional)") },
                placeholder = { Text("Ej: Press de banca") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto
                ),
                shape = RoundedCornerShape(12.dp)
            )
            message?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, color = VerdeTN)
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
                        withContext(Dispatchers.IO) {
                            try {
                                val name = exerciseName.ifBlank { "Ejercicio en ${categoryName.trim()}" }
                                exerciseRepository.insertExercises(
                                    listOf(
                                        ExerciseEntity(
                                            name = name,
                                            category = categoryName.trim(),
                                            description = "",
                                            videoUrl = "",
                                            isSystemDefault = true
                                        )
                                    )
                                )
                                withContext(Dispatchers.Main) {
                                    message = "Categoría creada. Añade más ejercicios en Biblioteca."
                                    onSuccess()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    message = e.message ?: "Error al crear"
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = NegroFondo)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = NegroFondo)
                else Text("Crear categoría")
            }
        }
    }
}
