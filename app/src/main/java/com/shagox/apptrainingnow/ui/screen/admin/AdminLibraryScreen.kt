package com.shagox.apptrainingnow.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.data.repository.IExerciseRepository
import com.shagox.apptrainingnow.ui.components.BackButtonTN
import com.shagox.apptrainingnow.ui.components.ScreenHeaderTN
import com.shagox.apptrainingnow.ui.screen.CategoryCard
import com.shagox.apptrainingnow.ui.screen.LibraryCategory
import com.shagox.apptrainingnow.ui.screen.iconForCategory
import com.shagox.apptrainingnow.ui.theme.GrisBorde
import com.shagox.apptrainingnow.ui.theme.GrisFondo
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Biblioteca vista por el admin: la misma grilla de categorías que ve el usuario
 * (mismas tarjetas, mismo estilo), más una tarjeta extra para crear una categoría nueva.
 * Manteniendo presionada una categoría sale un menú para Editar (renombrar) o Borrar;
 * se cierra tocando afuera o con el botón atrás, como cualquier menú de Android.
 */
@Composable
fun AdminLibraryScreen(
    exerciseRepository: IExerciseRepository,
    onBack: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onCreateCategory: () -> Unit,
    actorId: Int = 0,
    actorName: String = "",
    actorRole: String = "ADMIN"
) {
    val auditLogRepository = remember { com.shagox.apptrainingnow.data.repository.AuditLogRepository() }
    val scope = rememberCoroutineScope()

    var categories by remember { mutableStateOf<List<LibraryCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        val stats = exerciseRepository.getCategoryStats().firstOrNull().orEmpty()
        categories = stats.map { LibraryCategory(it.category, it.count, iconForCategory(it.category)) }
        isLoading = false
    }

    var menuAbiertoPara by remember { mutableStateOf<String?>(null) }
    var categoriaARenombrar by remember { mutableStateOf<LibraryCategory?>(null) }
    var categoriaABorrar by remember { mutableStateOf<LibraryCategory?>(null) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val totalEjercicios = categories.sumOf { it.exerciseCount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NegroFondo)
            .padding(horizontal = 16.dp)
    ) {
        BackButtonTN(text = "Panel", onClick = onBack)
        ScreenHeaderTN(
            subtitle = "Gestionar",
            title = "BIBLIOTECA"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 14.dp),
        ) {
            Column {
                Text(
                    text = "CATEGORÍAS",
                    color = VerdeTN,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (totalEjercicios > 0)
                        "$totalEjercicios ejercicios en total · mantén presionada una categoría para editarla o borrarla"
                    else
                        "Mantén presionada una categoría para editarla o borrarla",
                    color = GrisTexto,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        mensajeError?.let {
            Text(it, color = Color(0xFFE57373), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = VerdeTN)
                    Spacer(Modifier.height(16.dp))
                    Text("Cargando categorías...", color = GrisTexto, fontSize = 14.sp)
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
                    Box {
                        CategoryCard(
                            name = category.name,
                            exerciseCount = category.exerciseCount,
                            icon = category.icon,
                            onClick = { onCategoryClick(category.name) },
                            onLongClick = { menuAbiertoPara = category.name }
                        )
                        DropdownMenu(
                            expanded = menuAbiertoPara == category.name,
                            onDismissRequest = { menuAbiertoPara = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                onClick = {
                                    menuAbiertoPara = null
                                    categoriaARenombrar = category
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Borrar") },
                                onClick = {
                                    menuAbiertoPara = null
                                    categoriaABorrar = category
                                }
                            )
                        }
                    }
                }
                item {
                    CrearCategoriaCard(onClick = onCreateCategory)
                }
            }
        }
    }

    categoriaARenombrar?.let { categoria ->
        var nuevoNombre by remember(categoria) { mutableStateOf(categoria.name) }
        var guardando by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!guardando) categoriaARenombrar = null },
            containerColor = GrisFondo,
            title = { Text("Editar categoría", color = VerdeTN, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Se actualizarán todos los ejercicios en \"${categoria.name}\".",
                        color = GrisTexto,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nuevoNombre,
                        onValueChange = { nuevoNombre = it },
                        singleLine = true,
                        label = { Text("Nombre") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal,
                            focusedBorderColor = VerdeTN,
                            unfocusedBorderColor = GrisTexto,
                            focusedLabelColor = VerdeTN,
                            cursorColor = VerdeTN
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !guardando && nuevoNombre.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeTN, contentColor = TextoSobreVerde),
                    onClick = {
                        val destino = nuevoNombre.trim()
                        if (destino == categoria.name) {
                            categoriaARenombrar = null
                            return@Button
                        }
                        guardando = true
                        mensajeError = null
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    exerciseRepository.renameCategory(categoria.name, destino)
                                    auditLogRepository.log(
                                        actorId = actorId,
                                        actorName = actorName,
                                        actorRole = actorRole,
                                        action = "CATEGORY_RENAMED",
                                        targetType = "CATEGORY",
                                        targetName = destino,
                                        details = "\"${categoria.name}\" → \"$destino\" (${categoria.exerciseCount} ejercicios)"
                                    )
                                }
                                categoriaARenombrar = null
                                refreshTrigger++
                            } catch (e: Exception) {
                                mensajeError = e.message ?: "No se pudo renombrar la categoría"
                            } finally {
                                guardando = false
                            }
                        }
                    }
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(20.dp), color = TextoSobreVerde)
                    else Text("Guardar")
                }
            },
            dismissButton = {
                Button(
                    onClick = { if (!guardando) categoriaARenombrar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = Color.White)
                ) { Text("Cancelar") }
            }
        )
    }

    categoriaABorrar?.let { categoria ->
        var borrando by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!borrando) categoriaABorrar = null },
            containerColor = GrisFondo,
            title = { Text("¿Borrar \"${categoria.name}\"?", color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (categoria.exerciseCount > 0)
                        "Esta categoría tiene ${categoria.exerciseCount} ejercicio(s). Se eliminarán también. Esta acción no se puede deshacer."
                    else
                        "Esta categoría está vacía. Esta acción no se puede deshacer.",
                    color = GrisTexto
                )
            },
            confirmButton = {
                Button(
                    enabled = !borrando,
                    onClick = {
                        borrando = true
                        mensajeError = null
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    exerciseRepository.deleteCategory(categoria.name)
                                    auditLogRepository.log(
                                        actorId = actorId,
                                        actorName = actorName,
                                        actorRole = actorRole,
                                        action = "CATEGORY_DELETED",
                                        targetType = "CATEGORY",
                                        targetName = categoria.name,
                                        details = "${categoria.exerciseCount} ejercicio(s) eliminados con ella"
                                    )
                                }
                                categoriaABorrar = null
                                refreshTrigger++
                            } catch (e: Exception) {
                                mensajeError = e.message ?: "No se pudo borrar la categoría"
                            } finally {
                                borrando = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935), contentColor = Color.White)
                ) {
                    if (borrando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    else Text("BORRAR", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { if (!borrando) categoriaABorrar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GrisBorde, contentColor = Color.White)
                ) { Text("Cancelar") }
            }
        )
    }
}

/** Tarjeta "+" al final de la grilla: mismo tamaño que las categorías, borde punteado visual. */
@Composable
private fun CrearCategoriaCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GrisFondo)
            .border(1.dp, GrisTexto.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VerdeTN.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = VerdeTN,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Crear categoría",
                color = TextoPrincipal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
