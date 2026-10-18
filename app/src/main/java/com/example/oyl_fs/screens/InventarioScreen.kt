
package com.example.oyl_fs.screens

import kotlinx.coroutines.launch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.viewmodel.ProductoViewModel

// Colores de OYL Frutos Secos.
private val FondoInventario = Color(0xFFFFF5E7)
private val CafeInventario = Color(0xFF92623D)
private val OscuroInventario = Color(0xFF4B2E15)
private val VerdeInventario = Color(0xFF2E7D32)
private val NaranjaInventario = Color(0xFFD9822B)
private val RojoInventario = Color(0xFFC0392B)

@Composable
fun InventarioScreen(
    viewModel: ProductoViewModel,
    onVolver: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var eliminando by remember { mutableStateOf(false) }

    // Observamos el catálogo actualizado.
    val productos by viewModel.productos.collectAsState()
    val error by viewModel.error.collectAsState()

    // Controlamos la navegación dentro del inventario.
    var pantalla by rememberSaveable {
        mutableStateOf("inventario")
    }

    // Guardamos el identificador del producto seleccionado.
    var productoId by rememberSaveable {
        mutableStateOf<Int?>(null)
    }

    val productoSeleccionado = productos.find {
        it.id == productoId
    }

    // Regresamos a la pantalla anterior.
    fun volver() {
        viewModel.limpiarError()

        when (pantalla) {
            "editar", "qr" -> pantalla = "detalle"
            "detalle" -> {
                productoId = null
                pantalla = "inventario"
            }
            else -> onVolver()
        }
    }

    // Maneja el botón Atrás del dispositivo.
    BackHandler(enabled = pantalla != "inventario") {
        volver()
    }

    // Si se elimina un producto, regresamos al listado.
    LaunchedEffect(productos, productoId, pantalla) {
        if (pantalla != "inventario" &&
            productoSeleccionado == null
        ) {
            productoId = null
            pantalla = "inventario"
        }
    }

    when (pantalla) {

        // ---------- DETALLE ----------
        "detalle" -> {
            if (productoSeleccionado != null) {
                DetalleProductoAdminScreen(
                    producto = productoSeleccionado,
                    onVolver = { volver() },
                    onEditar = {
                        viewModel.limpiarError()
                        pantalla = "editar"
                    },
                    onEliminar = {
                        if (!eliminando) {
                            eliminando = true
                            val idEliminar = productoSeleccionado.id
                            scope.launch {
                                try {
                                    if (viewModel.eliminarProductoPersistente(idEliminar)) {
                                        productoId = null
                                        pantalla = "inventario"
                                    }
                                } finally {
                                    eliminando = false
                                }
                            }
                        }
                    },
                    onGenerarQr = {
                        pantalla = "qr"
                    }
                )
            }
        }

        // ---------- EDITAR ----------
        "editar" -> {
            if (productoSeleccionado != null) {
                EditarProductoAdmin(
                    producto = productoSeleccionado,
                    viewModel = viewModel,
                    onVolver = { volver() }
                )
            }
        }

        // ---------- CÓDIGO QR ----------
        "qr" -> {
            if (productoSeleccionado != null) {
                QrProductoScreen(
                    producto = productoSeleccionado,
                    onVolver = { volver() }
                )
            }
        }

        // ---------- INVENTARIO ----------
        else -> {
            InventarioListado(
                productos = productos,
                error = error,
                onProductoClick = { seleccionado ->
                    viewModel.limpiarError()
                    productoId = seleccionado.id
                    pantalla = "detalle"
                }
            )
        }
    }
}

// Pantalla principal con el listado de productos.
@Composable
private fun InventarioListado(
    productos: List<ProductoInfo>,
    error: String?,
    onProductoClick: (ProductoInfo) -> Unit
) {

    // Indicadores calculados desde el catálogo.
    val stockTotal = productos.sumOf { it.stock.toLong() }

    val productosConAlerta = productos.count {
        !it.disponible ||
                it.stock <= it.stockMinimo
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoInventario)
            .padding(horizontal = 16.dp)
    ) {

        Spacer(Modifier.height(20.dp))

        // ---------- ENCABEZADO ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Inventario",
                    color = OscuroInventario,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Control de existencias",
                    color = CafeInventario,
                    fontSize = 13.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Inventario",
                    tint = OscuroInventario,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- INDICADORES ----------
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IndicadorInventario(
                titulo = "Productos",
                valor = "${productos.size}",
                color = OscuroInventario,
                modifier = Modifier.weight(1f)
            )

            IndicadorInventario(
                titulo = "Unidades",
                valor = "$stockTotal",
                color = VerdeInventario,
                modifier = Modifier.weight(1f)
            )

            IndicadorInventario(
                titulo = "Alertas",
                valor = "$productosConAlerta",
                color = NaranjaInventario,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Productos registrados",
            color = OscuroInventario,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Selecciona un producto para consultar sus detalles.",
            color = CafeInventario,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(14.dp))

        if (error != null) {
            Text(
                text = error,
                color = RojoInventario,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(10.dp))
        }

        // ---------- LISTADO VERTICAL ----------
        if (productos.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay productos registrados.",
                    color = CafeInventario
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(
                    items = productos,
                    key = { it.id }
                ) { producto ->

                    TarjetaInventario(
                        producto = producto,
                        onVerDetalle = {
                            onProductoClick(producto)
                        }
                    )
                }
            }
        }
    }
}

// Indicador superior del inventario.
@Composable
private fun IndicadorInventario(
    titulo: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = titulo,
                color = CafeInventario,
                fontSize = 11.sp
            )

            Text(
                text = valor,
                color = color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Tarjeta individual de cada producto.
@Composable
private fun TarjetaInventario(
    producto: ProductoInfo,
    onVerDetalle: () -> Unit
) {

    // Determinamos el estado del inventario.
    val estado = when {
        !producto.disponible -> "Inactivo"
        producto.stock <= 0 -> "Sin stock"
        producto.stock <= producto.stockMinimo -> "Stock bajo"
        else -> "Disponible"
    }

    val colorEstado = when {
        !producto.disponible -> RojoInventario
        producto.stock <= 0 -> RojoInventario
        producto.stock <= producto.stockMinimo -> NaranjaInventario
        else -> VerdeInventario
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ---------- INFORMACIÓN PRINCIPAL ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Imagen del producto.
                ImagenProducto(
                    producto = producto,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = producto.nombre,
                        color = OscuroInventario,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = producto.codigo,
                        color = CafeInventario,
                        fontSize = 12.sp
                    )

                    Text(
                        text = producto.presentacion,
                        color = CafeInventario,
                        fontSize = 12.sp
                    )

                    // Cantidad disponible.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "${producto.stock}",
                            color = OscuroInventario,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "unidades",
                            color = CafeInventario,
                            fontSize = 12.sp
                        )
                    }

                    // Estado del producto.
                    Surface(
                        color = colorEstado.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = estado,
                            color = colorEstado,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                        )
                    }
                }
            }

            Divider(
                color = FondoInventario
            )

            // ---------- ACCIONES ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column {
                    Text(
                        text = "Stock mínimo",
                        color = CafeInventario,
                        fontSize = 11.sp
                    )

                    Text(
                        text = "${producto.stockMinimo} unidades",
                        color = OscuroInventario,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onVerDetalle,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OscuroInventario
                    )
                ) {
                    Text(
                        text = "Ver detalle",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
