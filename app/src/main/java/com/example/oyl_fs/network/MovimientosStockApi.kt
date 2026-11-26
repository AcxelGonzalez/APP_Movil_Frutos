package com.example.oyl_fs.network

import retrofit2.http.*

data class MovimientoStockRequest(val productoId: Long, val cambio: Int)
data class MovimientoStockDto(
    val actorId: Long, val clave: String, val productoId: Long?, val cambio: Int?,
    val estado: String, val stockAnterior: Int?, val stockResultante: Int?, val mensaje: String?
)
interface MovimientosStockApi {
    @GET("api/productos/movimientos-stock/{clave}")
    suspend fun consultar(@Path("clave") clave: String): MovimientoStockDto
    @POST("api/productos/movimientos-stock/{clave}")
    suspend fun continuar(@Path("clave") clave: String, @Body solicitud: MovimientoStockRequest): MovimientoStockDto
}
