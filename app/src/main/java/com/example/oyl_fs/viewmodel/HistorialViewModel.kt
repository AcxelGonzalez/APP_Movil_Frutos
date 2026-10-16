package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.repository.PedidoRepository
import com.example.oyl_fs.model.ItemCarrito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel del historial de pedidos.
 *
 * Se encarga de:
 * - Obtener los pedidos del repositorio.
 * - Filtrar pedidos según el usuario y su rol.
 * - Filtrar por estado.
 * - Actualizar el estado de pedidos como administrador.
 * - Mantener la interfaz actualizada mediante StateFlow.
 */
class HistorialViewModel : ViewModel() {

    // Repositorio temporal con pedidos de demostración.
    private val repository = PedidoRepository()

    // Identificador del usuario que inició sesión.
    private val _usuarioId = MutableStateFlow("")

    // Indica si el usuario es administrador.
    private val _esAdmin = MutableStateFlow(false)

    // Estado seleccionado para filtrar pedidos.
    // null significa mostrar todos los estados.
    private val _filtroEstado =
        MutableStateFlow<EstadoPedido?>(null)

    val filtroEstado: StateFlow<EstadoPedido?> =
        _filtroEstado.asStateFlow()

    /**
     * Lista de pedidos que debe mostrar la pantalla.
     *
     * combine observa los cambios del repositorio,
     * el usuario, su rol y el filtro seleccionado.
     */
    val pedidosVisibles: StateFlow<List<Pedido>> =
        combine(
            repository.pedidos,
            _usuarioId,
            _esAdmin,
            _filtroEstado
        ) { pedidos, usuarioId, esAdmin, filtro ->

            pedidos.filter { pedido ->

                // El administrador puede consultar todos los pedidos.
                // El cliente solo puede consultar los suyos.
                val perteneceAlUsuario =
                    esAdmin || (
                            usuarioId.isNotBlank() &&
                                    pedido.usuarioId == usuarioId
                            )

                // Comprueba el filtro seleccionado.
                val coincideConFiltro =
                    filtro == null || pedido.estado == filtro

                perteneceAlUsuario && coincideConFiltro

            }.sortedByDescending { it.fecha }

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    /**
     * Configura el usuario y su rol.
     *
     * Se llamará desde la navegación cuando
     * el usuario ingrese a HistorialScreen.
     */
    fun configurarSesion(
        usuarioId: String,
        esAdmin: Boolean
    ) {
        _usuarioId.value = usuarioId
        _esAdmin.value = esAdmin

        // Reinicia el filtro al cambiar de sesión.
        _filtroEstado.value = null
    }

    /**
     * Cambia el filtro del historial.
     *
     * Si recibe null, muestra todos los pedidos.
     */
    fun seleccionarFiltro(estado: EstadoPedido?) {
        _filtroEstado.value = estado
    }

    /**
     * Obtiene un pedido específico por su ID.
     */
    fun obtenerPedido(id: String): Pedido? {
        return pedidosVisibles.value.find { it.id == id }
    }

    /**
     * Permite actualizar el estado de un pedido.
     *
     * Solo se habilita para el perfil administrador.
     * La validación definitiva se hará en el backend.
     */
    fun cambiarEstadoPedido(
        pedidoId: String,
        nuevoEstado: EstadoPedido
    ): Boolean {

        if (!_esAdmin.value) {
            return false
        }

        return repository.actualizarEstadoPedido(
            pedidoId,
            nuevoEstado
        )
    }

    /**
     * Registra un nuevo pedido desde el carrito.
     *
     * Recibe el identificador del cliente y los productos
     * seleccionados, delegando el registro al repositorio.
     *
     * Devuelve el pedido creado o null si ocurre
     * un problema de validación.
     *
     * Los datos permanecen temporalmente en memoria.
     */
    fun registrarPedido(
        usuarioId: String,
        productos: List<ItemCarrito>
    ): Pedido? {

        // Validar que exista un cliente identificado.
        if (usuarioId.isBlank()) {
            return null
        }

        // Delegar el registro al repositorio.
        return repository.registrarPedido(
            usuarioId = usuarioId,
            items = productos
        )
    }

}
