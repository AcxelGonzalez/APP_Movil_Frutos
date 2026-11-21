package com.example.oyl_fs.repository

import android.content.Context
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.ProductoDto
import com.example.oyl_fs.network.RetrofitClient

class ProductoRemoteRepository(context: Context) {

    private val contexto = context.applicationContext
    private val api = RetrofitClient.productos

    suspend fun listar(): List<ProductoInfo> =
        api.listar().map { it.aProductoInfo() }

    suspend fun crear(producto: ProductoInfo): ProductoInfo =
        api.crear(producto.aDto(conId = false)).aProductoInfo()

    suspend fun actualizar(producto: ProductoInfo): ProductoInfo =
        api.actualizar(producto.id.toLong(), producto.aDto(conId = true)).aProductoInfo()

    // true si se eliminó; false si el producto ya no existía
    suspend fun eliminar(id: Int): Boolean =
        api.eliminar(id.toLong()).isSuccessful

    // Backend -> app. La imagen viene como "drawable:nombre" o como una URI
    private fun ProductoDto.aProductoInfo(): ProductoInfo {
        val url = imagenUrl
        val esDrawable = url != null && url.startsWith("drawable:")
        val imagenId = if (esDrawable) {
            contexto.resources.getIdentifier(
                url!!.removePrefix("drawable:"), "drawable", contexto.packageName
            )
        } else 0

        return ProductoInfo(
            id = (id ?: 0L).toInt(),
            nombre = nombre,
            codigo = codigo,
            categoria = categoria,
            descripcion = descripcion.orEmpty(),
            presentacion = presentacion,
            precio = precio,
            stock = stock,
            stockMinimo = stockMinimo,
            disponible = disponible,
            imagen = imagenId,
            imagenUri = if (url != null && !esDrawable) url else null
        )
    }

    // App -> backend
    private fun ProductoInfo.aDto(conId: Boolean): ProductoDto {
        val url = imagenUri ?: if (imagen != 0) {
            runCatching { "drawable:" + contexto.resources.getResourceEntryName(imagen) }
                .getOrNull()
        } else null

        return ProductoDto(
            id = if (conId) id.toLong() else null,
            nombre = nombre,
            codigo = codigo,
            categoria = categoria,
            descripcion = descripcion,
            presentacion = presentacion,
            precio = precio,
            stock = stock,
            stockMinimo = stockMinimo,
            disponible = disponible,
            imagenUrl = url
        )
    }
}