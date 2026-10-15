package com.example.oyl_fs.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FondoAlmendra = Color(0xFFFFF5E7)
private val CafeAlmendra = Color(0xFF92623D)
private val OscuroAlmendra = Color(0xFF4B2E15)
private val RojoAlmendra = Color(0xFFC0392B)

@Composable
fun AlmendraScreen(
    onVolver: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    producto: ProductoInfo = ProductosDatos.almendras
) {
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
        RojoAlmendra
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoAlmendra)
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
                    color = OscuroAlmendra,
                    fontSize = 30.sp
                )
            }

            Text(
                text = "Detalle del producto",
                color = OscuroAlmendra,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        // Fotografía de las almendras.
        Image(
            painter = painterResource(producto.imagen),
            contentDescription = producto.nombre,
            contentScale = ContentScale.Crop,
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
                    color = OscuroAlmendra,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Código: ${producto.codigo}",
                    color = CafeAlmendra
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = producto.presentacion,
                        color = CafeAlmendra,
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
                        color = OscuroAlmendra,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${producto.stock} uds",
                        color = CafeAlmendra
                    )
                }
            }
        }

        SeccionAlmendra(
            titulo = "Categoría",
            contenido = producto.categoria
        )

        SeccionAlmendra(
            titulo = "Descripción",
            contenido = producto.descripcion
        )

        // Resumen de stock y precio.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DatoAlmendra(
                titulo = "Stock actual",
                valor = "${producto.stock} uds",
                modifier = Modifier.weight(1f)
            )

            DatoAlmendra(
                titulo = "Stock mínimo",
                valor = "${producto.stockMinimo} uds",
                modifier = Modifier.weight(1f)
            )

            DatoAlmendra(
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
                    contentColor = OscuroAlmendra
                )
            ) {
                Text("Editar")
            }

            OutlinedButton(
                onClick = { confirmarEliminar = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RojoAlmendra
                )
            ) {
                Text("Eliminar")
            }
        }
    }

    // Solicita confirmación antes de eliminar.
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = FondoAlmendra,
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
                    Text("Eliminar", color = RojoAlmendra)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmarEliminar = false }
                ) {
                    Text("Cancelar", color = OscuroAlmendra)
                }
            }
        )
    }
}

@Composable
private fun SeccionAlmendra(
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
                color = OscuroAlmendra,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = contenido,
                color = CafeAlmendra
            )
        }
    }
}

@Composable
private fun DatoAlmendra(
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
                color = CafeAlmendra,
                fontSize = 12.sp
            )

            Text(
                text = valor,
                color = OscuroAlmendra,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Solo para revisar el diseño en Android Studio.
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 1100
)
@Composable
private fun AlmendraPreview() {
    MaterialTheme {
        AlmendraScreen(
            onVolver = {},
            onEditar = {},
            onEliminar = {}
        )
    }
}