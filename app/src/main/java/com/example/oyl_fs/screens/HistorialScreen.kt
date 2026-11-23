
package com.example.oyl_fs.screens

import com.example.oyl_fs.ui.theme.BotonVolverOYL

import com.example.oyl_fs.ui.theme.MensajeErrorOYL


import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import android.widget.Toast
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.ui.components.EstadoPedidoChip
import com.example.oyl_fs.viewmodel.HistorialViewModel


import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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
    onPrepararPedido: (Pedido) -> Unit,
    onNavegar: (Int) -> Unit = { onVolver() }
) {

    val contexto = LocalContext.current

    // Configura el usuario al ingresar a la pantalla.
    LaunchedEffect(usuarioId, esAdmin) {
        viewModel.configurarSesion(usuarioId, esAdmin)
    }

    // Observa los cambios del ViewModel.
    val pedidos by viewModel.pedidosVisibles.collectAsState()
    val filtro by viewModel.filtroEstado.collectAsState()
    val errorCarga by viewModel.error.collectAsState()


    val scope = rememberCoroutineScope()

    // Impide modificaciones simultáneas
    // de estado desde esta pantalla.
    var cambiandoEstado by remember {
        mutableStateOf(false)
    }


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
        bottomBar = {
            // Reutiliza la misma barra de los Home, con Pedidos seleccionado.
            val navegar: (Int) -> Unit = { indice ->
                if (indice != 3) onNavegar(indice)
            }
            if (esAdmin) {
                BarraAdmin(seleccion = 3, onSelect = navegar)
            } else {
                BarraCliente(seleccion = 3, onSelect = navegar)
            }
        },

        topBar = {
            TopAppBar(
                title = {
                    com.example.oyl_fs.ui.theme.TituloPantallaOYL(
                        if (esAdmin) "Pedidos" else "Mis pedidos", Modifier.fillMaxWidth())
                },

                navigationIcon = {
                    BotonVolverOYL(onVolver)
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

            if (errorCarga != null) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    MensajeErrorOYL(errorCarga.orEmpty())
                    TextButton(onClick = { viewModel.refrescar() }) { Text("Reintentar") }
                }
            }

            // Encabezado según el perfil del usuario.
            Text(
                text = if (esAdmin) {
                    "Consulta y gestiona los pedidos de tu tienda."
                } else {
                    "Sigue el estado de tus compras."
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = PrincipalHistorial,
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
                            containerColor = Color(0xFFFFFBF5),
                            labelColor = SecundarioHistorial,
                            selectedContainerColor = SecundarioHistorial,
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
                        text = if (errorCarga == null) "No hay pedidos para mostrar" else "No pudimos cargar los pedidos",
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
                                // El backend valida el cambio; si lo rechaza, mostramos su motivo.
                                viewModel.cambiarEstadoPedido(
                                    pedido.id,
                                    nuevoEstado
                                ) { error ->
                                    if (error != null) {
                                        Toast.makeText(contexto, error, Toast.LENGTH_LONG).show()
                                    }
                                }
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

        DetallePedidoDialogo(pedidoSeleccionado) { pedidoSeleccionadoId = null }
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

    /**
     * Opciones administrativas disponibles
     * según el estado actual del pedido.
     *
     * PREPARADO no aparece aquí porque requiere
     * la verificación de productos mediante QR.
     */
    val opcionesCambio = when (pedido.estado) {

        EstadoPedido.PENDIENTE_VALIDACION -> {
            listOf(EstadoPedido.CONFIRMADO)
        }

        EstadoPedido.CONFIRMADO -> {
            // Se prepara exclusivamente mediante
            // el flujo de lectura QR.
            emptyList()
        }

        EstadoPedido.PREPARADO -> {
            listOf(EstadoPedido.ENTREGADO)
        }

        EstadoPedido.ENTREGADO -> {
            // Un pedido entregado está finalizado.
            emptyList()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0E3D3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = FondoHistorial, shape = RoundedCornerShape(14.dp)) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ReceiptLong, null, tint = SecundarioHistorial)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Pedido", color = SecundarioHistorial,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(pedido.id, color = PrincipalHistorial, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatearFecha(pedido.fecha), color = PrincipalHistorial, fontSize = 12.sp)
                }
            }

            EstadoPedidoChip(estado = pedido.estado)

            val unidades = pedido.productos.sumOf { it.cantidad.toLong() }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$unidades ${if (unidades == 1L) "unidad" else "unidades"}",
                    color = SecundarioHistorial, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                if (pedido.productos.isNotEmpty()) {
                    Text(pedido.productos.joinToString(" · ") { it.nombreProducto },
                        color = PrincipalHistorial, fontSize = 13.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }

            Divider(color = Color(0xFFF0E3D3))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Total del pedido", Modifier.weight(1f), color = PrincipalHistorial, fontSize = 13.sp)
                Text(formatearPrecio(pedido.total), color = SecundarioHistorial,
                    fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }

            Button(
                onClick = onVerDetalle,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecundarioHistorial, contentColor = Color.White)
            ) {
                Text("Ver detalle", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
            }

            // Se mantienen las mismas transiciones y el flujo de preparación QR.
            if (esAdmin && opcionesCambio.isNotEmpty()) {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { menuAbierto = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White, contentColor = SecundarioHistorial)
                    ) {
                        Text("Cambiar estado")
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ExpandMore, null, Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        opcionesCambio.forEach { estado ->
                            DropdownMenuItem(
                                text = { Text(estado.descripcion) },
                                onClick = { menuAbierto = false; onCambiarEstado(estado) }
                            )
                        }
                    }
                }
            }
            if (esAdmin && pedido.estado == EstadoPedido.CONFIRMADO) {
                Button(
                    onClick = onPrepararPedido,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecundarioHistorial),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Preparar pedido (QR)")
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

// Detalle compartido por cliente y administrador, con lista desplazable.
@Composable
private fun DetallePedidoDialogo(pedido: Pedido, onCerrar: () -> Unit) {
    val altoMaximo = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.84f
    androidx.compose.ui.window.Dialog(onDismissRequest = onCerrar,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 20.dp).heightIn(max = altoMaximo),
            shape = RoundedCornerShape(28.dp), color = Color.White, tonalElevation = 0.dp
        ) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 22.dp, top = 16.dp, end = 10.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Detalle del pedido", Modifier.weight(1f), color = SecundarioHistorial,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
                    IconButton(onClick = onCerrar) { Icon(Icons.Default.Close, "Cerrar detalle", tint = PrincipalHistorial) }
                }
                Divider(color = Color(0xFFF0E3D3))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("NÚMERO DE PEDIDO", color = PrincipalHistorial, fontSize = 10.sp,
                            letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(pedido.id, color = SecundarioHistorial, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Text(formatearFecha(pedido.fecha), color = PrincipalHistorial, fontSize = 13.sp)
                    }
                    EstadoPedidoChip(pedido.estado)
                    Text("Productos del pedido", color = SecundarioHistorial,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    pedido.productos.forEach { producto ->
                        Surface(color = Color(0xFFFFFBF5), shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFF0E3D3))) {
                            Column(Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(producto.nombreProducto, color = SecundarioHistorial, fontWeight = FontWeight.SemiBold)
                                Text("${producto.cantidad} × ${formatearPrecio(producto.precioUnitario)}",
                                    color = PrincipalHistorial, fontSize = 13.sp)
                                Text(formatearPrecio(producto.subtotal), color = SecundarioHistorial,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
                            }
                        }
                    }
                }
                Divider(color = Color(0xFFF0E3D3))
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Total", Modifier.weight(1f), color = PrincipalHistorial, fontSize = 15.sp)
                        Text(formatearPrecio(pedido.total), color = SecundarioHistorial,
                            fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    }
                    Button(onClick = onCerrar, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecundarioHistorial)) { Text("Cerrar detalle") }
                }
            }
        }
    }
}
