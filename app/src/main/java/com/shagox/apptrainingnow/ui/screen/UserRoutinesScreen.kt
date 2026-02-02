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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.local.routine.RoutineEntity
import com.shagox.apptrainingnow.data.repository.RoutineRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN

/** Texto con contorno negro (stroke). */
@Composable
private fun TextWithOutline(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    outlineColor: Color = NegroFondo,
    outlineWidth: Dp = 1.dp
) {
    val offsets = listOf(
        outlineWidth to 0.dp,
        (-outlineWidth) to 0.dp,
        0.dp to outlineWidth,
        0.dp to (-outlineWidth),
        outlineWidth to outlineWidth,
        outlineWidth to (-outlineWidth),
        (-outlineWidth) to outlineWidth,
        (-outlineWidth) to (-outlineWidth)
    )
    Box(modifier = modifier) {
        offsets.forEach { (x, y) ->
            Text(
                text = text,
                color = outlineColor,
                fontSize = fontSize,
                fontWeight = fontWeight ?: FontWeight.Normal,
                modifier = Modifier.offset(x, y)
            )
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight ?: FontWeight.Normal
        )
    }
}

/**
 * Pantalla Mis Rutinas: mismo diseño que Biblioteca y resto de la app.
 * Crear RUTINA, RUTINAS RECOMENDADAS (lista vertical, tarjetas mismo tamaño), MIS RUTINAS.
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Mis",
            title = "RUTINAS",
            actionIcon = Icons.Filled.FitnessCenter,
            onActionClick = { /* opcional: ordenar */ }
        )

        // Bloque Crear RUTINA (estilo app: verde, esquinas redondeadas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VerdeTN)
                .clickable(onClick = onCreateRoutine)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TextWithOutline(
                        text = "Crear",
                        color = Color.White,
                        fontSize = 14.sp,
                        outlineWidth = 1.dp
                    )
                    TextWithOutline(
                        text = "RUTINA",
                        color = Color.White,
                        fontSize = 20.sp,
                        outlineWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextWithOutline(
                        text = "Diseña tu plan personalizado",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        outlineWidth = 1.dp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NegroFondo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Crear rutina",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Título sección (mismo estilo que CATEGORÍAS en Biblioteca, con contorno negro)
        TextWithOutline(
            text = "RUTINAS RECOMENDADAS",
            color = VerdeTN,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            outlineWidth = 1.dp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(globalRoutines) { routine ->
                RoutineCard(
                    title = routine.name,
                    subtitle = routine.dayInfo,
                    icon = Icons.Filled.FitnessCenter,
                    onClick = { onRoutineClick(routine.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                TextWithOutline(
                    text = "MIS RUTINAS",
                    color = VerdeTN,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    outlineWidth = 1.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(myRoutines) { routine ->
                RoutineCard(
                    title = routine.name,
                    subtitle = routine.dayInfo,
                    icon = Icons.Filled.FitnessCenter,
                    onClick = { onRoutineClick(routine.id) }
                )
            }

            if (myRoutines.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GrisFondo)
                            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                                text = "Aún no tienes rutinas propias. Crea una o elige una recomendada.",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta de rutina: mismo estilo que categorías/ejercicios (GrisFondo, borde VerdeTN, icono verde). */
@Composable
private fun RoutineCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VerdeTN.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VerdeTN,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null && subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = GrisTexto,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

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
