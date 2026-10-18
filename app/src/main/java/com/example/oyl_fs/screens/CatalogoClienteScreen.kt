package com.example.oyl_fs.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.viewmodel.CarritoViewModel
import com.example.oyl_fs.viewmodel.ProductoViewModel

private val FondoCliente = Color(0xFFFFF5E7)
private val CafeCliente = Color(0xFF92623D)
private val OscuroCliente = Color(0xFF4B2E15)

// Comparte el catálogo y el carrito con el inicio del cliente.
@Composable
fun ProductosCliente(
    productoViewModel: ProductoViewModel,
    carritoViewModel: CarritoViewModel,
    onVolver: () -> Unit,
    onAbrirCarrito: () -> Unit
) {
    val productos by productoViewModel.productos.collectAsState()
    val carrito by carritoViewModel.uiState.collectAsState()
    var productoId by rememberSaveable { mutableStateOf<Int?>(null) }
    val seleccionado = productos.find { it.id == productoId && it.disponible }

    fun unidadesDisponibles(producto: ProductoInfo): Int {
        val enCarrito = carrito.items.find {
            it.productoId == producto.id.toString()
        }?.cantidad ?: 0
        return (producto.stock - enCarrito).coerceAtLeast(0)
    }

    BackHandler {
        if (productoId != null) productoId = null else onVolver()
    }

    LaunchedEffect(productos, productoId) {
        if (productoId != null && seleccionado == null) productoId = null
    }

    if (seleccionado != null) {
        DetalleProductoClienteScreen(
            producto = seleccionado,
            disponibleParaAgregar = unidadesDisponibles(seleccionado),
            onVolver = { productoId = null },
            onAgregar = { cantidad ->
                carritoViewModel.agregarProducto(seleccionado, cantidad)
            },
            onAbrirCarrito = onAbrirCarrito
        )
    } else {
        CatalogoClienteScreen(
            productos = productos.filter { it.disponible },
            onVolver = onVolver,
            onDetalle = { productoId = it.id },
            onAgregar = { producto, cantidad -> carritoViewModel.agregarProducto(producto, cantidad) },
            unidadesDisponibles = { unidadesDisponibles(it) },
            cantidadEnCarrito = carrito.cantidadProductos,
            totalCarrito = carrito.total,
            onAbrirCarrito = onAbrirCarrito
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoClienteScreen(
    productos: List<ProductoInfo>,
    onVolver: () -> Unit,
    onDetalle: (ProductoInfo) -> Unit,
    onAgregar: (ProductoInfo, Int) -> Boolean,
    unidadesDisponibles: (ProductoInfo) -> Int = { it.stock },
    cantidadEnCarrito: Int = 0,
    totalCarrito: Long = 0L,
    onAbrirCarrito: () -> Unit = {}
) {
    var productoParaAgregarId by rememberSaveable { mutableStateOf<Int?>(null) }
    var cantidad by rememberSaveable { mutableStateOf(1) }
    var confirmacion by rememberSaveable { mutableStateOf<String?>(null) }
    val productoParaAgregar = productos.find { it.id == productoParaAgregarId && it.disponible }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf("Todos") }
    val categorias = listOf("Todos", "Frutos secos", "Semillas", "Deshidratados")
    val filtrados = productos.filter {
        it.disponible && it.nombre.contains(busqueda.trim(), ignoreCase = true) &&
            (categoria == "Todos" || it.categoria == categoria)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(FondoCliente).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, "Volver al inicio", tint = OscuroCliente)
            }
            Text("Productos", color = OscuroCliente, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            // El contador se dibuja dentro del botón para mantenerlo alineado.
            Box(Modifier.size(56.dp)) {
                FilledIconButton(onClick = onAbrirCarrito, modifier = Modifier.fillMaxSize(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = OscuroCliente)) {
                    Icon(Icons.Default.ShoppingCart, "Abrir carrito: $cantidadEnCarrito unidades", tint = Color.White)
                }
                if (cantidadEnCarrito > 0) {
                    Badge(Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp),
                        containerColor = Color(0xFFB3261E), contentColor = Color.White) {
                        Text(if (cantidadEnCarrito > 99) "99+" else "$cantidadEnCarrito")
                    }
                }
            }
        }
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar productos...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = CafeCliente) },
            trailingIcon = {
                if (busqueda.isNotEmpty()) IconButton(onClick = { busqueda = "" }) {
                    Icon(Icons.Default.Close, "Borrar búsqueda", tint = CafeCliente)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = OscuroCliente, containerColor = Color.White,
                focusedBorderColor = CafeCliente, unfocusedBorderColor = CafeCliente
            ),
            modifier = Modifier.fillMaxWidth()
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categorias.size) { indice ->
                val texto = categorias[indice]
                FilterChip(
                    selected = categoria == texto,
                    onClick = { categoria = texto },
                    label = { Text(texto) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White, labelColor = CafeCliente,
                        selectedContainerColor = OscuroCliente, selectedLabelColor = Color.White
                    )
                )
            }
        }
        Text("${filtrados.size} ${if (filtrados.size == 1) "producto" else "productos"}",
            color = CafeCliente, fontSize = 12.sp)
        if (filtrados.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, null, tint = CafeCliente, modifier = Modifier.size(32.dp))
                Text("No encontramos productos", color = OscuroCliente, fontWeight = FontWeight.Bold)
                Text("Prueba otro nombre o categoría.", color = CafeCliente)
                TextButton(onClick = { busqueda = ""; categoria = "Todos" }) { Text("Ver todos") }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(filtrados, key = { it.id }) { producto ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ImagenProducto(producto, Modifier.fillMaxWidth().height(100.dp)
                            .clip(RoundedCornerShape(14.dp)).clickable { onDetalle(producto) })
                        TextoTarjetaProducto(producto.nombre, color = OscuroCliente, fontWeight = FontWeight.Bold)
                        TextoTarjetaProducto(producto.presentacion, color = CafeCliente, fontSize = 12.sp)
                        TextoTarjetaProducto("$" + "%,d".format(producto.precio).replace(',', '.'),
                            color = OscuroCliente, fontWeight = FontWeight.Bold, lineas = 1)
                        TextoTarjetaProducto(when {
                            producto.stock <= 0 -> "Sin stock"
                            unidadesDisponibles(producto) <= 0 -> "Límite en carrito"
                            else -> "Disponible"
                        }, color = CafeCliente, fontSize = 12.sp, lineas = 1)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { onDetalle(producto) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OscuroCliente)
                            ) { Text("Detalle", fontSize = 11.sp) }
                            Button(
                                onClick = { productoParaAgregarId = producto.id; cantidad = 1 },
                                enabled = unidadesDisponibles(producto) > 0,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OscuroCliente)
                            ) { Text("Agregar", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
        if (cantidadEnCarrito > 0) {
            Surface(color = Color.White, shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CafeCliente.copy(alpha = 0.15f))) {
                Row(Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("$cantidadEnCarrito ${if (cantidadEnCarrito == 1) "unidad" else "unidades"} en tu carrito",
                            color = CafeCliente, fontSize = 12.sp)
                        Text("$" + "%,d".format(totalCarrito).replace(',', '.'),
                            color = OscuroCliente, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Button(onClick = onAbrirCarrito, shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OscuroCliente)) {
                        Text("Ver carrito")
                    }
                }
            }
        }

    }

    if (productoParaAgregar != null) {
        val limite = unidadesDisponibles(productoParaAgregar).coerceAtLeast(0)
        val cantidadActual = cantidad.coerceIn(1, limite.coerceAtLeast(1))
        AlertDialog(
            onDismissRequest = { productoParaAgregarId = null },
            title = { Text("Agregar al carrito", color = OscuroCliente, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ImagenProducto(productoParaAgregar, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(productoParaAgregar.nombre, color = OscuroCliente, fontWeight = FontWeight.Bold)
                            Text(productoParaAgregar.presentacion, color = CafeCliente, fontSize = 12.sp)
                            Text("$" + "%,d".format(productoParaAgregar.precio).replace(',', '.'), color = OscuroCliente)
                        }
                    }
                    Text("Elige la cantidad", color = OscuroCliente, fontWeight = FontWeight.Bold)
                    Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            FilledTonalIconButton(onClick = { cantidad = cantidadActual - 1 }, enabled = cantidadActual > 1) {
                                Text("−", fontSize = 24.sp)
                            }
                            Text(if (limite > 0) "$cantidadActual" else "0", fontSize = 24.sp,
                                fontWeight = FontWeight.Bold, color = OscuroCliente)
                            FilledTonalIconButton(onClick = { cantidad = cantidadActual + 1 }, enabled = cantidadActual < limite) {
                                Text("+", fontSize = 24.sp)
                            }
                        }
                    }
                    Text("Disponibles: $limite", color = CafeCliente, fontSize = 12.sp)
                    Text("Subtotal: $" + "%,d".format(productoParaAgregar.precio.toLong() * if (limite > 0) cantidadActual else 0).replace(',', '.'),
                        color = OscuroCliente, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = OscuroCliente),
                    shape = RoundedCornerShape(12.dp), enabled = limite > 0, onClick = {
                    if (onAgregar(productoParaAgregar, cantidadActual)) {
                        confirmacion = "${productoParaAgregar.nombre} · $cantidadActual ${if (cantidadActual == 1) "unidad" else "unidades"} en tu carrito."
                        productoParaAgregarId = null
                    }
                }) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { productoParaAgregarId = null }) { Text("Cancelar") } },
            containerColor = FondoCliente
        )
    }
    if (confirmacion != null) {
        AvisoProductoAgregado(confirmacion.orEmpty(), onCerrar = { confirmacion = null })
    }

}

