package com.example.oyl_fs.repository

import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.network.CambiarEstadoRequest
import com.example.oyl_fs.network.CrearPedidoRequest
import com.example.oyl_fs.network.ItemPedidoRequest
import com.example.oyl_fs.network.PedidoDto
import com.example.oyl_fs.network.RetrofitClient
import com.example.oyl_fs.network.mensajeApi
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// El resultado de intentar crear un pedido: salió bien, o salió mal con un mensaje
sealed class ResultadoPedido {
    data class Exito(val pedido: PedidoDto) : ResultadoPedido()
    data class Error(val mensaje: String) : ResultadoPedido()
}

// Conecta la app con ms-pedidos
object PedidoRemoteRepository {

    // ---------- Crear un pedido desde el carrito ----------
    suspend fun crearPedido(usuarioId: Long, items: List<ItemCarrito>): ResultadoPedido {
        // El backend usa ids numéricos; el carrito guarda el id como texto
        val itemsRequest = items.map {
            val id = it.productoId.toLongOrNull()
                ?: return ResultadoPedido.Error("El producto \"${it.nombre}\" no tiene un id válido")
            ItemPedidoRequest(productoId = id, cantidad = it.cantidad)
        }

        return try {
            val pedido = RetrofitClient.pedidos.crear(CrearPedidoRequest(usuarioId, itemsRequest))
            ResultadoPedido.Exito(pedido)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin stock, producto inexistente, servidor apagado, etc.
            ResultadoPedido.Error(e.mensajeApi())
        }
    }

    // ---------- Consultar pedidos ----------
    // Todos los pedidos (administrador)
    suspend fun listarTodos(): Result<List<Pedido>> = try {
        Result.success(RetrofitClient.pedidos.listar().map { it.aPedido() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Solo los pedidos de un cliente
    suspend fun listarPorUsuario(usuarioId: Long): Result<List<Pedido>> = try {
        Result.success(RetrofitClient.pedidos.listarPorUsuario(usuarioId).map { it.aPedido() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------- Cambiar el estado ----------
    suspend fun cambiarEstado(pedidoId: String, nuevoEstado: EstadoPedido): Result<Pedido> {
        val id = idNumerico(pedidoId)
            ?: return Result.failure(IllegalArgumentException("Pedido no válido"))
        return try {
            val actualizado = RetrofitClient.pedidos.cambiarEstado(
                id, CambiarEstadoRequest(nuevoEstado.name)
            )
            Result.success(actualizado.aPedido())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Conversión backend -> modelo de la app ----------
    // El backend usa id numérico (7); la app lo muestra como "PED-007"
    private fun textoId(id: Long) = "PED-" + id.toString().padStart(3, '0')

    private fun idNumerico(texto: String): Long? = texto.removePrefix("PED-").toLongOrNull()

    private fun PedidoDto.aPedido() = Pedido(
        id = textoId(id),
        usuarioId = usuarioId.toString(),
        fecha = parsearFecha(fecha),
        productos = detalles.map {
            DetallePedido(
                productoId = it.productoId.toString(),
                nombreProducto = it.nombreProducto,
                cantidad = it.cantidad,
                precioUnitario = it.precioUnitario
            )
        },
        total = total.toInt(),
        estado = EstadoPedido.values().firstOrNull { it.name == estado }
            ?: EstadoPedido.PENDIENTE_VALIDACION
    )

    // El servidor manda la fecha como "2026-11-21T20:51:07.934514" (hora UTC)
    private fun parsearFecha(texto: String): Long = try {
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        formato.timeZone = TimeZone.getTimeZone("UTC")
        formato.parse(texto.take(19))?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}
