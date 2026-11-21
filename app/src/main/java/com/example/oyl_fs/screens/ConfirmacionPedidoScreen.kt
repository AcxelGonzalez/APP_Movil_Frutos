package com.example.oyl_fs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.oyl_fs.model.*
import com.example.oyl_fs.repository.PuntoRetiroRepository
import com.example.oyl_fs.ui.components.EstadoPedidoChip
import java.text.NumberFormat
import java.util.Locale

private val FondoConfirmacion = Color(0xFFFFF5E7)

internal fun precioPedido(valor: Long): String = "$" + NumberFormat.getIntegerInstance(Locale("es", "CL")).format(valor)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmacionPedidoScreen(
    items: List<ItemCarrito>, perfil: PerfilUsuario?, procesando: Boolean,
    error: String?, onVolver: () -> Unit, onConfirmar: (EntregaPedido) -> Unit
) {
    var modalidadNombre by rememberSaveable { mutableStateOf(ModalidadEntrega.RETIRO.name) }
    val modalidad = ModalidadEntrega.valueOf(modalidadNombre)
    var nombre by rememberSaveable { mutableStateOf(listOfNotNull(perfil?.nombre, perfil?.apellido).joinToString(" ").trim()) }
    var telefono by rememberSaveable { mutableStateOf(perfil?.telefono.orEmpty()) }
    var direccion by rememberSaveable { mutableStateOf(perfil?.direccion.orEmpty()) }
    var puntoId by rememberSaveable { mutableStateOf("") }
    var errores by remember { mutableStateOf(ErroresEntrega()) }
    val puntos = PuntoRetiroRepository.obtenerPuntos()
    val datos = EntregaPedido(modalidad, nombre, telefono, direccion, puntoId)
    Column(Modifier.fillMaxSize().background(FondoConfirmacion).imePadding()
        .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver, enabled = !procesando) { Text("Volver al carrito") }
        Text("Confirmar pedido", style = MaterialTheme.typography.headlineSmall)
        Text("Revisa los artículos y utiliza datos ficticios para la demostración.")
        items.forEach { item ->
            Text("${item.nombre} × ${item.cantidad}", fontWeight = FontWeight.Bold)
            Text("${precioPedido(item.precio.toLong())} c/u · ${precioPedido(item.subtotal)}")
        }
        Text("Total de productos: ${precioPedido(items.sumOf { it.subtotal })}", fontWeight = FontWeight.Bold)
        Divider()
        Text("Modalidad", fontWeight = FontWeight.Bold)
        ModalidadEntrega.values().forEach { opcion ->
            Row(Modifier.fillMaxWidth().selectable(selected = modalidad == opcion, enabled = !procesando,
                role = Role.RadioButton, onClick = { modalidadNombre = opcion.name; errores = ErroresEntrega() }),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = modalidad == opcion, onClick = null, enabled = !procesando)
                Text(opcion.descripcion)
            }
        }
        OutlinedTextField(value = nombre, onValueChange = { nombre = it; errores = errores.copy(nombre = null) },
            label = { Text("Nombre de contacto") }, enabled = !procesando, singleLine = true,
            isError = errores.nombre != null, supportingText = errores.nombre?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = telefono, onValueChange = { telefono = it; errores = errores.copy(telefono = null) },
            label = { Text("Teléfono de contacto") }, enabled = !procesando, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = errores.telefono != null, supportingText = errores.telefono?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth())
        if (modalidad == ModalidadEntrega.RETIRO) {
            Text("Selecciona dónde retirar", fontWeight = FontWeight.Bold)
            puntos.forEach { punto ->
                Row(Modifier.fillMaxWidth().selectable(selected = puntoId == punto.id, enabled = !procesando,
                    role = Role.RadioButton, onClick = { puntoId = punto.id; errores = errores.copy(puntoRetiro = null) }),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = puntoId == punto.id, onClick = null, enabled = !procesando)
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(punto.nombre)
                        Text(punto.direccion, style = MaterialTheme.typography.bodySmall)
                        Text(punto.horario, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            errores.puntoRetiro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            OutlinedTextField(value = direccion, onValueChange = { direccion = it; errores = errores.copy(direccion = null) },
                label = { Text("Dirección de entrega") }, enabled = !procesando,
                isError = errores.direccion != null, supportingText = errores.direccion?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth())
            Text("Entrega de demostración. No se calculan tarifas ni se procesa un pago.", style = MaterialTheme.typography.bodySmall)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (items.any { !it.stockSuficiente }) Text("Hay productos sin stock suficiente. Vuelve al carrito para corregirlos.", color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            errores = ValidacionEntregaPedido.validar(datos, puntos)
            if (!errores.hayErrores()) onConfirmar(ValidacionEntregaPedido.normalizar(datos, puntos))
        }, enabled = !procesando && items.isNotEmpty() && items.all { it.stockSuficiente }, modifier = Modifier.fillMaxWidth()) {
            Text(if (procesando) "Registrando pedido…" else "Finalizar pedido")
        }
    }
}

@Composable
fun DatosEntregaResumen(entrega: EntregaPedido?) {
    if (entrega == null) {
        Text("Este pedido no tiene datos de entrega guardados en este dispositivo.")
    } else {
        Text("Datos de entrega guardados en este dispositivo.", style = MaterialTheme.typography.bodySmall)
        Text("Modalidad: ${entrega.modalidad.descripcion}", fontWeight = FontWeight.Bold)
        Text("Contacto: ${entrega.nombreContacto}")
        Text("Teléfono: ${entrega.telefonoContacto}")
        if (entrega.modalidad == ModalidadEntrega.RETIRO) Text("Punto de retiro: ${entrega.puntoRetiroNombre}")
        Text("Dirección: ${entrega.direccion}")
    }
}

@Composable
fun PedidoConfirmadoScreen(pedido: Pedido, advertencia: String? = null, onMisPedidos: () -> Unit) {
    Column(Modifier.fillMaxSize().background(FondoConfirmacion).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(pedido.id) { visible = true }
        AnimatedVisibility(visible, enter = fadeIn()) {
            Text("Pedido registrado", style = MaterialTheme.typography.headlineSmall)
        }
        Text("Número de solicitud: ${pedido.id}")
        EstadoPedidoChip(pedido.estado)
        pedido.productos.forEach { Text("${it.nombreProducto} × ${it.cantidad} · ${precioPedido(it.subtotal.toLong())}") }
        Text("Total: ${precioPedido(pedido.total.toLong())}", fontWeight = FontWeight.Bold)
        Divider()
        DatosEntregaResumen(pedido.entrega)
        advertencia?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Tu solicitud está pendiente de validación. Esta demostración no realiza cobros.")
        Button(onClick = onMisPedidos, modifier = Modifier.fillMaxWidth()) { Text("Ver mis pedidos") }
    }
}
