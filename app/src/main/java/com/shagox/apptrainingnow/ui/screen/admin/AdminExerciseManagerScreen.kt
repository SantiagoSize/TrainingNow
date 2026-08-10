package com.shagox.apptrainingnow.ui.screen.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.utils.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gestión de la biblioteca (solo admin): crear, editar y eliminar ejercicios.
 * Las imágenes se comprimen en el dispositivo antes de enviarse (~60-100 KB).
 */
@Composable
fun AdminExerciseManagerScreen(
    exerciseRepository: IExerciseRepository,
    onBack: () -> Unit
) {
    val exercises by exerciseRepository.getAllExercises().collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<ExerciseEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        BackButtonTN(text = "Volver", onClick = onBack)
        ScreenHeaderTN(
            subtitle = "Gestionar",
            title = "BIBLIOTECA",
            actionIcon = Icons.Filled.Add,
            onActionClick = { creating = true }
        )

        message?.let {
            Text(
                text = it,
                color = VerdeTN,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Text(
            text = "${exercises.size} ejercicios en la biblioteca",
            color = GrisTexto,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(exercises) { exercise ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GrisFondo)
                        .border(1.dp, GrisBorde, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VerdeTN.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!exercise.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = exercise.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exercise.name, color = TextoPrincipal, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(exercise.category, color = GrisTexto, fontSize = 12.sp)
                    }
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar",
                        tint = VerdeTN,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { editing = exercise }
                    )
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFE53935),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { confirmDelete = exercise }
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        ExerciseFormDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { entity ->
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            if (entity.id > 0) exerciseRepository.updateExercise(entity)
                            else exerciseRepository.createExercise(entity)
                        }
                        message = if (entity.id > 0) "Ejercicio actualizado" else "Ejercicio creado"
                    } catch (e: Exception) {
                        message = e.message
                    }
                    creating = false
                    editing = null
                }
            }
        )
    }

    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = GrisFondo,
            title = { Text("Eliminar ejercicio", color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar \"${target.name}\" de la biblioteca?", color = GrisTexto) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { exerciseRepository.deleteExercise(target.id) }
                                message = "Ejercicio eliminado"
                            } catch (e: Exception) {
                                message = e.message
                            }
                            confirmDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White)
                ) { Text("ELIMINAR", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Button(
                    onClick = { confirmDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = Color.White)
                ) { Text("Cancelar") }
            }
        )
    }
}

/** Formulario de creación/edición con selector de imagen comprimida. */
@Composable
private fun ExerciseFormDialog(
    initial: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (ExerciseEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var videoUrl by remember { mutableStateOf(initial?.videoUrl ?: "") }
    var muscles by remember { mutableStateOf(initial?.muscles ?: "") }
    var equipment by remember { mutableStateOf(initial?.equipment ?: "") }
    var difficulty by remember { mutableStateOf(initial?.difficulty ?: "PRINCIPIANTE") }
    var imageData by remember { mutableStateOf(initial?.imageUrl) }
    var comprimiendo by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            comprimiendo = true
            scope.launch {
                val comprimida = withContext(Dispatchers.IO) {
                    ImageCompressor.compressToDataUri(context, uri)
                }
                imageData = comprimida
                comprimiendo = false
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = VerdeTN,
        unfocusedBorderColor = GrisTexto,
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal,
        cursorColor = VerdeTN,
        focusedLabelColor = VerdeTN,
        unfocusedLabelColor = GrisTexto
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrisFondo,
        title = {
            Text(
                if (initial == null) "Nuevo ejercicio" else "Editar ejercicio",
                color = VerdeTN,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Imagen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NegroFondo)
                        .border(1.dp, VerdeTN.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        comprimiendo -> CircularProgressIndicator(color = VerdeTN)
                        !imageData.isNullOrBlank() -> AsyncImage(
                            model = imageData,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PhotoLibrary, null, tint = VerdeTN, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Toca para elegir imagen", color = GrisTexto, fontSize = 12.sp)
                        }
                    }
                }
                if (!imageData.isNullOrBlank()) {
                    Text(
                        "Imagen comprimida: ~${ImageCompressor.sizeKb(imageData)} KB",
                        color = VerdeTN,
                        fontSize = 11.sp
                    )
                }

                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nombre *") }, singleLine = true, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it },
                    label = { Text("Categoría *") }, singleLine = true, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Descripción") }, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = muscles, onValueChange = { muscles = it },
                    label = { Text("Músculos (separados por coma)") }, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = equipment, onValueChange = { equipment = it },
                    label = { Text("Equipamiento") }, singleLine = true, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it },
                    label = { Text("URL del video") }, singleLine = true, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())

                Text("Dificultad", color = GrisTexto, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PRINCIPIANTE", "INTERMEDIO", "AVANZADO").forEach { nivel ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (difficulty == nivel) VerdeTN else NegroFondo)
                                .border(1.dp, VerdeTN, RoundedCornerShape(20.dp))
                                .clickable { difficulty = nivel }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                nivel.take(4),
                                color = if (difficulty == nivel) NegroFondo else VerdeTN,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ExerciseEntity(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            category = category.trim(),
                            description = description.trim(),
                            videoUrl = videoUrl.trim(),
                            imageUrl = imageData,
                            muscles = muscles.trim().takeIf { it.isNotBlank() },
                            difficulty = difficulty,
                            equipment = equipment.trim().takeIf { it.isNotBlank() },
                            isSystemDefault = initial?.isSystemDefault ?: false
                        )
                    )
                },
                enabled = name.isNotBlank() && category.isNotBlank() && !comprimiendo,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde)
            ) { Text("GUARDAR", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = Color.White)
            ) { Text("Cancelar") }
        }
    )
}
