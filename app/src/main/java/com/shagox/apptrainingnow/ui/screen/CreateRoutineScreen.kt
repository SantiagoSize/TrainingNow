package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.repository.DayRoutineInput
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.GrisFondo

private val DAYS = listOf(
    "L" to "Lunes", "M" to "Martes", "X" to "Miércoles", "J" to "Jueves",
    "V" to "Viernes", "S" to "Sábado", "D" to "Domingo"
)

/** Estado de un día: nombre de actividad y lista de ejercicios (0–10). */
private data class DayData(val activityName: String, val exerciseNames: List<String>)

/**
 * Pantalla para crear una nueva rutina.
 * Una rutina tiene nombre y 7 días (cajas); cada día tiene actividad y 0–10 ejercicios.
 * - Si [clientDisplayName] es null: rutina propia (usuario); al guardar se usa onSaveRoutine.
 * - Si [clientDisplayName] no es null: el entrenador crea una rutina para ese cliente; al guardar
 *   la rutina se asigna al cliente (ownerId = cliente) y el usuario la ve en "Mis Rutinas".
 */
@Composable
fun CreateRoutineScreen(
    onBack: () -> Unit,
    onSaveRoutine: (name: String, days: List<DayRoutineInput>) -> Unit = { _, _ -> },
    clientDisplayName: String? = null
) {
    var routineName by remember { mutableStateOf("") }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var daysData by remember { mutableStateOf(List(7) { DayData("", emptyList()) }) }
    var exerciseName by remember { mutableStateOf("") }
    val maxExercises = 10

    val dayActivity = daysData[selectedDayIndex].activityName
    val exerciseNames = daysData[selectedDayIndex].exerciseNames

    fun updateDayActivity(value: String) {
        daysData = daysData.mapIndexed { i, d -> if (i == selectedDayIndex) d.copy(activityName = value) else d }
    }
    fun updateDayExercises(list: List<String>) {
        daysData = daysData.mapIndexed { i, d -> if (i == selectedDayIndex) d.copy(exerciseNames = list) else d }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NegroFondo)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            ScreenHeaderTN(
                subtitle = if (clientDisplayName != null) "Para $clientDisplayName" else "Nueva",
                title = "RUTINA",
                actionIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onActionClick = onBack,
                actionTint = Color.White,
                actionBackgroundColor = Color(0xFFE53935)
            )
            Spacer(modifier = Modifier.height(24.dp))

        // NOMBRE DE LA RUTINA
        LabelGreen("NOMBRE DE LA RUTINA")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextFieldTN(
            value = routineName,
            onValueChange = { routineName = it },
            placeholder = "Ej: Mi rutina de fuerza"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // DÍA — 7 cuadrados iguales, mismo tamaño y separación
        LabelGreen("DÍA")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DAYS.forEachIndexed { index, (letter, _) ->
                val isSelected = selectedDayIndex == index
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { selectedDayIndex = index },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) VerdeTN else NegroFondo,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextoPrincipal.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // NOMBRE DE LA ACTIVIDAD DEL DÍA
        LabelGreen("NOMBRE DE LA ACTIVIDAD DEL DÍA")
        Spacer(modifier = Modifier.height(8.dp))
        // key() fuerza a recrear el campo al cambiar de día, si no conserva el texto anterior
        androidx.compose.runtime.key(selectedDayIndex) {
            OutlinedTextFieldTN(
                value = dayActivity,
                onValueChange = { updateDayActivity(it) },
                placeholder = "Ej: Pecho y Tríceps (vacío = descanso)"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AÑADE UN EJERCICIO — campo gris oscuro + botón verde + contador 0/10
        LabelGreen("AÑADE UN EJERCICIO")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Campo gris oscuro (más claro que el fondo)
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = GrisFondo,
                border = androidx.compose.foundation.BorderStroke(1.dp, TextoPrincipal.copy(alpha = 0.4f))
            ) {
                BasicTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextoPrincipal,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(VerdeTN),
                    decorationBox = { inner ->
                        Box {
                            if (exerciseName.isEmpty()) {
                                Text(
                                    "Nombre del ejercicio",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                            inner()
                        }
                    }
                )
            }
            // Botón verde cuadrado con +
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        if (exerciseName.isNotBlank() && exerciseNames.size < maxExercises) {
                            updateDayExercises(exerciseNames + exerciseName.trim())
                            exerciseName = ""
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = VerdeTN
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Añadir ejercicio",
                        tint = TextoPrincipal,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            // Contador 0/10
            Text(
                text = "${exerciseNames.size}/$maxExercises",
                color = TextoPrincipal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Lista de ejercicios del día — tarjetas claras con número y nombre
        if (exerciseNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exerciseNames.forEachIndexed { index, name ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                updateDayExercises(exerciseNames.filterIndexed { i, _ -> i != index })
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = TextoPrincipal.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TextoPrincipal.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${name.ifBlank { "Ejercicio" }}",
                                color = TextoPrincipal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Quitar",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE53935), RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE53935))
                                    .clickable {
                                        updateDayExercises(exerciseNames.filterIndexed { i, _ -> i != index })
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Guardar rutina
        Button(
            onClick = {
                val days = daysData.zip(DAYS) { d, dayPair ->
                    DayRoutineInput(dayLabel = dayPair.second, activityName = d.activityName, exerciseNames = d.exerciseNames)
                }
                onSaveRoutine(routineName, days)
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeTN),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Guardar rutina",
                color = TextoPrincipal,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // cierra Column con padding
    }
}

@Composable
private fun LabelGreen(text: String) {
    Text(
        text = text,
        color = TextoPrincipal,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun OutlinedTextFieldTN(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TextoPrincipal.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = TextoPrincipal,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(VerdeTN),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
                inner()
            }
        }
    )
}

// ==================== VISTA PREVIA ====================

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewCreateRoutineScreen() {
    CreateRoutineScreen(
        onBack = { },
        onSaveRoutine = { _: String, _: List<DayRoutineInput> -> }
    )
}
