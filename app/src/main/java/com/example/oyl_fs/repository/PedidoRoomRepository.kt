package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.aEntidad
import com.example.oyl_fs.data.local.aEntidadesDetalle
import com.example.oyl_fs.data.local.aModeloPedido

import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido

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
}
