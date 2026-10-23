
package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.util.leerIdQrProducto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Controla la verificación QR de los productos
 * que componen un pedido.
 *
 * Agrupa los detalles repetidos por productoId,
 * impide exceder las cantidades solicitadas y
 * permite completar solo pedidos CONFIRMADOS.
 */
class PrepararPedidoViewModel : ViewModel() {

    private val _pedido = MutableStateFlow<Pedido?>(null)
    val pedido: StateFlow<Pedido?> = _pedido.asStateFlow()

    // Cada producto aparece una sola vez,
    // aunque tenga varios detalles en el pedido.
    private val _productosPreparacion =
        MutableStateFlow<List<DetallePedido>>(emptyList())

    val productosPreparacion:
            StateFlow<List<DetallePedido>> =
        _productosPreparacion.asStateFlow()

    // Cantidad de unidades verificadas por producto.
    private val _cantidadesEscaneadas =
        MutableStateFlow<Map<String, Int>>(emptyMap())

    val cantidadesEscaneadas:
            StateFlow<Map<String, Int>> =
        _cantidadesEscaneadas.asStateFlow()

    private val _mensaje = MutableStateFlow("")
    val mensaje: StateFlow<String> = _mensaje.asStateFlow()

    /**
     * Carga el pedido seleccionado.
     *
     * Normaliza los IDs y agrupa los detalles
     * que corresponden al mismo producto.
     *
     * Si cambia la información del pedido,
     * reinicia la verificación anterior.
     */
    fun cargarPedido(pedidoSeleccionado: Pedido) {

        if (_pedido.value == pedidoSeleccionado) return

        _pedido.value = pedidoSeleccionado
        _cantidadesEscaneadas.value = emptyMap()

        val detalles = pedidoSeleccionado.productos

        // Validar identificadores y cantidades.
        val normalizados = detalles.mapNotNull { detalle ->

            val id = detalle.productoId.toIntOrNull()

            if (
                id == null ||
                id <= 0 ||
                detalle.cantidad <= 0 ||
                detalle.nombreProducto.isBlank()
            ) {
                null
            } else {
                detalle.copy(productoId = id.toString())
            }
        }

        if (
            normalizados.isEmpty() ||
            normalizados.size != detalles.size
        ) {
            _productosPreparacion.value = emptyList()
            _mensaje.value =
                "El pedido contiene productos o cantidades inválidas."
            return
        }

        // Agrupar por ID y sumar las unidades.
        val agrupados = normalizados
            .groupBy { it.productoId }
            .values
            .map { lineas ->

                val total = lineas.sumOf {
                    it.cantidad.toLong()
                }

                // Evitar desbordamiento de Int.
                if (total > Int.MAX_VALUE.toLong()) {
                    null
                } else {
                    lineas.first().copy(
                        cantidad = total.toInt()
                    )
                }
            }

        if (agrupados.any { it == null }) {
            _productosPreparacion.value = emptyList()
            _mensaje.value =
                "Las cantidades del pedido exceden el límite permitido."
            return
        }

        _productosPreparacion.value =
            agrupados.filterNotNull()

        _mensaje.value =
            if (
                pedidoSeleccionado.estado ==
                EstadoPedido.CONFIRMADO
            ) {
                "Escanea los productos del pedido."
            } else {
                "Solo se pueden preparar pedidos confirmados."
            }
    }

    /**
     * Valida el contenido de un código QR.
     *
     * Cada lectura correcta representa
     * una unidad verificada del producto.
     */
    fun validarQr(textoQr: String) {

        val pedidoActual = _pedido.value

        if (pedidoActual == null) {
            _mensaje.value = "No hay un pedido seleccionado."
            return
        }

        if (
            pedidoActual.estado != EstadoPedido.CONFIRMADO
        ) {
            _mensaje.value =
                "Solo se pueden preparar pedidos confirmados."
            return
        }

        if (_productosPreparacion.value.isEmpty()) {
            _mensaje.value =
                "El pedido no contiene productos válidos."
            return
        }

        val productoId =
            leerIdQrProducto(textoQr)?.toString()

        if (productoId == null) {
            _mensaje.value = "El código QR no es válido."
            return
        }

        // Buscar en los productos ya agrupados.
        val producto = _productosPreparacion.value
            .find { it.productoId == productoId }

        if (producto == null) {
            _mensaje.value =
                "Este producto no pertenece al pedido."
            return
        }

        val cantidadActual =
            _cantidadesEscaneadas.value[productoId] ?: 0

        if (cantidadActual >= producto.cantidad) {
            _mensaje.value =
                "Ya se verificaron todas las unidades de ${producto.nombreProducto}."
            return
        }

        val nuevaCantidad = cantidadActual + 1

        _cantidadesEscaneadas.update { cantidades ->
            cantidades + (productoId to nuevaCantidad)
        }

        _mensaje.value =
            "${producto.nombreProducto}: $nuevaCantidad de ${producto.cantidad} verificadas."
    }

    /**
     * Comunica fallos del escáner a la interfaz.
     */
    fun informarFalloEscaneo(motivo: String) {
        _mensaje.value = motivo.ifBlank {
            "No se pudo completar el escaneo."
        }
    }

    /**
     * El pedido está completo solamente cuando:
     * - Tiene estado CONFIRMADO.
     * - Contiene productos válidos.
     * - Se verificó la cantidad total requerida
     *   de cada producto agrupado.
     */
    fun pedidoCompleto(): Boolean {

        if (
            _pedido.value?.estado !=
            EstadoPedido.CONFIRMADO
        ) {
            return false
        }

        val productos = _productosPreparacion.value

        if (productos.isEmpty()) return false

        return productos.all { producto ->

            val verificadas =
                _cantidadesEscaneadas.value[
                    producto.productoId
                ] ?: 0

            verificadas == producto.cantidad
        }
    }
}
