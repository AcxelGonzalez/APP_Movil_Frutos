
package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.util.leerIdQrProducto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Controla los productos verificados durante la preparación de un pedido.
class PrepararPedidoViewModel : ViewModel() {

    private val _pedido = MutableStateFlow<Pedido?>(null)
    val pedido: StateFlow<Pedido?> = _pedido.asStateFlow()

    // Guarda cuántas unidades se han escaneado por producto.
    // Ejemplo: "15" -> 2 unidades verificadas.
    private val _cantidadesEscaneadas =
        MutableStateFlow<Map<String, Int>>(emptyMap())

    val cantidadesEscaneadas: StateFlow<Map<String, Int>> =
        _cantidadesEscaneadas.asStateFlow()

    private val _mensaje = MutableStateFlow("")
    val mensaje: StateFlow<String> = _mensaje.asStateFlow()

    // Carga el pedido seleccionado por el administrador.
    fun cargarPedido(pedidoSeleccionado: Pedido) {
        if (_pedido.value?.id == pedidoSeleccionado.id) return

        _pedido.value = pedidoSeleccionado
        _cantidadesEscaneadas.value = emptyMap()
        _mensaje.value = "Escanea los productos del pedido."
    }

    // Recibe el texto obtenido desde la cámara.
    fun validarQr(textoQr: String) {

        val pedidoActual = _pedido.value

        if (pedidoActual == null) {
            _mensaje.value = "No hay un pedido seleccionado."
            return
        }

        // Utilizamos la misma interpretación QR que EscanerScreen.
        // Convertimos el ID a String porque DetallePedido
        // almacena productoId como texto.
        val productoId = leerIdQrProducto(textoQr)?.toString()

        if (productoId == null) {
            _mensaje.value = "El código QR no es válido."
            return
        }

        // Verificar que el producto esté incluido en el pedido.
        val producto = pedidoActual.productos.find {
            it.productoId == productoId
        }

        if (producto == null) {
            _mensaje.value = "Este producto no pertenece al pedido."
            return
        }

        val cantidadActual =
            _cantidadesEscaneadas.value[productoId] ?: 0

        // Impedir registrar más unidades de las solicitadas.
        if (cantidadActual >= producto.cantidad) {
            _mensaje.value =
                "Ya se verificaron todas las unidades de ${producto.nombreProducto}."
            return
        }

        // Registrar una unidad verificada.
        _cantidadesEscaneadas.update { cantidades ->
            cantidades + (productoId to (cantidadActual + 1))
        }

        _mensaje.value =
            "${producto.nombreProducto}: ${cantidadActual + 1} de ${producto.cantidad} verificadas."
    }

    // Comprueba si se verificaron todas las unidades.
    fun pedidoCompleto(): Boolean {
        val pedidoActual = _pedido.value ?: return false

        if (pedidoActual.productos.isEmpty()) return false

        return pedidoActual.productos.all { producto ->
            val verificadas =
                _cantidadesEscaneadas.value[producto.productoId] ?: 0

            verificadas == producto.cantidad
        }
    }
}
