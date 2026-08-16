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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
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
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal
import com.shagox.apptrainingnow.ui.theme.TextoSobreVerde
import kotlinx.coroutines.launch


/**
 * Pantalla Mis Rutinas: mismo diseño que Biblioteca y resto de la app.
 * Crear RUTINA, RUTINAS RECOMENDADAS (lista vertical, tarjetas mismo tamaño), MIS RUTINAS.
 */
private val RojoAviso = Color(0xFFE53935)

@Composable
fun UserRoutinesScreen(
    routineRepository: RoutineRepository,
    userId: Int,
    isLoggedIn: Boolean = userId > 0,
    avisoYaMostrado: Boolean = false,
    onAvisoMostrado: () -> Unit = {},
    onCrearCuenta: () -> Unit = {},
    onCreateRoutine: () -> Unit,
    onRoutineClick: (routineId: Int) -> Unit,
    userRepository: com.shagox.apptrainingnow.data.repository.IUserRepository? = null
) {
    val globalRoutines by routineRepository.getGlobalRoutines().collectAsState(initial = emptyList())
    val myRoutines by routineRepository.getUserOwnRoutines(userId).collectAsState(initial = emptyList())

    // Nombre de cada entrenador que compartió (y le aceptaron) una rutina, para agruparlas
    // bajo "Hechas por el entrenador X" en vez de mezclarlas con lo que el usuario creó.
    var trainerNames by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    androidx.compose.runtime.LaunchedEffect(myRoutines) {
        val trainerIds = myRoutines.filter { it.creatorId != userId }.map { it.creatorId }.distinct()
        if (trainerIds.isNotEmpty() && userRepository != null) {
            trainerNames = trainerIds.associateWith { id ->
                userRepository.getUserById(id)?.let { "${it.name} ${it.lastName}".trim() } ?: "tu entrenador"
            }
        }
    }

    UserRoutinesScreenContent(
        globalRoutines = globalRoutines,
        myRoutines = myRoutines,
        isLoggedIn = isLoggedIn,
        avisoYaMostrado = avisoYaMostrado,
        onAvisoMostrado = onAvisoMostrado,
        onCrearCuenta = onCrearCuenta,
        onCreateRoutine = onCreateRoutine,
        onRoutineClick = onRoutineClick,
        currentUserId = userId,
        trainerNames = trainerNames
    )
}


