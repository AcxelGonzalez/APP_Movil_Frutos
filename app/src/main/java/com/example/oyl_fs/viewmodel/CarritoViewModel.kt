package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.repository.CarritoRoomRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CarritoUiState(
    val items: List<ItemCarrito> = emptyList()
) {

    /**
      Calcula el total del carrito
     **/
    val total: Long
        get() = items.sumOf { it.subtotal }

    val cantidadProductos: Int get() = items.sumOf { it.cantidad }
    val estaVacio: Boolean get() = items.isEmpty()
    val sinStock: List<ItemCarrito> get() = items.filterNot { it.stockSuficiente }
}

class CarritoViewModel @JvmOverloads constructor(
    application: Application,
    private val repo: CarritoRoomRepository = CarritoRoomRepository(application)
) : AndroidViewModel(application) {

    private val clienteId = MutableStateFlow("")

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _uiState = MutableStateFlow(CarritoUiState())
    val uiState = _uiState.asStateFlow()
    private var observacion: kotlinx.coroutines.Job? = null

    fun iniciar(id: String) {
        if (clienteId.value == id && observacion?.isActive == true) return
        observacion?.cancel()
        clienteId.value = id
        _mensaje.value = null
        _uiState.value = CarritoUiState()
        if (id.isBlank()) return
        observacion = viewModelScope.launch {
            try {
                repo.observar(id).collect { lista ->
                    if (clienteId.value == id) _uiState.value = CarritoUiState(lista)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (clienteId.value == id) _mensaje.value = "No se pudo recuperar el carrito"
            }
        }
    }

    /**
     * Agrega un producto al carrito del cliente.
     *
     * Comprueba que exista un producto esté disponible y que haya stock.
     * Devuelve true si el producto se agregó correctamente.
     */
    suspend fun agregarProducto(producto: ProductoInfo, cantidad: Int = 1): Boolean {

        val id = clienteId.value

        // Comprueba que exista un cliente identificado.
        if (id.isBlank()) {
            _mensaje.value = "No se ha identificado al cliente"
            return false
        }

        if (cantidad <= 0 || cantidad > producto.stock) {
            _mensaje.value = "La cantidad supera las unidades disponibles"
            return false
        }

        // No permite agregar productos inactivos o agotados.
        if (!producto.disponible || producto.stock <= 0) {
            _mensaje.value = "Este producto no está disponible"
            return false
        }

        val agregado = try {
            repo.agregar(id, producto, cantidad)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (clienteId.value == id) _mensaje.value = "No se pudo guardar el carrito"
            return false
        }

        // Mensaje para mostrar al usuario.
        if (clienteId.value == id) {
            _mensaje.value = if (agregado) {
                "${producto.nombre} agregado al carrito"
            } else {
                "No hay stock suficiente de ${producto.nombre}"
            }
        }

        return agregado
    }

    fun aumentar(item: ItemCarrito) = ajustar(item, 1)

    fun disminuir(item: ItemCarrito) = ajustar(item, -1)

    private fun ajustar(item: ItemCarrito, delta: Int) {
        val id = clienteId.value
        val productoId = item.productoId.toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                if (!repo.ajustar(id, productoId, delta) && clienteId.value == id) {
                    _mensaje.value = "No hay unidades disponibles para este cambio"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (clienteId.value == id) _mensaje.value = "No se pudo actualizar el carrito"
            }
        }
    }

    fun quitar(item: ItemCarrito) {
        val id = clienteId.value
        val productoId = item.productoId.toIntOrNull() ?: return
        viewModelScope.launch {
            try {
                repo.quitar(id, productoId)
                if (clienteId.value == id) _mensaje.value = "${item.nombre} se quitó del carrito"
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (clienteId.value == id) _mensaje.value = "No se pudo quitar el producto"
            }
        }
    }

    fun validarAntesDeConfirmar(): Boolean {
        val estado = uiState.value
        if (estado.estaVacio) {
            _mensaje.value = "Tu carrito está vacío"
            return false
        }
        if (estado.sinStock.isNotEmpty()) {
            _mensaje.value = "Stock insuficiente: " + estado.sinStock.joinToString { it.nombre }
            return false
        }
        return true
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }
}
