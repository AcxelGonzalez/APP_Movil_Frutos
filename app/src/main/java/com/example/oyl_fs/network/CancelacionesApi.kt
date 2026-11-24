package com.example.oyl_fs.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class CancelacionDto(val pedidoId: Long, val usuarioId: Long, val estado: String, val mensaje: String?)

interface CancelacionesApi {
    @GET("api/pedidos/{id}/cancelacion")
    suspend fun consultar(@Path("id") pedidoId: Long): CancelacionDto
    @POST("api/pedidos/{id}/cancelacion")
    suspend fun continuar(@Path("id") pedidoId: Long): CancelacionDto
}
