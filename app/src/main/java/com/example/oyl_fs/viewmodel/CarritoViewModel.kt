package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.repository.CarritoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map as mapFlow
import kotlinx.coroutines.flow.stateIn

data class CarritoUiState(
    val items: List<ItemCarrito> = emptyList()
) {
    val total: Int get() = items.sumOf { it.subtotal }
    val cantidadProductos: Int get() = items.sumOf { it.cantidad }
    val estaVacio: Boolean get() = items.isEmpty()
    val sinStock: List<ItemCarrito> get() = items.filterNot { it.stockSuficiente }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CarritoViewModel : ViewModel() {

    private val repo = CarritoRepository

    private val clienteId = MutableStateFlow("")

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    val uiState: StateFlow<CarritoUiState> = clienteId
        .flatMapLatest { id -> repo.obtenerCarrito(id) }
        .mapFlow { lista -> CarritoUiState(items = lista) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CarritoUiState())

    fun iniciar(id: String) {
        clienteId.value = id
    }

    fun aumentar(item: ItemCarrito) {
        if (item.cantidad >= item.stockDisponible) {
            _mensaje.value = "Solo hay ${item.stockDisponible} unidades de ${item.nombre}"
            return
        }
        repo.cambiarCantidad(clienteId.value, item.productoId, item.cantidad + 1)
    }

    fun disminuir(item: ItemCarrito) {
        repo.cambiarCantidad(clienteId.value, item.productoId, item.cantidad - 1)
    }

    fun quitar(item: ItemCarrito) {
        repo.quitar(clienteId.value, item.productoId)
        _mensaje.value = "${item.nombre} se quitó del carrito"
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