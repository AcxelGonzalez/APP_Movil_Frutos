package com.example.oyl_fs.viewmodel

import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.example.oyl_fs.repository.DatosLocalesRemotosRepository
import com.example.oyl_fs.repository.PedidoRemoteRepository
import com.example.oyl_fs.network.mensajeApi
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
    // MainActivity usa remoto; Room y memoria conservan compatibilidad con los datos y pruebas anteriores.
    private val roomRepository: PedidoRoomRepository? = null,
    private val remoto: Boolean = false,
    private val complementos: DatosLocalesRemotosRepository? = null,
    private val api: PedidoRemoteRepository = PedidoRemoteRepository()
) : ViewModel() {

    // Repositorio temporal con pedidos de demostración.
    private val repository by lazy { PedidoRepository() }


    /**
     * Lista general de pedidos.
     *
     * En modo Room observa los registros SQLite.
     * En modo demostración conserva el repositorio
     * en memoria utilizado actualmente.
     */
    private val remotos = MutableStateFlow<List<Pedido>>(emptyList())
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _cargando = MutableStateFlow(false)
    val cargando = _cargando.asStateFlow()
    private var carga: Job? = null
    private var generacion = 0

    fun refrescar() {
        if (!remoto) return
        carga?.cancel()
        val actual = ++generacion
        val id = _usuarioId.value.toLongOrNull()
        val admin = _esAdmin.value
        if (id == null || id <= 0) {
            remotos.value = emptyList()
            _cargando.value = false
            return
        }
        _cargando.value = true
        _error.value = null
        carga = viewModelScope.launch {
            val resultado = if (admin) api.listarTodos() else api.listarPorUsuario(id)
            if (actual != generacion) return@launch
            resultado.fold(
                onSuccess = { remotos.value = it.filter { p -> admin || p.usuarioId == id.toString() }.map { p -> complementos?.completarPedido(p) ?: p } },
                onFailure = { _error.value = it.mensajeApi() }
            )
            _cargando.value = false
        }
    }

    val pedidosParaAdmin: StateFlow<List<Pedido>> =
        if (remoto) remotos.asStateFlow() else roomRepository?.pedidos?.stateIn(
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
        val cambio = _usuarioId.value != usuarioId || _esAdmin.value != esAdmin
        if (remoto && cambio) remotos.value = emptyList()
        _usuarioId.value = usuarioId
        _esAdmin.value = esAdmin

        // Reinicia el filtro al cambiar de sesión.
        if (cambio) _filtroEstado.value = null
        refrescar()
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

        val origen = if (remoto) remotos.map { lista -> lista.filter { it.usuarioId == usuarioId } } else roomRepository
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

        if (remoto) {
            val actual = generacion
            val resultado = api.cambiarEstado(pedidoId, nuevoEstado)
            if (actual != generacion) return false
            return resultado.fold(
                onSuccess = { actualizado ->
                    remotos.value = remotos.value.map { if (it.id == pedidoId) (complementos?.completarPedido(actualizado) ?: actualizado) else it }
                    _error.value = null
                    true
                },
                onFailure = { _error.value = it.mensajeApi(); false }
            )
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
