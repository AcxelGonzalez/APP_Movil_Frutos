package com.example.oyl_fs.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PedidosApi {

    @POST("api/pedidos")
    suspend fun crear(@Body pedido: CrearPedidoRequest): PedidoDto

    @GET("api/pedidos/usuario/{usuarioId}")
    suspend fun listarPorUsuario(@Path("usuarioId") usuarioId: Long): List<PedidoDto>

    @GET("api/pedidos/{id}")
    suspend fun obtener(@Path("id") id: Long): PedidoDto

    // Cancelar solo funciona si el pedido está en PENDIENTE_VALIDACION (devuelve el stock)
    @DELETE("api/pedidos/{id}")
    suspend fun cancelar(@Path("id") id: Long): Response<Unit>
}