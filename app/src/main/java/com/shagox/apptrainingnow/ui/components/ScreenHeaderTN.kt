package com.shagox.apptrainingnow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shagox.apptrainingnow.ui.theme.GrisTexto
import com.shagox.apptrainingnow.ui.theme.NegroFondo
import com.shagox.apptrainingnow.ui.theme.VerdeTN
import com.shagox.apptrainingnow.ui.theme.TextoPrincipal

/**
 * Encabezado unificado para todas las pantallas: subtítulo en gris, título en blanco,
 * botón cuadrado verde a la derecha con icono blanco. Fondo negro.
 */
@Composable
fun ScreenHeaderTN(
    subtitle: String,
    title: String,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector = Icons.Filled.FitnessCenter,
    onActionClick: (() -> Unit)? = null,
    actionTint: Color = Color.White,
    actionBackgroundColor: Color = VerdeTN,
    actionBadgeCount: Int? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = subtitle,
                    color = GrisTexto,
                    fontSize = 14.sp
                )
                Text(
                    text = title,
                    color = TextoPrincipal,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (onActionClick != null) {
            val count = actionBadgeCount ?: 0
            val boxModifier = Modifier
                .size(48.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(actionBackgroundColor)
                .clickable(onClick = onActionClick)
            if (count > 0) {
                BadgedBox(
                    badge = { Badge { Text(count.toString()) } }
                ) {
                    Box(
                        modifier = boxModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = actionTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = boxModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        tint = actionTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
