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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.screen.ExerciseDetailScreen
import com.shagox.apptrainingnow.ui.screen.ExerciseListItem
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.utils.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ejercicios de una categoría, vistos por el admin: misma cabecera y tarjetas que
 * ve el usuario, más lápiz (editar), X (eliminar) en cada ejercicio y un botón "+"
 * para crear un ejercicio nuevo directamente en esta categoría.
 */
@Composable
fun AdminLibraryCategoryScreen(
    categoryName: String,
    exerciseRepository: IExerciseRepository,
    onBack: () -> Unit,
    actorId: Int = 0,
    actorName: String = "",
    actorRole: String = "ADMIN"
) {
    val auditLogRepository = remember { com.shagox.apptrainingnow.data.repository.AuditLogRepository() }
    val exercises by exerciseRepository.getExercisesByCategory(categoryName)
        .collectAsState(initial = emptyList())

    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<ExerciseEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(NegroFondo)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Cabecera con degradado (misma estética que ve el usuario) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(VerdeTN.copy(alpha = 0.20f), NegroFondo)
                        )
                    )
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp)
            ) {
                Column {
                    BackButtonTN(text = "Biblioteca", onClick = onBack)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(VerdeTN.copy(alpha = 0.2f))
                                .border(1.dp, VerdeTN.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ejercicios de",
                                color = GrisTexto,
                                fontSize = 13.sp
                            )
                            Text(
                                text = categoryName.uppercase(),
                                color = TextoPrincipal,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 27.sp
                            )
                        }
                        // "+" para crear un ejercicio nuevo en esta categoría
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(VerdeTN)
                                .clickable { creating = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Crear ejercicio en $categoryName",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    message?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, color = VerdeTN, fontSize = 13.sp)
                    }
                }
            }

            // ===== Lista =====
            if (exercises.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = GrisTexto.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aún no hay ejercicios en esta categoría",
                        color = GrisTexto,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Usa el botón + de arriba para crear el primer ejercicio",
                        color = VerdeTN,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseListItem(
                            exercise = exercise,
                            onClick = { selectedExerciseId = exercise.id },
                            onEdit = { editing = exercise },
                            onDelete = { confirmDelete = exercise }
                        )
                    }
                }
            }
        }

        // Detalle del ejercicio sobre la lista (misma pantalla que ve el usuario)
        if (selectedExerciseId != null) {
            Box(modifier = Modifier.fillMaxSize().background(NegroFondo)) {
                ExerciseDetailScreen(
                    exerciseId = selectedExerciseId!!,
                    exerciseRepository = exerciseRepository,
                    onClose = { selectedExerciseId = null }
                )
            }
        }
    }

    if (creating || editing != null) {
        AdminExerciseFormDialog(
            initial = editing,
            lockedCategory = if (editing == null) categoryName else null,
            onDismiss = { creating = false; editing = null },
            onSave = { entity ->
                scope.launch {
                    try {
                        val esEdicion = entity.id > 0
                        withContext(Dispatchers.IO) {
                            if (esEdicion) exerciseRepository.updateExercise(entity)
                            else exerciseRepository.createExercise(entity)
                            auditLogRepository.log(
                                actorId = actorId,
                                actorName = actorName,
                                actorRole = actorRole,
                                action = if (esEdicion) "EXERCISE_UPDATED" else "EXERCISE_CREATED",
                                targetType = "EXERCISE",
                                targetId = entity.id.takeIf { it > 0 },
                                targetName = entity.name,
                                details = "Categoría: ${entity.category}"
                            )
                        }
                        message = if (esEdicion) "Ejercicio actualizado" else "Ejercicio creado"
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
            title = { Text("¿Eliminar ejercicio?", color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = { Text("Se eliminará \"${target.name}\" de la biblioteca. Esta acción no se puede deshacer.", color = GrisTexto) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    exerciseRepository.deleteExercise(target.id)
                                    auditLogRepository.log(
                                        actorId = actorId,
                                        actorName = actorName,
                                        actorRole = actorRole,
                                        action = "EXERCISE_DELETED",
                                        targetType = "EXERCISE",
                                        targetName = target.name,
                                        details = "Categoría: ${target.category}"
                                    )
                                }
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

/**
 * Formulario de creación/edición con selector de imagen comprimida.
 * Si [lockedCategory] no es null (siempre al crear desde dentro de una categoría),
 * la categoría queda fija y se muestra como etiqueta en vez de campo editable.
 */
@Composable
private fun AdminExerciseFormDialog(
    initial: ExerciseEntity?,
    lockedCategory: String?,
    onDismiss: () -> Unit,
    onSave: (ExerciseEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: lockedCategory ?: "") }
    // Migra ejercicios viejos que solo tenían "descripción" (un bloque) a pasos editables.
    var instructions by remember {
        mutableStateOf(initial?.instructions?.replace("|", "\n") ?: initial?.description ?: "")
    }
    var videoUrl by remember { mutableStateOf(initial?.videoUrl ?: "") }
    var muscles by remember { mutableStateOf(initial?.muscles ?: "") }
    var alternatives by remember { mutableStateOf(initial?.alternatives ?: "") }
    var equipment by remember { mutableStateOf(initial?.equipment ?: "") }
    var recommendedSets by remember { mutableStateOf(initial?.recommendedSets?.toString() ?: "") }
    var recommendedReps by remember { mutableStateOf(initial?.recommendedReps ?: "") }
    var restSeconds by remember { mutableStateOf(initial?.restSeconds?.toString() ?: "") }
    var tips by remember { mutableStateOf(initial?.tips?.replace("|", "\n") ?: "") }
    var commonMistakes by remember { mutableStateOf(initial?.commonMistakes?.replace("|", "\n") ?: "") }
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
                when {
                    initial != null -> "Editar ejercicio"
                    lockedCategory != null -> "Nuevo ejercicio en $lockedCategory"
                    else -> "Nuevo ejercicio"
                },
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

                // La categoría es implícita (ya se creó desde dentro de ella): solo se
                // muestra un campo editable al EDITAR, por si hay que recategorizar algo.
                if (lockedCategory == null) {
                    OutlinedTextField(value = category, onValueChange = { category = it },
                        label = { Text("Categoría *") }, singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instrucciones") },
                    placeholder = { Text("Párate con los pies al ancho de hombros\nBaja flexionando las rodillas\n...") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp)
                )
                OutlinedTextField(value = muscles, onValueChange = { muscles = it },
                    label = { Text("Músculos que trabaja") }, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = alternatives,
                    onValueChange = { alternatives = it },
                    label = { Text("Alternativas para hacerlo") },
                    placeholder = { Text("Mancuernas, Barra, Máquina, Peso libre") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Equipamiento") },
                    placeholder = { Text("Ej: Prensa 45°, Barra y banco plano, Mancuernas") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it },
                    label = { Text("URL del video") }, singleLine = true, colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())

                Text("Volumen recomendado", color = GrisTexto, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = recommendedSets,
                        onValueChange = { recommendedSets = it.filter { c -> c.isDigit() } },
                        label = { Text("Series") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = recommendedReps,
                        onValueChange = { recommendedReps = it },
                        label = { Text("Reps") },
                        placeholder = { Text("8-12") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = restSeconds,
                        onValueChange = { restSeconds = it.filter { c -> c.isDigit() } },
                        label = { Text("Descanso (s)") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tips,
                    onValueChange = { tips = it },
                    label = { Text("Consejos de técnica") },
                    placeholder = { Text("Un consejo por línea\nOtro consejo...") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                )
                OutlinedTextField(
                    value = commonMistakes,
                    onValueChange = { commonMistakes = it },
                    label = { Text("Errores comunes") },
                    placeholder = { Text("Un error por línea\nOtro error...") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp)
                )

                Text("Dificultad", color = GrisTexto, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "PRINCIPIANTE" to "Principiante",
                        "INTERMEDIO" to "Intermedio",
                        "AVANZADO" to "Avanzado"
                    ).forEach { (nivel, etiqueta) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (difficulty == nivel) VerdeTN else NegroFondo)
                                .border(1.dp, VerdeTN, RoundedCornerShape(20.dp))
                                .clickable { difficulty = nivel }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                etiqueta,
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
                    val pasos = instructions.lines().map { it.trim() }.filter { it.isNotBlank() }
                    onSave(
                        ExerciseEntity(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            category = (lockedCategory ?: category).trim(),
                            description = "",
                            videoUrl = videoUrl.trim(),
                            imageUrl = imageData,
                            muscles = muscles.trim().takeIf { it.isNotBlank() },
                            difficulty = difficulty,
                            alternatives = alternatives.trim().takeIf { it.isNotBlank() },
                            instructions = pasos.takeIf { it.isNotEmpty() }?.joinToString("|"),
                            equipment = equipment.trim().takeIf { it.isNotBlank() },
                            recommendedSets = recommendedSets.toIntOrNull(),
                            recommendedReps = recommendedReps.trim().takeIf { it.isNotBlank() },
                            restSeconds = restSeconds.toIntOrNull(),
                            tips = tips.lines().map { it.trim() }.filter { it.isNotBlank() }
                                .takeIf { it.isNotEmpty() }?.joinToString("|"),
                            commonMistakes = commonMistakes.lines().map { it.trim() }.filter { it.isNotBlank() }
                                .takeIf { it.isNotEmpty() }?.joinToString("|"),
                            isSystemDefault = initial?.isSystemDefault ?: false
                        )
                    )
                },
                enabled = name.isNotBlank() && (lockedCategory != null || category.isNotBlank()) && !comprimiendo,
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
