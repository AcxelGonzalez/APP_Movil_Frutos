package com.example.oyl_fs.repository

import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio encargado de administrar los pedidos.
 *
 * Actualmente utiliza datos ficticios en memoria.
 * Posteriormente se conectará con Room y la API REST.
 */
class PedidoRepository {

    // Lista observable de pedidos.
    private val _pedidos = MutableStateFlow(crearPedidosDemo())

    // Permite consultar los pedidos sin modificarlos directamente.
    val pedidos: StateFlow<List<Pedido>> = _pedidos.asStateFlow()

    /**
     * Obtiene únicamente los pedidos de un cliente.
     * La lista se actualiza cuando cambian los datos.
     */
    fun observarPedidosUsuario(
        usuarioId: String
    ): Flow<List<Pedido>> {
        return pedidos.map { lista ->
            lista.filter { it.usuarioId == usuarioId }
                .sortedByDescending { it.fecha }
        }
    }

    /**
     * Busca un pedido mediante su identificador.
     */
    fun obtenerPedidoPorId(id: String): Pedido? {
        return pedidos.value.find { it.id == id }
    }

    /**
     * Actualiza el estado de un pedido.
     * Devuelve false si el pedido no existe.
     */
    fun actualizarEstadoPedido(
        pedidoId: String,
        nuevoEstado: EstadoPedido
    ): Boolean {

        if (obtenerPedidoPorId(pedidoId) == null) {
            return false
        }

        _pedidos.value = _pedidos.value.map { pedido ->
            if (pedido.id == pedidoId) {
                pedido.copy(estado = nuevoEstado)
            } else {
                pedido
            }
        }

        return true
    }

    /**
     * Genera pedidos ficticios para probar la interfaz.
     * No utiliza información real de clientes.
     */
    private fun crearPedidosDemo(): List<Pedido> {

        val ahora = System.currentTimeMillis()
        val dia = 86_400_000L

        return listOf(
            Pedido(
                id = "PED-001",
                usuarioId = "cliente-demo",
                fecha = ahora - dia,
                productos = listOf(
                    DetallePedido(
                        productoId = "PROD-001",
                        nombreProducto = "Almendras",
                        cantidad = 2,
                        precioUnitario = 4500
                    ),
                    DetallePedido(
                        productoId = "PROD-002",
                        nombreProducto = "Nueces",
                        cantidad = 1,
                        precioUnitario = 5200
                    )
                ),
                total = 14200,
                estado = EstadoPedido.CONFIRMADO
            ),

            Pedido(
                id = "PED-002",
                usuarioId = "cliente-demo",
                fecha = ahora - (3 * dia),
                productos = listOf(
                    DetallePedido(
                        productoId = "PROD-003",
                        nombreProducto = "Semillas de Chia",
                        cantidad = 3,
                        precioUnitario = 2300
                    )
                ),
                total = 6900,
                estado = EstadoPedido.ENTREGADO
            ),

            Pedido(
                id = "PED-003",
                usuarioId = "otro-cliente",
                fecha = ahora - (2 * dia),
                productos = listOf(
                    DetallePedido(
                        productoId = "PROD-004",
                        nombreProducto = "Castanas de Caju",
                        cantidad = 1,
                        precioUnitario = 6500
                    )
                ),
                total = 6500,
                estado = EstadoPedido.PENDIENTE_VALIDACION
            ),

            Pedido(
                id = "PED-004",
                usuarioId = "cliente-demo",
                fecha = ahora,
                productos = listOf(
                    DetallePedido(
                        productoId = "PROD-005",
                        nombreProducto = "Pasas",
                        cantidad = 2,
                        precioUnitario = 1900
                    )
                ),
                total = 3800,
                estado = EstadoPedido.PREPARADO
            )
        )
    }
}
