package com.example.oyl_fs.repository

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.aEntidad
import retrofit2.HttpException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.content.Context
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.ProductoDto
import com.example.oyl_fs.network.RetrofitClient

class ProductoRemoteRepository(context: Context, private val api: com.example.oyl_fs.network.ProductosApi = RetrofitClient.productos, private val database: OylDatabase = OylDatabase.obtener(context)) {

    companion object { private val cargaMutex = Mutex() }

    private val contexto = context.applicationContext

    // Solo lectura: nunca reemplaza la copia local si falla la petición remota.
    suspend fun cargarCache(): List<ProductoInfo> =
        ProductoRoomRepository(contexto, database.productoDao()).cargarProductos()

    suspend fun listar(): List<ProductoInfo> = cargaMutex.withLock {
        val lista = api.listar().map { it.aProductoInfo() }
        database.productoDao().reemplazarCache(lista.map { producto ->
            producto.aEntidad(if (producto.imagen == 0) null else contexto.resources.getResourceEntryName(producto.imagen))
        })
        lista
    }

    suspend fun crear(producto: ProductoInfo): ProductoInfo =
        api.crear(producto.aDto(conId = false)).aProductoInfo()

    suspend fun actualizar(producto: ProductoInfo): ProductoInfo =
        api.actualizar(producto.id.toLong(), producto.aDto(conId = true)).aProductoInfo()

    // true si se eliminó; false si el producto ya no existía
    suspend fun eliminar(id: Int): Boolean {
        val respuesta = api.eliminar(id.toLong())
        if (respuesta.code() == 404) return false
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
        return true
    }

    // Backend -> app. La imagen viene como "drawable:nombre" o como una URI
    private fun ProductoDto.aProductoInfo(): ProductoInfo {
        require(id != null && id in 1..Int.MAX_VALUE.toLong()) { "Identificador de producto fuera de rango" }
        val url = imagenUrl
        val esDrawable = url != null && url.startsWith("drawable:")
        val imagenId = if (esDrawable) {
            contexto.resources.getIdentifier(
                url!!.removePrefix("drawable:"), "drawable", contexto.packageName
            )
        } else 0

        return ProductoInfo(
            id = requireNotNull(id).toInt(),
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
