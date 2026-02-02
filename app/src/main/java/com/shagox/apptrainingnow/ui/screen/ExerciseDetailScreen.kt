package com.shagox.apptrainingnow.ui.screen

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.shagox.apptrainingnow.data.local.exercise.ExerciseEntity
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo

@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    exerciseRepository: IExerciseRepository,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exercise by exerciseRepository.observeExercise(exerciseId).collectAsState(initial = null)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NegroFondo)
    ) {
        if (exercise == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando...", color = GrisTexto)
            }
            return@Column
        }
        ExerciseDetailContent(
            exercise = exercise!!,
            onClose = onClose
        )
    }
}

@Composable
fun ExerciseDetailContent(
    exercise: ExerciseEntity,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        // Fila: título + botón CERRAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = exercise.name.uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }
        }

        // Video (por URL) o placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GrisFondo)
        ) {
            if (exercise.videoUrl.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            try {
                                setVideoURI(Uri.parse(exercise.videoUrl))
                                setOnPreparedListener { it.start() }
                            } catch (_: Exception) { }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                VideoPlaceholder(exercise.name)
            }
        }

        // Contenido textual
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Cómo realizar correctamente el ${exercise.name}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = exercise.description,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Variantes:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "• Mancuernas\n• Poleas\n• Barra",
                color = GrisTexto,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "CERRAR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun VideoPlaceholder(exerciseName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = exerciseName,
            color = GrisTexto,
            fontSize = 16.sp
        )
    }
}
