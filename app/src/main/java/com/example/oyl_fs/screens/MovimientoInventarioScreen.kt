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
    onRevisarInventario: () -> Unit,
    onConsultar: () -> Unit = {},
    onContinuar: () -> Unit = {},
    onCerrarResultado: () -> Unit = {}
) {
    var tipoNombre by rememberSaveable(producto.id, estado.pendiente?.clave) {
        mutableStateOf(if ((estado.pendiente?.cambio ?: 1) > 0) TipoMovimientoStock.ENTRADA.name else TipoMovimientoStock.SALIDA.name)
    }
    var cantidad by rememberSaveable(producto.id, estado.pendiente?.clave) {
        mutableStateOf(estado.pendiente?.cambio?.let { kotlin.math.abs(it).toString() }.orEmpty())
    }
    var errorCantidad by rememberSaveable(producto.id) { mutableStateOf<String?>(null) }
    var confirmar by rememberSaveable(producto.id) { mutableStateOf(false) }
    val tipo = TipoMovimientoStock.valueOf(tipoNombre)
    val otroProducto = estado.productoId != null && estado.productoId != producto.id
    val bloqueado = otroProducto || estado.procesando || estado.pendiente != null || !estado.registroLegible

    BackHandler(enabled = estado.procesando) { /* Esperar el resultado del envío. */ }
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF5E7))
            .imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoOYL("Movimiento de inventario", { if (!estado.procesando) onVolver() })
        Text(producto.nombre, style = MaterialTheme.typography.headlineSmall)
        Text("Código: ${producto.codigo}")
        if (estado.pendiente == null) Text("Stock publicado: ${producto.stock} unidades")
        else Text("Producto del movimiento guardado. Consulta el inventario para conocer sus existencias actuales.")

        if (otroProducto) {
            Text("Hay un movimiento de otro producto. Espera su resultado y vuelve a seleccionar este producto.")
            Button(onClick = onVolver, enabled = !estado.procesando) { Text("Volver al producto") }
        } else if (estado.pendiente?.finalizado == true) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (estado.pendiente.estado == "APLICADA") "Movimiento registrado" else "Movimiento rechazado", style = MaterialTheme.typography.titleLarge)
                    Text("${if (estado.pendiente.cambio > 0) "Entrada" else "Salida"}: ${kotlin.math.abs(estado.pendiente.cambio)} unidades.")
                    estado.pendiente.stockResultante?.let {
                        Text("Stock después de este movimiento: $it unidades. Este comprobante conserva el resultado original; las existencias actuales pueden haber cambiado.")
                    }
                    estado.error?.let { MensajeErrorOYL(it) }
                    estado.advertencia?.let { Text(it) }
                }
            }
            Button(onClick = onCerrarResultado, enabled = !estado.procesando, modifier = Modifier.fillMaxWidth()) { Text("Cerrar resultado") }
            TextButton(onClick = onRevisarInventario, enabled = !estado.procesando) { Text("Actualizar y revisar inventario") }
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
            Text("El movimiento se guarda en este teléfono antes de enviarlo. Un pendiente se recupera con la misma cuenta y no se envía automáticamente.")
            estado.error?.let { MensajeErrorOYL(it) }
            if (estado.pendiente != null || !estado.registroLegible) {
                Button(onClick = onConsultar, enabled = !estado.procesando, modifier = Modifier.fillMaxWidth()) {
                    Text(if (estado.pendiente == null) "Reintentar lectura local" else "Consultar resultado")
                }
                if (estado.pendiente != null) OutlinedButton(onClick = { confirmar = true }, enabled = !estado.procesando, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar el mismo movimiento")
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
    if (confirmar && (!bloqueado || (estado.pendiente != null && !estado.pendiente.finalizado && !estado.procesando))) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("Confirmar ${tipo.titulo.lowercase()}") },
            text = {
                Text("${producto.nombre} (${producto.codigo})\n${tipo.titulo}: ${cantidad.trim()} unidades.\n\nEl cambio se enviará al servidor al confirmar.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmar = false
                    if (estado.pendiente != null) onContinuar() else onRegistrar(tipo, cantidad)
                }) {
                    Text(if (estado.pendiente == null) "Confirmar movimiento" else "Continuar el mismo movimiento")
                }
            },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } }
        )
    }
}
