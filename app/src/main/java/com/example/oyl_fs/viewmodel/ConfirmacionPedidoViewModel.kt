package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.model.EntregaPedido
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.repository.PedidoRoomRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfirmacionPedidoUiState(
    val usuarioId: String = "",
    val procesando: Boolean = false,
    val pedido: Pedido? = null,
    val error: String? = null
)

class ConfirmacionPedidoViewModel @JvmOverloads constructor(
    application: Application,
    private val repo: PedidoRoomRepository = PedidoRoomRepository(application)
) : AndroidViewModel(application) {
    private val _estado = MutableStateFlow(ConfirmacionPedidoUiState())
    val estado = _estado.asStateFlow()

    fun iniciar(usuarioId: String) {
        if (!_estado.value.procesando && _estado.value.usuarioId != usuarioId) {
            _estado.value = ConfirmacionPedidoUiState(usuarioId)
        }
    }

    fun confirmar(usuarioId: String, items: List<ItemCarrito>, entrega: EntregaPedido) {
        val actual = _estado.value
        if (usuarioId.isBlank() || actual.usuarioId != usuarioId || actual.procesando || actual.pedido != null) return
        // Marcar antes de iniciar la corrutina también bloquea pulsaciones repetidas.
        _estado.value = actual.copy(procesando = true, error = null)
        val seleccion = items.toList()
        viewModelScope.launch {
            try {
                val pedido = repo.registrarCompraDesdeCarrito(usuarioId, seleccion, entrega)
                _estado.value = if (pedido != null) {
                    ConfirmacionPedidoUiState(usuarioId, pedido = pedido)
                } else {
                    ConfirmacionPedidoUiState(usuarioId,
                        error = "No se pudo confirmar: revisa los datos y el stock. Si cambió un precio, quita el producto del carrito y agrégalo nuevamente.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _estado.value = ConfirmacionPedidoUiState(usuarioId,
                    error = "No se pudo guardar el pedido. Inténtalo nuevamente.")
            }
        }
    }

    fun limpiar() {
        if (!_estado.value.procesando) _estado.value = ConfirmacionPedidoUiState(_estado.value.usuarioId)
    }
}
