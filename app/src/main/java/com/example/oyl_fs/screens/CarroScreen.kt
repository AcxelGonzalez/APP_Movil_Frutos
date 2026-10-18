package com.example.oyl_fs.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.example.oyl_fs.model.ProductoInfo
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.viewmodel.CarritoUiState
import com.example.oyl_fs.viewmodel.CarritoViewModel
import java.text.NumberFormat
import java.util.Locale

// Paleta de la hoja de ruta
private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)
private val CriticoOYL = Color(0xFFC0392B)

// Pantalla que se usa en la app: conecta el ViewModel con el diseño
@Composable
fun CarroScreen(
    clienteId: String,
    onVolver: () -> Unit,
    onConfirmar: (List<ItemCarrito>) -> Unit,
    vm: CarritoViewModel = viewModel()
) {
    LaunchedEffect(clienteId) { vm.iniciar(clienteId) }

    val estado by vm.uiState.collectAsState()
    val mensaje by vm.mensaje.collectAsState()


    var mostrarConfirmacion by remember { mutableStateOf(false) }

    var itemAEliminar by remember { mutableStateOf<ItemCarrito?>(null) }

    CarroContenido(
        estado = estado,
        mensaje = mensaje,
        onMensajeMostrado = vm::mensajeMostrado,
        onVolver = onVolver,
        onAumentar = vm::aumentar,
        onDisminuir = { item ->
            if (item.cantidad <= 1) itemAEliminar = item else vm.disminuir(item)
        },
        onQuitar = { item -> itemAEliminar = item },
        onConfirmar = {
            if (vm.validarAntesDeConfirmar()) mostrarConfirmacion = true
        }
    )

    if (mostrarConfirmacion) {
        DialogoConfirmarCompra(
            estado = estado,
            onCancelar = { mostrarConfirmacion = false },
            onAceptar = {
                mostrarConfirmacion = false
                onConfirmar(estado.items)
            }
        )
    }

    // mensaje para confirmar que se quita un producto
    itemAEliminar?.let { item ->
        DialogoQuitarProducto(
            item = item,
            onCancelar = { itemAEliminar = null },
            onAceptar = {
                vm.quitar(item)
                itemAEliminar = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarroContenido(
    estado: CarritoUiState,
    mensaje: String?,
    onMensajeMostrado: () -> Unit,
    onVolver: () -> Unit,
    onAumentar: (ItemCarrito) -> Unit,
    onDisminuir: (ItemCarrito) -> Unit,
    onQuitar: (ItemCarrito) -> Unit,
    onConfirmar: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            snackbar.showSnackbar(mensaje)
            onMensajeMostrado()
        }
    }

    Scaffold(
        containerColor = FondoOYL,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = SecundarioOYL)
                }
                Text("Mi carrito", fontWeight = FontWeight.Bold, color = SecundarioOYL, fontSize = 20.sp)
            }
        },
        bottomBar = {
            if (!estado.estaVacio) ResumenTotal(estado, onConfirmar)
        }
    ) { padding ->
        if (estado.estaVacio) {
            CarritoVacio(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(estado.items, key = { it.productoId }) { item ->
                    FilaCarrito(
                        item = item,
                        onAumentar = { onAumentar(item) },
                        onDisminuir = { onDisminuir(item) },
                        onQuitar = { onQuitar(item) }
                    )
                }
            }
        }
    }
}
@Composable
private fun FilaCarrito(
    item: ItemCarrito,
    onAumentar: () -> Unit,
    onDisminuir: () -> Unit,
    onQuitar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // Foto del producto (o un ícono si no tiene)
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)).background(FondoOYL),
                contentAlignment = Alignment.Center
            ) {
                if (item.imagen != 0 || !item.imagenUri.isNullOrBlank()) {
                    ImagenProducto(producto = item.comoProducto(), modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Inventory2, null, tint = PrincipalOYL)
                }
            }

            Spacer(Modifier.width(12.dp))

            // Nombre, precio y botones - +
            Column(Modifier.weight(1f)) {
                Text(item.nombre, fontWeight = FontWeight.Bold, color = SecundarioOYL)
                Text(

                    "${item.formato} · ${formatoPrecio(item.precio.toLong())} c/u",

                    fontSize = 12.sp,
                    color = PrincipalOYL
                )
                if (!item.stockSuficiente) {
                    Text(
                        "Solo quedan ${item.stockDisponible}",
                        fontSize = 12.sp,
                        color = CriticoOYL,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BotonCantidad(Icons.Default.Remove, "Disminuir", onDisminuir)
                    Text(
                        "${item.cantidad}",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        fontWeight = FontWeight.Bold,
                        color = SecundarioOYL
                    )
                    BotonCantidad(Icons.Default.Add, "Aumentar", onAumentar)
                }
            }

            // Basurero y subtotal
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onQuitar) {
                    Icon(Icons.Default.Delete, "Quitar", tint = PrincipalOYL)
                }
                Text(formatoPrecio(item.subtotal), fontWeight = FontWeight.Bold, color = SecundarioOYL)
            }
        }
    }
}

