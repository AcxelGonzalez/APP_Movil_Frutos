
package com.example.oyl_fs.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.ui.components.EstadoPedidoChip
import com.example.oyl_fs.viewmodel.HistorialViewModel

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Colores oficiales definidos para OYL Frutos Secos.
private val FondoHistorial = Color(0xFFFFF5E7)
private val PrincipalHistorial = Color(0xFF92623D)
private val SecundarioHistorial = Color(0xFF4B2E15)

/**
 * Pantalla de historial de pedidos.
 *
 * Muestra los pedidos del cliente o administrador.
 * Utiliza un ViewModel para obtener los datos,
 * gestionar filtros y actualizar estados.
 *
 * Permite al administrador acceder a la preparación
 * de pedidos mediante validación de códigos QR.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    usuarioId: String,
    esAdmin: Boolean,
    viewModel: HistorialViewModel,
    onVolver: () -> Unit,
    onPrepararPedido: (Pedido) -> Unit
) {

    // Configura el usuario al ingresar a la pantalla.
    LaunchedEffect(usuarioId, esAdmin) {
        viewModel.configurarSesion(usuarioId, esAdmin)
    }

    // Observa los cambios del ViewModel.
    val pedidos by viewModel.pedidosVisibles.collectAsState()
    val filtro by viewModel.filtroEstado.collectAsState()

    // Identificador del pedido seleccionado para ver detalles.
    var pedidoSeleccionadoId by remember {
        mutableStateOf<String?>(null)
    }

    val pedidoSeleccionado = pedidos.firstOrNull {
        it.id == pedidoSeleccionadoId
    }

    // Pantalla principal con encabezado y contenido.
    Scaffold(
        containerColor = FondoHistorial,

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de pedidos",
                        color = SecundarioHistorial,
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {
                    TextButton(onClick = onVolver) {
                        Text(
                            text = "Volver",
                            color = PrincipalHistorial
                        )
                    }
                },

                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = FondoHistorial
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Encabezado según el perfil del usuario.
            Text(
                text = if (esAdmin) {
                    "Gestión de pedidos"
                } else {
                    "Mis pedidos"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SecundarioHistorial,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )

            // Opciones disponibles para filtrar.
            // null representa todos los estados.
            val estados = listOf<EstadoPedido?>(null) +
                    EstadoPedido.values().toList()

            // Filtros horizontales.
            LazyRow(
                contentPadding = PaddingValues(
                    horizontal = 16.dp
                ),
                horizontalArrangement =
                Arrangement.spacedBy(8.dp)
            ) {
                items(estados.size) { indice ->
                    val estado = estados[indice]

                    FilterChip(
                        selected = filtro == estado,

                        onClick = {
                            viewModel.seleccionarFiltro(estado)
                        },

                        label = {
                            Text(
                                text = estado?.descripcion ?: "Todos"
                            )
                        },

                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor =
                            PrincipalHistorial,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Muestra una advertencia si no existen pedidos.
            if (pedidos.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay pedidos para mostrar",
                        color = PrincipalHistorial
                    )
                }

            } else {

                // Lista desplazable de pedidos.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),

                    contentPadding = PaddingValues(16.dp),

                    verticalArrangement =
                    Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = pedidos,
                        key = { it.id }
                    ) { pedido ->

                        TarjetaPedido(
                            pedido = pedido,
                            esAdmin = esAdmin,

                            onVerDetalle = {
                                pedidoSeleccionadoId = pedido.id
                            },

                            onCambiarEstado = { nuevoEstado ->
                                viewModel.cambiarEstadoPedido(
                                    pedido.id,
                                    nuevoEstado
                                )
                            },

                            // Abre la preparación del pedido seleccionado.
                            onPrepararPedido = {
                                onPrepararPedido(pedido)
                            }
                        )
                    }
                }
            }
        }
    }

    // Ventana con el detalle del pedido seleccionado.
    if (pedidoSeleccionado != null) {

        AlertDialog(
            onDismissRequest = {
                pedidoSeleccionadoId = null
            },

            title = {
                Text("Pedido ${pedidoSeleccionado.id}")
            },

            text = {
                Column(
                    verticalArrangement =
                    Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Fecha: ${formatearFecha(pedidoSeleccionado.fecha)}"
                    )

                    Text(
                        "Estado: ${pedidoSeleccionado.estado.descripcion}"
                    )

                    Divider()

                    Text(
                        text = "Productos:",
                        fontWeight = FontWeight.Bold
                    )

                    // Muestra los productos del pedido.
                    pedidoSeleccionado.productos.forEach { detalle ->
                        Text(
                            text = "${detalle.nombreProducto} " +
                                    "x${detalle.cantidad} - " +
                                    formatearPrecio(detalle.subtotal)
                        )
                    }

                    Divider()

                    Text(
                        text = "Total: " +
                                formatearPrecio(pedidoSeleccionado.total),
                        fontWeight = FontWeight.Bold,
                        color = SecundarioHistorial
                    )
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        pedidoSeleccionadoId = null
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

/**
 * Tarjeta reutilizable para mostrar un pedido.
 *
 * Permite consultar sus detalles y,
 * si corresponde, modificar su estado.
 *
 * En pedidos confirmados permite al administrador
 * iniciar la preparación mediante códigos QR.
 */
