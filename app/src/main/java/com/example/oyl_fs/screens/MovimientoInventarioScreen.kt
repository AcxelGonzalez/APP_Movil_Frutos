package com.example.oyl_fs.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.model.TipoMovimientoStock
import com.example.oyl_fs.model.ValidacionMovimientoStock
import com.example.oyl_fs.ui.theme.EncabezadoOYL
import com.example.oyl_fs.ui.theme.MensajeErrorOYL
import com.example.oyl_fs.viewmodel.MovimientoStockUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientoInventarioScreen(
    producto: ProductoInfo,
    estado: MovimientoStockUiState,
    onVolver: () -> Unit,
    onRegistrar: (TipoMovimientoStock, String) -> Unit,
    onRevisarInventario: () -> Unit
) {
    var tipoNombre by rememberSaveable(producto.id) { mutableStateOf(TipoMovimientoStock.ENTRADA.name) }
    var cantidad by rememberSaveable(producto.id) { mutableStateOf("") }
    var errorCantidad by rememberSaveable(producto.id) { mutableStateOf<String?>(null) }
    var confirmar by rememberSaveable(producto.id) { mutableStateOf(false) }
    val tipo = TipoMovimientoStock.valueOf(tipoNombre)
    val otroProducto = estado.productoId != null && estado.productoId != producto.id
    val bloqueado = otroProducto || estado.procesando || estado.confirmado != null || estado.resultadoIncierto

    BackHandler(enabled = estado.procesando) { /* Esperar el resultado del envío. */ }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF5E7))
            .imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoOYL("Movimiento de inventario", { if (!estado.procesando) onVolver() })
        Text(producto.nombre, style = MaterialTheme.typography.headlineSmall)
        Text("Código: ${producto.codigo}")
        Text("Stock publicado: ${producto.stock} unidades")

        if (otroProducto) {
            Text("Hay un movimiento de otro producto. Espera su resultado y vuelve a seleccionar este producto.")
            Button(onClick = onVolver, enabled = !estado.procesando) { Text("Volver al producto") }
        } else if (estado.confirmado != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Movimiento registrado", style = MaterialTheme.typography.titleLarge)
                    Text("Stock confirmado por el servidor: ${estado.confirmado.stock} unidades.")
                    estado.advertencia?.let { Text(it) }
                }
            }
            Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al producto") }
        } else {
            Text("Tipo de movimiento", style = MaterialTheme.typography.titleMedium)
            TipoMovimientoStock.values().forEach { opcion ->
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = tipo == opcion, enabled = !bloqueado, role = Role.RadioButton,
                        onClick = { tipoNombre = opcion.name }
                    ), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(selected = tipo == opcion, onClick = null, enabled = !bloqueado)
                    Text(if (opcion == TipoMovimientoStock.ENTRADA) "Entrada: agregar unidades" else "Salida: retirar unidades")
                }
            }
            OutlinedTextField(
                value = cantidad,
                onValueChange = { cantidad = it; errorCantidad = null },
                label = { Text("Cantidad de unidades") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, enabled = !bloqueado, isError = errorCantidad != null,
                supportingText = { Text(errorCantidad ?: "Ingresa un número entero mayor que cero.") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("El servidor comprobará las existencias al confirmar. No se guardan movimientos pendientes sin conexión.")
            estado.error?.let { MensajeErrorOYL(it) }
            if (estado.resultadoIncierto) {
                Button(onClick = onRevisarInventario, modifier = Modifier.fillMaxWidth()) {
                    Text("Actualizar y revisar inventario")
                }
            } else {
                Button(
                    onClick = {
                        errorCantidad = ValidacionMovimientoStock.errorCantidad(cantidad)
                        if (errorCantidad == null) confirmar = true
                    }, enabled = !bloqueado, modifier = Modifier.fillMaxWidth()
                ) { Text(if (estado.procesando) "Registrando…" else "Revisar movimiento") }
            }
            if (estado.procesando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Espera la respuesta antes de registrar otro movimiento.")
            }
        }
    }
    if (confirmar && !bloqueado) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Confirmar ${tipo.titulo.lowercase()}") },
            text = {
                Text("${producto.nombre} (${producto.codigo})\n${tipo.titulo}: ${cantidad.trim()} unidades.\n\nEl cambio se enviará al servidor al confirmar.")
            },
            confirmButton = {
                TextButton(onClick = { confirmar = false; onRegistrar(tipo, cantidad) }) {
                    Text("Confirmar movimiento")
                }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }
}
