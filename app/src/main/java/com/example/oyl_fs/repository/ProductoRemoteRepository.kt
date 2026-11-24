package com.example.oyl_fs.repository

import androidx.room.withTransaction
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.CancellationException
import com.example.oyl_fs.network.AjusteStockRequest
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.aEntidad
import retrofit2.HttpException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.content.Context
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.ProductoDto
import com.example.oyl_fs.network.ProductosApi
import com.example.oyl_fs.network.RetrofitClient

class ProductoRemoteRepository(
    context: Context,
    private val api: ProductosApi = RetrofitClient.productos,
    private val database: OylDatabase = OylDatabase.obtener(context),
    private val fotosApi: com.example.oyl_fs.network.FotosProductoApi = RetrofitClient.fotosProductos,
    private val prepararFoto: suspend (String) -> ByteArray = { com.example.oyl_fs.util.CargadorImagenProducto.prepararSubida(context, it) }
) {

    companion object { private val cargaMutex = Mutex() }

    private val contexto = context.applicationContext
    // Protegido por cargaMutex. Evita recuperar una ficha retirada si falla SQLite.
    private val eliminadosConfirmados = mutableSetOf<Int>()

    // Solo lectura: nunca reemplaza la copia local si falla la petición remota.
    suspend fun cargarCache(): List<ProductoInfo> = cargaMutex.withLock {
        ProductoRoomRepository(contexto, database.productoDao()).cargarProductos()
            .filterNot { it.id in eliminadosConfirmados }
    }

    suspend fun listar(): List<ProductoInfo> = cargaMutex.withLock {
        val lista = api.listar().map { it.aProductoInfo() }
            .filterNot { it.id in eliminadosConfirmados }
        database.productoDao().reemplazarCache(lista.map { producto ->
            producto.aEntidad(if (producto.imagen == 0) null else contexto.resources.getResourceEntryName(producto.imagen))
        })
        lista
    }

    data class MovimientoRegistrado(val producto: ProductoInfo, val cacheActualizada: Boolean)

    suspend fun ajustarStock(id: Int, cambio: Int): MovimientoRegistrado = cargaMutex.withLock {
        require(id > 0 && cambio != 0) { "Movimiento de stock inválido" }
        val producto = api.ajustarStock(id.toLong(), AjusteStockRequest(cambio)).aProductoInfo()
        check(producto.id == id && producto.stock >= 0) { "Respuesta de stock inválida" }
        // Un fallo local posterior al éxito HTTP no debe invitar a repetir el movimiento.
        val cacheActualizada = try {
            val entidad = producto.aEntidad(if (producto.imagen == 0) null else contexto.resources.getResourceEntryName(producto.imagen))
            database.withTransaction {
                if (database.productoDao().actualizar(entidad) == 0) database.productoDao().insertar(entidad)
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        MovimientoRegistrado(producto, cacheActualizada)
    }

    suspend fun crear(producto: ProductoInfo): ProductoInfo = guardar(producto, creando = true)

    suspend fun actualizar(producto: ProductoInfo): ProductoInfo {
        require(producto.id > 0) { "Identificador de producto inválido" }
        return guardar(producto, creando = false)
    }

    private suspend fun guardar(producto: ProductoInfo, creando: Boolean): ProductoInfo {
        val local = producto.imagenUri?.startsWith("content://") == true
        // Preparar antes del HTTP: un archivo inválido nunca crea ni modifica el producto.
        val foto = if (local) prepararFoto(requireNotNull(producto.imagenUri)) else null
        require(foto == null || (foto.isNotEmpty() && foto.size <= 2 * 1024 * 1024)) { "La fotografía procesada supera los 2 MB" }
        var dto = producto.aDto(conId = !creando)
        val parteFoto = foto?.let {
            dto = dto.copy(imagenUrl = null)
            okhttp3.MultipartBody.Part.createFormData("foto", "producto.jpg", it.toRequestBody("image/jpeg".toMediaType()))
        }
        val ficha = if (parteFoto != null) com.google.gson.Gson().toJson(dto).toRequestBody("application/json; charset=utf-8".toMediaType()) else null
        return try {
            val guardado = if (parteFoto != null) {
                if (creando) fotosApi.crear(requireNotNull(ficha), parteFoto)
                else fotosApi.actualizar(producto.id.toLong(), requireNotNull(ficha), parteFoto)
            } else {
                if (creando) api.crear(dto) else api.actualizar(producto.id.toLong(), dto)
            }
            require(creando || guardado.id == producto.id.toLong()) { "El servidor respondió con otro producto" }
            if (parteFoto != null) check(com.example.oyl_fs.network.RutasFotoProducto.correspondeAlProducto(guardado.imagenUrl, requireNotNull(guardado.id))) {
                "La fotografía no se pudo confirmar"
            }
            guardado.aProductoInfo()
        } catch (e: CancellationException) { throw e
        } catch (e: com.example.oyl_fs.network.SesionExpiradaException) { throw e
        } catch (e: HttpException) {
            if (e.code() in setOf(400, 401, 403, 404, 409, 413, 415, 422)) throw e
            throw guardadoIncierto()
        } catch (_: Exception) { throw guardadoIncierto() }
    }

    private fun guardadoIncierto() = IllegalStateException(
        "No se pudo confirmar el guardado del producto. Revisa el catálogo antes de volver a guardar: el servidor podría haber registrado los cambios.")

    data class EliminacionRegistrada(val existia: Boolean, val cacheActualizada: Boolean)

    // Conserva el contrato de los consumidores que distinguen un 404.
    suspend fun eliminar(id: Int): Boolean = eliminarRegistrado(id).existia

    suspend fun eliminarRegistrado(id: Int): EliminacionRegistrada = cargaMutex.withLock {
        require(id > 0) { "Identificador de producto inválido" }
        val respuesta = api.eliminar(id.toLong())
        if (!respuesta.isSuccessful && respuesta.code() != 404) throw HttpException(respuesta)
        eliminadosConfirmados.add(id)
        // Un 404 también confirma que la ficha local debe retirarse.
        val cacheActualizada = try {
            database.productoDao().quitarDeCache(id)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        EliminacionRegistrada(respuesta.code() != 404, cacheActualizada)
    }

    // Backend -> app. Conserva la ruta relativa versionada, drawable o referencia anterior.
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
        val url = imagenUri?.let(com.example.oyl_fs.network.RutasFotoProducto::referencia) ?: if (imagen != 0) {
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
