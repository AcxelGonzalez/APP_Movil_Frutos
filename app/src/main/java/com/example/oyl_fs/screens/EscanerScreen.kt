package com.example.oyl_fs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.util.LanzadorQr
import com.example.oyl_fs.util.escanearQr
import com.example.oyl_fs.util.leerIdQrProducto
import com.example.oyl_fs.viewmodel.ProductoViewModel

private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)
private val CriticoOYL = Color(0xFFC0392B)

@Composable
fun EscanerScreen(
    viewModel: ProductoViewModel,
    esAdmin: Boolean,
    onVolver: () -> Unit,
    onAbrirProducto: (Int) -> Unit,
    lanzarEscaner: LanzadorQr = ::escanearQr
) {
    val contexto = LocalContext.current
    val productos by viewModel.productos.collectAsState()
    val cargando by viewModel.cargandoProductos.collectAsState()
    val errorProducto by viewModel.error.collectAsState()
    val errorCatalogo = errorProducto?.takeIf { productos.isEmpty() }
    var codigoLeido by rememberSaveable { mutableStateOf<String?>(null) }
    var errorEscaneo by rememberSaveable { mutableStateOf<String?>(null) }
    var escaneando by remember { mutableStateOf(false) }
    var activa by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { activa = false } }

    // Resolver contra el catálogo actual evita mostrar una copia desactualizada.
    val productoId = codigoLeido?.let(::leerIdQrProducto)
    val encontrado = productos.find { it.id == productoId }
    val mensaje = when {
        cargando -> "Cargando catálogo…"
        errorCatalogo != null -> errorCatalogo.orEmpty()
        errorEscaneo != null -> errorEscaneo.orEmpty()
        codigoLeido == null -> "Toca el botón y apunta la cámara al QR de un producto OYL."
        productoId == null -> "El código QR no es un producto OYL válido."
        encontrado == null -> "El producto no existe en el catálogo actual."
        !encontrado.disponible && !esAdmin -> "Este producto no está disponible para la venta."
        else -> "Producto encontrado."
    }

    Column(
        Modifier.fillMaxSize().background(FondoOYL)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = SecundarioOYL)
            }
            Text("Escanear producto", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SecundarioOYL)
        }
        Text(mensaje, color = if (errorEscaneo != null || (codigoLeido != null && encontrado == null)) CriticoOYL else PrincipalOYL)
        Button(
            enabled = !escaneando && !cargando && errorCatalogo == null,
            onClick = {
                if (!escaneando) {
                    escaneando = true
                    codigoLeido = null
                    errorEscaneo = null
                    try {
                        lanzarEscaner(contexto, { texto ->
                            if (activa) {
                                escaneando = false
                                codigoLeido = texto
                            }
                        }, { error ->
                            if (activa) {
                                escaneando = false
                                errorEscaneo = error.ifBlank { "No se pudo completar el escaneo." }
                            }
                        })
                    } catch (_: Exception) {
                        escaneando = false
                        errorEscaneo = "No se pudo abrir el escáner. Inténtalo nuevamente."
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SecundarioOYL),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, null)
            Spacer(Modifier.width(8.dp))
            Text(if (escaneando) "Abriendo cámara…" else "Escanear código QR")
        }
        AnimatedVisibility(visible = encontrado != null && !cargando && errorCatalogo == null,
            enter = fadeIn() + slideInVertically { it / 4 }) {
            encontrado?.let { producto ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.CheckCircle, "Lectura correcta", tint = Color(0xFF2E7D32))
                        Text(producto.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SecundarioOYL)
                        Text(producto.categoria, color = PrincipalOYL)
                        Text("$" + "%,d".format(producto.precio).replace(',', '.'), color = SecundarioOYL)
                        Text("Stock: ${producto.stock}", color = PrincipalOYL)
                        if (!producto.disponible) Text("Producto inactivo", color = CriticoOYL)
                        Button(
                            enabled = esAdmin || producto.disponible,
                            onClick = { onAbrirProducto(producto.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SecundarioOYL)
                        ) { Text(if (esAdmin) "Ajustar inventario" else "Ver producto") }
                    }
                }
            }
        }
    }
}
