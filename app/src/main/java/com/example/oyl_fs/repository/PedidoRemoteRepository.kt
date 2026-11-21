package com.example.oyl_fs.repository

import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.network.CrearPedidoRequest
import com.example.oyl_fs.network.ItemPedidoRequest
import com.example.oyl_fs.network.PedidoDto
import com.example.oyl_fs.network.RetrofitClient
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

// El resultado de intentar crear un pedido: salió bien, o salió mal con un mensaje
sealed class ResultadoPedido {
    data class Exito(val pedido: PedidoDto) : ResultadoPedido()
    data class Error(val mensaje: String) : ResultadoPedido()
}

object PedidoRemoteRepository {

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
        } catch (e: HttpException) {
            // El servidor respondió con error (409 sin stock, 404 producto no existe, etc.)
            ResultadoPedido.Error(mensajeDelServidor(e))
        } catch (e: IOException) {
            // Ni siquiera llegó al servidor: sin internet o backend apagado
            ResultadoPedido.Error("No se pudo conectar con el servidor")
        }
    }

    // El backend manda el motivo en el campo "message" del JSON de error
    private fun mensajeDelServidor(e: HttpException): String {
        return try {
            val cuerpo = e.response()?.errorBody()?.string().orEmpty()
            JSONObject(cuerpo).optString("message", "Error ${e.code()}")
        } catch (ex: Exception) {
            "Error ${e.code()}"
        }
    }
}