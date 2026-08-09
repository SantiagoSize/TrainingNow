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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN

/** Categoría de ejercicios para la biblioteca. */
private data class LibraryCategory(
    val name: String,
    val exerciseCount: Int,
    val icon: ImageVector
)

private val categoryIconMap: Map<String, ImageVector> = mapOf(
    "Pectorales" to Icons.Filled.FitnessCenter,
    "Fuerza" to Icons.Filled.FitnessCenter,
    "Espalda" to Icons.Filled.Accessibility,
    "Piernas" to Icons.Filled.FitnessCenter,
    "Hombros" to Icons.Filled.Person,
    "Brazos" to Icons.Filled.FitnessCenter,
    "Core" to Icons.Filled.FitnessCenter,
    "Cardio" to Icons.Filled.Favorite
)

private fun iconForCategory(category: String): ImageVector =
    categoryIconMap[category] ?: Icons.Filled.FitnessCenter

@Composable
fun LibraryScreen(
    exerciseRepository: IExerciseRepository,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onExerciseClick: (Int) -> Unit = {}
) {
    val categoryStats by exerciseRepository.getCategoryStats().collectAsState(initial = emptyList())
    val categories = categoryStats.map { LibraryCategory(it.category, it.count, iconForCategory(it.category)) }
    val allExercises by exerciseRepository.getAllExercises().collectAsState(initial = emptyList())
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchResults = if (searchQuery.isBlank()) emptyList()
    else allExercises.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
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
            actionIcon = Icons.Filled.Search,
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
                placeholder = { Text("Buscar ejercicio...", color = GrisTexto) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VerdeTN,
                    unfocusedBorderColor = GrisTexto,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = VerdeTN
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                listItems(searchResults) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExerciseClick(exercise.id) }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Column {
                            Text(exercise.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = "CATEGORÍAS",
            color = VerdeTN,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = VerdeTN)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cargando categorías...",
                        color = GrisTexto,
                        fontSize = 14.sp
                    )
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

@Composable
private fun CategoryCard(
    name: String,
    exerciseCount: Int,
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
            .padding(16.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VerdeTN,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = name,
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$exerciseCount ejercicios",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 13.sp
            )
        }
    }
}
