package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.aEntidad
import com.example.oyl_fs.data.local.aEntidadesDetalle
import com.example.oyl_fs.data.local.aModeloPedido
import com.example.oyl_fs.data.local.DescuentoStock
import com.example.oyl_fs.data.local.StockNoDisponibleException

import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.ItemCarrito

import com.example.oyl_fs.repository.PedidoRepository

import java.util.UUID

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio de pedidos persistentes.
 *
 * Utiliza Room para almacenar pedidos,
 * detalles y cambios de estado.
 *
 * Todavía no administra descuentos de
 * inventario. Eso se integrará después.
 */
class PedidoRoomRepository(
    private val database: OylDatabase
) {

    constructor(context: Context) : this(
        OylDatabase.obtener(context.applicationContext)
    )

    private val dao = database.pedidoDao()

    /**
     * Observa todos los pedidos.
     */
    val pedidos: Flow<List<Pedido>> =
        dao.observarPedidos().map { lista ->

            lista.map { registro ->
                registro.aModeloPedido()
            }
        }

    /**
     * Observa los pedidos de un cliente.
     */
    fun observarPedidosUsuario(
        usuarioId: String
    ): Flow<List<Pedido>> {

        return dao.observarPedidosUsuario(
            usuarioId
        ).map { lista ->

            lista.map { registro ->
                registro.aModeloPedido()
            }
        }
    }

    /**
     * Obtiene un pedido por identificador.
     */
    suspend fun obtenerPedidoPorId(
        id: String
    ): Pedido? {

        return dao.obtenerPedidoPorId(id)
            ?.aModeloPedido()
    }

    /**
     * Guarda el pedido y sus detalles
     * dentro de una única transacción.
     *
     * Si falla alguna inserción, Room
     * revierte toda la operación.
     *
     * El identificador debe ser único.
     */
    suspend fun guardarPedido(
        pedido: Pedido
    ) {

        require(pedido.id.isNotBlank()) {
            "El pedido debe tener identificador"
        }

        require(pedido.usuarioId.isNotBlank()) {
            "El pedido debe tener cliente"
        }

        require(pedido.productos.isNotEmpty()) {
            "El pedido debe contener productos"
        }

        // Validar el total antes de escribir.
        var totalCalculado = 0L

        for (detalle in pedido.productos) {

            require(
                detalle.productoId.isNotBlank() &&
                        detalle.nombreProducto.isNotBlank() &&
                        detalle.cantidad > 0 &&
                        detalle.precioUnitario > 0
            ) {
                "Detalle de pedido invalido"
            }

            val subtotal =
                detalle.cantidad.toLong() *
                        detalle.precioUnitario.toLong()

            require(
                subtotal <=
                        Int.MAX_VALUE.toLong() -
                        totalCalculado
            ) {
                "El total excede el limite permitido"
            }

            totalCalculado += subtotal
        }

        require(
            totalCalculado == pedido.total.toLong()
        ) {
            "El total no coincide con los detalles"
        }

        // Garantiza que pedido y detalles
        // se confirmen conjuntamente.
        database.withTransaction {

            dao.insertarPedido(
                pedido.aEntidad()
            )

            dao.insertarDetalles(
                pedido.aEntidadesDetalle()
            )
        }
    }

    /**
     * Registra una compra completa en SQLite.
     *
     * Una misma transacción:
     * 1. Guarda el pedido.
     * 2. Guarda sus detalles.
     * 3. Descuenta el stock.
     *
     * Si algún producto no tiene stock suficiente,
     * Room revierte todas las operaciones.
     *
     * Devuelve false cuando el inventario no permite
     * completar la compra.
     *
     * Otros errores se propagan al llamador para
     * evitar ocultar fallos de SQLite.
     */
    suspend fun registrarCompraConStock(
        pedido: Pedido
    ): Boolean {

        require(pedido.id.isNotBlank()) {
            "El pedido necesita un identificador"
        }

        require(pedido.usuarioId.isNotBlank()) {
            "El pedido necesita un cliente"
        }

        require(pedido.productos.isNotEmpty()) {
            "El pedido necesita productos"
        }

        val descuentos = linkedMapOf<Int, DescuentoStock>()

        var totalCalculado = 0L

        for (detalle in pedido.productos) {

            val productoId = detalle.productoId.toIntOrNull()
                ?: throw IllegalArgumentException(
                    "Identificador de producto invalido"
                )

            require(
                productoId > 0 &&
                        detalle.nombreProducto.isNotBlank() &&
                        detalle.cantidad > 0 &&
                        detalle.precioUnitario > 0
            ) {
                "Detalle de pedido invalido"
            }

            val subtotal =
                detalle.cantidad.toLong() *
                        detalle.precioUnitario.toLong()

            require(
                subtotal <=
                        Int.MAX_VALUE.toLong() - totalCalculado
            ) {
                "El total excede el limite permitido"
            }

            totalCalculado += subtotal

            // Agrupar cantidades cuando el mismo
            // producto aparece varias veces.
            val anterior = descuentos[productoId]

            require(
                anterior == null ||
                        anterior.precioUnitario ==
                        detalle.precioUnitario
            ) {
                "Precios inconsistentes para el producto"
            }

            val cantidadTotal =
                (anterior?.cantidad?.toLong() ?: 0L) +
                        detalle.cantidad.toLong()

            require(cantidadTotal <= Int.MAX_VALUE) {
                "Cantidad de producto demasiado grande"
            }

            descuentos[productoId] = DescuentoStock(
                productoId = productoId,
                cantidad = cantidadTotal.toInt(),
                precioUnitario = detalle.precioUnitario
            )
        }

        require(totalCalculado == pedido.total.toLong()) {
            "El total no coincide con los detalles"
        }

        return try {

            database.withTransaction {

                // 1. Insertar el pedido.
                dao.insertarPedido(
                    pedido.aEntidad()
                )

                // 2. Insertar los detalles.
                dao.insertarDetalles(
                    pedido.aEntidadesDetalle()
                )

                // 3. Descontar el inventario.
                // Este DAO pertenece a la misma
                // instancia de OylDatabase.
                database.productoDao()
                    .descontarStockLote(
                        descuentos.values.toList()
                    )
            }

            true

        } catch (e: StockNoDisponibleException) {

            // Room revierte el pedido, sus detalles
            // y cualquier descuento previo.
            false
        }
    }

    /**
     * Registra una compra directamente desde el carrito.
     *
     * 1. Valida los artículos recibidos.
     * 2. Calcula el total de forma segura.
     * 3. Genera un identificador nuevo.
     * 4. Guarda el pedido y descuenta stock
     *    mediante una única transacción Room.
     *
     * Devuelve el pedido si fue confirmado.
     * Devuelve null cuando los datos son inválidos
     * o no existe stock suficiente.
     *
     * Los errores inesperados de SQLite se propagan.
     */
    suspend fun registrarCompraDesdeCarrito(
        usuarioId: String,
        items: List<ItemCarrito>
    ): Pedido? {

        if (usuarioId.isBlank() || items.isEmpty()) {
            return null
        }

        val detalles = mutableListOf<DetallePedido>()

        var totalSeguro = 0L

        for (item in items) {

            val productoId = item.productoId.toIntOrNull()
                ?: return null

            if (
                productoId <= 0 ||
                item.nombre.isBlank() ||
                item.precio <= 0 ||
                item.cantidad <= 0
            ) {
                return null
            }

            val subtotal =
                item.precio.toLong() *
                        item.cantidad.toLong()

            if (
                subtotal >
                Int.MAX_VALUE.toLong() - totalSeguro
            ) {
                return null
            }

            totalSeguro += subtotal

            detalles.add(
                DetallePedido(
                    productoId = productoId.toString(),
                    nombreProducto = item.nombre,
                    cantidad = item.cantidad,
                    precioUnitario = item.precio
                )
            )
        }

        // UUID evita depender de contadores en memoria.
        // La clave primaria de SQLite también protege
        // frente a un eventual identificador duplicado.
        val nuevoId = "PED-${UUID.randomUUID()}"

        val pedido = Pedido(
            id = nuevoId,
            usuarioId = usuarioId.trim(),
            fecha = System.currentTimeMillis(),
            productos = detalles,
            total = totalSeguro.toInt(),
            estado = EstadoPedido.PENDIENTE_VALIDACION
        )

        // Utiliza la transacción ya probada.
        // No vuelve a descontar el stock por separado.
        val confirmado = registrarCompraConStock(pedido)

        return if (confirmado) pedido else null
    }

    /**
     * Actualiza el estado de un pedido
     * previamente almacenado.
     */
    suspend fun actualizarEstadoPedido(
        pedidoId: String,
        nuevoEstado: EstadoPedido
    ): Boolean {

        return dao.actualizarEstado(
            pedidoId,
            nuevoEstado.name
        ) == 1
    }

    /**
     * Cuenta los pedidos persistentes.
     */
    suspend fun contarPedidos(): Int {
        return dao.contarPedidos()
    }

    /**
     * Inicializa los pedidos de demostración una
     * sola vez para cada base de datos.
     *
     * Estos pedidos son históricos:
     * NO deben descontar nuevamente el stock.
     *
     * Utiliza SharedPreferences únicamente para
     * recordar que la carga inicial fue realizada.
     * Los pedidos se almacenan realmente en Room.
     */
    suspend fun inicializarPedidosDemoUnaVez(
        context: Context
    ) {

        val nombreBase = database.openHelper.databaseName
            ?: error("La base de datos necesita un nombre")

        val preferencias = context.applicationContext
            .getSharedPreferences(
                "oyl_demo_$nombreBase",
                Context.MODE_PRIVATE
            )

        val claveInicializacion = "pedidos_cargados"

        // Si ya se inicializó, no volver a agregar
        // los pedidos aunque la aplicación reinicie.
        if (
            preferencias.getBoolean(
                claveInicializacion,
                false
            )
        ) {
            return
        }

        // Recuperar los cuatro pedidos que ya utiliza
        // la aplicación para la demostración.
        val pedidosDemo = PedidoRepository().pedidos.value

        database.withTransaction {

            for (pedido in pedidosDemo) {

                // Evita reemplazar pedidos que ya existan.
                val existente = dao.obtenerPedidoPorId(
                    pedido.id
                )

                if (existente == null) {

                    dao.insertarPedido(
                        pedido.aEntidad()
                    )

                    dao.insertarDetalles(
                        pedido.aEntidadesDetalle()
                    )
                }
            }
        }

        // Se marca como inicializado solo después
        // de guardar correctamente los registros.
        check(
            preferencias.edit()
                .putBoolean(claveInicializacion, true)
                .commit()
        ) {
            "No se pudo registrar la inicializacion"
        }
    }

}
