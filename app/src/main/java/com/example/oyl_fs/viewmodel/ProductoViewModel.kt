package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.TipoMovimientoStock
import com.example.oyl_fs.model.ValidacionMovimientoStock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.lifecycle.ViewModel
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.screens.ProductosDatos
import com.example.oyl_fs.model.ItemCarrito
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.repository.ProductoRoomRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import com.example.oyl_fs.network.mensajeApi
import com.example.oyl_fs.repository.ProductoRemoteRepository
import com.example.oyl_fs.repository.MovimientoInventarioRepository
import com.example.oyl_fs.repository.MovimientoInventarioGuardado
import com.example.oyl_fs.repository.SesionRemotaRepository

data class MovimientoStockUiState(
    val productoId: Int? = null,
    val procesando: Boolean = false,
    val error: String? = null,
    val resultadoIncierto: Boolean = false,
    val confirmado: ProductoInfo? = null,
    val advertencia: String? = null,
    val pendiente: MovimientoInventarioGuardado? = null,
    val registroLegible: Boolean = true
)

class ProductoViewModel(
    private val roomRepository: ProductoRoomRepository? = null,
    // Si se entrega, el backend (ms-productos) es la fuente de datos.
    private val remoteRepository: ProductoRemoteRepository? = null,
    private val movimientosRepository: MovimientoInventarioRepository? = null
) : ViewModel() {

    private val _movimientoStock = MutableStateFlow(MovimientoStockUiState())
    val movimientoStock = _movimientoStock.asStateFlow()

    private var sesionInventario: String? = null
    private var versionInventario = 0L

    fun configurarSesionInventario() {
        val sesion = SesionRemotaRepository.sesionActual()?.takeIf { it.identidad.esAdmin }
        if (sesionInventario == sesion?.idSesion) return
        sesionInventario = sesion?.idSesion
        versionInventario++
        _movimientoStock.value = MovimientoStockUiState()
        if (sesion != null && movimientosRepository != null) leerMovimiento(null)
    }

    private fun movimientoVigente(version: Long, idSesion: String?) =
        version == versionInventario && idSesion != null &&
            SesionRemotaRepository.sesionActual()?.idSesion == idSesion

    private fun mostrarMovimiento(registro: MovimientoInventarioGuardado) {
        _movimientoStock.value = MovimientoStockUiState(
            productoId = registro.producto.id, pendiente = registro,
            resultadoIncierto = !registro.finalizado,
            confirmado = if (registro.estado == "APLICADA") registro.producto.copy(stock = requireNotNull(registro.stockResultante)) else null,
            error = registro.mensaje.takeIf { registro.estado != "APLICADA" }
        )
    }

    private fun leerMovimiento(productoId: Int?) {
        val repo = movimientosRepository ?: return
        val sesion = SesionRemotaRepository.sesionActual()?.takeIf { it.identidad.esAdmin } ?: return
        val version = versionInventario
        _movimientoStock.value = MovimientoStockUiState(productoId, procesando = true)
        viewModelScope.launch {
            try {
                val registro = repo.obtener(sesion.identidad.usuarioId)
                if (!movimientoVigente(version, sesion.idSesion)) return@launch
                if (registro == null) _movimientoStock.value = MovimientoStockUiState(productoId)
                else mostrarMovimiento(registro)
            } catch (e: CancellationException) {
                if (movimientoVigente(version, sesion.idSesion)) _movimientoStock.value = MovimientoStockUiState(
                    productoId, registroLegible = false, error = "Se interrumpió la lectura del registro local. Reintenta su lectura antes de registrar un movimiento.")
                throw e
            } catch (_: Exception) {
                if (movimientoVigente(version, sesion.idSesion)) _movimientoStock.value = MovimientoStockUiState(
                    productoId, registroLegible = false,
                    error = "No se pudo leer el registro local de inventario. Reintenta la lectura antes de registrar otro movimiento.")
            }
        }
    }

    fun iniciarMovimientoStock(productoId: Int) {
        if (_movimientoStock.value.procesando) return
        configurarSesionInventario()
        if (_movimientoStock.value.procesando || _movimientoStock.value.pendiente != null) return
        leerMovimiento(productoId)
    }

    fun registrarMovimientoStock(productoId: Int, tipo: TipoMovimientoStock, cantidadTexto: String) {
        val anterior = _movimientoStock.value
        if ((anterior.productoId != null && anterior.productoId != productoId) || anterior.procesando ||
            anterior.pendiente != null || !anterior.registroLegible) return
        val errorCantidad = ValidacionMovimientoStock.errorCantidad(cantidadTexto)
        if (errorCantidad != null) {
            _movimientoStock.value = MovimientoStockUiState(productoId, error = errorCantidad)
            return
        }
        val repo = movimientosRepository
        val sesion = SesionRemotaRepository.sesionActual()?.takeIf { it.identidad.esAdmin }
        val producto = _productos.value.firstOrNull { it.id == productoId }
        if (productoId <= 0 || repo == null || sesion == null || producto == null) {
            _movimientoStock.value = MovimientoStockUiState(productoId, error = "Los movimientos de inventario requieren conexión con el servidor.")
            return
        }
        val cambio = tipo.cambio(cantidadTexto.trim().toInt())
        ejecutarMovimiento(productoId) { repo.iniciar(sesion.identidad.usuarioId, producto, cambio) }
    }

    fun recuperarMovimientoStock(continuar: Boolean) {
        val estado = _movimientoStock.value
        if (estado.procesando) return
        val registro = estado.pendiente
        if (registro == null) { leerMovimiento(estado.productoId); return }
        val repo = movimientosRepository ?: return
        ejecutarMovimiento(registro.producto.id) { repo.recuperar(registro.actorId, registro.clave, continuar) }
    }

    private fun ejecutarMovimiento(productoId: Int, accion: suspend () -> MovimientoInventarioGuardado) {
        val sesion = SesionRemotaRepository.sesionActual()?.takeIf { it.identidad.esAdmin } ?: return
        val version = versionInventario
        _movimientoStock.value = _movimientoStock.value.copy(productoId = productoId, procesando = true)
        viewModelScope.launch {
            try {
                val resultado = accion()
                if (!movimientoVigente(version, sesion.idSesion)) return@launch
                mostrarMovimiento(resultado)
                if (resultado.finalizado) {
                    // El comprobante histórico no reemplaza el stock vigente del catálogo.
                    cargarRemoto()
                    if (movimientoVigente(version, sesion.idSesion) && _movimientoStock.value.pendiente?.clave == resultado.clave && _errorCatalogo.value != null)
                        _movimientoStock.value = _movimientoStock.value.copy(advertencia = "El resultado quedó guardado. No se pudo actualizar el stock actual del catálogo.")
                }
            } catch (e: CancellationException) {
                if (currentCoroutineContext().isActive && movimientoVigente(version, sesion.idSesion))
                    recuperarRegistroTrasError(productoId, sesion.identidad.usuarioId, version, sesion.idSesion)
                throw e
            } catch (_: Exception) {
                if (movimientoVigente(version, sesion.idSesion))
                    recuperarRegistroTrasError(productoId, sesion.identidad.usuarioId, version, sesion.idSesion)
            }
        }
    }

    private suspend fun recuperarRegistroTrasError(productoId: Int, actorId: String, version: Long, idSesion: String) {
        try {
            val registro = movimientosRepository?.obtener(actorId)
            if (!movimientoVigente(version, idSesion)) return
            if (registro != null) {
                mostrarMovimiento(registro)
                if (!registro.finalizado) _movimientoStock.value = _movimientoStock.value.copy(
                    error = "No se pudo confirmar el resultado. El movimiento está guardado: consulta su resultado o continúa el mismo movimiento. No registres otro ajuste para reemplazarlo.")
            } else _movimientoStock.value = MovimientoStockUiState(productoId,
                error = "No se pudo guardar el movimiento localmente. No se envió un movimiento nuevo. Revisa el almacenamiento antes de intentarlo nuevamente.")
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            if (movimientoVigente(version, idSesion)) _movimientoStock.value = MovimientoStockUiState(productoId,
                registroLegible = false, error = "No se pudo recuperar el registro local. Reintenta su lectura antes de operar sobre el inventario.")
        }
    }

    fun cerrarMovimientoStock() {
        val registro = _movimientoStock.value.pendiente ?: return
        if (!registro.finalizado || _movimientoStock.value.procesando) return
        val sesion = SesionRemotaRepository.sesionActual() ?: return
        val version = versionInventario
        _movimientoStock.value = _movimientoStock.value.copy(procesando = true)
        viewModelScope.launch {
            try {
                movimientosRepository?.cerrar(registro.actorId, registro.clave)
                if (movimientoVigente(version, sesion.idSesion)) _movimientoStock.value = MovimientoStockUiState(registro.producto.id)
            } catch (e: CancellationException) {
                if (movimientoVigente(version, sesion.idSesion)) _movimientoStock.value = _movimientoStock.value.copy(procesando = false)
                throw e
            } catch (_: Exception) {
                if (movimientoVigente(version, sesion.idSesion)) _movimientoStock.value = _movimientoStock.value.copy(
                    procesando = false, advertencia = "No se pudo cerrar el resultado local. El movimiento no se repetirá al consultarlo.")
            }
        }
    }

    private val cargaMutex = Mutex()

    private val _productos = MutableStateFlow<List<ProductoInfo>>(
        if (roomRepository == null && remoteRepository == null) {
            // Modo de pruebas unitarias actual.
            ProductosDatos.productos
        } else {
            // En modo Room esperamos los datos de SQLite.
            emptyList()
        }
    )

    // Evita reutilizar el ID de un producto eliminado durante la simulación.
    private var siguienteId = (ProductosDatos.productos.maxOfOrNull { it.id } ?: 0) + 1

    val productos: StateFlow<List<ProductoInfo>> =
        _productos.asStateFlow()

    private val _errorCatalogo = MutableStateFlow<String?>(null)
    val errorCatalogo = _errorCatalogo.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    val error: StateFlow<String?> = _error.asStateFlow()

    // Indica si todavía estamos cargando los
// productos desde la base de datos.
    private val _cargandoProductos =
        MutableStateFlow(roomRepository != null || remoteRepository != null)

    val cargandoProductos: StateFlow<Boolean> =
        _cargandoProductos.asStateFlow()

    /**
     * Observa SQLite cuando se utiliza Room.
     *
     * Room será la fuente de información.
     * No copiamos productos ficticios encima
     * de los registros persistentes.
     */
    init {
        roomRepository?.let { repositorio ->

            viewModelScope.launch {
                try {
                    repositorio.productos.collect { lista ->

                        _productos.value = lista
                        _cargandoProductos.value = false
                    }

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    _error.value =
                        "No se pudieron cargar los productos"
                    _cargandoProductos.value = false
                }
            }
        }
    }

    init {
        if (remoteRepository != null) refrescar()
    }

    // Vuelve a pedir la lista al backend (también se usará después de comprar)
    fun refrescar() {
        viewModelScope.launch { cargarRemoto() }
    }

    private suspend fun cargarRemoto() = cargaMutex.withLock {
        val remoto = remoteRepository ?: return@withLock
        _cargandoProductos.value = true
        try {
            _productos.value = remoto.listar()
            _errorCatalogo.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            try {
                _productos.value = remoto.cargarCache()
                _errorCatalogo.value = if (_productos.value.isNotEmpty()) {
                    "Mostrando catálogo guardado. Los precios y el stock pueden haber cambiado. ${e.mensajeApi()}"
                } else {
                    "No hay catálogo guardado para consultar sin conexión. ${e.mensajeApi()}"
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                _errorCatalogo.value = "No se pudo recuperar el catálogo guardado. ${e.mensajeApi()}"
            }
        } finally {
            _cargandoProductos.value = false
        }
    }

    // Busca un producto
    fun obtenerProducto(id: Int): ProductoInfo? {
        return _productos.value.find { it.id == id }
    }

    fun limpiarError() {
        _error.value = null
    }

    // Comprueba los datos antes de agregar o editar.
    private fun validarProducto(
        producto: ProductoInfo
    ): Boolean {

        val datos = DatosFormularioProducto(
            nombre = producto.nombre,
            codigo = producto.codigo,
            categoria = producto.categoria,
            precio = producto.precio.toString(),
            stock = producto.stock.toString(),
            stockMinimo = producto.stockMinimo.toString(),
            presentacion = producto.presentacion,
            descripcion = producto.descripcion,
            imagenPresente = producto.imagen != 0 ||
                    !producto.imagenUri.isNullOrBlank()
        )

        val errores = ValidacionFormularioProducto.validar(
            datos = datos
        )

        _error.value = errores.primerError()

        return !errores.hayErrores
    }

    fun agregarProducto(producto: ProductoInfo): Boolean {
        if (!validarProducto(producto)) return false

        val codigoExiste = _productos.value.any {
            it.codigo.trim().equals(
                producto.codigo.trim(),
                ignoreCase = true
            )
        }

        if (codigoExiste) {
            _error.value = "Ya existe un producto con ese código"
            return false
        }

        // despues bd
        val nuevoId = siguienteId++

        val nuevoProducto = producto.copy(
            id = nuevoId,
            nombre = producto.nombre.trim(),
            codigo = producto.codigo.trim()
        )

        // Después guardaremos en SQLite y actualizaremos la lista.
        _productos.value = _productos.value + nuevoProducto
        return true
    }

    fun editarProducto(producto: ProductoInfo): Boolean {
        limpiarError()

        if (obtenerProducto(producto.id) == null) {
            _error.value = "El producto ya no existe"
            return false
        }

        if (!validarProducto(producto)) return false

        val codigoExiste = _productos.value.any {
            it.id != producto.id &&
                    it.codigo.trim().equals(
                        producto.codigo.trim(),
                        ignoreCase = true
                    )
        }

        if (codigoExiste) {
            _error.value = "Otro producto ya utiliza ese código"
            return false
        }

        val productoEditado = producto.copy(
            nombre = producto.nombre.trim(),
            codigo = producto.codigo.trim()
        )

         //  bd
        _productos.value = _productos.value.map {
            if (it.id == producto.id) productoEditado else it
        }

        return true
    }

    // elimina producto

    fun eliminarProducto(id: Int): Boolean {
        limpiarError()

        if (obtenerProducto(id) == null) {
            _error.value = "El producto ya no existe"
            return false
        }

        // bd
        _productos.value = _productos.value.filter {
            it.id != id
        }

        return true
    }

    /**
     * Agrega un producto a Room.
     *
     * La operación solo devuelve true después
     * de finalizar correctamente la escritura.
     */
    suspend fun agregarProductoPersistente(
        producto: ProductoInfo
    ): Boolean {

        remoteRepository?.let { remoto ->
            if (!validarProducto(producto)) return false
            return try {
                remoto.crear(producto.copy(
                    nombre = producto.nombre.trim(), codigo = producto.codigo.trim()))
                cargarRemoto()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.mensajeApi()
                false
            }
        }

        val repo = roomRepository
            ?: return agregarProducto(producto)

        if (!validarProducto(producto)) return false

        val limpio = producto.copy(
            nombre = producto.nombre.trim(),
            codigo = producto.codigo.trim()
        )

        return try {

            val duplicado = repo.cargarProductos().any {
                it.codigo.trim().equals(
                    limpio.codigo,
                    ignoreCase = true
                )
            }

            if (duplicado) {
                _error.value =
                    "Ya existe un producto con ese código"
                false

            } else {
                // Room genera y devuelve el ID definitivo.
                repo.insertarYObtener(limpio)

                _error.value = null
                true
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            _error.value = "No se pudo guardar el producto"
            false
        }
    }

    /**
     * Actualiza un producto existente en SQLite.
     */
    suspend fun editarProductoPersistente(
        producto: ProductoInfo
    ): Boolean {

        remoteRepository?.let { remoto ->
            if (!validarProducto(producto)) return false
            return try {
                remoto.actualizar(producto.copy(
                    nombre = producto.nombre.trim(), codigo = producto.codigo.trim()))
                cargarRemoto()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.mensajeApi()
                false
            }
        }

            val repo = roomRepository
            ?: return editarProducto(producto)

        if (!validarProducto(producto)) return false

        val limpio = producto.copy(
            nombre = producto.nombre.trim(),
            codigo = producto.codigo.trim()
        )

        return try {

            if (repo.obtenerPorId(limpio.id) == null) {
                _error.value = "El producto ya no existe"
                false

            } else {

                val duplicado = repo.cargarProductos().any {
                    it.id != limpio.id &&
                            it.codigo.trim().equals(
                                limpio.codigo,
                                ignoreCase = true
                            )
                }

                if (duplicado) {
                    _error.value =
                        "Otro producto ya utiliza ese código"
                    false

                } else {

                    val actualizado = repo.actualizar(limpio)

                    if (!actualizado) {
                        _error.value =
                            "No se pudo actualizar el producto"
                    } else {
                        _error.value = null
                    }

                    actualizado
                }
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            _error.value = "Error al actualizar el producto"
            false
        }
    }

    /**
     * Elimina un producto persistente.
     *
     * En modo remoto, éxito o 404 confirman que debe retirarse del catálogo.
     */
    suspend fun eliminarProductoPersistente(
        id: Int
    ): Boolean {

        remoteRepository?.let { remoto ->
            limpiarError()
            return try {
                val resultado = cargaMutex.withLock {
                    val confirmacion = remoto.eliminarRegistrado(id)
                    _productos.value = _productos.value.filterNot { it.id == id }
                    confirmacion
                }
                cargarRemoto()
                if (!resultado.cacheActualizada && _errorCatalogo.value != null) {
                    _errorCatalogo.value = "El producto fue retirado del servidor, pero no se pudo actualizar su copia local. Actualiza el catálogo con conexión antes de usarlo sin conexión."
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.mensajeApi()
                false
            }
        }

        val repo = roomRepository
            ?: return eliminarProducto(id)

        limpiarError()

        return try {

            val eliminado = repo.eliminar(id)

            if (!eliminado) {
                _error.value = "El producto ya no existe"
            }

            eliminado

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            _error.value = "No se pudo eliminar el producto"
            false
        }
    }

    /**
     * Comprueba que los artículos de una compra
     * sigan disponibles en el catálogo.
     *
     * También detecta cambios de precio realizados
     * después de agregar un producto al carrito.
     *
     * Devuelve null si la compra es válida,
     * o un mensaje explicando el problema.
     */
    fun validarStockParaCompra(
        items: List<ItemCarrito>
    ): String? {

        if (items.isEmpty()) {
            return "El carrito está vacío"
        }

        // Validar los montos utilizando Long para evitar
        // desbordamientos antes de calcular el total.
        var totalSeguro = 0L

        for (item in items) {

            // Rechazar precios o cantidades inválidas.
            if (item.precio <= 0 || item.cantidad <= 0) {
                return "Precio o cantidad inválidos en ${item.nombre}"
            }

            // Convertir a Long ANTES de multiplicar.
            val subtotalSeguro =
                item.precio.toLong() * item.cantidad.toLong()

            // No permitir totales superiores al máximo
            // representable por los modelos actuales.
            if (subtotalSeguro > Int.MAX_VALUE.toLong() - totalSeguro) {
                return "El monto de la compra supera el máximo permitido"
            }

            totalSeguro += subtotalSeguro
        }

        // Agrupar cantidades por identificador.
        val cantidades = mutableMapOf<Int, Int>()

        for (item in items) {

            val id = item.productoId.toIntOrNull()
                ?: return "Identificador de producto inválido"

            if (item.cantidad <= 0) {
                return "Cantidad inválida para ${item.nombre}"
            }

            val acumulada =
                (cantidades[id] ?: 0).toLong() + item.cantidad

            if (acumulada > Int.MAX_VALUE) {
                return "Cantidad inválida para ${item.nombre}"
            }

            cantidades[id] = acumulada.toInt()

            val producto = obtenerProducto(id)
                ?: return "El producto ${item.nombre} ya no existe"

            if (producto.precio != item.precio) {
                return "El precio de ${item.nombre} cambió. Quítalo y agrégalo nuevamente."
            }
        }

        // Consultar el stock actual, no el que quedó
        // guardado cuando se agregó al carrito.
        for ((id, cantidad) in cantidades) {

            val producto = obtenerProducto(id)
                ?: return "El producto ya no existe"

            if (!producto.disponible) {
                return "${producto.nombre} no está disponible"
            }

            if (producto.stock < cantidad) {
                return "Stock insuficiente de ${producto.nombre}. Disponibles: ${producto.stock}"
            }
        }

        return null
    }

    /**
     * Descuenta las unidades vendidas del stock.
     *
     * Devuelve false si la compra no supera
     * la validación actual de existencias.
     */
    fun descontarStockPorCompra(
        items: List<ItemCarrito>
    ): Boolean {

        // No descontar si existen errores.
        if (validarStockParaCompra(items) != null) {
            return false
        }

        val cantidades = items
            .groupBy { it.productoId.toInt() }
            .mapValues { (_, lista) ->
                lista.sumOf { it.cantidad }
            }

        // Actualizar la lista observable de productos.
        _productos.value = _productos.value.map { producto ->

            val cantidadVendida =
                cantidades[producto.id] ?: 0

            if (cantidadVendida > 0) {
                producto.copy(
                    stock = producto.stock - cantidadVendida
                )
            } else {
                producto
            }
        }

        return true
    }

    // La compra actualiza SQLite antes de confirmar el pedido de demostración.
    suspend fun descontarStockPorCompraPersistente(items: List<ItemCarrito>): Boolean {
        val repo = roomRepository ?: return descontarStockPorCompra(items)
        limpiarError()
        return try {
            val actualizado = repo.descontarStockPorCompra(items)
            if (actualizado) {
                _productos.value = repo.cargarProductos()
            } else {
                _error.value = "El stock o el precio cambió. Revisa tu carrito."
            }
            actualizado
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = "No se pudo guardar el stock"
            false
        }
    }

}