@Composable
private fun TarjetaPedido(
    pedido: Pedido,
    esAdmin: Boolean,
    onVerDetalle: () -> Unit,
    onCambiarEstado: (EstadoPedido) -> Unit,
    onPrepararPedido: () -> Unit
) {

    // Controla el menú administrativo.
    var menuAbierto by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Identificador, fecha y total del pedido.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pedido.id,
                        fontWeight = FontWeight.Bold,
                        color = SecundarioHistorial
                    )

                    Text(
                        text = formatearFecha(pedido.fecha),
                        fontSize = 12.sp,
                        color = PrincipalHistorial
                    )
                }

                Text(
                    text = formatearPrecio(pedido.total),
                    fontWeight = FontWeight.Bold,
                    color = SecundarioHistorial
                )
            }

            // Componente reutilizable del estado.
            EstadoPedidoChip(
                estado = pedido.estado
            )

            // Acciones disponibles para el usuario.
            // Se usa Column para evitar que los botones
            // ocupen más espacio que el ancho de la pantalla.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TextButton(onClick = onVerDetalle) {
                        Text("Ver detalle")
                    }

                    // Solo el administrador puede modificar estados.
                    if (esAdmin) {

                        Box {

                            OutlinedButton(
                                onClick = {
                                    menuAbierto = true
                                }
                            ) {
                                Text("Cambiar estado")
                            }

                            DropdownMenu(
                                expanded = menuAbierto,
                                onDismissRequest = {
                                    menuAbierto = false
                                }
                            ) {

                                // PREPARADO solo se podrá establecer
                                // luego de completar la validación QR.
                                EstadoPedido.values()
                                    .filter {
                                        it != EstadoPedido.PREPARADO
                                    }
                                    .forEach { estado ->

                                        DropdownMenuItem(
                                            text = {
                                                Text(estado.descripcion)
                                            },

                                            onClick = {
                                                menuAbierto = false
                                                onCambiarEstado(estado)
                                            }
                                        )
                                    }
                            }
                        }
                    }
                }

                // El administrador puede preparar pedidos confirmados.
                if (esAdmin && pedido.estado == EstadoPedido.CONFIRMADO) {

                    Button(
                        onClick = onPrepararPedido,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecundarioHistorial
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Preparar pedido (QR)")
                    }
                }
            }
        }
    }
}

/**
 * Convierte un precio entero a pesos chilenos.
 * Ejemplo: 14200 -> $14.200
 */
private fun formatearPrecio(valor: Int): String {
    val formato = NumberFormat.getIntegerInstance(
        Locale("es", "CL")
    )

    return "$" + formato.format(valor)
}

/**
 * Convierte el timestamp a una fecha legible.
 * Ejemplo: 14/10/2026
 */
private fun formatearFecha(fecha: Long): String {
    val formato = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale("es", "CL")
    )

    return formato.format(Date(fecha))
}
