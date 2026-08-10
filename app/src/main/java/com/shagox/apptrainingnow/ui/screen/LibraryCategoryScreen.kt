package com.shagox.apptrainingnow.ui.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN

/**
 * Ejercicios de una categoría de la biblioteca.
 * Cabecera con degradado, contador y tarjetas con miniatura, nivel y músculos.
 */
@Composable
fun LibraryCategoryScreen(
    categoryName: String,
    exerciseRepository: IExerciseRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    val exercises by exerciseRepository.getExercisesByCategory(categoryName)
        .collectAsState(initial = emptyList())

    Box(modifier = modifier.fillMaxSize().background(NegroFondo)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ===== Cabecera con degradado =====
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
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 27.sp
                            )
                        }
                        if (exercises.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(VerdeTN)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${exercises.size}",
                                    color = NegroFondo,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
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
                        text = "No hay ejercicios en esta categoría",
                        color = GrisTexto,
                        fontSize = 14.sp,
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
                            onClick = { selectedExerciseId = exercise.id }
                        )
                    }
                }
            }
        }

        // Detalle del ejercicio sobre la lista
        if (selectedExerciseId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NegroFondo)
            ) {
                ExerciseDetailScreen(
                    exerciseId = selectedExerciseId!!,
                    exerciseRepository = exerciseRepository,
                    onClose = { selectedExerciseId = null }
                )
            }
        }
    }
}

/**
 * Tarjeta de ejercicio: miniatura (imagen o ícono), nombre,
 * nivel + equipamiento, músculos y flecha de acceso.
 */
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
            .border(1.dp, GrisBorde, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Miniatura
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(VerdeTN.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (!exercise.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = exercise.imageUrl,
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = VerdeTN,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp
            )
            // Nivel + equipamiento
            val detalle = listOfNotNull(
                exercise.difficulty?.lowercase()?.replaceFirstChar { it.uppercase() },
                exercise.equipment
            ).filter { it.isNotBlank() }
            if (detalle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(VerdeTN)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = detalle.joinToString(" · "),
                        color = GrisTexto,
                        fontSize = 12.sp
                    )
                }
            }
            // Músculos
            if (!exercise.muscles.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = exercise.muscles,
                    color = VerdeTN.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = VerdeTN,
            modifier = Modifier.size(20.dp)
        )
    }
}