@Composable
private fun BotonCantidad(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(SecundarioOYL)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, descripcion, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}
@Composable
private fun ResumenTotal(estado: CarritoUiState, onConfirmar: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Total (${estado.cantidadProductos} productos)",
                    color = PrincipalOYL,
                    modifier = Modifier.weight(1f)
                )
                // Transición entre importes exactos, sin modificar el cálculo del pedido.
                Crossfade(targetState = estado.total, animationSpec = tween(450)) { total ->
                    Text(
                        formatoPrecio(total),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecundarioOYL
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onConfirmar,
                colors = ButtonDefaults.buttonColors(containerColor = SecundarioOYL),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text("Confirmar pedido")
            }
        }
    }
}

@Composable
private fun CarritoVacio(modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ShoppingCart, null,
            tint = PrincipalOYL.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Tu carrito está vacío", fontWeight = FontWeight.Bold, color = SecundarioOYL, fontSize = 18.sp)
        Text("Agrega productos desde el catálogo", color = PrincipalOYL)
    }
}


/**
 * Formatea montos en pesos chilenos.
 * Acepta valores Long para representar
 * correctamente subtotales y totales elevados.
 */
private fun formatoPrecio(valor: Long): String =
    "$" + NumberFormat
        .getIntegerInstance(Locale("es", "CL"))
        .format(valor)

private fun ItemCarrito.comoProducto() = ProductoInfo(
    id = productoId.toIntOrNull() ?: 0,
    nombre = nombre,
    codigo = "",
    categoria = "",
    descripcion = "",
    presentacion = formato,
    precio = precio,
    stock = stockDisponible,
    stockMinimo = 0,
    disponible = true,
    imagen = imagen,
    imagenUri = imagenUri
)

@Composable
private fun DialogoConfirmarCompra(
    estado: CarritoUiState,
    onCancelar: () -> Unit,
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,   // tocar fuera o "atrás" = cancelar
        containerColor = Color.White,
        icon = {
            Icon(Icons.Default.ShoppingCart, null, tint = SecundarioOYL)
        },
        title = {
            Text("¿Deseas finalizar la compra?", fontWeight = FontWeight.Bold, color = SecundarioOYL)
        },
        text = {
            Text(
                "Vas a comprar ${estado.cantidadProductos} productos por un total de " +
                        "${formatoPrecio(estado.total)}.",
                color = PrincipalOYL
            )
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                colors = ButtonDefaults.buttonColors(containerColor = SecundarioOYL),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Finalizar compra") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = PrincipalOYL)
            }
        }
    )
}
@Composable
private fun DialogoQuitarProducto(
    item: ItemCarrito,
    onCancelar: () -> Unit,
    onAceptar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        icon = {
            Icon(Icons.Default.Delete, null, tint = CriticoOYL)
        },
        title = {
            Text("¿Quitar producto?", fontWeight = FontWeight.Bold, color = SecundarioOYL)
        },
        text = {
            Text(
                "Se quitará ${item.nombre} (${item.cantidad} " +
                        "${if (item.cantidad == 1) "unidad" else "unidades"}) de tu carrito.",
                color = PrincipalOYL
            )
        },
        confirmButton = {
            Button(
                onClick = onAceptar,
                colors = ButtonDefaults.buttonColors(containerColor = CriticoOYL),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Quitar") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = PrincipalOYL)
            }
        }
    )
}

// ---------- Preview (se ve sin emulador) ----------
@Preview(showBackground = true)
@Composable
private fun CarroPreview() {
    CarroContenido(
        estado = CarritoUiState(
            items = listOf(
                ItemCarrito("1", "Almendras", "500 g", 6990, 2, 10, imagen = ProductosDatos.almendras.imagen),
                ItemCarrito("2", "Nueces", "500 g", 5990, 3, 2, imagen = ProductosDatos.nueces.imagen),
                ItemCarrito("3", "Pasas", "500 g", 6000, 1, 15, imagen = ProductosDatos.pasas.imagen)
            )
        ),
        mensaje = null,
        onMensajeMostrado = {},
        onVolver = {},
        onAumentar = {},
        onDisminuir = {},
        onQuitar = {},
        onConfirmar = {}
    )
}