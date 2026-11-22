package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.network.mensajeApi
import com.example.oyl_fs.repository.PedidoRoomRepository
import com.example.oyl_fs.repository.PedidoRemoteRepository
import com.example.oyl_fs.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del historial de pedidos.
 *
 * Los pedidos vienen del backend (ms-pedidos):
 * - El administrador recibe todos los pedidos.
 * - El cliente recibe solo los suyos (el servidor los filtra por su id).
 */

class HistorialViewModel(
    // Sin repositorio Room mantiene el
    // comportamiento actual de demostración.
    private val roomRepository: PedidoRoomRepository? = null
) : ViewModel() {

    // Pedidos cargados desde el backend.
    private val _pedidos = MutableStateFlow<List<Pedido>>(emptyList())

    // Lo usa el panel del administrador (HomeAdmin) y la preparación por QR.
    val pedidosParaAdmin: StateFlow<List<Pedido>> = _pedidos.asStateFlow()

    // Estado seleccionado para filtrar pedidos.
    // null significa mostrar todos los estados.
    private val _filtroEstado = MutableStateFlow<EstadoPedido?>(null)
    val filtroEstado: StateFlow<EstadoPedido?> = _filtroEstado.asStateFlow()

    // Último problema al cargar los pedidos (por ejemplo, servidor apagado).
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Lista que muestra HistorialScreen: aplica el filtro y ordena por fecha.
    val pedidosVisibles: StateFlow<List<Pedido>> =
        combine(_pedidos, _filtroEstado) { pedidos, filtro ->
            pedidos
                .filter { filtro == null || it.estado == filtro }
                .sortedByDescending { it.fecha }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    /**
     * Se llama al iniciar sesión o al abrir el historial.
     * El usuario y su rol salen de la sesión del backend;
     * los parámetros se mantienen para no cambiar las pantallas.
     */
    fun configurarSesion(usuarioId: String, esAdmin: Boolean) {
        _filtroEstado.value = null
        refrescar()
    }

    // Vuelve a pedir los pedidos al backend.
    fun refrescar() {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val usuario = SesionRepository.usuario.value
        if (usuario == null) {
            _pedidos.value = emptyList()
            return
        }

        val resultado = if (usuario.rol == "ADMIN") {
            PedidoRemoteRepository.listarTodos()
        } else {
            PedidoRemoteRepository.listarPorUsuario(usuario.id)
        }

        resultado
            .onSuccess {
                _pedidos.value = it
                _error.value = null
            }
            .onFailure { _error.value = it.mensajeApi() }
    }

    // Se llama al cerrar sesión para no dejar pedidos del usuario anterior.
    fun limpiar() {
        _pedidos.value = emptyList()
        _filtroEstado.value = null
        _error.value = null
    }

    // Cambia el filtro del historial. Si recibe null, muestra todos los pedidos.
    fun seleccionarFiltro(estado: EstadoPedido?) {
        _filtroEstado.value = estado
    }

    // Obtiene un pedido específico por su ID.
    fun obtenerPedido(id: String): Pedido? {
        return _pedidos.value.find { it.id == id }
    }

    /**
     * Cambia el estado de un pedido en el backend.
     * Solo el administrador puede hacerlo, y el servidor solo permite avanzar un paso.
     *
     * onResultado recibe null si salió bien, o el mensaje del servidor si falló.
     */
    fun cambiarEstadoPedido(
        pedidoId: String,
        nuevoEstado: EstadoPedido,
        onResultado: (String?) -> Unit = {}
    ) {
        if (SesionRepository.usuario.value?.rol != "ADMIN") {
            onResultado("Solo el administrador puede cambiar el estado")
            return
        }

        viewModelScope.launch {
            PedidoRemoteRepository.cambiarEstado(pedidoId, nuevoEstado)
                .onSuccess {
                    cargar()
                    onResultado(null)
                }
                .onFailure { onResultado(it.mensajeApi()) }
        }
    }

    /**
     * Observa el pedido más reciente del cliente (lo usa la tarjeta de
     * seguimiento de HomeCliente). Se actualiza cuando se recargan los pedidos.
     */
    fun observarUltimoPedidoCliente(usuarioId: String): Flow<Pedido?> {
        return _pedidos.map { pedidos -> pedidos.maxByOrNull { it.fecha } }
    }
}

