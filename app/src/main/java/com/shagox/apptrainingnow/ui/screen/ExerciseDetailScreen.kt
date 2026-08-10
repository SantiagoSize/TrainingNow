package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde

/**
 * Detalle de un ejercicio: portada con imagen, chips de datos,
 * descripción, músculos, equipamiento y acceso al video.
 */
@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    exerciseRepository: IExerciseRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise by exerciseRepository.observeExercise(exerciseId).collectAsState(initial = null)
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroFondo)
            .verticalScroll(rememberScrollState())
    ) {
        val ejercicio = exercise
        if (ejercicio == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = VerdeTN) }
            return@Column
        }

        // ===== Portada =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            if (!ejercicio.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ejercicio.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = ejercicio.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Sin imagen: patrón con degradado e ícono
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(VerdeTN.copy(alpha = 0.35f), NegroFondo)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = VerdeTN.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Degradado inferior para legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to NegroFondo
                        )
                    )
            )

            // Botón cerrar
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TextoSobreVerde.copy(alpha = 0.7f))
                    .border(1.dp, VerdeTN, CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = VerdeTN,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Título sobre la portada
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = ejercicio.category.uppercase(),
                    color = VerdeTN,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = ejercicio.name,
                    color = TextoPrincipal,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 30.sp
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            // ===== Chips de información =====
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ejercicio.difficulty?.takeIf { it.isNotBlank() }?.let {
                    InfoChip(texto = it.lowercase().replaceFirstChar { c -> c.uppercase() })
                }
                ejercicio.equipment?.takeIf { it.isNotBlank() }?.let {
                    InfoChip(texto = it)
                }
            }

            // ===== Volumen recomendado =====
            if (ejercicio.recommendedSets != null || ejercicio.recommendedReps != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ejercicio.recommendedSets?.let {
                        DatoCard("SERIES", "$it", Modifier.weight(1f))
                    }
                    ejercicio.recommendedReps?.takeIf { it.isNotBlank() }?.let {
                        DatoCard("REPS", it, Modifier.weight(1f))
                    }
                    ejercicio.restSeconds?.let {
                        DatoCard("DESCANSO", "${it}s", Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ===== Descripción =====
            if (ejercicio.description.isNotBlank()) {
                SectionTitle("CÓMO SE HACE")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GrisFondo)
                        .border(1.dp, GrisBorde, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = ejercicio.description,
                        color = TextoPrincipal.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ===== Músculos =====
            if (!ejercicio.muscles.isNullOrBlank()) {
                SectionTitle("MÚSCULOS TRABAJADOS")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ejercicio.muscles.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .forEach { musculo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GrisFondo)
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(VerdeTN)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(musculo, color = TextoPrincipal, fontSize = 14.sp)
                            }
                        }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ===== Ejecución paso a paso =====
            if (!ejercicio.instructions.isNullOrBlank()) {
                SectionTitle("EJECUCIÓN PASO A PASO")
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ejercicio.instructions.split("|").map { it.trim() }.filter { it.isNotBlank() }
                        .forEachIndexed { index, paso ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GrisFondo)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(VerdeTN),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = TextoSobreVerde,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = paso,
                                    color = TextoPrincipal.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ===== Consejos =====
            if (!ejercicio.tips.isNullOrBlank()) {
                SectionTitle("CONSEJOS DE TÉCNICA")
                ListaConIcono(
                    items = ejercicio.tips.split("|").map { it.trim() }.filter { it.isNotBlank() },
                    color = VerdeTN,
                    icono = Icons.Filled.CheckCircle
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ===== Errores comunes =====
            if (!ejercicio.commonMistakes.isNullOrBlank()) {
                SectionTitle("ERRORES COMUNES")
                ListaConIcono(
                    items = ejercicio.commonMistakes.split("|").map { it.trim() }.filter { it.isNotBlank() },
                    color = RojoError,
                    icono = Icons.Filled.Warning
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ===== Video =====
            if (!ejercicio.videoUrl.isBlank()) {
                Button(
                    onClick = { runCatching { uriHandler.openUri(ejercicio.videoUrl) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdeTN,
                        contentColor = TextoSobreVerde
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VER VIDEO", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ===== Cerrar =====
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GrisFondo,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("CERRAR", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private val RojoError = Color(0xFFE53935)

/** Tarjeta compacta con un dato de volumen (series, reps, descanso). */
@Composable
private fun DatoCard(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, color = VerdeTN, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(titulo, color = GrisTexto, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Lista de puntos con ícono de color (consejos o errores). */
@Composable
private fun ListaConIcono(
    items: List<String>,
    color: Color,
    icono: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { texto ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.08f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = texto,
                    color = TextoPrincipal.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(texto: String) {
    Text(
        text = texto,
        color = VerdeTN,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun InfoChip(texto: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(VerdeTN.copy(alpha = 0.15f))
            .border(1.dp, VerdeTN.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(texto, color = VerdeTN, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
