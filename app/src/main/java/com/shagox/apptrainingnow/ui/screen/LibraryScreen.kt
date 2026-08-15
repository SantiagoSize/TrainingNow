package com.shagox.apptrainingnow.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal

/** Categoría de la biblioteca con su conteo de ejercicios. */
data class LibraryCategory(
    val name: String,
    val exerciseCount: Int,
    val icon: ImageVector
)

/** internal: reutilizado por las pantallas de biblioteca del admin para mantener la misma estética. */
internal val categoryIconMap = mapOf(
    "Pectorales" to Icons.Filled.FitnessCenter,
    "Espalda" to Icons.Filled.FitnessCenter,
    "Piernas" to Icons.AutoMirrored.Filled.DirectionsRun,
    "Hombros" to Icons.Filled.FitnessCenter,
    "Bíceps" to Icons.Filled.FitnessCenter,
    "Tríceps" to Icons.Filled.FitnessCenter,
    "Brazos" to Icons.Filled.FitnessCenter,
    "Core" to Icons.Filled.SelfImprovement,
    "Abdominales" to Icons.Filled.SelfImprovement,
    "Cardio" to Icons.Filled.Favorite,
    "Personalizado" to Icons.Filled.Accessibility
)

internal fun iconForCategory(category: String): ImageVector =
    categoryIconMap[category] ?: Icons.Filled.FitnessCenter

/**
 * Biblioteca de ejercicios: grilla de categorías con buscador integrado.
 */
@Composable
fun LibraryScreen(
    exerciseRepository: IExerciseRepository,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onExerciseClick: (Int) -> Unit = {}
) {
    // null = cargando; lista vacía = sin conexión o sin datos
    val categoryStats by exerciseRepository.getCategoryStats().collectAsState(initial = null)
    val categories = (categoryStats ?: emptyList())
        .map { LibraryCategory(it.category, it.count, iconForCategory(it.category)) }
    val isLoading = categoryStats == null
    val totalEjercicios = categories.sumOf { it.exerciseCount }

    val allExercises by exerciseRepository.getAllExercises().collectAsState(initial = emptyList())
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = if (searchQuery.isBlank()) emptyList()
    else allExercises.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                (it.muscles?.contains(searchQuery, ignoreCase = true) == true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        ScreenHeaderTN(
            subtitle = "Explora la",
            title = "BIBLIOTECA",
            actionIcon = if (searchActive) Icons.Filled.Close else Icons.Filled.Search,
            onActionClick = {
                searchActive = !searchActive
                if (!searchActive) searchQuery = ""
                onSearchClick()
            }
        )

        if (searchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar ejercicio o músculo...", color = GrisTexto) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = VerdeTN) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisBorde,
                    focusedTextColor = TextoPrincipal,
                    unfocusedTextColor = TextoPrincipal,
                    cursorColor = VerdeTN,
                    focusedContainerColor = GrisFondo,
                    unfocusedContainerColor = GrisFondo
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listItems(searchResults) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrisFondo)
                            .border(1.dp, GrisBorde, RoundedCornerShape(12.dp))
                            .clickable { onExerciseClick(exercise.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VerdeTN.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconForCategory(exercise.category),
                                contentDescription = null,
                                tint = VerdeTN,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                exercise.name,
                                color = TextoPrincipal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(exercise.category, color = GrisTexto, fontSize = 12.sp)
                        }
                    }
                }
                if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                    listItems(listOf("empty")) {
                        Text(
                            "Sin resultados para \"$searchQuery\"",
                            color = GrisTexto,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
            return@Column
        }

        // Resumen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "CATEGORÍAS",
                color = VerdeTN,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            if (totalEjercicios > 0) {
                Text(
                    text = "$totalEjercicios ejercicios",
                    color = GrisTexto,
                    fontSize = 12.sp
                )
            }
        }

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isLoading) {
                        CircularProgressIndicator(color = VerdeTN)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando categorías...", color = GrisTexto, fontSize = 14.sp)
                    } else {
                        Text(
                            text = "No se pudo cargar la biblioteca",
                            color = TextoPrincipal,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Verifica que los microservicios estén corriendo\n(TrainNow-Biblioteca en el puerto 8082)",
                            color = GrisTexto,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    CategoryCard(
                        name = category.name,
                        exerciseCount = category.exerciseCount,
                        icon = category.icon,
                        onClick = { onCategoryClick(category.name) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta de categoría: degradado verde, ícono grande e indicador de cantidad.
 * internal: la reutiliza la biblioteca del admin para verse idéntica a la del usuario.
 * [onLongClick] es solo para el admin (editar/borrar categoría); un usuario normal no lo recibe.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryCard(
    name: String,
    exerciseCount: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(VerdeTN.copy(alpha = 0.22f), GrisFondo)
                )
            )
            .border(1.dp, VerdeTN.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
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
            Column {
                Text(
                    text = name,
                    color = TextoPrincipal,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp
                )
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
                        text = "$exerciseCount ejercicios",
                        color = GrisTexto,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
