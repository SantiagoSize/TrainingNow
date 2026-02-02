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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN

/**
 * Pantalla para elegir o crear una rutina (usuario).
 * Muestra: Crear RUTINA, RUTINAS RECOMENDADAS, MIS RUTINAS.
 */
@Composable
fun UserRoutinesScreen(
    routineRepository: RoutineRepository,
    userId: Int,
    onCreateRoutine: () -> Unit,
    onRoutineClick: (routineId: Int) -> Unit
) {
    val globalRoutines by routineRepository.getGlobalRoutines().collectAsState(initial = emptyList())
    val myRoutines by routineRepository.getUserOwnRoutines(userId).collectAsState(initial = emptyList())
    UserRoutinesScreenContent(
        globalRoutines = globalRoutines,
        myRoutines = myRoutines,
        onCreateRoutine = onCreateRoutine,
        onRoutineClick = onRoutineClick
    )
}

@Composable
private fun UserRoutinesScreenContent(
    globalRoutines: List<RoutineEntity>,
    myRoutines: List<RoutineEntity>,
    onCreateRoutine: () -> Unit,
    onRoutineClick: (routineId: Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        item {
            ScreenHeaderTN(
                subtitle = "Mis",
                title = "RUTINAS",
                actionIcon = Icons.Filled.FitnessCenter,
                onActionClick = { /* gestionar rutinas */ }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Tarjeta verde: Crear RUTINA — derecha: cuadrado negro con + blanco
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clickable(onClick = onCreateRoutine),
                colors = CardDefaults.cardColors(containerColor = VerdeTN),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Crear",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "RUTINA",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Diseña tu plan personalizado",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Crear rutina",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // RUTINAS RECOMENDADAS — título en verde apagado
            SectionTitleMuted(text = "RUTINAS RECOMENDADAS")
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Lista horizontal o en grid de recomendadas (mostramos hasta 3 en fila o lista)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(globalRoutines.take(6)) { routine ->
                    RoutineCard(
                        title = routine.name,
                        subtitle = routine.dayInfo,
                        icon = Icons.Filled.FitnessCenter,
                        onClick = { onRoutineClick(routine.id) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            SectionTitleMuted(text = "MIS RUTINAS")
            Spacer(modifier = Modifier.height(14.dp))
        }

        items(myRoutines) { routine ->
            RoutineCard(
                title = routine.name,
                subtitle = routine.dayInfo,
                icon = Icons.Filled.FitnessCenter,
                onClick = { onRoutineClick(routine.id) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Si no hay rutinas propias, mostrar al menos una tarjeta de ejemplo o mensaje
        if (myRoutines.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, VerdeTN, RoundedCornerShape(16.dp))
                        .clickable(enabled = false) { },
        colors = CardDefaults.cardColors(containerColor = GrisFondo),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VerdeTN),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FitnessCenter,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Aún no tienes rutinas propias. Crea una o elige una recomendada.",
                            color = GrisTexto,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/** Título de sección en verde apagado (muted green), sin icono. */
@Composable
private fun SectionTitleMuted(text: String) {
    Text(
        text = text,
        color = GrisTexto,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
}

// ==================== VISTA PREVIA ====================

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewUserRoutinesScreen() {
    val sampleRecommended = listOf(
        RoutineEntity(id = 1, name = "Rutina Full Body - Principiantes", dayInfo = "Lun, Mié, Vie", ownerId = null, creatorId = 1),
        RoutineEntity(id = 2, name = "Push Day - Pecho, Hombros, Tríceps", dayInfo = "Día de Empuje", ownerId = null, creatorId = 2),
        RoutineEntity(id = 3, name = "Pull Day - Espalda, Bíceps", dayInfo = "Día de Tirón", ownerId = null, creatorId = 2)
    )
    val sampleMyRoutines = listOf(
        RoutineEntity(id = 4, name = "Mi plan semanal", dayInfo = "Lunes - Pecho", ownerId = 4, creatorId = 2)
    )
    UserRoutinesScreenContent(
        globalRoutines = sampleRecommended,
        myRoutines = sampleMyRoutines,
        onCreateRoutine = { },
        onRoutineClick = { }
    )
}

/** Tarjeta de rutina: fondo negro, borde verde neón, icono pesa en caja verde. */
@Composable
private fun RoutineCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .border(1.5.dp, VerdeTN, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GrisFondo),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(VerdeTN),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null && subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = GrisTexto,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