@Composable
private fun UserRoutinesScreenContent(
    globalRoutines: List<RoutineEntity>,
    myRoutines: List<RoutineEntity>,
    isLoggedIn: Boolean = true,
    avisoYaMostrado: Boolean = false,
    onAvisoMostrado: () -> Unit = {},
    onCrearCuenta: () -> Unit = {},
    onCreateRoutine: () -> Unit,
    onRoutineClick: (routineId: Int) -> Unit,
    currentUserId: Int = 0,
    trainerNames: Map<Int, String> = emptyMap()
) {
    var mostrarAvisoCuenta by remember { mutableStateOf(false) }

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
                .clickable {
                    if (isLoggedIn || avisoYaMostrado) onCreateRoutine()
                    else mostrarAvisoCuenta = true
                }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Crear",
                        color = TextoSobreVerde.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "RUTINA",
                        color = TextoSobreVerde,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Diseña tu plan personalizado",
                        color = TextoSobreVerde.copy(alpha = 0.7f),
                        fontSize = 12.sp
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
                        tint = TextoPrincipal,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Título sección (mismo estilo que CATEGORÍAS en Biblioteca, con contorno negro)
        Text(
            text = "RUTINAS RECOMENDADAS",
            color = VerdeTN,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        if (globalRoutines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GrisFondo)
                    .padding(18.dp)
            ) {
                Text("Aún no hay rutinas recomendadas publicadas", color = GrisTexto, fontSize = 13.sp)
            }
        } else {
            RoutinesCarousel(
                routines = globalRoutines,
                onRoutineClick = onRoutineClick
            )
        }

        val propias = myRoutines.filter { it.creatorId == currentUserId }
        val porEntrenador = myRoutines.filter { it.creatorId != currentUserId }.groupBy { it.creatorId }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            porEntrenador.forEach { (trainerId, rutinas) ->
                item(key = "trainer_header_$trainerId") {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "HECHAS POR EL ENTRENADOR ${(trainerNames[trainerId] ?: "tu entrenador").uppercase()}",
                        color = VerdeTN,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(rutinas, key = { "trainer_${trainerId}_${it.id}" }) { routine ->
                    RoutineCard(
                        title = routine.name,
                        subtitle = routine.dayInfo,
                        icon = Icons.Filled.FitnessCenter,
                        onClick = { onRoutineClick(routine.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "CREADOS POR TI",
                    color = VerdeTN,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(propias, key = { "own_${it.id}" }) { routine ->
                RoutineCard(
                    title = routine.name,
                    subtitle = routine.dayInfo,
                    icon = Icons.Filled.FitnessCenter,
                    onClick = { onRoutineClick(routine.id) }
                )
            }

            if (propias.isEmpty()) {
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
                                color = TextoPrincipal,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Aviso al crear una rutina sin cuenta
    if (mostrarAvisoCuenta) {
        CuentaRecomendadaDialog(
            onCrearCuenta = {
                mostrarAvisoCuenta = false
                onAvisoMostrado()
                onCrearCuenta()
            },
            onContinuarSinCuenta = {
                mostrarAvisoCuenta = false
                onAvisoMostrado()
                onCreateRoutine()
            },
            onDismiss = { mostrarAvisoCuenta = false }
        )
    }
}

/** Milisegundos dentro de los cuales una rutina global se marca como "Nuevo entrenamiento". */
private const val VENTANA_NUEVO_ENTRENAMIENTO_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Carrusel horizontal de rutinas recomendadas: se desliza con el dedo y también
 * con flechas a los costados. Cada tarjeta muestra "Nuevo entrenamiento" si el
 * admin la publicó hace menos de 7 días.
 */
@Composable
private fun RoutinesCarousel(
    routines: List<RoutineEntity>,
    onRoutineClick: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val ahora = remember { System.currentTimeMillis() }

    // Las flechas van SUPERPUESTAS sobre el carrusel (Box + Alignment), no en celdas propias
    // al costado: así no dejan un cuadrado de fondo separado junto a las tarjetas.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 36.dp)
        ) {
            items(routines, key = { it.id }) { routine ->
                val esNuevo = routine.ownerId == null &&
                        (ahora - routine.creationDate) in 0..VENTANA_NUEVO_ENTRENAMIENTO_MS
                RoutineCard(
                    title = routine.name,
                    subtitle = routine.dayInfo,
                    icon = Icons.Filled.FitnessCenter,
                    badge = if (esNuevo) "Nuevo entrenamiento" else null,
                    modifier = Modifier.width(230.dp).height(158.dp),
                    onClick = { onRoutineClick(routine.id) }
                )
            }
        }

        CarouselArrow(
            icon = Icons.Filled.ChevronLeft,
            contentDescription = "Anterior",
            modifier = Modifier.align(Alignment.CenterStart),
            onClick = {
                scope.launch {
                    val destino = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                    listState.animateScrollToItem(destino)
                }
            }
        )

        CarouselArrow(
            icon = Icons.Filled.ChevronRight,
            contentDescription = "Siguiente",
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = {
                scope.launch {
                    val destino = (listState.firstVisibleItemIndex + 1).coerceAtMost((routines.size - 1).coerceAtLeast(0))
                    listState.animateScrollToItem(destino)
                }
            }
        )
    }
}

/**
 * Flecha flotante sobre el carrusel: círculo semitransparente con sombra, sin fondo
 * cuadrado detrás (antes cada flecha vivía en su propia celda del Row, dejando un
 * cuadrado GrisFondo feo junto a las tarjetas).
 */
@Composable
private fun CarouselArrow(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .shadow(elevation = 3.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(GrisFondo.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = VerdeTN, modifier = Modifier.size(20.dp))
    }
}

/** Límite de caracteres para que el título/subtítulo nunca desborde la tarjeta de tamaño fijo. */
private const val LIMITE_TITULO = 42
private const val LIMITE_SUBTITULO = 34

private fun String.limitado(max: Int): String =
    if (length > max) trim().take(max).trimEnd() + "…" else this

/** Tarjeta de rutina: mismo estilo que categorías/ejercicios (GrisFondo, borde VerdeTN, icono verde). */
@Composable
private fun RoutineCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrisFondo)
            .border(1.dp, VerdeTN, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VerdeTN)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge.uppercase(),
                        color = TextoSobreVerde,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VerdeTN.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VerdeTN,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.limitado(LIMITE_TITULO),
                        color = TextoPrincipal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null && subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = subtitle.limitado(LIMITE_SUBTITULO),
                            color = GrisTexto,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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


/**
 * Aviso al crear una rutina sin haber iniciado sesión.
 * Fondo negro con acentos verdes; la opción de continuar sin cuenta se destaca en rojo.
 */
@Composable
private fun CuentaRecomendadaDialog(
    onCrearCuenta: () -> Unit,
    onContinuarSinCuenta: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(18.dp))
                .background(NegroFondo)
                .border(1.5.dp, VerdeTN, RoundedCornerShape(18.dp))
        ) {
            // Contenido
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VerdeTN.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = VerdeTN,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "¿Creamos tu cuenta?",
                        color = VerdeTN,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Sin cuenta puedes crear tu rutina, pero se guarda solo en este teléfono y se pierde al desinstalar la app.",
                    color = TextoPrincipal.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // Botones horizontales pegados al borde inferior
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(RojoAviso)
                        .clickable(onClick = onContinuarSinCuenta)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO, CONTINUAR",
                        color = TextoPrincipal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(VerdeTN)
                        .clickable(onClick = onCrearCuenta)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SÍ, CREAR CUENTA",
                        color = TextoSobreVerde,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