@Preview(showBackground = true)
@Composable
private fun CatalogoClientePreview() {
    MaterialTheme {
        CatalogoClienteScreen(
            productos = ProductosDatos.productos,
            onVolver = {}, onDetalle = {}, onAgregar = { _, _ -> false },
            cantidadEnCarrito = 2, totalCarrito = 13980L
        )
    }
}

@Composable
private fun AvisoProductoAgregado(mensaje: String, onCerrar: () -> Unit) {
    val cerrarActual by rememberUpdatedState(onCerrar)
    val opacidad = remember { Animatable(0f) }
    val escala = remember { Animatable(0.75f) }
    val escalaCheck = remember { Animatable(0.35f) }
    Dialog(onDismissRequest = onCerrar) {
        // Empieza dentro de la ventana, después de que Compose dibuje el aviso.
        LaunchedEffect(mensaje) {
            withFrameNanos { }
            withFrameNanos { }
            delay(150)
            coroutineScope {
                launch { opacidad.animateTo(1f, tween(450)) }
                launch { escala.animateTo(1f, tween(450)) }
                launch {
                    escalaCheck.animateTo(1.22f, tween(350))
                    escalaCheck.animateTo(0.9f, tween(220))
                    escalaCheck.animateTo(1.08f, tween(180))
                    escalaCheck.animateTo(1f, tween(180))
                }
            }
            delay(1800)
            cerrarActual()
        }
        Surface(
            modifier = Modifier.graphicsLayer {
                alpha = opacidad.value
                scaleX = escala.value
                scaleY = escala.value
            },
            shape = RoundedCornerShape(24.dp), color = Color(0xFFFFF5E7)
        ) {
            Column(Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(modifier = Modifier.graphicsLayer {
                    scaleX = escalaCheck.value
                    scaleY = escalaCheck.value
                }, shape = CircleShape, color = Color(0xFFE6EFDF)) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF427337),
                        modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Text("Producto agregado", fontWeight = FontWeight.Bold, color = Color(0xFF4B2E15))
                Text(mensaje, color = Color(0xFF92623D), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun TextoTarjetaProducto(
    text: String,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: TextUnit = 16.sp,
    lineas: Int = 2
) {
    val altoLinea = (fontSize.value * 1.5f).sp
    val alto = with(LocalDensity.current) { (altoLinea.value * lineas).sp.toDp() }
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = altoLinea,
        maxLines = lineas,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().height(alto)
    )
}
