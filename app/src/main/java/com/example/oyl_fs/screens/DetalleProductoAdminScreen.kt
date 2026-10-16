package com.example.oyl_fs.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity

import com.example.oyl_fs.model.ProductoInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FondoProductoAdmin = Color(0xFFFFF5E7)
private val CafeProductoAdmin = Color(0xFF92623D)
private val OscuroProductoAdmin = Color(0xFF4B2E15)
private val RojoProductoAdmin = Color(0xFFC0392B)

@Composable
fun DetalleProductoAdminScreen(
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    producto: ProductoInfo,
    onGenerarQr: () -> Unit
) {
    val entrada = remember(producto.id) { Animatable(0f) }
    val desplazamiento = with(LocalDensity.current) { 18.dp.toPx() }
    LaunchedEffect(producto.id) {
        withFrameNanos { }
        entrada.animateTo(1f, tween(650))
    }

    var confirmarEliminar by remember(producto.id) {
        mutableStateOf(false)
    }

    val precioTexto = "$" + "%,d"
        .format(producto.precio)
        .replace(',', '.')

    val estado = when {
        !producto.disponible -> "Inactivo"
        producto.stock <= 0 -> "Sin stock"
        else -> "En stock"
    }

    val colorEstado = if (producto.disponible && producto.stock > 0) {
        Color(0xFF2E7D32)
    } else {
        RojoProductoAdmin
    }

    Column(
        modifier = Modifier.graphicsLayer {
                alpha = entrada.value
                translationY = (1f - entrada.value) * desplazamiento
            }
            .fillMaxSize()
            .background(FondoProductoAdmin)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onVolver) {
                Text(
                    text = "‹",
                    color = OscuroProductoAdmin,
                    fontSize = 30.sp
                )
            }

            Text(
                text = "Detalle del producto",
                color = OscuroProductoAdmin,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // Muestra una imagen de drawable o del teléfono.
        ImagenProducto(
            producto = producto,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(18.dp))
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = producto.nombre,
                    color = OscuroProductoAdmin,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Código: ${producto.codigo}",
                    color = CafeProductoAdmin
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = producto.presentacion,
                        color = CafeProductoAdmin,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = estado,
                        color = colorEstado,
                        modifier = Modifier
                            .background(
                                colorEstado.copy(alpha = 0.1f),
                                RoundedCornerShape(50)
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 4.dp
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = precioTexto,
                        color = OscuroProductoAdmin,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${producto.stock} uds",
                        color = CafeProductoAdmin
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onGenerarQr,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OscuroProductoAdmin)
        ) { Text("Generar QR del producto") }

        SeccionProductoAdmin(
            titulo = "Categoría",
            contenido = producto.categoria
        )

        SeccionProductoAdmin(
            titulo = "Descripción",
            contenido = producto.descripcion
        )

        // Resumen de stock y precio.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DatoProductoAdmin(
                titulo = "Stock actual",
                valor = "${producto.stock} uds",
                modifier = Modifier.weight(1f)
            )

            DatoProductoAdmin(
                titulo = "Stock mínimo",
                valor = "${producto.stockMinimo} uds",
                modifier = Modifier.weight(1f)
            )

            DatoProductoAdmin(
                titulo = "Precio",
                valor = precioTexto,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onEditar,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OscuroProductoAdmin
                )
            ) {
                Text("Editar")
            }

            OutlinedButton(
                onClick = { confirmarEliminar = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RojoProductoAdmin
                )
            ) {
                Text("Eliminar")
            }
        }
    }

    // Ejecuta la acción de eliminar después de confirmar.
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = FondoProductoAdmin,
            title = {
                Text("¿Eliminar producto?")
            },
            text = {
                Text("¿Deseas eliminar ${producto.nombre}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarEliminar = false
                        onEliminar()
                    }
                ) {
                    Text("Eliminar", color = RojoProductoAdmin)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmarEliminar = false }
                ) {
                    Text("Cancelar", color = OscuroProductoAdmin)
                }
            }
        )
    }
}

@Composable
private fun SeccionProductoAdmin(
    titulo: String,
    contenido: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titulo,
                color = OscuroProductoAdmin,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = contenido,
                color = CafeProductoAdmin
            )
        }
    }
}

@Composable
private fun DatoProductoAdmin(
    titulo: String,
    valor: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titulo,
                color = CafeProductoAdmin,
                fontSize = 12.sp
            )

            Text(
                text = valor,
                color = OscuroProductoAdmin,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Datos de ejemplo únicamente para revisar el diseño.
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 1100
)
@Composable
private fun DetalleProductoAdminPreview() {
    MaterialTheme {
        DetalleProductoAdminScreen(
            producto = ProductosDatos.almendras,
            onVolver = {},
            onEditar = {},
            onEliminar = {},
            onGenerarQr = {}
        )
    }
}