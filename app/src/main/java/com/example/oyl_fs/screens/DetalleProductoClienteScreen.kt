package com.example.oyl_fs.screens

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.model.ProductoInfo

@Composable
fun DetalleProductoClienteScreen(
    producto: ProductoInfo,
    disponibleParaAgregar: Int,
    onVolver: () -> Unit,
    onAgregar: (Int) -> Boolean,
    onAbrirCarrito: () -> Unit
) {
    val entrada = remember(producto.id) { Animatable(0f) }
    val desplazamiento = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(producto.id) {
        withFrameNanos { }
        entrada.animateTo(1f, tween(650))
    }

    val fondo = Color(0xFFFFF5E7)
    val cafe = Color(0xFF92623D)
    val oscuro = Color(0xFF4B2E15)
    var cantidad by rememberSaveable(producto.id) { mutableStateOf(1) }
    var cantidadAgregada by rememberSaveable(producto.id) { mutableStateOf(0) }
    var agregado by rememberSaveable(producto.id) { mutableStateOf(false) }
    val limite = if (producto.disponible) disponibleParaAgregar.coerceAtLeast(0) else 0
    val cantidadActual = cantidad.coerceIn(1, limite.coerceAtLeast(1))

    Column(
        modifier = Modifier.graphicsLayer {
                alpha = entrada.value
                translationY = (1f - entrada.value) * desplazamiento
            }.fillMaxSize().background(fondo)
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver) { Text("‹", color = oscuro, fontSize = 28.sp) }
            Text("Detalle del producto", color = oscuro, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        ImagenProducto(producto, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)))
        Text(producto.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = oscuro)
        Text(producto.presentacion, color = cafe)
        Text("$" + "%,d".format(producto.precio).replace(',', '.'),
            color = oscuro, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Divider(color = cafe.copy(alpha = 0.15f))
        Text("Descripción", fontWeight = FontWeight.Bold, color = oscuro)
        Text(producto.descripcion, color = cafe)
        Text("Formato", fontWeight = FontWeight.Bold, color = oscuro)
        // Se muestra la presentación real; no se inventan otros tamaños ni precios.
        Surface(color = oscuro, shape = RoundedCornerShape(12.dp)) {
            Text(producto.presentacion, color = Color.White, modifier = Modifier.padding(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Cantidad", color = oscuro, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { cantidad = cantidadActual - 1 }, enabled = cantidadActual > 1) { Text("−") }
            Text(if (limite > 0) "$cantidadActual" else "0", color = oscuro)
            TextButton(onClick = { cantidad = cantidadActual + 1 }, enabled = cantidadActual < limite) { Text("+") }
        }
        Text(
            text = when {
                !producto.disponible -> "Producto no disponible"
                producto.stock <= 0 -> "Sin stock"
                limite == 0 -> "Ya agregaste al carrito las unidades disponibles"
                else -> "Puedes agregar hasta $limite unidades"
            }, color = cafe, fontSize = 12.sp
        )
        Button(
            onClick = {
                if (onAgregar(cantidadActual)) {
                    cantidadAgregada = cantidadActual
                    agregado = true
                }
            },
            enabled = limite > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = oscuro)
        ) { Text("Agregar al carrito") }
        OutlinedButton(onClick = onAbrirCarrito, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = oscuro)) { Text("Ver carrito") }
    }
    if (agregado) {
        AvisoProductoAgregado(
            mensaje = "${producto.nombre} · $cantidadAgregada ${if (cantidadAgregada == 1) "unidad" else "unidades"} en tu carrito.",
            onCerrar = { agregado = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetalleClientePreview() {
    MaterialTheme {
        DetalleProductoClienteScreen(ProductosDatos.almendras, 10, {}, { false }, {})
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
