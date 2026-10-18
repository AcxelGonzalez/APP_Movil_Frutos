package com.example.oyl_fs.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.util.escanearQr
import com.example.oyl_fs.viewmodel.ProductoViewModel
import com.example.oyl_fs.util.leerIdQrProducto

private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)
private val CriticoOYL = Color(0xFFC0392B)

@Composable
fun EscanerScreen(
    viewModel: ProductoViewModel,
    onVolver: () -> Unit
) {
    val contexto = LocalContext.current
    val productos by viewModel.productos.collectAsState()

    var codigoLeido by remember { mutableStateOf<String?>(null) }

    var encontrado by remember { mutableStateOf<ProductoInfo?>(null) }

    var mensaje by remember { mutableStateOf("Toca el botón y apunta la cámara a un código QR.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOYL)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ---------- Encabezado ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = SecundarioOYL)
            }
            Text(
                "Escanear producto",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SecundarioOYL
            )
        }

        Text(mensaje, color = PrincipalOYL, fontSize = 13.sp)

        // boton escanear
        Button(
            onClick = {
                escanearQr(
                    context = contexto,
                    alLeer = { texto ->
                        codigoLeido = texto

                        // Leer el ID que contiene el QR generado por OYL.
                        val productoId = leerIdQrProducto(texto)

                        // Buscar ese producto en el catálogo actualizado.
                        encontrado = productos.find { producto ->
                            producto.id == productoId
                        }

                        mensaje = if (encontrado != null) {
                            "Producto encontrado."
                        } else {
                            "Ese código no corresponde a ningún producto."
                        }
                    },
                    alFallar = { error ->
                        mensaje = error
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = SecundarioOYL),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Escanear código QR")
        }

        // resultado
        val codigo = codigoLeido
        if (codigo != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {

                    Text("Código leído", fontSize = 12.sp, color = PrincipalOYL)
                    Text(codigo, fontWeight = FontWeight.Bold, color = SecundarioOYL)

                    Spacer(Modifier.height(12.dp))
                    Divider(color = FondoOYL)
                    Spacer(Modifier.height(12.dp))

                    val producto = encontrado
                    if (producto != null) {
                        Text(producto.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SecundarioOYL)
                        Text(producto.categoria, fontSize = 13.sp, color = PrincipalOYL)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$" + "%,d".format(producto.precio).replace(',', '.'),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecundarioOYL
                        )
                        Text("Stock: ${producto.stock}", fontSize = 13.sp, color = PrincipalOYL)
                    } else {
                        Text(
                            "No hay ningún producto con ese código.",
                            color = CriticoOYL,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}