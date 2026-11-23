package com.example.oyl_fs.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

// Cada función = una petición que antes hacías en Postman
interface ProductosApi {

    @GET("api/productos")
    suspend fun listar(): List<ProductoDto>

    @GET("api/productos/{id}")
    suspend fun obtener(@Path("id") id: Long): ProductoDto

    @POST("api/productos")
    suspend fun crear(@Body producto: ProductoDto): ProductoDto

    @PUT("api/productos/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body producto: ProductoDto): ProductoDto

    @PATCH("api/productos/{id}/stock")
    suspend fun ajustarStock(@Path("id") id: Long, @Body ajuste: AjusteStockRequest): ProductoDto

    @DELETE("api/productos/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}
data class AjusteStockRequest(val cambio: Int)
