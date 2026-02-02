package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN

@Composable
fun LibraryCategoryScreen(
    categoryName: String,
    exerciseRepository: IExerciseRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    val exercises by exerciseRepository.getExercisesByCategory(categoryName).collectAsState(initial = emptyList())

    Box(modifier = modifier.fillMaxSize().background(NegroFondo)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            BackButtonTN(
                text = "Volver a Biblioteca",
                onClick = onBack,
                textColor = Color.White
            )
            Text(
                text = "Ejercicios",
                color = GrisTexto,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = categoryName.uppercase(),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
            )
            if (exercises.isEmpty()) {
                Text(
                    text = "No hay ejercicios en esta categoría.",
                    color = GrisTexto,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseListItem(
                            exercise = exercise,
                            onClick = { selectedExerciseId = exercise.id }
                        )
                    }
                }
            }
        }

        // Overlay: detalle del ejercicio (video, descripción, CERRAR) encima de la lista
        if (selectedExerciseId != null) {
            ExerciseDetailOverlay(
                exerciseId = selectedExerciseId!!,
                exerciseRepository = exerciseRepository,
                onClose = { selectedExerciseId = null }
            )
        }
    }
}

@Composable
private fun ExerciseDetailOverlay(
    exerciseId: Int,
    exerciseRepository: IExerciseRepository,
    onClose: () -> Unit
) {
    val exercise by exerciseRepository.observeExercise(exerciseId).collectAsState(initial = null)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
    ) {
        if (exercise != null) {
            ExerciseDetailContent(exercise = exercise!!, onClose = onClose)
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando...", color = GrisTexto)
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: ExerciseEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VerdeTN.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FitnessCenter,
                contentDescription = null,
                tint = VerdeTN,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = exercise.name,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}
