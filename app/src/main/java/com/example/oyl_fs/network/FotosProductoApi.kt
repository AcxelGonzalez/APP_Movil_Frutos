package com.example.oyl_fs.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface FotosProductoApi {
    @Multipart
    @POST("api/productos/con-foto")
    suspend fun crear(@Part("producto") producto: RequestBody, @Part foto: MultipartBody.Part): ProductoDto

    @Multipart
    @PUT("api/productos/{id}/con-foto")
    suspend fun actualizar(@Path("id") id: Long, @Part("producto") producto: RequestBody, @Part foto: MultipartBody.Part): ProductoDto
}
