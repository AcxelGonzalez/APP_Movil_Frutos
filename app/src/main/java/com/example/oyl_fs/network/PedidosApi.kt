package com.example.oyl_fs.network

import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface PedidosApi {

    @POST("api/pedidos")
    suspend fun crear(@Header("Idempotency-Key") clave: String, @Body pedido: CrearPedidoRequest): SolicitudCompraDto

    @GET("api/pedidos/solicitudes/{clave}")
    suspend fun consultarSolicitud(@Path("clave") clave: String): SolicitudCompraDto

    // Todos los pedidos (para el administrador)
    @GET("api/pedidos")
    suspend fun listar(): List<PedidoDto>

    // Solo los pedidos de un cliente
    @GET("api/pedidos/usuario/{usuarioId}")
    suspend fun listarPorUsuario(@Path("usuarioId") usuarioId: Long): List<PedidoDto>

    @GET("api/pedidos/{id}")
    suspend fun obtener(@Path("id") id: Long): PedidoDto

    // Avanza el pedido al estado indicado (el backend solo permite avanzar un paso)
    @PATCH("api/pedidos/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Body request: CambiarEstadoRequest
    ): PedidoDto

    // Cancelar solo funciona si el pedido está en PENDIENTE_VALIDACION (devuelve el stock)
    @DELETE("api/pedidos/{id}")
    suspend fun cancelar(@Path("id") id: Long): Response<Unit>
}
