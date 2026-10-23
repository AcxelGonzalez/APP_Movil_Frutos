package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.repository.PedidoRepository
import com.example.oyl_fs.repository.PedidoRoomRepository
import com.example.oyl_fs.model.ItemCarrito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

class HistorialViewModel(
    // Sin repositorio Room mantiene el
    // comportamiento actual de demostración.
    private val roomRepository: PedidoRoomRepository? = null
) : ViewModel() {

    // Repositorio temporal con pedidos de demostración.
    private val repository = PedidoRepository()


    /**
     * Lista general de pedidos.
     *
     * En modo Room observa los registros SQLite.
     * En modo demostración conserva el repositorio
     * en memoria utilizado actualmente.
     */
    val pedidosParaAdmin: StateFlow<List<Pedido>> =
        roomRepository?.pedidos?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        ) ?: repository.pedidos


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
            pedidosParaAdmin,
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

        if (!_esAdmin.value || roomRepository != null) {
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
        if (usuarioId.isBlank() || roomRepository != null) {
            return null
        }

        // Delegar el registro al repositorio.
        return repository.registrarPedido(
            usuarioId = usuarioId,
            items = productos
        )
    }

    /**
     * Observa el pedido más reciente de un cliente.
     *
     * Funciona con SQLite cuando Room está activo
     * y con los datos demo en el modo anterior.
     */
    fun observarUltimoPedidoCliente(
        usuarioId: String
    ): Flow<Pedido?> {

        val origen = roomRepository
            ?.observarPedidosUsuario(usuarioId)
            ?: repository.observarPedidosUsuario(usuarioId)

        return origen.map { pedidos ->
            pedidos.maxByOrNull { it.fecha }
        }
    }


    /**
     * Registra una compra utilizando Room.
     *
     * El repositorio se encarga de guardar el
     * pedido, sus detalles y descontar stock.
     *
     * No debe llamarse junto al descuento antiguo
     * de ProductoViewModel.
     */
    suspend fun registrarCompraPersistente(
        usuarioId: String,
        productos: List<ItemCarrito>
    ): Pedido? {

        val repo = roomRepository ?: return null

        return repo.registrarCompraDesdeCarrito(
            usuarioId = usuarioId,
            items = productos
        )
    }

    /**
     * Actualiza el estado del pedido.
     *
     * Solo permite la operación al administrador.
     * En modo Room espera la confirmación de SQLite.
     */
    suspend fun cambiarEstadoPedidoPersistente(
        pedidoId: String,
        nuevoEstado: EstadoPedido
    ): Boolean {

        if (!_esAdmin.value) {
            return false
        }

        val repo = roomRepository

        return if (repo != null) {

            repo.actualizarEstadoPedido(
                pedidoId = pedidoId,
                nuevoEstado = nuevoEstado
            )

        } else {

            repository.actualizarEstadoPedido(
                pedidoId,
                nuevoEstado
            )
        }
    }

}
