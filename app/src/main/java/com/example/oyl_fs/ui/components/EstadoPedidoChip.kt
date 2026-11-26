package com.example.oyl_fs.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.model.EstadoPedido

/**
 * Componente visual reutilizable que muestra
 * el estado de un pedido mediante texto y color.
 *
 * Los colores permiten identificar rápidamente
 * el avance de cada solicitud.
 */
@Composable
fun EstadoPedidoChip(
    estado: EstadoPedido,
    modifier: Modifier = Modifier
) {

    // Selecciona los colores según el estado.
    val (colorFondo, colorTexto) = when (estado) {

        EstadoPedido.PENDIENTE_VALIDACION ->
            Color(0xFFFDE8E6) to Color(0xFFB3261E)

        EstadoPedido.CONFIRMADO ->
            Color(0xFFE7F0FF) to Color(0xFF225CB5)

        EstadoPedido.PREPARADO ->
            Color(0xFFF8E8BD) to Color(0xFF795317)

        EstadoPedido.ENTREGADO ->
            Color(0xFFE5F3E8) to Color(0xFF236B3A)
    }

    // Contenedor visual del estado.
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colorFondo,
        contentColor = colorTexto
    ) {

        Text(
            text = estado.descripcion,
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
