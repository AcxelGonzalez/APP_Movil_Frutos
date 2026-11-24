package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.network.mensajeApi
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.model.EntregaPedido
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.repository.PedidoRoomRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConfirmacionPedidoUiState(
    val usuarioId: String = "",
    val procesando: Boolean = false,
    val pedido: Pedido? = null,
    val error: String? = null,
    val pendiente: com.example.oyl_fs.repository.IntentoCompra? = null,
    val recuperacionBloqueada: Boolean = false
)

class ConfirmacionPedidoViewModel @JvmOverloads constructor(
    application: Application,
    private val repo: PedidoRoomRepository? = null,
    private val remoto: com.example.oyl_fs.repository.CompraRemotaRepository = com.example.oyl_fs.repository.CompraRemotaRepository(application)
) : AndroidViewModel(application) {
    private val _estado = MutableStateFlow(ConfirmacionPedidoUiState())
    val estado = _estado.asStateFlow()

    fun iniciar(usuarioId: String) {
        if (!_estado.value.procesando && _estado.value.usuarioId != usuarioId) {
            _estado.value = ConfirmacionPedidoUiState(usuarioId)
            if (repo == null) cargarPendiente(usuarioId)
        }
    }

    fun confirmar(usuarioId: String, items: List<ItemCarrito>, entrega: EntregaPedido) {
        val actual = _estado.value
        if (usuarioId.isBlank() || actual.usuarioId != usuarioId || actual.procesando || actual.pedido != null || actual.pendiente != null || actual.recuperacionBloqueada) return
        // Marcar antes de iniciar la corrutina también bloquea pulsaciones repetidas.
        _estado.value = actual.copy(procesando = true, error = null)
        val seleccion = items.toList()
        viewModelScope.launch {
            try {
                if (repo == null) {
                    val resultado = remoto.confirmar(usuarioId, seleccion, entrega)
                    _estado.value = ConfirmacionPedidoUiState(usuarioId, pedido = resultado.pedido, error = resultado.advertencia)
                    return@launch
                }
                val pedido = repo.registrarCompraDesdeCarrito(usuarioId, seleccion, entrega)
                _estado.value = if (pedido != null) {
                    ConfirmacionPedidoUiState(usuarioId, pedido = pedido)
                } else {
                    ConfirmacionPedidoUiState(usuarioId,
                        error = "No se pudo confirmar: revisa los datos y el stock. Si cambió un precio, quita el producto del carrito y agrégalo nuevamente.")
                }
            } catch (e: CancellationException) {
                // Una cancelación de la espera no prueba que el servidor haya rechazado
                // el pedido. Liberar la pantalla activa sin reenviar ni inventar un fallo.
                if (currentCoroutineContext().isActive) {
                    _estado.value = ConfirmacionPedidoUiState(usuarioId,
                        procesando = repo == null,
                        error = "Se canceló la espera de confirmación. Revisa Mis pedidos antes de volver a confirmar: el pedido podría haberse registrado.")
                    if (repo == null) {
                        actualizarPendiente(usuarioId)
                        if (_estado.value.usuarioId == usuarioId) _estado.value = _estado.value.copy(procesando = false)
                    }
                }
                throw e
            } catch (e: Exception) {
                _estado.value = ConfirmacionPedidoUiState(usuarioId,
                    procesando = repo == null,
                    error = if (repo == null) e.mensajeApi() else "No se pudo guardar el pedido. Inténtalo nuevamente.")
                if (repo == null) {
                    actualizarPendiente(usuarioId)
                    if (_estado.value.usuarioId == usuarioId) _estado.value = _estado.value.copy(procesando = false)
                }
            }
        }
    }

    private fun cargarPendiente(usuarioId: String) {
        _estado.value = _estado.value.copy(procesando = true)
        viewModelScope.launch {
            actualizarPendiente(usuarioId)
            if (_estado.value.usuarioId == usuarioId) _estado.value = _estado.value.copy(procesando = false)
        }
    }

    private suspend fun actualizarPendiente(usuarioId: String) {
        try {
            val pendiente = remoto.pendiente(usuarioId)
            if (_estado.value.usuarioId == usuarioId)
                _estado.value = _estado.value.copy(pendiente = pendiente, recuperacionBloqueada = false)
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            if (_estado.value.usuarioId == usuarioId) _estado.value = _estado.value.copy(recuperacionBloqueada = true,
                error = "No se pudo leer la compra guardada. No confirmes otra hasta recuperar el registro local.")
        }
    }

    fun recuperar(usuarioId: String, continuar: Boolean) {
        val actual = _estado.value
        if (repo != null || actual.usuarioId != usuarioId || actual.procesando || actual.pedido != null) return
        _estado.value = actual.copy(procesando = true, error = null)
        viewModelScope.launch {
            try {
                val resultado = if (actual.recuperacionBloqueada) null
                    else if (continuar) remoto.continuar(usuarioId) else remoto.consultar(usuarioId)
                _estado.value = ConfirmacionPedidoUiState(usuarioId, procesando = true,
                    pedido = resultado?.confirmada?.pedido,
                    error = resultado?.confirmada?.advertencia ?: resultado?.mensaje)
            } catch (e: CancellationException) {
                if (currentCoroutineContext().isActive) _estado.value = actual.copy(procesando = false,
                    error = "Se canceló la espera. El intento sigue guardado; consulta antes de continuar.")
                throw e
            } catch (e: Exception) {
                _estado.value = actual.copy(procesando = true, error = e.mensajeApi())
            } finally {
                if (currentCoroutineContext().isActive) {
                    actualizarPendiente(usuarioId)
                    _estado.value = _estado.value.copy(procesando = false)
                }
            }
        }
    }

    fun borrarMensaje() { if (!_estado.value.procesando) _estado.value = _estado.value.copy(error = null) }

    fun limpiar() {
        if (!_estado.value.procesando) {
            _estado.value = _estado.value.copy(pedido = null, error = null)
            if (repo == null) cargarPendiente(_estado.value.usuarioId)
        }
    }
}
