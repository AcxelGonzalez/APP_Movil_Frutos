
package com.example.oyl_fs.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.util.escanearQr
import com.example.oyl_fs.viewmodel.PrepararPedidoViewModel

private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)

@Composable
fun PrepararPedidoScreen(
    pedido: Pedido,
    viewModel: PrepararPedidoViewModel,
    onVolver: () -> Unit,
    onConfirmarPreparacion: (String) -> Unit
) {
    val contexto = LocalContext.current


    val cantidades by viewModel.cantidadesEscaneadas.collectAsState()

    val productosPreparacion by viewModel
        .productosPreparacion
        .collectAsState()

    val pedidoActual by viewModel.pedido.collectAsState()

    val mensaje by viewModel.mensaje.collectAsState()


    var escaneando by remember { mutableStateOf(false) }

    // Cargar el pedido seleccionado.
    LaunchedEffect(pedido) {
        viewModel.cargarPedido(pedido)
    }


    /**
     * Verificar que la pantalla esté utilizando
     * el pedido correcto y que esté confirmado.
     */
    val pedidoListo =
        pedidoActual == pedido &&
                pedido.estado == EstadoPedido.CONFIRMADO

    /**
     * Comprobar todas las cantidades agrupadas.
     */
    val completo =
        pedidoListo &&
                productosPreparacion.isNotEmpty() &&
                productosPreparacion.all { producto ->

                    val verificadas =
                        cantidades[producto.productoId] ?: 0

                    verificadas == producto.cantidad
                }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOYL)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ---------- Encabezado ----------
        Row {
            IconButton(onClick = onVolver) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = SecundarioOYL
                )
            }

            Column {
                Text(
                    "Preparar pedido",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecundarioOYL
                )

                Text(
                    pedido.id,
                    color = PrincipalOYL
                )
            }
        }

        Text(
            "Escanea cada producto hasta completar las cantidades solicitadas.",
            color = PrincipalOYL
        )

        // ---------- Productos del pedido ----------
        productosPreparacion.forEach { producto ->

            val verificadas = cantidades[producto.productoId] ?: 0
            val completado = verificadas == producto.cantidad

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        producto.nombreProducto,
                        fontWeight = FontWeight.Bold,
                        color = SecundarioOYL
                    )

                    Text(
                        "Producto ID: ${producto.productoId}",
                        color = PrincipalOYL
                    )

                    Text(
                        "Verificadas: $verificadas / ${producto.cantidad}",
                        color = if (completado) {
                            Color(0xFF238636)
                        } else {
                            SecundarioOYL
                        }
                    )

                    LinearProgressIndicator(
                        progress = if (producto.cantidad > 0) {
                            verificadas.toFloat() / producto.cantidad
                        } else {
                            0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF238636)
                    )
                }
            }
        }

        // ---------- Escanear QR ----------
        Button(
            onClick = {
                if (!escaneando) {
                    escaneando = true

                    escanearQr(
                        context = contexto,
                        alLeer = { texto ->
                            escaneando = false
                            viewModel.validarQr(texto)
                        },

                        alFallar = { motivo ->

                            escaneando = false

                            viewModel.informarFalloEscaneo(
                                motivo
                            )
                        }

                    )
                }
            },
            enabled = pedidoListo && !completo && !escaneando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SecundarioOYL
            )
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Escanear producto")
        }

        if (mensaje.isNotBlank()) {
            Text(
                mensaje,
                color = PrincipalOYL
            )
        }

        // ---------- Confirmar ----------
        Button(
            onClick = {
                if (viewModel.pedidoCompleto()) {
                    onConfirmarPreparacion(pedido.id)
                }
            },
            enabled = completo,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF238636)
            )
        ) {
            Text("Confirmar pedido preparado")
        }
    }
}
