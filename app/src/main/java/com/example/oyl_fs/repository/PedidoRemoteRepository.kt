package com.example.oyl_fs.repository

import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.network.CambiarEstadoRequest
import com.example.oyl_fs.network.CrearPedidoRequest
import com.example.oyl_fs.network.ItemPedidoRequest
import com.example.oyl_fs.network.PedidoDto
import com.example.oyl_fs.network.PedidosApi
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
class PedidoRemoteRepository(private val api: com.example.oyl_fs.network.PedidosApi = RetrofitClient.pedidos) {


    // ---------- Crear un pedido desde el carrito ----------
    suspend fun crearPedido(usuarioId: Long, items: List<ItemCarrito>): ResultadoPedido {
        // El backend usa ids numéricos; el carrito guarda el id como texto
        if (usuarioId <= 0 || items.isEmpty() || items.any { it.cantidad <= 0 }) return ResultadoPedido.Error("Carrito inválido")
        val itemsRequest = items.map {
            val id = it.productoId.toLongOrNull()
                ?: return ResultadoPedido.Error("El producto \"${it.nombre}\" no tiene un id válido")
            if (id <= 0) return ResultadoPedido.Error("Identificador de producto inválido")
            ItemPedidoRequest(productoId = id, cantidad = it.cantidad)
        }

        return try {
            val pedido = api.crear(CrearPedidoRequest(usuarioId, itemsRequest))
            require(pedido.usuarioId == usuarioId && pedido.id > 0) { "El servidor respondió con un pedido inesperado. Revisa Mis pedidos antes de volver a comprar." }
            ResultadoPedido.Exito(pedido)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin stock, producto inexistente, servidor apagado, etc.
            ResultadoPedido.Error(if (e is java.io.IOException || (e is retrofit2.HttpException && e.code() >= 500)) "No se pudo confirmar la respuesta del servidor. Revisa Mis pedidos antes de volver a comprar: el pedido podría haberse registrado." else e.mensajeApi())
        }
    }

    // ---------- Consultar pedidos ----------
    // Todos los pedidos (administrador)
    suspend fun listarTodos(): Result<List<Pedido>> = try {
        Result.success(api.listar().map { it.aPedido() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Solo los pedidos de un cliente
    suspend fun listarPorUsuario(usuarioId: Long): Result<List<Pedido>> = try {
        Result.success(api.listarPorUsuario(usuarioId).map { it.aPedido() })
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
            val actualizado = api.cambiarEstado(
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
    private fun textoId(id: Long): String {
        require(id > 0) { "Identificador de pedido inválido" }
        return "PED-" + id.toString().padStart(3, '0')
    }

    private fun idNumerico(texto: String): Long? = texto.removePrefix("PED-").toLongOrNull()?.takeIf { it > 0 }

    fun PedidoDto.aPedido() = Pedido(
        id = textoId(id),
        usuarioId = usuarioId.also { require(it > 0) { "Identificador de usuario inválido" } }.toString(),
        fecha = parsearFecha(fecha),
        productos = detalles.map {
            DetallePedido(
                productoId = it.productoId.toString(),
                nombreProducto = it.nombreProducto,
                cantidad = it.cantidad,
                precioUnitario = it.precioUnitario
            )
        },
        total = total.also { require(it in 0..Int.MAX_VALUE.toLong()) { "Total de pedido fuera de rango" } }.toInt(),
        estado = EstadoPedido.values().firstOrNull { it.name == estado }
            ?: error("Estado de pedido desconocido: $estado")
    )

    // LocalDateTime llega sin zona: la integración existente lo interpreta como UTC.
    private fun parsearFecha(texto: String): Long {
        require(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?").matches(texto)) { "Fecha de pedido inválida" }
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        formato.timeZone = TimeZone.getTimeZone("UTC")
        formato.isLenient = false
        return requireNotNull(formato.parse(texto.take(19))) { "Fecha de pedido inválida" }.time
    }
}